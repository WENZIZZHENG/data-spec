package com.dataspec.businessglossary.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.dataspec.businessglossary.entity.BusinessGlossary;
import com.dataspec.businessglossary.model.BusinessGlossaryConflictReport;
import com.dataspec.businessglossary.service.BusinessGlossaryService;
import com.dataspec.common.result.PageResult;
import com.dataspec.common.result.R;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/glossary")
@RequiredArgsConstructor
public class BusinessGlossaryController {

    private final BusinessGlossaryService glossaryService;

    @GetMapping
    public R<PageResult<BusinessGlossary>> page(
            @RequestParam Long projectId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "20") int size) {
        IPage<BusinessGlossary> page = glossaryService.page(projectId, keyword, status, current, size);
        return R.ok(PageResult.of(page));
    }

    @GetMapping("/all")
    public R<List<BusinessGlossary>> listAll(
            @RequestParam Long projectId,
            @RequestParam(required = false) String status) {
        return R.ok(glossaryService.listByProject(projectId, status));
    }

    @GetMapping("/conflicts")
    public R<BusinessGlossaryConflictReport> conflicts(@RequestParam Long projectId) {
        return R.ok(glossaryService.conflicts(projectId));
    }

    @GetMapping("/{id}")
    public R<BusinessGlossary> getById(@PathVariable Long id) {
        return R.ok(glossaryService.getById(id));
    }

    @PostMapping
    public R<BusinessGlossary> create(@Valid @RequestBody BusinessGlossaryReq req) {
        return R.ok(glossaryService.create(toEntity(req)));
    }

    @PutMapping("/{id}")
    public R<BusinessGlossary> update(@PathVariable Long id, @Valid @RequestBody BusinessGlossaryReq req) {
        return R.ok(glossaryService.update(id, toEntity(req)));
    }

    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        glossaryService.delete(id);
        return R.ok();
    }

    private BusinessGlossary toEntity(BusinessGlossaryReq req) {
        BusinessGlossary glossary = new BusinessGlossary();
        glossary.setProjectId(req.projectId());
        glossary.setTerm(req.term());
        glossary.setSynonyms(req.synonyms());
        glossary.setRootTerms(req.rootTerms());
        glossary.setAbbreviations(req.abbreviations());
        glossary.setDisabledTerms(req.disabledTerms());
        glossary.setCanonicalFieldId(req.canonicalFieldId());
        glossary.setScopeType(req.scopeType());
        glossary.setScopeValue(req.scopeValue());
        glossary.setExampleFields(req.exampleFields());
        glossary.setDescription(req.description());
        glossary.setStatus(req.status());
        return glossary;
    }

    public record BusinessGlossaryReq(
            @NotNull(message = "项目ID不能为空") Long projectId,
            @NotBlank(message = "术语不能为空") String term,
            String synonyms,
            String rootTerms,
            String abbreviations,
            String disabledTerms,
            Long canonicalFieldId,
            String scopeType,
            String scopeValue,
            String exampleFields,
            String description,
            String status
    ) {
    }
}
