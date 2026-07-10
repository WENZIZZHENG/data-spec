package com.dataspec.standardref;

import com.dataspec.standardref.controller.StandardReferenceController;
import com.dataspec.standardref.model.StandardReferenceResolveRequest;
import com.dataspec.standardref.model.StandardReferenceResolveResponse;
import com.dataspec.standardref.model.StandardReferenceResolutionResult;
import com.dataspec.standardref.model.StandardReferenceResolutionStatus;
import com.dataspec.standardref.model.StandardReferenceConfidence;
import com.dataspec.standardref.model.StandardReferenceType;
import com.dataspec.standardref.service.StandardReferenceResolutionService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StandardReferenceControllerTest {

    @Test
    void resolve_forwardsReadOnlyRequestToService() {
        StandardReferenceResolutionService service = mock(StandardReferenceResolutionService.class);
        StandardReferenceResolveRequest request = new StandardReferenceResolveRequest(
                1L,
                StandardReferenceType.FIELD,
                List.of("mobile_no"));
        StandardReferenceResolveResponse result = new StandardReferenceResolveResponse(
                StandardReferenceResolveResponse.KIND,
                1,
                1L,
                List.of(new StandardReferenceResolutionResult(
                        "mobile_no",
                        StandardReferenceType.FIELD,
                        StandardReferenceResolutionStatus.CURRENT,
                        "field:1:10",
                        "field:1:10",
                        10L,
                        "mobile_no",
                        null,
                        "enabled",
                        null,
                        StandardReferenceConfidence.HIGH,
                        List.of("dataspec://fields/10"),
                        List.of())),
                List.of());
        when(service.resolve(request)).thenReturn(result);
        StandardReferenceController controller = new StandardReferenceController(service);

        var response = controller.resolve(request);

        assertEquals("dataspec-standard-reference-resolution", response.getData().kind());
        assertEquals("field:1:10", response.getData().results().getFirst().canonicalRef());
        verify(service).resolve(request);
    }
}
