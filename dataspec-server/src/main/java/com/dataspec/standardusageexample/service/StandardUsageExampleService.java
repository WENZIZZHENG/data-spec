package com.dataspec.standardusageexample.service;

import com.dataspec.common.result.PageResult;
import com.dataspec.standardusageexample.entity.StandardUsageExample;
import com.dataspec.standardusageexample.model.StandardUsageExampleSaveReq;

import java.util.List;

public interface StandardUsageExampleService {

    PageResult<StandardUsageExample> page(Long projectId,
                                          String scope,
                                          String exampleType,
                                          String status,
                                          String query,
                                          int current,
                                          int size);

    StandardUsageExample create(StandardUsageExampleSaveReq req);

    StandardUsageExample update(Long id, StandardUsageExampleSaveReq req);

    void delete(Long projectId, Long id);

    List<StandardUsageExample> selectForAiContext(Long projectId, List<Long> fieldIds, String query, int limit);
}
