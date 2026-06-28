package com.dataspec.aiprofile.service;

import com.dataspec.aiprofile.model.AiTaskProfile;
import com.dataspec.aiprofile.model.AiTaskProfileCatalog;
import com.dataspec.aiprofile.model.AiTaskProfileDetail;
import com.dataspec.lint.model.FixPolicy;

import java.util.Optional;

public interface AiTaskProfileService {

    AiTaskProfileCatalog listProfiles(Long projectId, String selectedProfile);

    AiTaskProfileDetail getProfile(Long projectId, String profileOrTaskType);

    Optional<AiTaskProfile> findProfile(String profileOrTaskType);

    FixPolicy resolveFixedSqlPolicy(String profileOrTaskType);
}
