package com.dataspec.importexport;

import com.dataspec.domain.entity.Domain;
import com.dataspec.domain.service.DomainService;
import com.dataspec.enumdict.entity.EnumDict;
import com.dataspec.enumdict.entity.EnumValue;
import com.dataspec.enumdict.service.EnumDictService;
import com.dataspec.field.entity.Field;
import com.dataspec.field.service.FieldService;
import com.dataspec.importexport.model.ExcelImportPreview;
import com.dataspec.importexport.model.ExcelImportResult;
import com.dataspec.importexport.service.ImportExportService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Excel 导入导出服务测试：锁定字段、代码集和枚举值的模板格式与 upsert 行为。
 */
@ExtendWith(MockitoExtension.class)
class ImportExportServiceTest {

    @Mock
    private FieldService fieldService;
    @Mock
    private EnumDictService enumDictService;
    @Mock
    private DomainService domainService;

    private ImportExportService service;

    @BeforeEach
    void setUp() {
        service = new ImportExportService(fieldService, new ObjectMapper(), enumDictService, domainService);
    }

    @Test
    void exportExcelTemplateContainsRequiredSheetsAndHeaders() throws Exception {
        byte[] bytes = service.exportExcelTemplate();

        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(bytes))) {
            assertThat(workbook.getSheet("fields")).isNotNull();
            assertThat(workbook.getSheet("enum_dicts")).isNotNull();
            assertThat(workbook.getSheet("enum_values")).isNotNull();
            assertThat(rowValues(workbook.getSheet("fields").getRow(0)))
                    .containsExactly("name", "displayName", "dataType", "length", "precisionVal", "scaleVal",
                            "nullable", "defaultValue", "comment", "domainCode", "tags", "aliases",
                            "category", "codeSetCode", "sensitive", "status", "exampleValue");
        }
    }

    @Test
    void previewExcelImportReportsCreateAndUpdateCounts() {
        mockExistingProjectData();

        ExcelImportPreview preview = service.previewExcelImport(1L, sampleWorkbook());

        assertThat(preview.getValid()).isTrue();
        assertThat(preview.getErrors()).isEmpty();
        assertThat(preview.getFields().getTotal()).isEqualTo(2);
        assertThat(preview.getFields().getCreateCount()).isEqualTo(1);
        assertThat(preview.getFields().getUpdateCount()).isEqualTo(1);
        assertThat(preview.getEnumDicts().getCreateCount()).isEqualTo(1);
        assertThat(preview.getEnumDicts().getUpdateCount()).isEqualTo(1);
        assertThat(preview.getEnumValues().getCreateCount()).isEqualTo(1);
        assertThat(preview.getEnumValues().getUpdateCount()).isEqualTo(1);
    }

    @Test
    void importExcelUpsertsEnumDataBeforeFieldsAndKeepsCodeSetAssociation() {
        mockExistingProjectData();
        when(enumDictService.create(any(EnumDict.class))).thenAnswer(invocation -> {
            EnumDict dict = invocation.getArgument(0);
            dict.setId(22L);
            return dict;
        });
        when(enumDictService.update(any(Long.class), any(EnumDict.class))).thenAnswer(invocation -> invocation.getArgument(1));
        when(enumDictService.createValue(any(EnumValue.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(enumDictService.updateValue(any(Long.class), any(EnumValue.class))).thenAnswer(invocation -> invocation.getArgument(1));
        when(fieldService.create(any(Field.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(fieldService.update(any(Long.class), any(Field.class))).thenAnswer(invocation -> invocation.getArgument(1));

        ExcelImportResult result = service.importExcel(1L, sampleWorkbook());

        assertThat(result.getImportedFields()).isEqualTo(2);
        assertThat(result.getImportedEnumDicts()).isEqualTo(2);
        assertThat(result.getImportedEnumValues()).isEqualTo(2);
        ArgumentCaptor<Field> createdField = ArgumentCaptor.forClass(Field.class);
        verify(fieldService).create(createdField.capture());
        assertThat(createdField.getValue().getName()).isEqualTo("user_email");
        assertThat(createdField.getValue().getDomainId()).isEqualTo(41L);
        assertThat(createdField.getValue().getCodeSetId()).isEqualTo(22L);
    }

    private void mockExistingProjectData() {
        Field mobileNo = new Field();
        mobileNo.setId(11L);
        mobileNo.setProjectId(1L);
        mobileNo.setName("mobile_no");
        mobileNo.setDataType("varchar(20)");

        EnumDict orderStatus = new EnumDict();
        orderStatus.setId(21L);
        orderStatus.setProjectId(1L);
        orderStatus.setCode("order_status");
        orderStatus.setName("订单状态");
        orderStatus.setValueType("integer");

        EnumValue paid = new EnumValue();
        paid.setId(31L);
        paid.setEnumId(21L);
        paid.setValue("1");
        paid.setLabel("已支付");

        Domain userDomain = new Domain();
        userDomain.setId(41L);
        userDomain.setProjectId(1L);
        userDomain.setCode("user");
        userDomain.setName("用户域");

        when(fieldService.listByProject(1L)).thenReturn(List.of(mobileNo));
        when(enumDictService.listByProject(1L)).thenReturn(List.of(orderStatus));
        when(enumDictService.listValues(21L)).thenReturn(List.of(paid));
        when(domainService.listByProject(1L)).thenReturn(List.of(userDomain));
    }

    private byte[] sampleWorkbook() {
        try (Workbook workbook = new XSSFWorkbook()) {
            addSheet(workbook, "enum_dicts",
                    new String[]{"code", "name", "valueType", "description"},
                    new String[]{"order_status", "订单状态", "integer", "订单生命周期"},
                    new String[]{"payment_type", "支付方式", "string", "支付渠道"});
            addSheet(workbook, "enum_values",
                    new String[]{"enumCode", "value", "label", "sortOrder"},
                    new String[]{"order_status", "1", "已支付", "10"},
                    new String[]{"payment_type", "wechat", "微信", "20"});
            addSheet(workbook, "fields",
                    new String[]{"name", "displayName", "dataType", "length", "precisionVal", "scaleVal",
                            "nullable", "defaultValue", "comment", "domainCode", "tags", "aliases",
                            "category", "codeSetCode", "sensitive", "status", "exampleValue"},
                    new String[]{"mobile_no", "手机号", "varchar(20)", "20", "", "", "否", "", "用户手机号",
                            "user", "用户", "phone,mobile", "contact", "order_status", "是", "enabled", "13800138000"},
                    new String[]{"user_email", "邮箱", "varchar(120)", "120", "", "", "是", "", "用户邮箱",
                            "user", "用户", "email", "contact", "payment_type", "否", "enabled", "a@example.com"});
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            workbook.write(output);
            return output.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private void addSheet(Workbook workbook, String name, String[] header, String[]... rows) {
        var sheet = workbook.createSheet(name);
        writeRow(sheet.createRow(0), header);
        for (int i = 0; i < rows.length; i++) {
            writeRow(sheet.createRow(i + 1), rows[i]);
        }
    }

    private void writeRow(Row row, String[] values) {
        for (int i = 0; i < values.length; i++) {
            row.createCell(i).setCellValue(values[i]);
        }
    }

    private List<String> rowValues(Row row) {
        List<String> values = new ArrayList<>();
        row.forEach(cell -> values.add(cell.getStringCellValue()));
        return values;
    }
}
