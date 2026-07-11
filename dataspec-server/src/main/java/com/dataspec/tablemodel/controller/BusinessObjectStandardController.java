package com.dataspec.tablemodel.controller;

import com.dataspec.common.result.R;
import com.dataspec.tablemodel.model.BusinessObjectStandardReq;
import com.dataspec.tablemodel.model.BusinessObjectStandardResp;
import com.dataspec.tablemodel.model.TableRelationSummary;
import com.dataspec.tablemodel.service.BusinessObjectStandardService;
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

/**
 * 业务对象与表结构标准 API，提供只读关系摘要和业务对象标准 CRUD。
 */
@RestController
@RequestMapping("/api/business-objects")
@RequiredArgsConstructor
public class BusinessObjectStandardController {

    private final BusinessObjectStandardService service;

    /** 查询项目业务对象标准列表。 */
    @GetMapping
    public R<List<BusinessObjectStandardResp>> list(@RequestParam @NotNull Long projectId) {
        return R.ok(service.listByProject(projectId));
    }

    /** 查询项目内业务对象标准。 */
    @GetMapping("/by-key/{objectKey}")
    public R<BusinessObjectStandardResp> getByObjectKey(@RequestParam @NotNull Long projectId,
                                                        @PathVariable @NotBlank String objectKey) {
        return R.ok(service.getByObjectKey(projectId, objectKey));
    }

    /** 查询业务对象标准详情。 */
    @GetMapping("/{id}")
    public R<BusinessObjectStandardResp> getById(@PathVariable Long id) {
        return R.ok(service.getById(id));
    }

    /** 创建业务对象标准。 */
    @PostMapping
    public R<BusinessObjectStandardResp> create(@Valid @RequestBody BusinessObjectStandardReq req) {
        return R.ok(service.create(req));
    }

    /** 更新业务对象标准。 */
    @PutMapping("/{id}")
    public R<BusinessObjectStandardResp> update(@PathVariable Long id,
                                                @Valid @RequestBody BusinessObjectStandardReq req) {
        return R.ok(service.update(id, req));
    }

    /** 删除业务对象标准。 */
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return R.ok();
    }

    /** 获取业务对象、表模板和关系提示的只读摘要。 */
    @GetMapping("/relation-summary")
    public R<TableRelationSummary> relationSummary(@RequestParam @NotNull Long projectId) {
        return R.ok(service.relationSummary(projectId));
    }
}
