package com.dataspec.enumdict.service.impl;

import com.dataspec.common.exception.BizException;
import com.dataspec.enumdict.entity.EnumDict;
import com.dataspec.enumdict.entity.EnumValue;
import com.dataspec.enumdict.repository.EnumDictRepository;
import com.dataspec.enumdict.service.EnumDictService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EnumDictServiceImpl implements EnumDictService {

    private final EnumDictRepository enumDictRepository;

    @Override
    public List<EnumDict> listByProject(Long projectId) {
        return enumDictRepository.findDictsByProjectId(projectId);
    }

    @Override
    public EnumDict getById(Long id) {
        return enumDictRepository.findDictById(id)
                .orElseThrow(() -> new BizException("枚举字典不存在: " + id));
    }

    @Override
    public EnumDict create(EnumDict enumDict) {
        if (enumDictRepository.existsDictByCodeInProject(enumDict.getCode(), enumDict.getProjectId())) {
            throw new BizException("枚举编码已存在: " + enumDict.getCode());
        }
        enumDict.setValueType(enumDict.getValueType() != null ? enumDict.getValueType() : "integer");
        enumDictRepository.insertDict(enumDict);
        return enumDict;
    }

    @Override
    public EnumDict update(Long id, EnumDict enumDict) {
        EnumDict existing = getById(id);
        existing.setName(enumDict.getName());
        existing.setCode(enumDict.getCode());
        existing.setDescription(enumDict.getDescription());
        existing.setValueType(enumDict.getValueType());
        enumDictRepository.updateDict(existing);
        return existing;
    }

    @Override
    @Transactional
    public void delete(Long id) {
        getById(id);
        enumDictRepository.deleteValuesByEnumId(id);
        enumDictRepository.deleteDictById(id);
    }

    @Override
    public List<EnumValue> listValues(Long enumId) {
        return enumDictRepository.findValuesByEnumId(enumId);
    }

    @Override
    public EnumValue createValue(EnumValue value) {
        enumDictRepository.insertValue(value);
        return value;
    }

    @Override
    public EnumValue updateValue(Long id, EnumValue value) {
        value.setId(id);
        enumDictRepository.updateValue(value);
        return value;
    }

    @Override
    public void deleteValue(Long id) {
        enumDictRepository.deleteValueById(id);
    }
}
