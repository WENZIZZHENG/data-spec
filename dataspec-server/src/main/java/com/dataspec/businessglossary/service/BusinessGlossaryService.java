package com.dataspec.businessglossary.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.dataspec.businessglossary.entity.BusinessGlossary;
import com.dataspec.businessglossary.model.BusinessGlossaryConflictReport;
import com.dataspec.businessglossary.model.BusinessGlossaryContextExport;
import com.dataspec.businessglossary.model.GlossaryMatch;

import java.util.List;

public interface BusinessGlossaryService {

    IPage<BusinessGlossary> page(Long projectId, String keyword, String status, int current, int size);

    List<BusinessGlossary> listByProject(Long projectId, String status);

    BusinessGlossary getById(Long id);

    BusinessGlossary create(BusinessGlossary glossary);

    BusinessGlossary update(Long id, BusinessGlossary glossary);

    void delete(Long id);

    BusinessGlossaryConflictReport conflicts(Long projectId);

    List<GlossaryMatch> match(Long projectId, String query);

    BusinessGlossaryContextExport contextExport(Long projectId, int limit);
}
