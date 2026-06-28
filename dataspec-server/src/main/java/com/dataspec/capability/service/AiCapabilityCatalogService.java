package com.dataspec.capability.service;

import com.dataspec.capability.model.AiCapabilityCatalog;
import com.dataspec.capability.model.AiCapabilityEntry;

public interface AiCapabilityCatalogService {

    AiCapabilityCatalog getCatalog(Long projectId);

    AiCapabilityEntry getCapability(String capabilityId, Long projectId);
}
