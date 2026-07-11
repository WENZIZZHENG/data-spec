package com.dataspec.tablemodel;

import com.dataspec.common.exception.BizException;
import com.dataspec.security.context.DataSpecSecurityContext;
import com.dataspec.tablemodel.entity.BusinessObjectStandard;
import com.dataspec.tablemodel.model.BusinessObjectStandardReq;
import com.dataspec.tablemodel.model.TableForeignKeyStandard;
import com.dataspec.tablemodel.model.TableRelationHint;
import com.dataspec.tablemodel.repository.BusinessObjectStandardRepository;
import com.dataspec.tablemodel.service.impl.BusinessObjectStandardServiceImpl;
import com.dataspec.tablemodel.service.impl.TableStandardsContextProviderImpl;
import com.dataspec.template.entity.Template;
import com.dataspec.template.service.TemplateService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 业务对象标准服务测试，覆盖项目归属、JSON 安全边界和只读关系摘要。
 */
class BusinessObjectStandardServiceImplTest {

    private static final Long PROJECT_ID = 1L;

    private final InMemoryBusinessObjectStandardRepository repository = new InMemoryBusinessObjectStandardRepository();
    private final RecordingTemplateService templateService = new RecordingTemplateService();
    private final BusinessObjectStandardServiceImpl service = new BusinessObjectStandardServiceImpl(
            repository,
            templateService,
            new ObjectMapper());

    @AfterEach
    void tearDown() {
        DataSpecSecurityContext.clear();
    }

    @Test
    void createAndListPersistStructuredBusinessObject() {
        templateService.templates.put(10L, template(10L, PROJECT_ID, "订单模板"));

        var response = service.create(req("order", "订单", 10L));

        assertEquals(PROJECT_ID, response.projectId());
        assertEquals("order", response.objectKey());
        assertEquals(List.of("id", "order_no"), response.requiredFields());
        assertEquals(1, response.relations().size());
        assertEquals("customer", response.relations().get(0).targetObjectKey());
        assertEquals("ENABLED", response.status());
        assertEquals(List.of("order"), service.listByProject(PROJECT_ID).stream()
                .map(item -> item.objectKey())
                .toList());
    }

    @Test
    void createRejectsDuplicateIdentityAndCrossProjectTemplate() {
        templateService.templates.put(10L, template(10L, 2L, "跨项目模板"));

        BizException crossProject = assertThrows(BizException.class, () -> service.create(req("order", "订单", 10L)));
        assertTrue(crossProject.getMessage().contains("关联表模板不属于当前项目"));

        templateService.templates.put(10L, template(10L, PROJECT_ID, "订单模板"));
        service.create(req("order", "订单", 10L));

        BizException duplicateKey = assertThrows(BizException.class, () -> service.create(req("order", "订单二", null)));
        assertTrue(duplicateKey.getMessage().contains("业务对象键已存在"));

        BizException duplicateName = assertThrows(BizException.class, () -> service.create(req("payment", "订单", null)));
        assertTrue(duplicateName.getMessage().contains("业务对象名称已存在"));
    }

    @Test
    void createRejectsSensitiveRelationContent() {
        BusinessObjectStandardReq req = new BusinessObjectStandardReq(
                PROJECT_ID,
                "order",
                "订单",
                "biz_order",
                null,
                List.of("id"),
                List.of(),
                List.of(new TableRelationHint("order", "customer", "MANY_TO_ONE", List.of("customer_id"), List.of("id"), false, "HIGH", "token=raw-token")),
                List.of(),
                null,
                List.of(),
                null,
                true,
                null);

        BizException error = assertThrows(BizException.class, () -> service.create(req));

        assertTrue(error.getMessage().contains("不能包含 token"));
    }

