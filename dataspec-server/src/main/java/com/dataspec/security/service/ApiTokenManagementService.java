package com.dataspec.security.service;

import com.dataspec.security.dto.ApiTokenCreateReq;
import com.dataspec.security.dto.ApiTokenCreateResp;
import com.dataspec.security.dto.ApiTokenInfo;

import java.util.List;

/**
 * API token 管理服务。
 */
public interface ApiTokenManagementService {

    List<ApiTokenInfo> listTokens();

    ApiTokenCreateResp createToken(ApiTokenCreateReq req);

    ApiTokenInfo disableToken(Long id);
}
