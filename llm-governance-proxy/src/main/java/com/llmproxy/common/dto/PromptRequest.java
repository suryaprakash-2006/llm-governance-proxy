package com.llmproxy.common.dto;

import lombok.Data;

@Data
public class PromptRequest {
    private String userId;
    private String prompt;
}