    @Test
    void relationSummaryReturnsEmptyAndDerivedEdges() {
        assertTrue(service.relationSummary(PROJECT_ID).nodes().isEmpty());
        assertTrue(service.relationSummary(PROJECT_ID).edges().isEmpty());

        templateService.templates.put(10L, template(10L, PROJECT_ID, "订单模板"));
        service.create(req("order", "订单", 10L));

        var summary = service.relationSummary(PROJECT_ID);

        assertFalse(summary.nodes().isEmpty());
        assertTrue(summary.edges().stream().anyMatch(edge -> "USES_TEMPLATE".equals(edge.kind())));
        assertTrue(summary.edges().stream().anyMatch(edge -> "MANY_TO_ONE".equals(edge.kind())));
        assertTrue(summary.edges().stream().anyMatch(edge -> "FOREIGN_KEY_HINT".equals(edge.kind())));
    }

    @Test
    void tableStandardsContextSupportsTemplateScopeAndReadOnlySafety() throws Exception {
        Template template = template(10L, PROJECT_ID, "订单模板");
        template.setPrimaryKeyJson("{\"columns\":[\"id\"],\"name\":\"pk_order\"}");
        template.setAiUsageNotes("建表前确认唯一键");
        templateService.templates.put(10L, template);
        service.create(req("order", "订单", 10L));
        TableStandardsContextProviderImpl provider = new TableStandardsContextProviderImpl(
                service,
                templateService,
                new ObjectMapper());

        JsonNode root = new ObjectMapper().readTree(provider.generateTableStandardsJson(
                PROJECT_ID,
                new com.dataspec.aicontext.model.AiContextScopeOptions("table-template", "10", null, 10)));

        assertEquals("dataspec-table-standards", root.path("kind").asText());
        assertEquals(1, root.path("templates").size());
        assertEquals(10L, root.path("templates").get(0).path("id").asLong());
        assertEquals("pk_order", root.path("templates").get(0).path("structure").path("primaryKey").path("name").asText());
        assertTrue(root.path("safety").path("readOnly").asBoolean());
        assertFalse(root.path("safety").path("writesProject").asBoolean());
        assertTrue(root.path("nextActions").toString().contains("DDL"));
    }

    @Test
    void tableStandardsContextUsesExactEndpointScopeMarkers() throws Exception {
        Template orderTemplate = template(1L, PROJECT_ID, "订单模板");
        Template preorderTemplate = template(10L, PROJECT_ID, "预订单模板");
        templateService.templates.put(1L, orderTemplate);
        templateService.templates.put(10L, preorderTemplate);
        service.create(req("order", "订单", 1L));
        service.create(req("preorder", "预订单", 10L));
        TableStandardsContextProviderImpl provider = new TableStandardsContextProviderImpl(
                service,
                templateService,
                new ObjectMapper());
        ObjectMapper mapper = new ObjectMapper();

        JsonNode templateScoped = mapper.readTree(provider.generateTableStandardsJson(
                PROJECT_ID,
                new com.dataspec.aicontext.model.AiContextScopeOptions("table-template", "template-id:1", null, 10)));
        JsonNode objectScoped = mapper.readTree(provider.generateTableStandardsJson(
                PROJECT_ID,
                new com.dataspec.aicontext.model.AiContextScopeOptions("business-object", "business-object:order", null, 10)));

        assertEquals(1, templateScoped.path("templates").size());
        assertEquals(1L, templateScoped.path("templates").get(0).path("id").asLong());
        assertEquals("1", templateScoped.path("contextScope").path("query").asText());
        assertEquals(1, objectScoped.path("businessObjects").size());
        assertEquals("order", objectScoped.path("businessObjects").get(0).path("objectKey").asText());
        assertEquals("order", objectScoped.path("contextScope").path("query").asText());
    }

    @Test
    void tableStandardsContextKeepsFuzzyAiContextScopeQuery() throws Exception {
        templateService.templates.put(1L, template(1L, PROJECT_ID, "订单模板"));
        templateService.templates.put(10L, template(10L, PROJECT_ID, "预订单模板"));
        TableStandardsContextProviderImpl provider = new TableStandardsContextProviderImpl(
                service,
                templateService,
                new ObjectMapper());

        JsonNode root = new ObjectMapper().readTree(provider.generateTableStandardsJson(
                PROJECT_ID,
                new com.dataspec.aicontext.model.AiContextScopeOptions("table-template", "1", null, 10)));

        assertEquals(2, root.path("templates").size());
        assertEquals(List.of(1L, 10L), java.util.stream.StreamSupport.stream(root.path("templates").spliterator(), false)
                .map(node -> node.path("id").asLong())
                .toList());
    }

