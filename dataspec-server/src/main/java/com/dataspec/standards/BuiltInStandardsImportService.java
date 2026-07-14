package com.dataspec.standards;

import com.dataspec.common.exception.BizException;
import com.dataspec.common.service.ProjectFieldNameReservationGuard;
import com.dataspec.domain.entity.Domain;
import com.dataspec.domain.repository.DomainRepository;
import com.dataspec.field.entity.Field;
import com.dataspec.field.repository.FieldRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 内置 standards 初始化服务。
 *
 * <p>运行时只读取 classpath 资源，保证打包后的 jar 不依赖仓库根目录。
 * 导入按项目内 code/name 去重，方便未来复用到“初始化标准”按钮。</p>
 */
@Service
public class BuiltInStandardsImportService {

    private static final String DOMAINS_RESOURCE = "standards/domains/standard-domains.yaml";
    private static final String FIELDS_RESOURCE = "standards/fields/standard-fields.yaml";
    private static final String DEFAULT_STATUS = "enabled";

    private final ObjectMapper yamlMapper;
    private final DomainRepository domainRepository;
    private final FieldRepository fieldRepository;
    private final ProjectFieldNameReservationGuard fieldNameReservationGuard;

    @Autowired
    public BuiltInStandardsImportService(DomainRepository domainRepository,
                                         FieldRepository fieldRepository,
                                         ProjectFieldNameReservationGuard fieldNameReservationGuard) {
        this(new ObjectMapper(new YAMLFactory()), domainRepository, fieldRepository, fieldNameReservationGuard);
    }

    public BuiltInStandardsImportService(ObjectMapper yamlMapper,
                                         DomainRepository domainRepository,
                                         FieldRepository fieldRepository,
                                         ProjectFieldNameReservationGuard fieldNameReservationGuard) {
        this.yamlMapper = yamlMapper;
        this.domainRepository = domainRepository;
        this.fieldRepository = fieldRepository;
        this.fieldNameReservationGuard = fieldNameReservationGuard;
    }

    @Transactional
    public void importBuiltInStandards(Long projectId) {
        if (projectId == null) {
            throw new BizException("项目ID不能为空");
        }
        importDomains(projectId, loadYaml(DOMAINS_RESOURCE, DomainsDocument.class).domains());
        importFields(projectId, loadYaml(FIELDS_RESOURCE, FieldsDocument.class).fields());
    }

    private void importDomains(Long projectId, List<DomainSeed> seeds) {
        if (seeds == null) {
            return;
        }
        for (DomainSeed seed : seeds) {
            if (seed.code() == null || seed.code().isBlank()) {
                continue;
            }
            if (domainRepository.existsByCodeInProject(seed.code(), projectId)) {
                continue;
            }
            Domain domain = new Domain();
            domain.setProjectId(projectId);
            domain.setCode(seed.code());
            domain.setName(seed.name());
            domain.setDescription(seed.description());
            domainRepository.insert(domain);
        }
    }

    private void importFields(Long projectId, List<FieldSeed> seeds) {
        if (seeds == null) {
            return;
        }
        List<FieldSeed> missingSeeds = seeds.stream()
                .filter(seed -> seed.name() != null && !seed.name().isBlank())
                .filter(seed -> !fieldRepository.existsByNameInProject(seed.name(), projectId))
                .toList();
        fieldNameReservationGuard.reserveAll(
                projectId,
                missingSeeds.stream().map(FieldSeed::name).toList());
        Set<String> existingNames = new HashSet<>();
        fieldRepository.findByNamesInProject(
                        missingSeeds.stream().map(FieldSeed::name).toList(),
                        projectId)
                .forEach(field -> existingNames.add(field.getName()));
        for (FieldSeed seed : missingSeeds) {
            if (existingNames.contains(seed.name())) {
                continue;
            }
            Field field = new Field();
            field.setProjectId(projectId);
            field.setName(seed.name());
            field.setDisplayName(seed.displayName());
            field.setDataType(seed.dataType());
            field.setLength(seed.length());
            field.setPrecisionVal(seed.precisionVal());
            field.setScaleVal(seed.scaleVal());
            field.setNullable(seed.nullable() != null ? seed.nullable() : true);
            field.setDefaultValue(seed.defaultValue());
            field.setComment(seed.comment());
            field.setTags(seed.tags());
            field.setAliases(seed.aliases());
            field.setCategory(seed.category());
            field.setCodeSetId(seed.codeSetId());
            field.setSensitive(seed.sensitive() != null ? seed.sensitive() : false);
            field.setStatus(seed.status() != null && !seed.status().isBlank() ? seed.status() : DEFAULT_STATUS);
            field.setExampleValue(seed.exampleValue());
            fieldRepository.insert(field);
            existingNames.add(seed.name());
        }
    }

    private <T> T loadYaml(String resourcePath, Class<T> valueType) {
        ClassPathResource resource = new ClassPathResource(resourcePath);
        try (InputStream inputStream = resource.getInputStream()) {
            return yamlMapper.readValue(inputStream, valueType);
        } catch (IOException e) {
            throw new IllegalStateException("加载内置 standards 失败: " + resourcePath, e);
        }
    }

    private record DomainsDocument(List<DomainSeed> domains) {
    }

    private record DomainSeed(String code, String name, String description) {
    }

    private record FieldsDocument(List<FieldSeed> fields) {
    }

    private record FieldSeed(String name,
                             String displayName,
                             String dataType,
                             Integer length,
                             Integer precisionVal,
                             Integer scaleVal,
                             Boolean nullable,
                             String defaultValue,
                             String comment,
                             String tags,
                             String aliases,
                             String category,
                             Long codeSetId,
                             Boolean sensitive,
                             String status,
                             String exampleValue) {
    }
}
