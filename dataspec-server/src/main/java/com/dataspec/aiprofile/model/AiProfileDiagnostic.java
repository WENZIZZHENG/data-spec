package com.dataspec.aiprofile.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AI profile 可用性诊断项。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiProfileDiagnostic {

    private String code;
    private String status;
    private String message;
    private String nextAction;
}
