package com.dataspec.standardcandidate;

import com.dataspec.common.result.PageResult;
import com.dataspec.common.result.R;
import com.dataspec.standardcandidate.controller.StandardCandidateController;
import com.dataspec.standardcandidate.entity.StandardCandidate;
import com.dataspec.standardcandidate.model.StandardCandidateCreateReq;
import com.dataspec.standardcandidate.model.StandardCandidateDecisionReq;
import com.dataspec.standardcandidate.model.StandardCandidateMergeReq;
import com.dataspec.standardcandidate.model.TokenEvidenceCandidateApplyReq;
import com.dataspec.standardcandidate.model.TokenEvidenceCandidateApplyResult;
import com.dataspec.standardcandidate.model.TokenEvidenceCandidatePreview;
import com.dataspec.standardcandidate.model.TokenEvidenceCandidatePreviewReq;
import com.dataspec.standardcandidate.service.StandardCandidateService;
import com.dataspec.standardcandidate.service.TokenEvidenceCandidateService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StandardCandidateControllerTest {

    @Test
    void page_returnsServicePage() {
        StandardCandidateService service = mock(StandardCandidateService.class);
        PageResult<StandardCandidate> page = new PageResult<>();
        page.setRecords(List.of(candidate(10L, "user_id", "PENDING")));
        page.setTotal(1);
        when(service.page(1L, "PENDING", "MANUAL", "user", 1, 10)).thenReturn(page);

        R<PageResult<StandardCandidate>> response = new StandardCandidateController(
                service,
                mock(TokenEvidenceCandidateService.class))
                .page(1L, "PENDING", "MANUAL", "user", 1, 10);

        assertThat(response.getCode()).isEqualTo(200);
        assertThat(response.getData().getRecords()).hasSize(1);
    }

    @Test
    void actions_delegateToService() {
        StandardCandidateService service = mock(StandardCandidateService.class);
        StandardCandidate candidate = candidate(10L, "user_id", "PENDING");
        StandardCandidateCreateReq createReq = new StandardCandidateCreateReq(1L, "user_id", "用户ID", "bigint", null, "manual", null, null, 80);
        StandardCandidateDecisionReq decisionReq = new StandardCandidateDecisionReq("确认");
        StandardCandidateMergeReq mergeReq = new StandardCandidateMergeReq(20L, "合并");
        when(service.create(createReq)).thenReturn(candidate);
        when(service.accept(10L, decisionReq)).thenReturn(candidate);
        when(service.merge(10L, mergeReq)).thenReturn(candidate);
        when(service.ignore(10L, decisionReq)).thenReturn(candidate);
        when(service.postpone(10L, decisionReq)).thenReturn(candidate);
        StandardCandidateController controller = new StandardCandidateController(
                service,
                mock(TokenEvidenceCandidateService.class));

        assertThat(controller.create(createReq).getData()).isSameAs(candidate);
        assertThat(controller.accept(10L, decisionReq).getData()).isSameAs(candidate);
        assertThat(controller.merge(10L, mergeReq).getData()).isSameAs(candidate);
        assertThat(controller.ignore(10L, decisionReq).getData()).isSameAs(candidate);
        assertThat(controller.postpone(10L, decisionReq).getData()).isSameAs(candidate);

        verify(service).create(createReq);
        verify(service).accept(10L, decisionReq);
        verify(service).merge(10L, mergeReq);
        verify(service).ignore(10L, decisionReq);
        verify(service).postpone(10L, decisionReq);
    }

    @Test
    void tokenEvidenceRoutes_delegateToDedicatedService() {
        StandardCandidateService service = mock(StandardCandidateService.class);
        TokenEvidenceCandidateService tokenEvidenceService = mock(TokenEvidenceCandidateService.class);
        TokenEvidenceCandidatePreviewReq previewReq = new TokenEvidenceCandidatePreviewReq(
                1L, "ord_amt", "订单金额", "decimal", null, "field:orders.ord_amt", null);
        TokenEvidenceCandidatePreview preview = mock(TokenEvidenceCandidatePreview.class);
        TokenEvidenceCandidateApplyReq applyReq = new TokenEvidenceCandidateApplyReq(previewReq, "tec.payload.signature", true);
        TokenEvidenceCandidateApplyResult applyResult = mock(TokenEvidenceCandidateApplyResult.class);
        when(tokenEvidenceService.preview(previewReq)).thenReturn(preview);
        when(tokenEvidenceService.apply(applyReq)).thenReturn(applyResult);
        StandardCandidateController controller = new StandardCandidateController(service, tokenEvidenceService);

        assertThat(controller.previewTokenEvidence(previewReq).getData()).isSameAs(preview);
        assertThat(controller.applyTokenEvidence(applyReq).getData()).isSameAs(applyResult);

        verify(tokenEvidenceService).preview(previewReq);
        verify(tokenEvidenceService).apply(applyReq);
    }

    private StandardCandidate candidate(Long id, String name, String status) {
        StandardCandidate candidate = new StandardCandidate();
        candidate.setId(id);
        candidate.setProjectId(1L);
        candidate.setCandidateName(name);
        candidate.setDataType("bigint");
        candidate.setSourceType("MANUAL");
        candidate.setStatus(status);
        return candidate;
    }
}
