package com.dataspec.projectbackup.model;

import com.dataspec.enumdict.entity.EnumDict;
import com.dataspec.enumdict.entity.EnumValue;

import java.util.List;

public record EnumDictBackup(
        EnumDict dict,
        List<EnumValue> values
) {
}
