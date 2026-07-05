package com.dataspec.standardreuse.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dataspec.standardreuse.entity.StandardReusePack;
import com.dataspec.standardreuse.mapper.StandardReusePackMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 标准复用包 Repository，封装项目内包版本和详情查询。
 */
@Repository
@RequiredArgsConstructor
public class StandardReusePackRepository {

    private final StandardReusePackMapper mapper;

    /** 新增标准复用包。 */
    public int insert(StandardReusePack pack) {
        return mapper.insert(pack);
    }

    /** 查询项目内标准复用包列表，按创建时间倒序。 */
    public List<StandardReusePack> findByProjectId(Long projectId) {
        return mapper.selectList(new LambdaQueryWrapper<StandardReusePack>()
                .eq(StandardReusePack::getProjectId, projectId)
                .orderByDesc(StandardReusePack::getCreatedAt)
                .orderByDesc(StandardReusePack::getId));
    }

    /** 按 ID 查询标准复用包。 */
    public Optional<StandardReusePack> findById(Long id) {
        return Optional.ofNullable(mapper.selectById(id));
    }

    /** 检查项目内包 key 和版本是否已存在。 */
    public boolean existsByProjectIdAndKeyAndVersion(Long projectId, String packKey, String basePackVersion) {
        return mapper.exists(new LambdaQueryWrapper<StandardReusePack>()
                .eq(StandardReusePack::getProjectId, projectId)
                .eq(StandardReusePack::getPackKey, packKey)
                .eq(StandardReusePack::getBasePackVersion, basePackVersion));
    }
}
