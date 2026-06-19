package com.dataspec.enumdict.service.impl;

import com.dataspec.changelog.service.StandardChangeLogService;
import com.dataspec.common.exception.BizException;
import com.dataspec.enumdict.entity.EnumDict;
import com.dataspec.enumdict.entity.EnumValue;
import com.dataspec.enumdict.repository.EnumDictRepository;
import com.dataspec.enumdict.service.EnumDictService;
import com.dataspec.security.context.ProjectAccessGuard;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 枚举字典服务实现
 */

@Service
@RequiredArgsConstructor
public class EnumDictServiceImpl implements EnumDictService {

    private final EnumDictRepository enumDictRepository;
    private final StandardChangeLogService changeLogService;

    @Override
    public List<EnumDict> listByProject(Long projectId) {
        ProjectAccessGuard.requireProjectAccess(projectId);
        return enumDictRepository.findDictsByProjectId(projectId);
    }

    @Override
    public EnumDict getById(Long id) {
        EnumDict enumDict = enumDictRepository.findDictById(id)
                .orElseThrow(() -> new BizException("枚举字典不存在: " + id));
        ProjectAccessGuard.requireProjectAccess(enumDict.getProjectId());
        return enumDict;
    }

    @Override
    public EnumDict create(EnumDict enumDict) {
        ProjectAccessGuard.requireProjectAccess(enumDict.getProjectId());
        if (enumDictRepository.existsDictByCodeInProject(enumDict.getCode(), enumDict.getProjectId())) {
            throw new BizException("枚举编码已存在: " + enumDict.getCode());
        }
        enumDict.setValueType(enumDict.getValueType() != null ? enumDict.getValueType() : "integer");
        enumDictRepository.insertDict(enumDict);
        changeLogService.recordChange(
                enumDict.getProjectId(),
                StandardChangeLogService.TARGET_ENUM_DICT,
                enumDict.getId(),
                StandardChangeLogService.ACTION_CREATE,
                null,
                changeLogService.snapshot(enumDict));
        return enumDict;
    }

    @Override
    public EnumDict update(Long id, EnumDict enumDict) {
        EnumDict existing = getById(id);
        String beforeJson = changeLogService.snapshot(existing);
        existing.setName(enumDict.getName());
        existing.setCode(enumDict.getCode());
        existing.setDescription(enumDict.getDescription());
        existing.setValueType(enumDict.getValueType());
        enumDictRepository.updateDict(existing);
        changeLogService.recordChange(
                existing.getProjectId(),
                StandardChangeLogService.TARGET_ENUM_DICT,
                existing.getId(),
                StandardChangeLogService.ACTION_UPDATE,
                beforeJson,
                changeLogService.snapshot(existing));
        return existing;
    }

    @Override
    @Transactional
    public void delete(Long id) {
        EnumDict existing = getById(id);
        String beforeJson = changeLogService.snapshot(existing);
        enumDictRepository.deleteValuesByEnumId(id);
        enumDictRepository.deleteDictById(id);
        changeLogService.recordChange(
                existing.getProjectId(),
                StandardChangeLogService.TARGET_ENUM_DICT,
                existing.getId(),
                StandardChangeLogService.ACTION_DELETE,
                beforeJson,
                null);
    }

    @Override
    public List<EnumValue> listValues(Long enumId) {
        getById(enumId);
        return enumDictRepository.findValuesByEnumId(enumId);
    }

    @Override
    public EnumValue createValue(EnumValue value) {
        EnumDict enumDict = getById(value.getEnumId());
        enumDictRepository.insertValue(value);
        changeLogService.recordChange(
                enumDict.getProjectId(),
                StandardChangeLogService.TARGET_ENUM_VALUE,
                value.getId(),
                StandardChangeLogService.ACTION_CREATE,
                null,
                changeLogService.snapshot(value));
        return value;
    }

    @Override
    public EnumValue updateValue(Long id, EnumValue value) {
        EnumValue existing = getValueById(id);
        EnumDict enumDict = getById(existing.getEnumId());
        String beforeJson = changeLogService.snapshot(existing);
        value.setId(id);
        value.setEnumId(existing.getEnumId());
        enumDictRepository.updateValue(value);
        changeLogService.recordChange(
                enumDict.getProjectId(),
                StandardChangeLogService.TARGET_ENUM_VALUE,
                id,
                StandardChangeLogService.ACTION_UPDATE,
                beforeJson,
                changeLogService.snapshot(value));
        return value;
    }

    @Override
    public void deleteValue(Long id) {
        EnumValue existing = getValueById(id);
        EnumDict enumDict = getById(existing.getEnumId());
        String beforeJson = changeLogService.snapshot(existing);
        enumDictRepository.deleteValueById(id);
        changeLogService.recordChange(
                enumDict.getProjectId(),
                StandardChangeLogService.TARGET_ENUM_VALUE,
                id,
                StandardChangeLogService.ACTION_DELETE,
                beforeJson,
                null);
    }

    private EnumValue getValueById(Long id) {
        EnumValue value = enumDictRepository.findValueById(id)
                .orElseThrow(() -> new BizException("枚举值不存在: " + id));
        EnumDict enumDict = getById(value.getEnumId());
        ProjectAccessGuard.requireProjectAccess(enumDict.getProjectId());
        return value;
    }
}
