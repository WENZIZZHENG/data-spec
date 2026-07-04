package com.dataspec.bootstrap.service;

import com.dataspec.bootstrap.model.AiSessionBootstrap;

public interface AiSessionBootstrapService {

    AiSessionBootstrap getBootstrap(Long projectId, String server, boolean tokenPresent);
}
