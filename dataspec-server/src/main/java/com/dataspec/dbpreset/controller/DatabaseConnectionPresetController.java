package com.dataspec.dbpreset.controller;

import com.dataspec.common.result.R;
import com.dataspec.dbpreset.entity.DatabaseConnectionPreset;
import com.dataspec.dbpreset.service.DatabaseConnectionPresetService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 数据库连接预设管理 API。
 */
@RestController
@RequestMapping("/api/database-connection-presets")
@RequiredArgsConstructor
public class DatabaseConnectionPresetController {

    private final DatabaseConnectionPresetService presetService;

    @GetMapping
    public R<List<DatabaseConnectionPreset>> list(@RequestParam Long projectId) {
        return R.ok(presetService.listByProject(projectId));
    }

    @GetMapping("/{id}")
    public R<DatabaseConnectionPreset> getById(@PathVariable Long id) {
        return R.ok(presetService.getById(id));
    }

    @PostMapping
    public R<DatabaseConnectionPreset> create(@Valid @RequestBody PresetReq req) {
        return R.ok(presetService.create(toPreset(req, null)));
    }

    @PutMapping("/{id}")
    public R<DatabaseConnectionPreset> update(@PathVariable Long id, @Valid @RequestBody PresetReq req) {
        return R.ok(presetService.update(id, toPreset(req, id)));
    }

    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        presetService.delete(id);
        return R.ok();
    }

    private DatabaseConnectionPreset toPreset(PresetReq req, Long id) {
        DatabaseConnectionPreset preset = new DatabaseConnectionPreset();
        preset.setId(id);
        preset.setProjectId(req.projectId());
        preset.setName(req.name());
        preset.setDatabaseType(req.databaseType());
        preset.setHost(req.host());
        preset.setPort(req.port());
        preset.setDatabaseName(req.databaseName());
        preset.setSchemaName(req.schemaName());
        preset.setTableNames(req.tableNames());
        return preset;
    }

    public record PresetReq(
            @NotNull(message = "项目ID不能为空") Long projectId,
            @NotBlank(message = "预设名称不能为空") String name,
            @NotBlank(message = "数据库类型不能为空") String databaseType,
            @NotBlank(message = "主机不能为空") String host,
            @NotNull(message = "端口不能为空") @Min(1) @Max(65535) Integer port,
            @NotBlank(message = "数据库名不能为空") String databaseName,
            String schemaName,
            List<String> tableNames
    ) {
    }
}
