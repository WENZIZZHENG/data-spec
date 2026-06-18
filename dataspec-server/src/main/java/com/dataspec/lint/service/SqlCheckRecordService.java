package com.dataspec.lint.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.dataspec.lint.entity.SqlCheckRecord;
import com.dataspec.lint.model.LintIssue;
import com.dataspec.lint.model.LintResult;

import java.util.List;

/**
 * SQL 检查记录服务
 */
public interface SqlCheckRecordService {

    /** 保存一次校验的记录(含原 SQL、修正 SQL、统计与结构化问题) */
    SqlCheckRecord save(Long projectId, String originalSql, LintResult result);

    /** 分页查询项目下的检查记录 */
    IPage<SqlCheckRecord> listByProject(Long projectId, int current, int size);

    /** 查询记录详情 */
    SqlCheckRecord getById(Long id);

    /** 解析记录中的结构化问题列表 */
    List<LintIssue> parseIssues(SqlCheckRecord record);
}
