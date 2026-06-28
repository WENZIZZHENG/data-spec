package com.dataspec.evidence.service;

import com.dataspec.evidence.model.AiEvidencePackage;
import com.dataspec.evidence.model.AiEvidencePackageReq;

public interface AiEvidencePackageService {

    AiEvidencePackage generate(AiEvidencePackageReq req);

    byte[] generateZip(AiEvidencePackageReq req);
}
