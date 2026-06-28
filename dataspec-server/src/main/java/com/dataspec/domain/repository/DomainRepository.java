package com.dataspec.domain.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dataspec.domain.entity.Domain;
import com.dataspec.domain.mapper.DomainMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 数据域 Repository —— 封装数据域维度的业务查询
 */

@Repository
@RequiredArgsConstructor
public class DomainRepository {

    private final DomainMapper domainMapper;

    /** 根据 ID 查找数据域 */
    public Optional<Domain> findById(Long id) {
        return Optional.ofNullable(domainMapper.selectById(id));
    }

    /** 查询项目下所有数据域 */
    public List<Domain> findByProjectId(Long projectId) {
        return domainMapper.selectList(
                new LambdaQueryWrapper<Domain>()
                        .eq(Domain::getProjectId, projectId)
                        .orderByAsc(Domain::getCode));
    }

    /** 根据项目内编码查找数据域 */
    public Optional<Domain> findByCodeInProject(String code, Long projectId) {
        return Optional.ofNullable(domainMapper.selectOne(
                new LambdaQueryWrapper<Domain>()
                        .eq(Domain::getCode, code)
                        .eq(Domain::getProjectId, projectId)
                        .last("limit 1")));
    }

    /** 检查项目内数据域编码是否重复 */
    public boolean existsByCodeInProject(String code, Long projectId) {
        return domainMapper.exists(
                new LambdaQueryWrapper<Domain>()
                        .eq(Domain::getCode, code)
                        .eq(Domain::getProjectId, projectId));
    }

    /** 检查项目内数据域编码是否重复（排除指定 ID） */
    public boolean existsByCodeInProjectExcludeId(String code, Long projectId, Long excludeId) {
        return domainMapper.exists(
                new LambdaQueryWrapper<Domain>()
                        .eq(Domain::getCode, code)
                        .eq(Domain::getProjectId, projectId)
                        .ne(Domain::getId, excludeId));
    }

    /** 新增数据域 */
    public int insert(Domain domain) {
        return domainMapper.insert(domain);
    }

    /** 更新数据域 */
    public int update(Domain domain) {
        return domainMapper.updateById(domain);
    }

    /** 逻辑删除数据域 */
    public int deleteById(Long id) {
        return domainMapper.deleteById(id);
    }
}
