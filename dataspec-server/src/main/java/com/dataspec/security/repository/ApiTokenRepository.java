package com.dataspec.security.repository;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dataspec.security.entity.ApiToken;
import com.dataspec.security.mapper.ApiTokenMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
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

    public List<ApiToken> findAllActiveRows() {
        return apiTokenMapper.selectList(new LambdaQueryWrapper<ApiToken>()
                .orderByDesc(ApiToken::getCreatedAt)
                .orderByDesc(ApiToken::getId));
    }

    public Optional<ApiToken> findById(Long id) {
        return Optional.ofNullable(apiTokenMapper.selectById(id));
    }

    public void save(ApiToken token) {
        apiTokenMapper.insert(token);
    }

    public void update(ApiToken token) {
        apiTokenMapper.updateById(token);
    }

    public void touchLastUsedAt(String tokenHash) {
        LocalDateTime now = LocalDateTime.now();
        apiTokenMapper.update(null, new LambdaUpdateWrapper<ApiToken>()
                .eq(ApiToken::getTokenHash, tokenHash)
                .set(ApiToken::getLastUsedAt, now)
                .set(ApiToken::getUpdatedAt, now));
    }
}
