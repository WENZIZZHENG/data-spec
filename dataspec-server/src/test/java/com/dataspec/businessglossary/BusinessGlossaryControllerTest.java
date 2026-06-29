package com.dataspec.businessglossary;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dataspec.businessglossary.controller.BusinessGlossaryController;
import com.dataspec.businessglossary.entity.BusinessGlossary;
import com.dataspec.businessglossary.model.BusinessGlossaryConflictReport;
import com.dataspec.businessglossary.model.BusinessGlossaryConflictSummary;
import com.dataspec.businessglossary.service.BusinessGlossaryService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BusinessGlossaryControllerTest {

    @Test
    void page_forwardsFiltersToService() {
        BusinessGlossaryService service = mock(BusinessGlossaryService.class);
        Page<BusinessGlossary> page = new Page<>(1, 20);
        page.setRecords(List.of(glossary(1L, "会员")));
        page.setTotal(1);
        when(service.page(1L, "会员", "enabled", 1, 20)).thenReturn(page);
        BusinessGlossaryController controller = new BusinessGlossaryController(service);

        var response = controller.page(1L, "会员", "enabled", 1, 20);

        assertEquals(1, response.getData().getRecords().size());
        assertEquals("会员", response.getData().getRecords().getFirst().getTerm());
        verify(service).page(1L, "会员", "enabled", 1, 20);
    }

    @Test
    void create_mapsRequestToEntity() {
        BusinessGlossaryService service = mock(BusinessGlossaryService.class);
        when(service.create(any(BusinessGlossary.class))).thenAnswer(invocation -> invocation.getArgument(0));
        BusinessGlossaryController controller = new BusinessGlossaryController(service);

        var response = controller.create(new BusinessGlossaryController.BusinessGlossaryReq(
                1L,
                "会员",
                "用户,账号",
                "user,member",
                "hy",
                "老用户",
                10L,
                "GLOBAL",
                null,
                "user_id",
                "会员相关术语",
                "enabled"));

        assertEquals("会员", response.getData().getTerm());
        assertEquals(10L, response.getData().getCanonicalFieldId());
        assertEquals("user_id", response.getData().getExampleFields());
    }

    @Test
    void update_mapsRequestAndUsesPathId() {
        BusinessGlossaryService service = mock(BusinessGlossaryService.class);
        when(service.update(any(Long.class), any(BusinessGlossary.class))).thenAnswer(invocation -> {
            BusinessGlossary entity = invocation.getArgument(1);
            entity.setId(invocation.getArgument(0));
            return entity;
        });
        BusinessGlossaryController controller = new BusinessGlossaryController(service);

        var response = controller.update(9L, new BusinessGlossaryController.BusinessGlossaryReq(
                1L,
                "客户",
                "账号",
                "customer",
                "kh",
                null,
                10L,
                "GLOBAL",
                null,
                "customer_id",
                "客户术语",
                "disabled"));

        assertEquals(9L, response.getData().getId());
        assertEquals("客户", response.getData().getTerm());
        assertEquals("disabled", response.getData().getStatus());
        verify(service).update(any(Long.class), any(BusinessGlossary.class));
    }

    @Test
    void delete_forwardsPathIdToService() {
        BusinessGlossaryService service = mock(BusinessGlossaryService.class);
        BusinessGlossaryController controller = new BusinessGlossaryController(service);

        var response = controller.delete(9L);

        assertEquals(null, response.getData());
        verify(service).delete(9L);
    }

    @Test
    void conflicts_returnsServiceReport() {
        BusinessGlossaryService service = mock(BusinessGlossaryService.class);
        when(service.conflicts(1L)).thenReturn(new BusinessGlossaryConflictReport(
                1L,
                new BusinessGlossaryConflictSummary(0, 0, 0),
                List.of()));
        BusinessGlossaryController controller = new BusinessGlossaryController(service);

        var response = controller.conflicts(1L);

        assertEquals(1L, response.getData().projectId());
        assertEquals(0, response.getData().summary().conflictCount());
        verify(service).conflicts(1L);
    }

    private BusinessGlossary glossary(Long id, String term) {
        BusinessGlossary glossary = new BusinessGlossary();
        glossary.setId(id);
        glossary.setProjectId(1L);
        glossary.setTerm(term);
        return glossary;
    }
}
