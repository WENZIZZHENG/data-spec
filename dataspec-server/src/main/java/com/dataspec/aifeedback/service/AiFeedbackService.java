package com.dataspec.aifeedback.service;

import com.dataspec.aifeedback.model.AiFeedbackReport;

public interface AiFeedbackService {

    AiFeedbackReport buildReport(Long projectId);
}
