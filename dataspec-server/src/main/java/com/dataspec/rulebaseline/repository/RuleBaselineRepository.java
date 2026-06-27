package com.dataspec.rulebaseline.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dataspec.rulebaseline.entity.RuleBaseline;
import com.dataspec.rulebaseline.mapper.RuleBaselineMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class RuleBaselineRepository {

    private final RuleBaselineMapper ruleBaselineMapper;

    public Optional<RuleBaseline> findByProjectId(Long projectId) {
        return Optional.ofNullable(ruleBaselineMapper.selectOne(
                new LambdaQueryWrapper<RuleBaseline>()
                        .eq(RuleBaseline::getProjectId, projectId)));
    }

    public int insert(RuleBaseline baseline) {
        return ruleBaselineMapper.insert(baseline);
    }

    public int update(RuleBaseline baseline) {
        return ruleBaselineMapper.updateById(baseline);
    }
}
