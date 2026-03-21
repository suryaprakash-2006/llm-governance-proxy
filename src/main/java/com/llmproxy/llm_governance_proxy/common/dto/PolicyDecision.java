package com.llmproxy.llm_governance_proxy.common.dto;

public class PolicyDecision {

    private String action;
    private String modifiedPrompt;

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getModifiedPrompt() {
        return modifiedPrompt;
    }

    public void setModifiedPrompt(String modifiedPrompt) {
        this.modifiedPrompt = modifiedPrompt;
    }
}