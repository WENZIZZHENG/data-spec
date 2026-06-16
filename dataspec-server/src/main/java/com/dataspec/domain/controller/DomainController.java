package com.dataspec.domain.controller;

import com.dataspec.common.result.R;
import com.dataspec.domain.entity.Domain;
import com.dataspec.domain.service.DomainService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/domains")
@RequiredArgsConstructor
public class DomainController {

    private final DomainService domainService;

    @GetMapping
    public R<List<Domain>> list(@RequestParam Long projectId) {
        return R.ok(domainService.listByProject(projectId));
    }

    @GetMapping("/{id}")
    public R<Domain> getById(@PathVariable Long id) {
        return R.ok(domainService.getById(id));
    }

    @PostMapping
    public R<Domain> create(@Valid @RequestBody DomainReq req) {
        Domain domain = new Domain();
        domain.setProjectId(req.projectId());
        domain.setName(req.name());
        domain.setCode(req.code());
        domain.setDescription(req.description());
        return R.ok(domainService.create(domain));
    }

    @PutMapping("/{id}")
    public R<Domain> update(@PathVariable Long id, @Valid @RequestBody DomainReq req) {
        Domain domain = new Domain();
        domain.setName(req.name());
        domain.setCode(req.code());
        domain.setDescription(req.description());
        return R.ok(domainService.update(id, domain));
    }

    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        domainService.delete(id);
        return R.ok();
    }

    public record DomainReq(
            @NotNull(message = "项目ID不能为空") Long projectId,
            @NotBlank(message = "数据域名称不能为空") String name,
            @NotBlank(message = "数据域编码不能为空") String code,
            String description
    ) {}
}
