package com.dataspec.importexport.service;

import com.dataspec.common.exception.BizException;
import com.dataspec.domain.entity.Domain;
import com.dataspec.domain.service.DomainService;
import com.dataspec.enumdict.entity.EnumDict;
import com.dataspec.enumdict.entity.EnumValue;
import com.dataspec.enumdict.service.EnumDictService;
import com.dataspec.field.entity.Field;
import com.dataspec.field.service.FieldService;
import com.dataspec.importexport.model.ExcelImportPreview;
import com.dataspec.importexport.model.ExcelImportResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 导入导出服务，支持 JSON 兼容入口和 Excel 批量维护入口。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ImportExportService {

    private static final String SHEET_FIELDS = "fields";
    private static final String SHEET_ENUM_DICTS = "enum_dicts";
    private static final String SHEET_ENUM_VALUES = "enum_values";
    private static final String[] FIELD_HEADERS = {
            "name", "displayName", "dataType", "length", "precisionVal", "scaleVal",
            "nullable", "defaultValue", "comment", "domainCode", "tags", "aliases",
            "category", "codeSetCode", "sensitive", "status", "exampleValue"
    };
    private static final String[] ENUM_DICT_HEADERS = {"code", "name", "valueType", "description"};
    private static final String[] ENUM_VALUE_HEADERS = {"enumCode", "value", "label", "sortOrder"};
    private static final Set<String> ALLOWED_FIELD_STATUSES = Set.of("enabled", "disabled", "deprecated");

    private final FieldService fieldService;
    private final ObjectMapper objectMapper;
    private final EnumDictService enumDictService;
    private final DomainService domainService;

    /**
     * 导出项目字段为 JSON
     */
    public String exportFields(Long projectId) {
        try {
            List<Field> fields = fieldService.listByProject(projectId);
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(fields);
        } catch (Exception e) {
            throw new RuntimeException("导出失败", e);
        }
    }

    /**
     * 从 JSON 导入字段
     */
    @Transactional
    public int importFields(Long projectId, String json) {
        try {
            Field[] fields = objectMapper.readValue(json, Field[].class);
            int count = 0;
            for (Field field : fields) {
                field.setId(null);
                field.setProjectId(projectId);
                field.setCreatedAt(null);
                field.setUpdatedAt(null);
                field.setIsDeleted(null);
                try {
                    fieldService.create(field);
                    count++;
                } catch (Exception e) {
                    log.warn("导入字段 {} 失败: {}", field.getName(), e.getMessage());
                }
            }
            return count;
        } catch (Exception e) {
            throw new RuntimeException("导入失败: " + e.getMessage(), e);
        }
    }

    /**
     * 导出空 Excel 模板。模板 Sheet 与导入解析共享同一组表头，防止格式漂移。
     */
    public byte[] exportExcelTemplate() {
        try (Workbook workbook = new XSSFWorkbook()) {
            createSheetWithHeader(workbook, SHEET_FIELDS, FIELD_HEADERS);
            createSheetWithHeader(workbook, SHEET_ENUM_DICTS, ENUM_DICT_HEADERS);
            createSheetWithHeader(workbook, SHEET_ENUM_VALUES, ENUM_VALUE_HEADERS);
            return writeWorkbook(workbook);
        } catch (Exception e) {
            throw new RuntimeException("导出 Excel 模板失败", e);
        }
    }

    /**
     * 按模板格式导出项目字段和代码集，导出的文件可直接改完再导入。
     */
    public byte[] exportExcel(Long projectId) {
        try (Workbook workbook = new XSSFWorkbook()) {
            ProjectSnapshot snapshot = loadSnapshot(projectId);
            Sheet fieldsSheet = createSheetWithHeader(workbook, SHEET_FIELDS, FIELD_HEADERS);
            int rowIndex = 1;
            for (Field field : snapshot.fieldsByName().values()) {
                Row row = fieldsSheet.createRow(rowIndex++);
                writeRow(row,
                        field.getName(),
                        field.getDisplayName(),
                        field.getDataType(),
                        numberText(field.getLength()),
                        numberText(field.getPrecisionVal()),
                        numberText(field.getScaleVal()),
                        boolText(field.getNullable()),
                        field.getDefaultValue(),
                        field.getComment(),
                        domainCode(field.getDomainId(), snapshot),
                        field.getTags(),
                        field.getAliases(),
                        field.getCategory(),
                        enumCode(field.getCodeSetId(), snapshot),
                        boolText(field.getSensitive()),
                        field.getStatus(),
                        field.getExampleValue());
            }

            Sheet enumDictSheet = createSheetWithHeader(workbook, SHEET_ENUM_DICTS, ENUM_DICT_HEADERS);
            rowIndex = 1;
            for (EnumDict enumDict : snapshot.enumDictsByCode().values()) {
                Row row = enumDictSheet.createRow(rowIndex++);
                writeRow(row, enumDict.getCode(), enumDict.getName(), enumDict.getValueType(), enumDict.getDescription());
            }

            Sheet enumValueSheet = createSheetWithHeader(workbook, SHEET_ENUM_VALUES, ENUM_VALUE_HEADERS);
            rowIndex = 1;
            for (EnumDict enumDict : snapshot.enumDictsByCode().values()) {
                for (EnumValue value : snapshot.enumValuesByEnumCode().getOrDefault(enumDict.getCode(), Map.of()).values()) {
                    Row row = enumValueSheet.createRow(rowIndex++);
                    writeRow(row, enumDict.getCode(), value.getValue(), value.getLabel(), numberText(value.getSortOrder()));
                }
            }
            return writeWorkbook(workbook);
        } catch (Exception e) {
            throw new RuntimeException("导出 Excel 失败", e);
        }
    }

    /**
     * 预览 Excel 导入，不写数据库。
     */
    public ExcelImportPreview previewExcelImport(Long projectId, byte[] bytes) {
        ExcelImportPreview preview = new ExcelImportPreview();
        ExcelWorkbookRows rows = readWorkbook(bytes, preview);
        ProjectSnapshot snapshot = loadSnapshot(projectId);
        Map<String, RowValues> workbookEnumDicts = previewEnumDicts(rows, snapshot, preview);
        Set<String> availableEnumCodes = new LinkedHashSet<>(snapshot.enumDictsByCode().keySet());
        availableEnumCodes.addAll(workbookEnumDicts.keySet());
        previewEnumValues(rows, snapshot, availableEnumCodes, preview);
        previewFields(rows, snapshot, availableEnumCodes, preview);
        return preview;
    }

    /**
     * 确认导入 Excel。若预览存在错误则不写入，避免部分导入造成数据不一致。
     */
    @Transactional
    public ExcelImportResult importExcel(Long projectId, byte[] bytes) {
        ExcelImportPreview preview = previewExcelImport(projectId, bytes);
        ExcelImportResult result = new ExcelImportResult();
        if (!Boolean.TRUE.equals(preview.getValid())) {
            result.setErrors(preview.getErrors());
            return result;
        }

        ExcelWorkbookRows rows = readWorkbook(bytes, new ExcelImportPreview());
        ProjectSnapshot snapshot = loadSnapshot(projectId);
        Map<String, EnumDict> enumDictsByCode = new LinkedHashMap<>(snapshot.enumDictsByCode());
        Map<String, Map<String, EnumValue>> enumValuesByEnumCode = mutableEnumValues(snapshot);

        for (RowValues row : rows.enumDicts()) {
            EnumDict enumDict = toEnumDict(projectId, row);
            EnumDict existing = enumDictsByCode.get(enumDict.getCode());
            EnumDict saved;
            if (existing == null) {
                saved = enumDictService.create(enumDict);
            } else {
                enumDict.setId(existing.getId());
                saved = enumDictService.update(existing.getId(), enumDict);
                if (saved.getId() == null) {
                    saved.setId(existing.getId());
                }
            }
            enumDictsByCode.put(saved.getCode(), saved);
            enumValuesByEnumCode.computeIfAbsent(saved.getCode(), key -> new LinkedHashMap<>());
            result.setImportedEnumDicts(result.getImportedEnumDicts() + 1);
        }

        for (RowValues row : rows.enumValues()) {
            String enumCode = row.get("enumCode");
            EnumDict enumDict = enumDictsByCode.get(enumCode);
            EnumValue enumValue = toEnumValue(enumDict.getId(), row);
            Map<String, EnumValue> valuesByValue = enumValuesByEnumCode.computeIfAbsent(enumCode, key -> new LinkedHashMap<>());
            EnumValue existing = valuesByValue.get(enumValue.getValue());
            EnumValue saved;
            if (existing == null) {
                saved = enumDictService.createValue(enumValue);
            } else {
                enumValue.setId(existing.getId());
                saved = enumDictService.updateValue(existing.getId(), enumValue);
                if (saved.getId() == null) {
                    saved.setId(existing.getId());
                }
            }
            valuesByValue.put(saved.getValue(), saved);
            result.setImportedEnumValues(result.getImportedEnumValues() + 1);
        }

        for (RowValues row : rows.fields()) {
            Field field = toField(projectId, row, snapshot.domainsByCode(), enumDictsByCode);
            Field existing = snapshot.fieldsByName().get(field.getName());
            if (existing == null) {
                fieldService.create(field);
            } else {
                fieldService.update(existing.getId(), field);
            }
            result.setImportedFields(result.getImportedFields() + 1);
        }

        return result;
    }

    private ProjectSnapshot loadSnapshot(Long projectId) {
        List<Field> fields = fieldService.listByProject(projectId);
        List<Domain> domains = domainService.listByProject(projectId);
        List<EnumDict> enumDicts = enumDictService.listByProject(projectId);

        Map<String, Field> fieldsByName = fields.stream()
                .filter(field -> !isBlank(field.getName()))
                .collect(Collectors.toMap(Field::getName, Function.identity(), (first, ignored) -> first, LinkedHashMap::new));
        Map<String, Domain> domainsByCode = domains.stream()
                .filter(domain -> !isBlank(domain.getCode()))
                .collect(Collectors.toMap(Domain::getCode, Function.identity(), (first, ignored) -> first, LinkedHashMap::new));
        Map<Long, Domain> domainsById = domains.stream()
                .filter(domain -> domain.getId() != null)
                .collect(Collectors.toMap(Domain::getId, Function.identity(), (first, ignored) -> first, LinkedHashMap::new));
        Map<String, EnumDict> enumDictsByCode = enumDicts.stream()
                .filter(enumDict -> !isBlank(enumDict.getCode()))
                .collect(Collectors.toMap(EnumDict::getCode, Function.identity(), (first, ignored) -> first, LinkedHashMap::new));
        Map<Long, EnumDict> enumDictsById = enumDicts.stream()
                .filter(enumDict -> enumDict.getId() != null)
                .collect(Collectors.toMap(EnumDict::getId, Function.identity(), (first, ignored) -> first, LinkedHashMap::new));
        Map<String, Map<String, EnumValue>> enumValuesByEnumCode = new LinkedHashMap<>();
        for (EnumDict enumDict : enumDicts) {
            if (enumDict.getId() == null || isBlank(enumDict.getCode())) {
                continue;
            }
            Map<String, EnumValue> values = enumDictService.listValues(enumDict.getId()).stream()
                    .filter(value -> !isBlank(value.getValue()))
                    .collect(Collectors.toMap(EnumValue::getValue, Function.identity(), (first, ignored) -> first, LinkedHashMap::new));
            enumValuesByEnumCode.put(enumDict.getCode(), values);
        }
        return new ProjectSnapshot(fieldsByName, domainsByCode, domainsById, enumDictsByCode, enumDictsById, enumValuesByEnumCode);
    }

    private ExcelWorkbookRows readWorkbook(byte[] bytes, ExcelImportPreview preview) {
        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(bytes))) {
            return new ExcelWorkbookRows(
                    readSheet(workbook, SHEET_FIELDS, FIELD_HEADERS, preview),
                    readSheet(workbook, SHEET_ENUM_DICTS, ENUM_DICT_HEADERS, preview),
                    readSheet(workbook, SHEET_ENUM_VALUES, ENUM_VALUE_HEADERS, preview));
        } catch (Exception e) {
            throw new BizException("读取 Excel 失败: " + e.getMessage());
        }
    }

    private List<RowValues> readSheet(Workbook workbook, String sheetName, String[] headers, ExcelImportPreview preview) {
        Sheet sheet = workbook.getSheet(sheetName);
        if (sheet == null) {
            preview.addError(sheetName, null, "sheet", "缺少 Sheet: " + sheetName);
            return List.of();
        }
        Row headerRow = sheet.getRow(0);
        if (headerRow == null) {
            preview.addError(sheetName, 1, "header", "缺少表头行");
            return List.of();
        }
        Map<String, Integer> headerIndex = headerIndex(headerRow);
        boolean headerValid = true;
        for (String header : headers) {
            if (!headerIndex.containsKey(header)) {
                preview.addError(sheetName, 1, header, "缺少表头: " + header);
                headerValid = false;
            }
        }
        if (!headerValid) {
            return List.of();
        }

        List<RowValues> rows = new ArrayList<>();
        DataFormatter formatter = new DataFormatter(Locale.CHINA);
        for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (row == null || isBlankRow(row, headers, headerIndex, formatter)) {
                continue;
            }
            Map<String, String> values = new LinkedHashMap<>();
            for (String header : headers) {
                values.put(header, cellText(row, headerIndex.get(header), formatter));
            }
            rows.add(new RowValues(sheetName, rowIndex + 1, values));
        }
        return rows;
    }

    private Map<String, RowValues> previewEnumDicts(ExcelWorkbookRows rows,
                                                    ProjectSnapshot snapshot,
                                                    ExcelImportPreview preview) {
        Map<String, RowValues> workbookEnumDicts = new LinkedHashMap<>();
        Set<String> seenCodes = new LinkedHashSet<>();
        for (RowValues row : rows.enumDicts()) {
            preview.getEnumDicts().increaseTotal();
            boolean rowValid = require(row, "code", preview) & require(row, "name", preview);
            String code = row.get("code");
            if (!isBlank(code) && !seenCodes.add(code)) {
                preview.getEnumDicts().increaseConflictCount();
                preview.addError(row.sheet(), row.rowNumber(), "code", "Excel 内代码集编码重复: " + code);
                rowValid = false;
            }
            rowValid = validateValueType(row, preview) && rowValid;
            if (!rowValid) {
                continue;
            }
            workbookEnumDicts.put(code, row);
            if (snapshot.enumDictsByCode().containsKey(code)) {
                preview.getEnumDicts().increaseUpdateCount();
            } else {
                preview.getEnumDicts().increaseCreateCount();
            }
        }
        return workbookEnumDicts;
    }

    private void previewEnumValues(ExcelWorkbookRows rows,
                                   ProjectSnapshot snapshot,
                                   Set<String> availableEnumCodes,
                                   ExcelImportPreview preview) {
        Set<String> seenValues = new LinkedHashSet<>();
        for (RowValues row : rows.enumValues()) {
            preview.getEnumValues().increaseTotal();
            boolean rowValid = require(row, "enumCode", preview)
                    & require(row, "value", preview)
                    & require(row, "label", preview);
            String enumCode = row.get("enumCode");
            String value = row.get("value");
            if (!isBlank(enumCode) && !availableEnumCodes.contains(enumCode)) {
                preview.addError(row.sheet(), row.rowNumber(), "enumCode", "未知代码集编码: " + enumCode);
                rowValid = false;
            }
            String valueKey = enumCode + "\u0000" + value;
            if (!isBlank(enumCode) && !isBlank(value) && !seenValues.add(valueKey)) {
                preview.getEnumValues().increaseConflictCount();
                preview.addError(row.sheet(), row.rowNumber(), "value", "Excel 内枚举值重复: " + enumCode + "/" + value);
                rowValid = false;
            }
            rowValid = validateInteger(row, "sortOrder", preview) && rowValid;
            if (!rowValid) {
                continue;
            }
            if (snapshot.enumValuesByEnumCode().getOrDefault(enumCode, Map.of()).containsKey(value)) {
                preview.getEnumValues().increaseUpdateCount();
            } else {
                preview.getEnumValues().increaseCreateCount();
            }
        }
    }

    private void previewFields(ExcelWorkbookRows rows,
                               ProjectSnapshot snapshot,
                               Set<String> availableEnumCodes,
                               ExcelImportPreview preview) {
        Set<String> seenNames = new LinkedHashSet<>();
        for (RowValues row : rows.fields()) {
            preview.getFields().increaseTotal();
            boolean rowValid = require(row, "name", preview) & require(row, "dataType", preview);
            String name = row.get("name");
            if (!isBlank(name) && !seenNames.add(name)) {
                preview.getFields().increaseConflictCount();
                preview.addError(row.sheet(), row.rowNumber(), "name", "Excel 内字段名重复: " + name);
                rowValid = false;
            }
            String domainCode = row.get("domainCode");
            if (!isBlank(domainCode) && !snapshot.domainsByCode().containsKey(domainCode)) {
                preview.addError(row.sheet(), row.rowNumber(), "domainCode", "未知数据域编码: " + domainCode);
                rowValid = false;
            }
            String codeSetCode = row.get("codeSetCode");
            if (!isBlank(codeSetCode) && !availableEnumCodes.contains(codeSetCode)) {
                preview.addError(row.sheet(), row.rowNumber(), "codeSetCode", "未知代码集编码: " + codeSetCode);
                rowValid = false;
            }
            rowValid = validateInteger(row, "length", preview) && rowValid;
            rowValid = validateInteger(row, "precisionVal", preview) && rowValid;
            rowValid = validateInteger(row, "scaleVal", preview) && rowValid;
            rowValid = validateBoolean(row, "nullable", preview) && rowValid;
            rowValid = validateBoolean(row, "sensitive", preview) && rowValid;
            rowValid = validateFieldStatus(row, preview) && rowValid;
            if (!rowValid) {
                continue;
            }
            if (snapshot.fieldsByName().containsKey(name)) {
                preview.getFields().increaseUpdateCount();
            } else {
                preview.getFields().increaseCreateCount();
            }
        }
    }

    private boolean require(RowValues row, String field, ExcelImportPreview preview) {
        if (!isBlank(row.get(field))) {
            return true;
        }
        preview.addError(row.sheet(), row.rowNumber(), field, "必填字段为空");
        return false;
    }

    private boolean validateInteger(RowValues row, String field, ExcelImportPreview preview) {
        String value = row.get(field);
        if (isBlank(value)) {
            return true;
        }
        try {
            toInteger(value);
            return true;
        } catch (Exception e) {
            preview.addError(row.sheet(), row.rowNumber(), field, "必须是整数");
            return false;
        }
    }

    private boolean validateBoolean(RowValues row, String field, ExcelImportPreview preview) {
        String value = row.get(field);
        if (isBlank(value)) {
            return true;
        }
        if (parseBoolean(value) != null) {
            return true;
        }
        preview.addError(row.sheet(), row.rowNumber(), field, "必须是 是/否/true/false/1/0");
        return false;
    }

    private boolean validateFieldStatus(RowValues row, ExcelImportPreview preview) {
        String status = row.get("status");
        if (isBlank(status) || ALLOWED_FIELD_STATUSES.contains(status.toLowerCase(Locale.ROOT))) {
            return true;
        }
        preview.addError(row.sheet(), row.rowNumber(), "status", "字段状态必须是 enabled/disabled/deprecated");
        return false;
    }

    private boolean validateValueType(RowValues row, ExcelImportPreview preview) {
        String valueType = row.get("valueType");
        if (isBlank(valueType) || "integer".equalsIgnoreCase(valueType) || "string".equalsIgnoreCase(valueType)) {
            return true;
        }
        preview.addError(row.sheet(), row.rowNumber(), "valueType", "值类型建议使用 integer 或 string");
        return false;
    }

    private EnumDict toEnumDict(Long projectId, RowValues row) {
        EnumDict enumDict = new EnumDict();
        enumDict.setProjectId(projectId);
        enumDict.setCode(row.get("code"));
        enumDict.setName(row.get("name"));
        enumDict.setValueType(isBlank(row.get("valueType")) ? "integer" : row.get("valueType"));
        enumDict.setDescription(row.get("description"));
        return enumDict;
    }

    private EnumValue toEnumValue(Long enumId, RowValues row) {
        EnumValue enumValue = new EnumValue();
        enumValue.setEnumId(enumId);
        enumValue.setValue(row.get("value"));
        enumValue.setLabel(row.get("label"));
        enumValue.setSortOrder(toInteger(row.get("sortOrder")));
        return enumValue;
    }

    private Field toField(Long projectId,
                          RowValues row,
                          Map<String, Domain> domainsByCode,
                          Map<String, EnumDict> enumDictsByCode) {
        Field field = new Field();
        field.setProjectId(projectId);
        field.setName(row.get("name"));
        field.setDisplayName(row.get("displayName"));
        field.setDataType(row.get("dataType"));
        field.setLength(toInteger(row.get("length")));
        field.setPrecisionVal(toInteger(row.get("precisionVal")));
        field.setScaleVal(toInteger(row.get("scaleVal")));
        field.setNullable(parseBoolean(row.get("nullable")));
        field.setDefaultValue(row.get("defaultValue"));
        field.setComment(row.get("comment"));
        String domainCode = row.get("domainCode");
        field.setDomainId(isBlank(domainCode) ? null : domainsByCode.get(domainCode).getId());
        field.setTags(row.get("tags"));
        field.setAliases(row.get("aliases"));
        field.setCategory(row.get("category"));
        String codeSetCode = row.get("codeSetCode");
        field.setCodeSetId(isBlank(codeSetCode) ? null : enumDictsByCode.get(codeSetCode).getId());
        field.setSensitive(parseBoolean(row.get("sensitive")));
        field.setStatus(row.get("status"));
        field.setExampleValue(row.get("exampleValue"));
        return field;
    }

    private Sheet createSheetWithHeader(Workbook workbook, String sheetName, String[] headers) {
        Sheet sheet = workbook.createSheet(sheetName);
        writeRow(sheet.createRow(0), headers);
        return sheet;
    }

    private void writeRow(Row row, String... values) {
        for (int i = 0; i < values.length; i++) {
            row.createCell(i).setCellValue(values[i] == null ? "" : values[i]);
        }
    }

    private byte[] writeWorkbook(Workbook workbook) throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        workbook.write(output);
        return output.toByteArray();
    }

    private Map<String, Integer> headerIndex(Row row) {
        Map<String, Integer> result = new LinkedHashMap<>();
        row.forEach(cell -> {
            String value = cell.getStringCellValue();
            if (!isBlank(value)) {
                result.put(value.trim(), cell.getColumnIndex());
            }
        });
        return result;
    }

    private boolean isBlankRow(Row row, String[] headers, Map<String, Integer> headerIndex, DataFormatter formatter) {
        for (String header : headers) {
            if (!isBlank(cellText(row, headerIndex.get(header), formatter))) {
                return false;
            }
        }
        return true;
    }

    private String cellText(Row row, Integer cellIndex, DataFormatter formatter) {
        if (cellIndex == null) {
            return "";
        }
        Cell cell = row.getCell(cellIndex);
        if (cell == null) {
            return "";
        }
        return formatter.formatCellValue(cell).trim();
    }

    private Map<String, Map<String, EnumValue>> mutableEnumValues(ProjectSnapshot snapshot) {
        Map<String, Map<String, EnumValue>> result = new LinkedHashMap<>();
        for (Map.Entry<String, Map<String, EnumValue>> entry : snapshot.enumValuesByEnumCode().entrySet()) {
            result.put(entry.getKey(), new LinkedHashMap<>(entry.getValue()));
        }
        return result;
    }

    private String domainCode(Long domainId, ProjectSnapshot snapshot) {
        if (domainId == null || !snapshot.domainsById().containsKey(domainId)) {
            return "";
        }
        return snapshot.domainsById().get(domainId).getCode();
    }

    private String enumCode(Long enumId, ProjectSnapshot snapshot) {
        if (enumId == null || !snapshot.enumDictsById().containsKey(enumId)) {
            return "";
        }
        return snapshot.enumDictsById().get(enumId).getCode();
    }

    private String boolText(Boolean value) {
        if (value == null) {
            return "";
        }
        return Boolean.TRUE.equals(value) ? "是" : "否";
    }

    private String numberText(Integer value) {
        return value == null ? "" : value.toString();
    }

    private Integer toInteger(String value) {
        if (isBlank(value)) {
            return null;
        }
        return new BigDecimal(value.trim()).intValueExact();
    }

    private Boolean parseBoolean(String value) {
        if (isBlank(value)) {
            return null;
        }
        return switch (value.trim().toLowerCase(Locale.ROOT)) {
            case "是", "true", "1", "yes", "y" -> true;
            case "否", "false", "0", "no", "n" -> false;
            default -> null;
        };
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private record ExcelWorkbookRows(
            List<RowValues> fields,
            List<RowValues> enumDicts,
            List<RowValues> enumValues) {
    }

    private record RowValues(String sheet, int rowNumber, Map<String, String> values) {
        String get(String key) {
            return values.getOrDefault(key, "");
        }
    }

    private record ProjectSnapshot(
            Map<String, Field> fieldsByName,
            Map<String, Domain> domainsByCode,
            Map<Long, Domain> domainsById,
            Map<String, EnumDict> enumDictsByCode,
            Map<Long, EnumDict> enumDictsById,
            Map<String, Map<String, EnumValue>> enumValuesByEnumCode) {
    }
}
