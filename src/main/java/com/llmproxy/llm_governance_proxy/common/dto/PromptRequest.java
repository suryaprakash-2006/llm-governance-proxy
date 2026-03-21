package com.llmproxy.llm_governance_proxy.common.dto;

public class PromptRequest {

    private String userId;
    private String prompt;

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }
}