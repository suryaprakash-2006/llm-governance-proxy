package com.llmproxy.common.dto;

import lombok.Data;
import java.util.List;

@Data
public class DetectionResult {

    private List<String> detectedTypes;

    private String action;          // BLOCK / MASK / ALLOW

    private String maskedPrompt;    // modified text
}