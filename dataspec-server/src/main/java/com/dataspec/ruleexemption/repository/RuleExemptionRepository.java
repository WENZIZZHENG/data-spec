package com.dataspec.ruleexemption.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dataspec.ruleexemption.entity.RuleExemption;
import com.dataspec.ruleexemption.mapper.RuleExemptionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 规则豁免 Repository。
 */
@Repository
@RequiredArgsConstructor
public class RuleExemptionRepository {

    private final RuleExemptionMapper ruleExemptionMapper;

    public Optional<RuleExemption> findById(Long id) {
        return Optional.ofNullable(ruleExemptionMapper.selectById(id));
    }

    public List<RuleExemption> findByProjectId(Long projectId) {
        return ruleExemptionMapper.selectList(
                new LambdaQueryWrapper<RuleExemption>()
                        .eq(RuleExemption::getProjectId, projectId)
                        .orderByDesc(RuleExemption::getUpdatedAt)
                        .orderByDesc(RuleExemption::getId));
    }

    public int insert(RuleExemption exemption) {
        return ruleExemptionMapper.insert(exemption);
    }

    public int update(RuleExemption exemption) {
        return ruleExemptionMapper.updateById(exemption);
    }

    public int deleteById(Long id) {
        return ruleExemptionMapper.deleteById(id);
    }
}
