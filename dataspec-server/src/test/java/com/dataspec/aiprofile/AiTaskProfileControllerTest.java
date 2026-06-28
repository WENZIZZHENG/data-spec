package com.dataspec.aiprofile;

import com.dataspec.aiprofile.controller.AiTaskProfileController;
import com.dataspec.aiprofile.model.AiTaskProfile;
import com.dataspec.aiprofile.model.AiTaskProfileCatalog;
import com.dataspec.aiprofile.model.AiTaskProfileDetail;
import com.dataspec.aiprofile.service.AiTaskProfileService;
import com.dataspec.common.result.R;
import com.dataspec.lint.model.FixPolicy;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AiTaskProfileControllerTest {

    @Test
    void listProfilesDelegatesProjectAndSelectedProfile() {
        RecordingAiTaskProfileService service = new RecordingAiTaskProfileService();
        AiTaskProfileController controller = new AiTaskProfileController(service);

        R<AiTaskProfileCatalog> response = controller.listProfiles(9L, "sql-fix");

        assertEquals(200, response.getCode());
        assertEquals(9L, service.listProjectId);
        assertEquals("sql-fix", service.listSelectedProfile);
        assertEquals("sql-fix", response.getData().getSelectedProfileId());
    }

    @Test
    void getProfileDelegatesProjectAndProfileKey() {
        RecordingAiTaskProfileService service = new RecordingAiTaskProfileService();
        AiTaskProfileController controller = new AiTaskProfileController(service);

        R<AiTaskProfileDetail> response = controller.getProfile("SQL_FIX", 9L);

        assertEquals(200, response.getCode());
        assertEquals(9L, service.getProjectId);
        assertEquals("SQL_FIX", service.getProfileOrTaskType);
        assertEquals("sql-fix", response.getData().getProfile().getProfileId());
    }

    private static class RecordingAiTaskProfileService implements AiTaskProfileService {
        private Long listProjectId;
        private String listSelectedProfile;
        private Long getProjectId;
        private String getProfileOrTaskType;

        @Override
        public AiTaskProfileCatalog listProfiles(Long projectId, String selectedProfile) {
            this.listProjectId = projectId;
            this.listSelectedProfile = selectedProfile;
            return AiTaskProfileCatalog.builder()
                    .projectId(projectId)
                    .selectedProfileId(selectedProfile)
                    .profiles(List.of(profile()))
                    .build();
        }

        @Override
        public AiTaskProfileDetail getProfile(Long projectId, String profileOrTaskType) {
            this.getProjectId = projectId;
            this.getProfileOrTaskType = profileOrTaskType;
            return AiTaskProfileDetail.builder()
                    .projectId(projectId)
                    .requestedProfile(profileOrTaskType)
                    .profile(profile())
                    .build();
        }

        @Override
        public Optional<AiTaskProfile> findProfile(String profileOrTaskType) {
            return Optional.of(profile());
        }

        @Override
        public FixPolicy resolveFixedSqlPolicy(String profileOrTaskType) {
            return null;
        }

        private AiTaskProfile profile() {
            return AiTaskProfile.builder()
                    .profileId("sql-fix")
                    .taskType("SQL_FIX")
                    .build();
        }
    }
}
