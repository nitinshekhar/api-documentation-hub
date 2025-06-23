package com.nitin.apihub.service;

import com.nitin.apihub.model.ApiChangeNotification;
import com.nitin.apihub.model.ApiDocumentation;
import com.nitin.apihub.model.ApiVersion;
import com.nitin.apihub.model.ChangeType;
import com.nitin.apihub.repository.ApiChangeNotificationRepository;
import com.nitin.apihub.repository.ApiDocumentationRepository;
import com.nitin.apihub.repository.ApiVersionRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class ApiDocumentationService {

    @Autowired
    private ApiDocumentationRepository repository;

    @Autowired
    private ApiVersionRepository apiVersionRepository;

    @Autowired
    private ApiChangeNotificationRepository apiChangeNotificationRepository;

    private final ObjectMapper jsonMapper = new ObjectMapper();
    private final ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public List<ApiDocumentation> getAllActiveApis() {
        return repository.findActiveApisSortedByName();
    }

    public Optional<ApiDocumentation> getApiById(Long id) {
        return repository.findById(id);
    }

    public ApiDocumentation findById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("API not found with id: " + id));
    }

    public void save(ApiDocumentation api) {
        repository.save(api);
    }

    public List<ApiDocumentation> searchApis(String searchTerm) {
        return repository.findByNameContainingIgnoreCaseAndActiveTrue(searchTerm);
    }

    public void saveFromFile(String name, String description, String createdBy, MultipartFile file) throws IOException {
        String content = new String(file.getBytes());
        String normalizedContent = normalizeSwaggerContent(content);
        String version = extractVersionFromSpecs(content);
        ApiDocumentation api = new ApiDocumentation(name, description, version,
                ApiDocumentation.SourceType.FILE_UPLOAD, null, normalizedContent);

        api.setContentHash(calculateHash(normalizedContent));
        api.setCreatedBy(createdBy);
        api.setUpdatedBy(createdBy);
        api.setCreatedAt(LocalDateTime.now());
        api.setUpdatedAt(LocalDateTime.now());

        repository.save(api);
    }

    public void saveFromUrl(String name, String description, String createdBy, String url) throws IOException, InterruptedException {
        String content = fetchContentFromUrl(url);
        String normalizedContent = normalizeSwaggerContent(content);
        String version = extractVersionFromSpecs(content);
        ApiDocumentation api = new ApiDocumentation(name, description, version,
                ApiDocumentation.SourceType.EXTERNAL_URL, url, normalizedContent);

        api.setContentHash(calculateHash(normalizedContent));
        api.setCreatedBy(createdBy);
        api.setUpdatedBy(createdBy);
        api.setCreatedAt(LocalDateTime.now());
        api.setUpdatedAt(LocalDateTime.now());

        repository.save(api);
    }

    // Update API with new file upload
    public void updateFromFile(Long id, String name, String description, String updatedBy, MultipartFile file) {
        Optional<ApiDocumentation> apiOpt = getApiById(id);
        if (apiOpt.isEmpty()) {
            throw new RuntimeException("API not found with id: " + id);
        }

        ApiDocumentation api = apiOpt.get();

        // Verify this is a file-based API
        if (api.getSourceType() != ApiDocumentation.SourceType.FILE_UPLOAD) {
            throw new IllegalArgumentException("This API is not file-based and cannot be updated with a file upload");
        }

        try {
            // Read and validate the new file content
            String content = new String(file.getBytes(), StandardCharsets.UTF_8);
            String normalizedContent = normalizeSwaggerContent(content);
            String version = extractVersionFromSpecs(content);

            // Detect changes
            String changeSummary = detectChanges(api.getSwaggerContent(), content);

            // Create new version before updating
            createNewVersion(api, changeSummary);

            // Update the API
            api.setName(name);
            api.setDescription(description);
            api.setVersion(version);
            api.setSwaggerContent(normalizedContent);
            api.setContentHash(calculateHash(normalizedContent));
            api.setUpdatedBy(updatedBy);
            api.setUpdatedAt(LocalDateTime.now());

            // Save the changes
            ApiDocumentation savedApi = repository.save(api);

            // Create change notification
            createChangeNotification(savedApi, "API updated with new file upload");

        } catch (IOException e) {
            throw new RuntimeException("Error reading uploaded file: " + e.getMessage());
        }
    }
    // Update basic API information
    public void updateApiWithUrlRefresh(Long id, String name, String description, String updatedBy, String sourceUrl) {
        Optional<ApiDocumentation> apiOpt = getApiById(id);

        if (apiOpt.isEmpty()) {
            throw new RuntimeException("API not found with id: " + id);
        }

        ApiDocumentation api = apiOpt.get();
        // Verify this is a file-based API
        if (api.getSourceType() != ApiDocumentation.SourceType.EXTERNAL_URL) {
            throw new IllegalArgumentException("This API is not URL-based and cannot be updated with a URL upload");
        }

        try {
            String content = fetchContentFromUrl(sourceUrl);
            String normalizedContent = normalizeSwaggerContent(content);
            String version = extractVersionFromSpecs(content);

            // Detect changes
            String changeSummary = detectChanges(api.getSwaggerContent(), content);

            // Create new version before updating
            createNewVersion(api, changeSummary);

            // Update the API
            api.setName(name);
            api.setDescription(description);
            api.setVersion(version);
            api.setSwaggerContent(normalizedContent);
            api.setContentHash(calculateHash(normalizedContent));
            api.setUpdatedBy(updatedBy);
            api.setUpdatedAt(LocalDateTime.now());

            // Save the changes
            ApiDocumentation savedApi = repository.save(api);

            // Create change notification
            createChangeNotification(savedApi, "API updated with URL refresh manually");

        } catch (Exception e) {
            throw new RuntimeException("Error reading content from URL : " + e.getMessage());
        }
    }

    public void deleteApi(Long id) {
        Optional<ApiDocumentation> api = repository.findById(id);
        if (api.isPresent()) {
            ApiDocumentation apiDoc = api.get();
            apiDoc.setActive(false);
            repository.save(apiDoc);
        }
    }

    public void refreshFromUrl(Long id) throws IOException, InterruptedException {
        Optional<ApiDocumentation> apiOpt = repository.findById(id);
        if (apiOpt.isPresent()) {
            ApiDocumentation api = apiOpt.get();
            if (api.getSourceType() == ApiDocumentation.SourceType.EXTERNAL_URL && api.getSourceUrl() != null) {
                String content = fetchContentFromUrl(api.getSourceUrl());
                String normalizedContent = normalizeSwaggerContent(content);
                String version = extractVersionFromSpecs(content);

                // Detect changes
                String changeSummary = detectChanges(api.getSwaggerContent(), content);

                // Create new version before updating
                createNewVersion(api, changeSummary);

                // Update the API
                api.setVersion(version);
                api.setSwaggerContent(normalizedContent);
                api.setContentHash(calculateHash(normalizedContent));
                api.setUpdatedBy("SYSTEM");
                api.setUpdatedAt(LocalDateTime.now());

                // Save the changes
                ApiDocumentation savedApi = repository.save(api);

                // Create change notification
                createChangeNotification(savedApi, "API updated with URL refresh");
            }
        }
    }

    private String fetchContentFromUrl(String url) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .timeout(Duration.ofSeconds(30))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new IOException("Failed to fetch content from URL: " + url +
                    " (Status: " + response.statusCode() + ")");
        }

        return response.body();
    }

    private String normalizeSwaggerContent(String content) {
        try {
            // Try to parse as JSON first
            JsonNode jsonNode = jsonMapper.readTree(content);
            return jsonMapper.writeValueAsString(jsonNode);
        } catch (Exception e) {
            try {
                // If JSON parsing fails, try YAML
                JsonNode yamlNode = yamlMapper.readTree(content);
                return jsonMapper.writeValueAsString(yamlNode);
            } catch (Exception yamlException) {
                // If both fail, return original content
                return content;
            }
        }
    }
    public void updateApiVersion(Long apiId, String newContent, String newVersion) {
        ApiDocumentation api = findById(apiId);
        String newContentHash = calculateHash(newContent);

        // Check if content actually changed
        if (api.getContentHash() != null && api.getContentHash().equals(newContentHash)) {
            api.setCreatedAt(LocalDateTime.now());
            save(api);
            return; // No changes detected
        }

        // Save current version to history before updating
        if (api.getSwaggerContent() != null) {
            // Detect changes
            String changeSummary = detectChanges(api.getSwaggerContent(), newContent);

            // Create new version before updating
            createNewVersion(api, changeSummary);

            // Create notification
            createChangeNotification(api, api.getVersion(), newVersion, changeSummary);
        }

        // Update current version in main entity
        api.setVersion(newVersion);
        api.setSwaggerContent(newContent);
        api.setContentHash(newContentHash);
        api.setUpdatedAt(LocalDateTime.now());
        api.setCreatedAt(LocalDateTime.now());

        save(api);
    }

    public List<ApiVersion> getVersionHistory(Long apiId) {
        return apiVersionRepository.findByApiDocumentationIdOrderByCreatedDateDesc(apiId);
    }

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

    private String detectChanges(String oldContent, String newContent) {
        List<String> changes = new ArrayList<>();

        try {
            JsonNode oldSpec = jsonMapper.readTree(oldContent);
            JsonNode newSpec = jsonMapper.readTree(newContent);

            // Compare version info
            JsonNode oldInfo = oldSpec.get("info");
            JsonNode newInfo = newSpec.get("info");
            if (oldInfo != null && newInfo != null) {
                JsonNode oldVersion = oldInfo.get("version");
                JsonNode newVersion = newInfo.get("version");
                if (oldVersion != null && newVersion != null &&
                        !oldVersion.asText().equals(newVersion.asText())) {
                    changes.add("Version updated from " + oldVersion.asText() + " to " + newVersion.asText());
                }
            }

            // Compare paths
            JsonNode oldPaths = oldSpec.get("paths");
            JsonNode newPaths = newSpec.get("paths");

            if (oldPaths != null && newPaths != null) {
                Set<String> oldPathSet = new HashSet<>();
                Set<String> newPathSet = new HashSet<>();

                oldPaths.fieldNames().forEachRemaining(oldPathSet::add);
                newPaths.fieldNames().forEachRemaining(newPathSet::add);

                // Detect new endpoints
                for (String path : newPathSet) {
                    if (!oldPathSet.contains(path)) {
                        changes.add("New endpoint added: " + path);
                    }
                }

                // Detect removed endpoints
                for (String path : oldPathSet) {
                    if (!newPathSet.contains(path)) {
                        changes.add("Endpoint removed: " + path);
                    }
                }

                // Detect modified endpoints
                for (String path : oldPathSet) {
                    if (newPathSet.contains(path)) {
                        JsonNode oldPath = oldPaths.get(path);
                        JsonNode newPath = newPaths.get(path);
                        if (!oldPath.equals(newPath)) {
                            changes.add("Endpoint modified: " + path);
                        }
                    }
                }
            }

            // Compare components/definitions (schemas)
            JsonNode oldComponents = oldSpec.get("components");
            JsonNode newComponents = newSpec.get("components");
            JsonNode oldDefinitions = oldSpec.get("definitions");
            JsonNode newDefinitions = newSpec.get("definitions");

            if ((oldComponents != null && newComponents != null) ||
                    (oldDefinitions != null && newDefinitions != null)) {
                if (!Objects.equals(oldComponents, newComponents) ||
                        !Objects.equals(oldDefinitions, newDefinitions)) {
                    changes.add("Schema definitions modified");
                }
            }

        } catch (Exception e) {
            changes.add("Content updated - detailed analysis failed: " + e.getMessage());
        }

        return changes.isEmpty() ? "Content updated" : String.join("; ", changes);
    }

    private void createChangeNotification(ApiDocumentation api, String oldVersion,
                                          String newVersion, String changeSummary) {
        ApiChangeNotification notification = new ApiChangeNotification();
        notification.setApiDocumentation(api);
        notification.setChangeType(ChangeType.NEW_VERSION);
        notification.setChangeDescription(changeSummary);
        notification.setOldVersion(oldVersion);
        notification.setNewVersion(newVersion);
        notification.setCreatedDate(LocalDateTime.now());
        notification.setRead(false);

        apiChangeNotificationRepository.save(notification);
    }

    // New methods for notification management
    public List<ApiChangeNotification> getUnreadNotifications() {
        return apiChangeNotificationRepository.findByIsReadOrderByCreatedDateDesc(false);
    }

    public void markNotificationAsRead(Long notificationId) {
        apiChangeNotificationRepository.findById(notificationId).ifPresent(notification -> {
            notification.setRead(true);
            apiChangeNotificationRepository.save(notification);
        });
    }

    // Method to get APIs that need checking (for scheduled tasks)
    public List<ApiDocumentation> getApisForAutoCheck() {
        return repository.findAll().stream()
                .filter(api -> api.isActive() &&
                        api.getAutoVersionDetection() != null &&
                        api.getAutoVersionDetection() &&
                        api.getSourceType() == ApiDocumentation.SourceType.EXTERNAL_URL &&
                        api.getSourceUrl() != null)
                .collect(Collectors.toList());
    }

    // Method for scheduled task update version check
    public void checkForUpdates(Long apiId) throws IOException, InterruptedException {
        ApiDocumentation api = findById(apiId);
        if (api.getSourceType() == ApiDocumentation.SourceType.EXTERNAL_URL &&
                api.getSourceUrl() != null) {

            String latestContent = fetchContentFromUrl(api.getSourceUrl());
            String normalizedContent = normalizeSwaggerContent(latestContent);
            String newVersion = extractVersionFromSpecs(normalizedContent);

            updateApiVersion(apiId, normalizedContent, newVersion);
        } else {
            throw new IllegalArgumentException("API does not have a valid external URL for checking updates");
        }
    }
    /**
     * Cleanup old notifications based on retention policy
     */
    public void cleanupOldNotifications() {
        try {
            // Get retention period from properties (default 30 days)
            int retentionDays = 30; // You can make this configurable
            LocalDateTime cutoffDate = LocalDateTime.now().minusDays(retentionDays);

            // Find old notifications
            List<ApiChangeNotification> oldNotifications = apiChangeNotificationRepository.findAll()
                    .stream()
                    .filter(notification -> notification.getCreatedDate().isBefore(cutoffDate))
                    .filter(ApiChangeNotification::getRead) // Only delete read notifications
                    .collect(Collectors.toList());

            // Delete old notifications
            if (!oldNotifications.isEmpty()) {
                apiChangeNotificationRepository.deleteAll(oldNotifications);
                System.out.println("Cleaned up " + oldNotifications.size() + " old notifications");
            }

        } catch (Exception e) {
            System.err.println("Error during notification cleanup: " + e.getMessage());
        }
    }

    /**
     * Get notification statistics
     */
    public Map<String, Long> getNotificationStats() {
        Map<String, Long> stats = new HashMap<>();
        try {
            List<ApiChangeNotification> allNotifications = apiChangeNotificationRepository.findAll();

            stats.put("total", (long) allNotifications.size());
            stats.put("unread", allNotifications.stream().filter(n -> !n.getRead()).count());
            stats.put("read", allNotifications.stream().filter(ApiChangeNotification::getRead).count());

            // Count by change type
            Map<ChangeType, Long> byType = allNotifications.stream()
                    .collect(Collectors.groupingBy(ApiChangeNotification::getChangeType, Collectors.counting()));

            byType.forEach((type, count) -> stats.put(type.name().toLowerCase(), count));

        } catch (Exception e) {
            System.err.println("Error calculating notification stats: " + e.getMessage());
        }
        return stats;
    }

    // Helper method to create a new version before updating
    private void createNewVersion(ApiDocumentation api, String changeSummary) {
        ApiVersion version = new ApiVersion();
        version.setApiDocumentation(api);
        version.setVersionNumber(api.getVersion());
        version.setContent(api.getSwaggerContent());
        version.setCreatedDate(LocalDateTime.now());
        version.setChangeSummary(changeSummary);

        // Save the version (assuming you have an ApiVersionRepository)
        apiVersionRepository.save(version);
    }

    // Helper method to create change notification
    private void createChangeNotification(ApiDocumentation api, String message) {
        ApiChangeNotification notification = new ApiChangeNotification();
        notification.setApiDocumentation(api);
        notification.setChangeDescription(message);
        notification.setCreatedDate(LocalDateTime.now());
        notification.setRead(false);

        // Save the notification (assuming you have an ApiChangeNotificationRepository)
        apiChangeNotificationRepository.save(notification);
    }
    // Extract version from the content
    private String extractVersionFromSpecs(String content) {
        JsonNode rootNode = null;
        try {
            // Try JSON first
            rootNode = jsonMapper.readTree(content);
        } catch (Exception e) {
            try {
                // Try YAML
                rootNode = yamlMapper.readTree(content);
            } catch (Exception yamlException) {

            }
        }
        // Try multiple common version locations
        String[] versionPaths = {
                "/info/version",    // Standard OpenAPI/Swagger location
                "/version",         // Direct version field
                "/api/version",     // Some custom API
                "/spec/version",    // Alternate location
                "/openapi"          // OpenAPI version
        };

        for (String path : versionPaths) {
            JsonNode versionNode = rootNode.at(path);
            if (!versionNode.isMissingNode() && !versionNode.isNull()) {
                String version = versionNode.asText().trim();
                if(!version.isEmpty()) {
                    return version;
                }
            }
        }
        // If not version found
        return "1.0.0";
    }
}
