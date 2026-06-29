package com.dataspec.businessglossary.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dataspec.businessglossary.entity.BusinessGlossary;
import com.dataspec.businessglossary.mapper.BusinessGlossaryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 业务术语表 Repository。
 */
@Repository
@RequiredArgsConstructor
public class BusinessGlossaryRepository {

    private final BusinessGlossaryMapper mapper;

    public Optional<BusinessGlossary> findById(Long id) {
        return Optional.ofNullable(mapper.selectById(id));
    }

    public IPage<BusinessGlossary> findPage(Long projectId, String keyword, String status, int current, int size) {
        LambdaQueryWrapper<BusinessGlossary> wrapper = baseProjectQuery(projectId)
                .orderByAsc(BusinessGlossary::getTerm);
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(query -> query
                    .like(BusinessGlossary::getTerm, keyword)
                    .or().like(BusinessGlossary::getSynonyms, keyword)
                    .or().like(BusinessGlossary::getRootTerms, keyword)
                    .or().like(BusinessGlossary::getAbbreviations, keyword)
                    .or().like(BusinessGlossary::getExampleFields, keyword));
        }
        if (status != null && !status.isBlank()) {
            wrapper.eq(BusinessGlossary::getStatus, status);
        }
        return mapper.selectPage(new Page<>(current, size), wrapper);
    }

    public List<BusinessGlossary> findAllByProjectId(Long projectId) {
        return mapper.selectList(baseProjectQuery(projectId).orderByAsc(BusinessGlossary::getTerm));
    }

    public int insert(BusinessGlossary glossary) {
        return mapper.insert(glossary);
    }

    public int update(BusinessGlossary glossary) {
        return mapper.updateById(glossary);
    }

    public int deleteById(Long id) {
        return mapper.deleteById(id);
    }

    private LambdaQueryWrapper<BusinessGlossary> baseProjectQuery(Long projectId) {
        return new LambdaQueryWrapper<BusinessGlossary>()
                .eq(BusinessGlossary::getProjectId, projectId);
    }
}
