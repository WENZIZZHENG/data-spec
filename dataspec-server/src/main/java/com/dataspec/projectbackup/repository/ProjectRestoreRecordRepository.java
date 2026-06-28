package com.dataspec.projectbackup.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dataspec.projectbackup.entity.ProjectRestoreRecord;
import com.dataspec.projectbackup.mapper.ProjectRestoreRecordMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class ProjectRestoreRecordRepository {

    private final ProjectRestoreRecordMapper mapper;

    public int insert(ProjectRestoreRecord record) {
        return mapper.insert(record);
    }

    public List<ProjectRestoreRecord> findByProjectId(Long projectId) {
        return mapper.selectList(new LambdaQueryWrapper<ProjectRestoreRecord>()
                .eq(ProjectRestoreRecord::getProjectId, projectId)
                .orderByDesc(ProjectRestoreRecord::getCreatedAt)
                .orderByDesc(ProjectRestoreRecord::getId));
    }
}
