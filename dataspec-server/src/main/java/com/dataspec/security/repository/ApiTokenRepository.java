package com.dataspec.security.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dataspec.security.entity.ApiToken;
import com.dataspec.security.mapper.ApiTokenMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * API token Repository。
 */
@Repository
@RequiredArgsConstructor
public class ApiTokenRepository {

    private final ApiTokenMapper apiTokenMapper;

    public Optional<ApiToken> findByTokenHash(String tokenHash) {
        return Optional.ofNullable(apiTokenMapper.selectOne(
                new LambdaQueryWrapper<ApiToken>()
                        .eq(ApiToken::getTokenHash, tokenHash)));
    }
}
