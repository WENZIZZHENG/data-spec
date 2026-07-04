package com.dataspec.bootstrap;

import com.dataspec.bootstrap.controller.AiSessionBootstrapController;
import com.dataspec.bootstrap.model.AiSessionBootstrap;
import com.dataspec.bootstrap.model.AiSessionBootstrapCheck;
import com.dataspec.bootstrap.model.AiSessionBootstrapNextAction;
import com.dataspec.bootstrap.model.AiSessionBootstrapSnapshot;
import com.dataspec.bootstrap.service.AiSessionBootstrapService;
import com.dataspec.common.result.R;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AiSessionBootstrapControllerTest {

    @Test
    void sessionDelegatesProjectServerAndAuthModeToService() {
        RecordingBootstrapService service = new RecordingBootstrapService();
        AiSessionBootstrapController controller = new AiSessionBootstrapController(service);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer ds_test_token");

        R<AiSessionBootstrap> response = controller.session(7L, "http://dataspec.local/", request);

        assertEquals(200, response.getCode());
        assertEquals(7L, service.lastProjectId);
        assertEquals("http://dataspec.local/", service.lastServer);
        assertTrue(service.lastTokenPresent);
        assertEquals("READY", response.getData().status());
    }

    private static class RecordingBootstrapService implements AiSessionBootstrapService {
        private Long lastProjectId;
        private String lastServer;
        private boolean lastTokenPresent;

        @Override
        public AiSessionBootstrap getBootstrap(Long projectId, String server, boolean tokenPresent) {
            lastProjectId = projectId;
            lastServer = server;
            lastTokenPresent = tokenPresent;
            return new AiSessionBootstrap(
                    "dataspec-ai-session-bootstrap",
                    1,
                    LocalDateTime.now(),
                    "READY",
                    projectId,
                    server,
                    tokenPresent ? "TOKEN_PRESENT" : "TOKEN_MISSING",
                    "v1",
                    new AiSessionBootstrapSnapshot(1L, projectId, "v1", "hash", true, "current"),
                    List.of(),
                    List.of("dataspec doctor --project " + projectId + " --format json"),
                    List.of(),
                    List.of("README.md#ai-会话启动包"),
                    List.of(new AiSessionBootstrapCheck("project", "pass", "项目已选择", null)),
                    List.of(new AiSessionBootstrapNextAction(
                            "RUN_DOCTOR",
                            "info",
                            "先运行 doctor",
                            "dataspec doctor --project " + projectId + " --format json",
                            "README.md#cli",
                            true
                    ))
            );
        }
    }
}
