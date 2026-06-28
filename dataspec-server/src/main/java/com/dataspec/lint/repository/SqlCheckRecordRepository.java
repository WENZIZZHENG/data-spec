package com.dataspec.lint.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dataspec.lint.entity.SqlCheckRecord;
import com.dataspec.lint.mapper.SqlCheckRecordMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * SQL 检查记录 Repository —— 封装检查记录维度的查询
 */
@Repository
@RequiredArgsConstructor
public class SqlCheckRecordRepository {

    private final SqlCheckRecordMapper sqlCheckRecordMapper;

    /** 根据 ID 查找记录 */
    public Optional<SqlCheckRecord> findById(Long id) {
        return Optional.ofNullable(sqlCheckRecordMapper.selectById(id));
    }

    /** 查询项目最近检查记录，供只读报告聚合使用。 */
    public List<SqlCheckRecord> findRecentByProjectId(Long projectId, int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 500));
        return sqlCheckRecordMapper.selectList(
                new LambdaQueryWrapper<SqlCheckRecord>()
                        .eq(SqlCheckRecord::getProjectId, projectId)
                        .orderByDesc(SqlCheckRecord::getCreatedAt)
                        .orderByDesc(SqlCheckRecord::getId)
                        .last("LIMIT " + safeLimit));
    }

    /** 分页查询项目下的检查记录(按创建时间倒序) */
    public IPage<SqlCheckRecord> findByProjectId(Long projectId, int current, int size) {
        LambdaQueryWrapper<SqlCheckRecord> wrapper = new LambdaQueryWrapper<>();
        if (projectId != null) {
            wrapper.eq(SqlCheckRecord::getProjectId, projectId);
        }
        wrapper.orderByDesc(SqlCheckRecord::getCreatedAt);
        return sqlCheckRecordMapper.selectPage(new Page<>(current, size), wrapper);
    }

    /** 新增检查记录 */
    public int insert(SqlCheckRecord record) {
        return sqlCheckRecordMapper.insert(record);
    }
}
