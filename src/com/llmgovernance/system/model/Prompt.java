package com.llmgovernance.system.model;

/**
 * Model class representing a single prompt/operation record.
 * Stores original, filtered, compressed text and hash values.
 */
public class Prompt {

    private int id;
    private String originalText;
    private String filteredText;
    private String compressedText;
    private String originalHash;
    private String decompressedHash;
    private String timestamp;

    // ── Constructors ──────────────────────────────────────────────────────────

    public Prompt() {}

    public Prompt(String originalText, String filteredText,
                  String compressedText, String originalHash,
                  String decompressedHash, String timestamp) {
        this.originalText    = originalText;
        this.filteredText    = filteredText;
        this.compressedText  = compressedText;
        this.originalHash    = originalHash;
        this.decompressedHash = decompressedHash;
        this.timestamp       = timestamp;
    }

    // ── Getters & Setters ─────────────────────────────────────────────────────

    public int getId()                         { return id; }
    public void setId(int id)                  { this.id = id; }

    public String getOriginalText()            { return originalText; }
    public void setOriginalText(String t)      { this.originalText = t; }

    public String getFilteredText()            { return filteredText; }
    public void setFilteredText(String t)      { this.filteredText = t; }

    public String getCompressedText()          { return compressedText; }
    public void setCompressedText(String t)    { this.compressedText = t; }

    public String getOriginalHash()            { return originalHash; }
    public void setOriginalHash(String h)      { this.originalHash = h; }

    public String getDecompressedHash()        { return decompressedHash; }
    public void setDecompressedHash(String h)  { this.decompressedHash = h; }

    public String getTimestamp()               { return timestamp; }
    public void setTimestamp(String t)         { this.timestamp = t; }

    @Override
    public String toString() {
        return "Prompt{id=" + id + ", timestamp=" + timestamp + "}";
    }
}
