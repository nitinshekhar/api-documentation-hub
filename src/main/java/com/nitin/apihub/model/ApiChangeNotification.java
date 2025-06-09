package com.nitin.apihub.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "api_change_notifications")
public class ApiChangeNotification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "api_documentation_id")
    private ApiDocumentation apiDocumentation;

    @Column(name = "change_type")
    @Enumerated(EnumType.STRING)
    private ChangeType changeType;

    @Column(name = "change_description", length = 2000)
    private String changeDescription;

    @Column(name = "old_version")
    private String oldVersion;

    @Column(name = "new_version")
    private String newVersion;

    @Column(name = "created_date")
    private LocalDateTime createdDate;

    @Column(name = "is_read")
    private Boolean isRead = false;

    // Default constructor (required by JPA)
    public ApiChangeNotification() {
        this.createdDate = LocalDateTime.now();
        this.isRead = false;
    }

    // Basic constructor
    public ApiChangeNotification(ApiDocumentation apiDocumentation, ChangeType changeType,
                                 String changeDescription) {
        this();
        this.apiDocumentation = apiDocumentation;
        this.changeType = changeType;
        this.changeDescription = changeDescription;
    }

    // Constructor with version information
    public ApiChangeNotification(ApiDocumentation apiDocumentation, ChangeType changeType,
                                 String changeDescription, String oldVersion, String newVersion) {
        this(apiDocumentation, changeType, changeDescription);
        this.oldVersion = oldVersion;
        this.newVersion = newVersion;
    }

    // Static factory methods for common notification types
    public static ApiChangeNotification newVersionNotification(ApiDocumentation api,
                                                               String oldVersion, String newVersion,
                                                               String changeSummary) {
        return new ApiChangeNotification(
                api,
                ChangeType.NEW_VERSION,
                changeSummary,
                oldVersion,
                newVersion
        );
    }

    public static ApiChangeNotification endpointAddedNotification(ApiDocumentation api,
                                                                  String endpointPath) {
        return new ApiChangeNotification(
                api,
                ChangeType.ENDPOINT_ADDED,
                "New endpoint added: " + endpointPath
        );
    }

    public static ApiChangeNotification endpointRemovedNotification(ApiDocumentation api,
                                                                    String endpointPath) {
        return new ApiChangeNotification(
                api,
                ChangeType.ENDPOINT_REMOVED,
                "Endpoint removed: " + endpointPath
        );
    }

    public static ApiChangeNotification breakingChangeNotification(ApiDocumentation api,
                                                                   String changeDescription,
                                                                   String oldVersion, String newVersion) {
        return new ApiChangeNotification(
                api,
                ChangeType.BREAKING_CHANGE,
                changeDescription,
                oldVersion,
                newVersion
        );
    }

    // Getters and setters...
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public ApiDocumentation getApiDocumentation() { return apiDocumentation; }
    public void setApiDocumentation(ApiDocumentation apiDocumentation) { this.apiDocumentation = apiDocumentation; }

    public ChangeType getChangeType() { return changeType;}
    public void setChangeType(ChangeType changeType) { this.changeType = changeType; }

    public String getChangeDescription() { return changeDescription; }
    public void setChangeDescription(String changeDescription) { this.changeDescription = changeDescription; }

    public String getOldVersion() { return oldVersion; }
    public void setOldVersion(String oldVersion) { this.oldVersion = oldVersion; }

    public String getNewVersion() { return newVersion; }
    public void setNewVersion(String newVersion) { this.newVersion = newVersion; }

    public LocalDateTime getCreatedDate() { return createdDate; }
    public void setCreatedDate(LocalDateTime createdDate) { this.createdDate = createdDate; }

    public Boolean getRead() { return isRead; }
    public void setRead(Boolean read) { isRead = read; }
}