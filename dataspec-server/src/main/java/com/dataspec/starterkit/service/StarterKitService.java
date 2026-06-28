package com.dataspec.starterkit.service;

import com.dataspec.starterkit.model.StarterKitApplyResult;
import com.dataspec.starterkit.model.StarterKitDefinition;
import com.dataspec.starterkit.model.StarterKitInstallationInfo;

import java.util.List;

public interface StarterKitService {

    List<StarterKitDefinition> listKits();

    StarterKitApplyResult applyKit(Long projectId, String kitKey, String kitVersion);

    List<StarterKitInstallationInfo> listInstallations(Long projectId);
}
