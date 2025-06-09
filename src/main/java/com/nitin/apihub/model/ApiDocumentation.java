package com.nitin.apihub.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "api_documentation")
public class ApiDocumentation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "API name is required")
    @Column(nullable = false)
    private String name;    // Name of the API

    @Column(length = 1000)
    private String description;     // API Description

    @NotBlank(message = "Version is required")
    @Column(nullable = false)
    private String version;     // Current Version

    @NotNull(message = "Source type is required")
    @Enumerated(EnumType.STRING)
    private SourceType sourceType;

    @Column(name = "source_url", length = 2000)
    private String sourceUrl;

    @Lob
    @Column(name = "swagger_content")
    private String swaggerContent;      // Current Content

    @Column(name = "content_hash")
    private String contentHash;         // Hash of current content

    @Column(name = "created_at")
    private LocalDateTime createdAt;    // When the current version was created

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;    // When the current version was last updated

    @Column(name = "auto_version_detection")
    private Boolean autoVersionDetection = true;

    @Column(name = "created_by")
    private String createdBy;           // User who created the version

    @Column(name = "updated_by")
    private String updatedBy;           // User who created the version

    private boolean active = true;

    public enum SourceType {
        FILE_UPLOAD, EXTERNAL_URL
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Constructors
    public ApiDocumentation() {
        this.autoVersionDetection = true;
        this.versionHistory = new ArrayList<>();
        this.notifications = new ArrayList<>();
    }

    public ApiDocumentation(String name, String description, String version,
                            SourceType sourceType, String sourceUrl, String swaggerContent) {
        this.name = name;
        this.description = description;
        this.version = version;
        this.sourceType = sourceType;
        this.sourceUrl = sourceUrl;
        this.swaggerContent = swaggerContent;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }


    // Relationships
    @OneToMany(mappedBy = "apiDocumentation", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @OrderBy("createdDate DESC")
    private List<ApiVersion> versionHistory = new ArrayList<>();

    @OneToMany(mappedBy = "apiDocumentation", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ApiChangeNotification> notifications = new ArrayList<>();

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }

    public SourceType getSourceType() { return sourceType; }
    public void setSourceType(SourceType sourceType) { this.sourceType = sourceType; }

    public String getSourceUrl() { return sourceUrl; }
    public void setSourceUrl(String sourceUrl) { this.sourceUrl = sourceUrl; }

    public String getSwaggerContent() { return swaggerContent; }
    public void setSwaggerContent(String swaggerContent) { this.swaggerContent = swaggerContent; }

    public String getContentHash() { return contentHash; }
    public void setContentHash(String contentHash) { this.contentHash = contentHash; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public String getUpdatedBy() { return updatedBy;}
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy;}

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public Boolean getAutoVersionDetection() { return autoVersionDetection; }
    public void setAutoVersionDetection(Boolean autoVersionDetection) { this.autoVersionDetection = autoVersionDetection;}

    public List<ApiVersion> getVersionHistory() { return versionHistory;}
    public void setVersionHistory(List<ApiVersion> versionHistory) { this.versionHistory = versionHistory; }

    public List<ApiChangeNotification> getNotifications() { return notifications; }
    public void setNotifications(List<ApiChangeNotification> notifications) { this.notifications = notifications; }
}