    private BusinessObjectStandardReq req(String objectKey, String entityName, Long templateId) {
        return new BusinessObjectStandardReq(
                PROJECT_ID,
                objectKey,
                entityName,
                "biz_" + objectKey,
                templateId,
                List.of("id", objectKey + "_no"),
                List.of("remark"),
                List.of(new TableRelationHint(objectKey, "customer", "MANY_TO_ONE", List.of("customer_id"), List.of("id"), false, "HIGH", "关联客户")),
                List.of(new TableForeignKeyStandard("fk_" + objectKey + "_customer", List.of("customer_id"), "customer", List.of("id"), "客户关系", "RESTRICT", "NO ACTION", false, "客户外键")),
                null,
                List.of("不要把状态码写成 magic number"),
                "建表前确认主键和唯一键",
                true,
                null);
    }

    private Template template(Long id, Long projectId, String name) {
        Template template = new Template();
        template.setId(id);
        template.setProjectId(projectId);
        template.setName(name);
        return template;
    }

    private static final class InMemoryBusinessObjectStandardRepository implements BusinessObjectStandardRepository {
        private final Map<Long, BusinessObjectStandard> data = new LinkedHashMap<>();
        private long nextId = 1L;

        @Override
        public List<BusinessObjectStandard> findByProjectId(Long projectId) {
            return data.values().stream()
                    .filter(item -> projectId.equals(item.getProjectId()))
                    .toList();
        }

        @Override
        public Optional<BusinessObjectStandard> findById(Long id) {
            return Optional.ofNullable(data.get(id));
        }

        @Override
        public Optional<BusinessObjectStandard> findByObjectKey(Long projectId, String objectKey) {
            return findByProjectId(projectId).stream()
                    .filter(item -> objectKey.equals(item.getObjectKey()))
                    .findFirst();
        }

        @Override
        public List<BusinessObjectStandard> findByTemplateId(Long templateId) {
            return data.values().stream()
                    .filter(item -> templateId.equals(item.getTemplateId()))
                    .toList();
        }

        @Override
        public boolean existsByObjectKey(Long projectId, String objectKey, Long excludeId) {
            return findByProjectId(projectId).stream()
                    .anyMatch(item -> !item.getId().equals(excludeId) && objectKey.equals(item.getObjectKey()));
        }

        @Override
        public boolean existsByEntityName(Long projectId, String entityName, Long excludeId) {
            return findByProjectId(projectId).stream()
                    .anyMatch(item -> !item.getId().equals(excludeId) && entityName.equals(item.getEntityName()));
        }

        @Override
        public int insert(BusinessObjectStandard standard) {
            standard.setId(nextId++);
            data.put(standard.getId(), standard);
            return 1;
        }

        @Override
        public int update(BusinessObjectStandard standard) {
            data.put(standard.getId(), standard);
            return 1;
        }

        @Override
        public int deleteById(Long id) {
            data.remove(id);
            return 1;
        }
    }

    private static final class RecordingTemplateService implements TemplateService {
        private final Map<Long, Template> templates = new LinkedHashMap<>();

        @Override
        public List<Template> listByProject(Long projectId) {
            return templates.values().stream()
                    .filter(template -> projectId.equals(template.getProjectId()))
                    .toList();
        }

        @Override
        public Template getById(Long id) {
            Template template = templates.get(id);
            if (template == null) {
                throw new BizException("模板不存在: " + id);
            }
            return template;
        }

        @Override
        public Template create(Template template) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Template update(Long id, Template template) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void delete(Long id) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<com.dataspec.template.entity.TemplateField> listFields(Long templateId) {
            return new ArrayList<>();
        }

        @Override
        public com.dataspec.template.entity.TemplateField createField(com.dataspec.template.entity.TemplateField field) {
            throw new UnsupportedOperationException();
        }

        @Override
        public com.dataspec.template.entity.TemplateField updateField(Long id, com.dataspec.template.entity.TemplateField field) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void deleteField(Long id) {
            throw new UnsupportedOperationException();
        }
    }
}
