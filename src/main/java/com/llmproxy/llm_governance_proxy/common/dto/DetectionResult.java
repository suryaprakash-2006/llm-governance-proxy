package com.llmproxy.llm_governance_proxy.common.dto;

import java.util.List;

public class DetectionResult {

    private List<String> detectedTypes;

    public List<String> getDetectedTypes() {
        return detectedTypes;
    }

    public void setDetectedTypes(List<String> detectedTypes) {
        this.detectedTypes = detectedTypes;
    }
}