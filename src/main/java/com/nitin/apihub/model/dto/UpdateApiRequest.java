package com.nitin.apihub.model.dto;

public class UpdateApiRequest {
    // Request DTO
    private String name;
    private String description;
    private String version;
    private String url;

    // Getters and setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }

    public String getUrl() { return url;}
    public void setUrl(String url) { this.url = url;}
}
