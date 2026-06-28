package com.dataspec.starterkit;

import com.dataspec.starterkit.controller.StarterKitController;
import com.dataspec.starterkit.model.StarterKitApplyCounts;
import com.dataspec.starterkit.model.StarterKitApplyReq;
import com.dataspec.starterkit.model.StarterKitApplyResult;
import com.dataspec.starterkit.service.BuiltInDomainStarterKits;
import com.dataspec.starterkit.service.StarterKitService;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StarterKitControllerTest {

    @Test
    void listKits_returnsCatalogFromService() {
        StarterKitService service = mock(StarterKitService.class);
        when(service.listKits()).thenReturn(BuiltInDomainStarterKits.list());
        StarterKitController controller = new StarterKitController(service);

        var response = controller.listKits();

        assertEquals(200, response.getCode());
        assertEquals(BuiltInDomainStarterKits.list().size(), response.getData().size());
    }

    @Test
    void apply_delegatesToService() {
        StarterKitService service = mock(StarterKitService.class);
        StarterKitApplyResult result = new StarterKitApplyResult(
                1L,
                "user_account",
                "用户账号 Starter Kit",
                BuiltInDomainStarterKits.VERSION,
                StarterKitApplyCounts.empty(),
                StarterKitApplyCounts.empty(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                LocalDateTime.now());
        when(service.applyKit(1L, "user_account", BuiltInDomainStarterKits.VERSION)).thenReturn(result);
        StarterKitController controller = new StarterKitController(service);

        var response = controller.apply(new StarterKitApplyReq(1L, "user_account", BuiltInDomainStarterKits.VERSION));

        assertEquals(200, response.getCode());
        assertEquals("user_account", response.getData().kitKey());
        verify(service).applyKit(1L, "user_account", BuiltInDomainStarterKits.VERSION);
    }
}
