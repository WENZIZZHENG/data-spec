package com.dataspec.standardreuse.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dataspec.standardreuse.entity.StandardReusePackApplication;
import com.dataspec.standardreuse.mapper.StandardReusePackApplicationMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 标准复用包应用记录 Repository，提供目标项目应用历史查询。
 */
@Repository
@RequiredArgsConstructor
public class StandardReusePackApplicationRepository {

    private final StandardReusePackApplicationMapper mapper;

    /** 新增应用摘要记录。 */
    public int insert(StandardReusePackApplication application) {
        return mapper.insert(application);
    }

    /** 查询目标项目最近应用记录。 */
    public List<StandardReusePackApplication> findByProjectId(Long projectId) {
        return mapper.selectList(new LambdaQueryWrapper<StandardReusePackApplication>()
                .eq(StandardReusePackApplication::getProjectId, projectId)
                .orderByDesc(StandardReusePackApplication::getAppliedAt)
                .orderByDesc(StandardReusePackApplication::getId));
    }
}
