package com.dataspec.fieldconflict.service;

import com.dataspec.fieldconflict.model.FieldConflictReport;

public interface FieldConflictService {
    FieldConflictReport report(Long projectId);
}
