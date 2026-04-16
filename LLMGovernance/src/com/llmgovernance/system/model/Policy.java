package com.llmgovernance.system.model;

/**
 * Policy model representing a governance rule in the POLICIES table.
 */
public class Policy {

    private int id;
    private String keyword;
    private String action;  // Block, Allow, or Mask
    private String description;
    private String createdAt;

    // ── Constructors ──────────────────────────────────────────────────────────

    public Policy() {}

    public Policy(String keyword, String action, String description) {
        this.keyword = keyword;
        this.action = action;
        this.description = description;
    }

    // ── Getters & Setters ─────────────────────────────────────────────────────

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "Policy{" +
                "id=" + id +
                ", keyword='" + keyword + '\'' +
                ", action='" + action + '\'' +
                ", createdAt='" + createdAt + '\'' +
                '}';
    }
}
