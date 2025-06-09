package com.nitin.apihub.model;

import jakarta.persistence.*;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Base64;

@Entity
@Table(name = "api_versions")
public class ApiVersion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "api_documentation_id")
    private ApiDocumentation apiDocumentation;

    @Column(name = "version_number")
    private String versionNumber;

    @Lob
    @Column(name = "content")
    private String content;

    @Column(name = "content_hash")
    private String contentHash;

    @Column(name = "created_date")
    private LocalDateTime createdDate;

    @Column(name = "change_summary", length = 1000)
    private String changeSummary;


    // Add this field
    @Column(name = "is_current")
    private Boolean isCurrent = false;

    // Note: No "isCurrent" field needed since current version is in main entity
    public ApiVersion() {
        this.createdDate = LocalDateTime.now();
    }

    // Constructor for creating new version entry
    public ApiVersion(ApiDocumentation apiDocumentation, String versionNumber, String content) {
        this();
        this.apiDocumentation = apiDocumentation;
        this.versionNumber = versionNumber;
        this.content = content;
        this.contentHash = calculateHash(content);
    }

    // Constructor with change summary
    public ApiVersion(ApiDocumentation apiDocumentation, String versionNumber,
                      String content, String changeSummary) {
        this(apiDocumentation, versionNumber, content);
        this.changeSummary = changeSummary;
    }

    // Constructor with specific creation date (useful for migrating existing data)
    public ApiVersion(ApiDocumentation apiDocumentation, String versionNumber,
                      String content, LocalDateTime createdDate) {
        this();
        this.apiDocumentation = apiDocumentation;
        this.versionNumber = versionNumber;
        this.content = content;
        this.contentHash = calculateHash(content);
        this.createdDate = createdDate;
    }

    // Full constructor
    public ApiVersion(ApiDocumentation apiDocumentation, String versionNumber, String content,
                      String changeSummary, LocalDateTime createdDate) {
        this();
        this.apiDocumentation = apiDocumentation;
        this.versionNumber = versionNumber;
        this.content = content;
        this.contentHash = calculateHash(content);
        this.changeSummary = changeSummary;
        this.createdDate = createdDate;
    }

    // Static factory method for creating from current API documentation
    public static ApiVersion fromApiDocumentation(ApiDocumentation api) {
        return new ApiVersion(
                api,
                api.getVersion(),
                api.getSwaggerContent(),
                api.getUpdatedAt() != null ? api.getUpdatedAt() : api.getCreatedAt()
        );
    }

    // Static factory method for creating with change summary
    public static ApiVersion fromApiDocumentationWithChanges(ApiDocumentation api, String changeSummary) {
        return new ApiVersion(
                api,
                api.getVersion(),
                api.getSwaggerContent(),
                changeSummary,
                api.getUpdatedAt() != null ? api.getUpdatedAt() : api.getCreatedAt()
        );
    }

    // Helper method for calculating content hash
    private String calculateHash(String content) {
        if (content == null || content.isEmpty()) {
            return null;
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(content.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Error calculating content hash", e);
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getVersionNumber() { return versionNumber; }
    public void setVersionNumber(String version) { this.versionNumber = version; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getContentHash() { return contentHash; }
    public void setContentHash(String contentHash) { this.contentHash = contentHash; }

    public LocalDateTime getCreatedDate() { return createdDate; }
    public void setCreatedDate(LocalDateTime createdDate) { this.createdDate = createdDate; }

    public String getChangeSummary() { return changeSummary; }
    public void setChangeSummary(String changeSummary) { this.changeSummary = changeSummary; }

    public ApiDocumentation getApiDocumentation() { return apiDocumentation; }
    public void setApiDocumentation(ApiDocumentation apiDocumentation) { this.apiDocumentation = apiDocumentation; }

    public Boolean getCurrent() { return isCurrent; }
    public void setCurrent(Boolean current) { isCurrent = current; }

}
