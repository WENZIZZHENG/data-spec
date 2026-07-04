package com.dataspec.standardqualitygate;

import com.dataspec.standardqualitygate.controller.StandardQualityGateController;
import com.dataspec.standardqualitygate.model.StandardQualityGateConfig;
import com.dataspec.standardqualitygate.model.StandardQualityGateEvaluateReq;
import com.dataspec.standardqualitygate.model.StandardQualityGateResult;
import com.dataspec.standardqualitygate.model.StandardQualityGateSaveReq;
import com.dataspec.standardqualitygate.service.StandardQualityGateService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class StandardQualityGateControllerTest {

    @Test
    void configAndEvaluate_delegateToService() {
        StandardQualityGateService service = mock(StandardQualityGateService.class);
        StandardQualityGateConfig config = new StandardQualityGateConfig();
        config.setProjectId(1L);
        config.setEnabled(true);
        StandardQualityGateResult result = new StandardQualityGateResult();
        result.setProjectId(1L);
        result.setStatus("PASS");
        StandardQualityGateSaveReq saveReq = new StandardQualityGateSaveReq();
        saveReq.setProjectId(1L);
        StandardQualityGateEvaluateReq evaluateReq = new StandardQualityGateEvaluateReq();
        evaluateReq.setProjectId(1L);
        when(service.getConfig(1L)).thenReturn(config);
        when(service.saveConfig(saveReq)).thenReturn(config);
        when(service.evaluate(evaluateReq)).thenReturn(result);
        StandardQualityGateController controller = new StandardQualityGateController(service);

        assertThat(controller.getConfig(1L).getData().getEnabled()).isTrue();
        assertThat(controller.saveConfig(saveReq).getData().getProjectId()).isEqualTo(1L);
        assertThat(controller.evaluate(evaluateReq).getData().getStatus()).isEqualTo("PASS");
    }
}
