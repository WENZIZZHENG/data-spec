package com.dataspec.enumdict;

import com.dataspec.changelog.service.StandardChangeLogService;
import com.dataspec.enumdict.entity.EnumDict;
import com.dataspec.enumdict.repository.EnumDictRepository;
import com.dataspec.enumdict.service.impl.EnumDictServiceImpl;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.*;

/**
 * 枚举字典服务变更记录测试。
 */
class EnumDictServiceImplTest {

    @Test
    void create_recordsChangeLog() {
        EnumDictRepository repository = mock(EnumDictRepository.class);
        StandardChangeLogService changeLogService = mock(StandardChangeLogService.class);
        when(repository.existsDictByCodeInProject("order_status", 1L)).thenReturn(false);
        when(changeLogService.snapshot(any(EnumDict.class))).thenReturn("after-json");
        doAnswer(invocation -> {
            EnumDict dict = invocation.getArgument(0);
            dict.setId(21L);
            return 1;
        }).when(repository).insertDict(any(EnumDict.class));
        EnumDictServiceImpl service = new EnumDictServiceImpl(repository, changeLogService);

        EnumDict dict = new EnumDict();
        dict.setProjectId(1L);
        dict.setCode("order_status");
        dict.setName("订单状态");

        service.create(dict);

        verify(changeLogService).recordChange(
                1L,
                "enum_dict",
                21L,
                "create",
                null,
                "after-json");
    }
}
