package com.dataspec.rule;

import com.dataspec.changelog.service.StandardChangeLogService;
import com.dataspec.rule.entity.RuleConfig;
import com.dataspec.rule.repository.RuleConfigRepository;
import com.dataspec.rule.service.impl.RuleConfigServiceImpl;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.mockito.Mockito.*;

/**
 * 规则配置服务变更记录测试。
 */
class RuleConfigServiceImplTest {

    @Test
    void toggle_recordsBeforeAndAfterChangeLog() {
        RuleConfigRepository repository = mock(RuleConfigRepository.class);
        StandardChangeLogService changeLogService = mock(StandardChangeLogService.class);
        RuleConfig existing = new RuleConfig();
        existing.setId(7L);
        existing.setProjectId(1L);
        existing.setRuleCode("field_naming_snake_case");
        existing.setEnabled(true);
        when(repository.findById(7L)).thenReturn(Optional.of(existing));
        when(changeLogService.snapshot(any(RuleConfig.class))).thenAnswer(invocation -> {
            RuleConfig rule = invocation.getArgument(0);
            return String.valueOf(rule.getEnabled());
        });
        RuleConfigServiceImpl service = new RuleConfigServiceImpl(repository, changeLogService);

        service.toggle(7L, false);

        verify(changeLogService).recordChange(
                1L,
                "rule_config",
                7L,
                "toggle",
                "true",
                "false");
    }
}
