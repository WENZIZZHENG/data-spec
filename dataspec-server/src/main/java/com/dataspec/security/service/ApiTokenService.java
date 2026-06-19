package com.dataspec.security.service;

import com.dataspec.security.model.ApiTokenPrincipal;

/**
 * API token 认证服务。
 */
public interface ApiTokenService {

    ApiTokenPrincipal authenticate(String rawToken);

    String hashToken(String rawToken);
}
