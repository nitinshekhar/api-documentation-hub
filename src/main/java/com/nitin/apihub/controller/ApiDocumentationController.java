package com.nitin.apihub.controller;

import com.nitin.apihub.model.ApiChangeNotification;
import com.nitin.apihub.model.ApiDocumentation;
import com.nitin.apihub.model.ApiVersion;
import com.nitin.apihub.model.dto.UpdateApiRequest;
import com.nitin.apihub.service.ApiDocumentationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.*;
import java.util.HashMap;
import java.util.stream.Collectors;

@Controller
public class ApiDocumentationController {
    private static final Logger logger = LoggerFactory.getLogger(ApiDocumentationController.class);
    @Autowired
    private ApiDocumentationService apiService;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private final ObjectMapper jsonMapper = new ObjectMapper();

    // List all API's
    @GetMapping("/apis")
    public String home(Model model, @RequestParam(required = false) String search) {
        try {
            List<ApiDocumentation> apis;
            if (search != null && !search.trim().isEmpty()) {
                apis = apiService.searchApis(search.trim());
                model.addAttribute("searchTerm", search.trim());
            } else {
                apis = apiService.getAllActiveApis();
            }
            model.addAttribute("apis", apis);
            return "index";
        } catch (Exception e) {
            logger.error(e.getMessage());
            model.addAttribute("error", "Failed to load APIs: " + e.getMessage());
            model.addAttribute("apis", Collections.emptyList());
            return "index";
        }
    }

    //List Specific API
    @GetMapping("/apis/{id}")
    public String viewApi(@PathVariable Long id, Model model) {
        Optional<ApiDocumentation> apiOpt = apiService.getApiById(id);
        if (apiOpt.isPresent()) {
            model.addAttribute("api", apiOpt.get());
            model.addAttribute("currentVersion", null); // null indicates latest version
            return "api-detail";
        }
        return "redirect:/apis";
    }

    @GetMapping("/add")
    public String showAddApiForm(Model model) {
        return "add-api"; // This should correspond to your add-api.html template
    }

    // Create an API from file
    @PostMapping("/apis/upload-file")
    public String addApiFromFile(@RequestParam String name,
                                 @RequestParam String description,
                                 //@RequestParam String version,
                                 @RequestParam String createdBy,
                                 @RequestParam MultipartFile file,
                                 RedirectAttributes redirectAttributes) {
        try {
            if (file.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Please select a file to upload");
                return "redirect:/add";
            }

            apiService.saveFromFile(name, description, createdBy, file);

            redirectAttributes.addFlashAttribute("success", "API documentation added successfully!");
            return "redirect:/apis";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error adding API: " + e.getMessage());
            return "redirect:/add";
        }
    }

    @PostMapping("/apis/upload-url")
    public String addApiFromUrl(@RequestParam String name,
                                @RequestParam String description,
                                //@RequestParam String version,
                                @RequestParam String createdBy,
                                @RequestParam String url,
                                RedirectAttributes redirectAttributes) {
        try {
            RestTemplate restTemplate = new RestTemplate();
            String content = restTemplate.getForObject(url, String.class);

            if (!content.contains("openapi") && !content.contains("swagger")) {
                redirectAttributes.addFlashAttribute("error", "Invalid OpenAPI/Swagger document");
                return "redirect:/add";
            }

            apiService.saveFromUrl(name, description, createdBy, url);
            redirectAttributes.addFlashAttribute("success", "API documentation added successfully!");
            return "redirect:/apis";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error adding API: " + e.getMessage());
            return "redirect:/add";
        }
    }

    @GetMapping("/apis/edit/{id}")
    public String editApiForm(@PathVariable Long id, Model model){
        try {
            Optional<ApiDocumentation> apiOpt =apiService.getApiById(id);
            if (apiOpt.isPresent()) {
                model.addAttribute("api", apiOpt.get());
                return "edit-api";
            } else {
                return "redirect:/apis";
            }
        } catch (Exception e){
            logger.error(e.getMessage());
            return "redirect:/apis";
        }
    }

    // Handle form submission for editing with file re-upload
    @PostMapping (value = "/apis/edit/{id}/file")
    public String  updateApiWithFile(@PathVariable Long id,
                                     @RequestParam String name,
                                     @RequestParam String description,
                                     //@RequestParam String version,
                                     @RequestParam String updatedBy,
                                     @RequestParam MultipartFile file,
                                     RedirectAttributes redirectAttributes) {
        logger.info("Update API with Upload");
        try {
            if (file != null && !file.isEmpty()){
                apiService.updateFromFile(id, name, description, updatedBy, file);
                redirectAttributes.addFlashAttribute("success", "API updated with file!");
            } else {
                redirectAttributes.addFlashAttribute("error", "No update data provided");
                return "redirect:/apis/edit/" +id;
            }
            return "redirect:/apis/" + id;
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Update Failed : "+e.getMessage());
            return "redirect:/apis/edit/" + id;
        }
    }

    @PostMapping (value = "/apis/edit/{id}/url")
    public String  updateApiWithUrl(@PathVariable Long id,
                                    @RequestParam String name,
                                    @RequestParam String description,
                                    //@RequestParam String version,
                                    @RequestParam String updatedBy,
                                    @RequestParam String url,
                                    RedirectAttributes redirectAttributes) {
        try {
            RestTemplate restTemplate = new RestTemplate();
            String content = restTemplate.getForObject(url, String.class);

            if (!content.contains("openapi") && !content.contains("swagger")) {
                redirectAttributes.addFlashAttribute("error", "Invalid OpenAPI/Swagger document");
                return "redirect:/apis/edit/" +id;
            }
            apiService.updateApiWithUrlRefresh(id, name, description, updatedBy, url);
            redirectAttributes.addFlashAttribute("success", "API updated with file!");

            return "redirect:/apis";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Update Failed : "+e.getMessage());
            return "redirect:/apis/edit/" + id;
        }
    }

    @DeleteMapping("/apis/{id}")
    public String deleteApi(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            apiService.deleteApi(id);
            redirectAttributes.addFlashAttribute("success", "API documentation deleted successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error deleting API: " + e.getMessage());
        }
        return "redirect:/apis";
    }

    // Get Version History
    @GetMapping("/apis/{id}/versions")
    public String viewVersionHistory(@PathVariable Long id, Model model) {
        try {
            Optional<ApiDocumentation> apiOpt = apiService.getApiById(id);
            if (apiOpt.isEmpty()) {
                return "redirect:/apis";
            }

            ApiDocumentation api = apiOpt.get();
            List<ApiVersion> versions = apiService.getVersionHistory(id);

            model.addAttribute("api", api);
            model.addAttribute("versions", versions);
            return "version-history";
        } catch (Exception e) {
            logger.error("Error is : "+ e.getMessage());
            return "redirect:/apis";
        }
    }

    // View Specific Version
    @GetMapping("/apis/{id}/version/{versionId}")
    public String viewSpecificVersion(@PathVariable Long id, @PathVariable String versionId, Model model) {
        try {
            Optional<ApiDocumentation> apiOpt = apiService.getApiById(id);
            if (apiOpt.isEmpty()) {
                return "redirect:/";
            }

            // Find the specific version by versionId string
            List<ApiVersion> versions = apiService.getVersionHistory(id);
            Optional<ApiVersion> versionOpt = versions.stream()
                    .filter(v -> versionId.equals(v.getVersionNumber()))
                    .findFirst();

            if (versionOpt.isEmpty()) {
                return "redirect:/apis/" + id + "/versions";
            }

            ApiDocumentation api = apiOpt.get();
            ApiVersion version = versionOpt.get();

            model.addAttribute("api", api);
            model.addAttribute("currentVersion", version.getVersionNumber());

            // Override the swagger content with the version-specific content
            api.setSwaggerContent(version.getContent()); // Assuming ApiVersion has swagger content

            return "api-detail";
        } catch (Exception e) {
            logger.error(e.getMessage());
            return "redirect:/apis/" + id + "/versions";
        }
    }



    // If you need JSON API endpoints keep them separate with /api prefix
    @GetMapping("/apis/json/{id}")
    public ResponseEntity<ApiDocumentation> getApiForEdit(@PathVariable Long id) {
        try {
            ApiDocumentation api = apiService.findById(id);
            return ResponseEntity.ok(api);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    // Get versions as JSON for dropdown
    @GetMapping("/api/apis/{id}/versions")
    public ResponseEntity<List<Map<String, Object>>> getVersionsJson(@PathVariable Long id) {
        try {
            Optional<ApiDocumentation> apiOpt = apiService.getApiById(id);
            if (apiOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            ApiDocumentation api = apiOpt.get();
            List<ApiVersion> versions = apiService.getVersionHistory(id);

            List<Map<String, Object>> versionData = new ArrayList<>();

            // Ensure versions are sorted by creation date descending (latest first)
            //versions.sort((v1,v2) -> v2.getCreatedDate().compareTo(v1.getCreatedDate()));

            // Always add the current version as the latest
            Map<String, Object> currentVersionData = new HashMap<>();
            currentVersionData.put("versionNumber", api.getVersion());
            currentVersionData.put("createdAt", api.getUpdatedAt());
            currentVersionData.put("isLatest", true);
            versionData.add(currentVersionData);

            // Add historical versions (if any exist)
            versions.stream()
                    .filter(version -> !version.getVersionNumber().equals(api.getVersion())) // Avoid duplicates
                    .sorted((v1, v2) -> v2.getCreatedDate().compareTo(v1.getCreatedDate())) // Latest first
                    .forEach(version -> {
                        Map<String, Object> data = new HashMap<>();
                        data.put("versionNumber", version.getVersionNumber());
                        data.put("createdAt", version.getCreatedDate());
                        data.put("isLatest", false);
                        versionData.add(data);
                    });

            logger.info("Returning {} versions for API {}", versionData.size(), id);
            return ResponseEntity.ok(versionData);
        } catch (Exception e) {
            logger.error("Error loading versions: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }



    @PostMapping("/api/{id}/refresh")
    public String refreshApi(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            apiService.refreshFromUrl(id);
            redirectAttributes.addFlashAttribute("success", "API documentation refreshed successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error refreshing API: " + e.getMessage());
        }
        return "redirect:/api/" + id;
    }

    // Get RAW API Content
    @CrossOrigin(origins = "*")
    @GetMapping("/apis/{id}/raw")
    @ResponseBody
    public ResponseEntity<String> getRawApiContent(@PathVariable Long id) {
        Optional<ApiDocumentation> apiOpt = apiService.getApiById(id);
        return apiOpt.map(apiDocumentation -> ResponseEntity.ok()
                .header("Content-Type", "application/json")
                .body(apiDocumentation.getSwaggerContent())).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/apis/notifications")
    public String viewNotifications(Model model) {
        try {
            List<ApiChangeNotification> notifications = apiService.getUnreadNotifications();
            model.addAttribute("notifications", notifications);
            return "notification";
        } catch (Exception e) {
            logger.error(e.getMessage());
            model.addAttribute("error", "Failed to load notifications: " + e.getMessage());
            model.addAttribute("notifications", Collections.emptyList());
            return "notification";
        }
    }

    @PostMapping("/notifications/{id}/read")
    @ResponseBody
    public ResponseEntity<String> markAsRead(@PathVariable Long id) {
        try {
            apiService.markNotificationAsRead(id);
            return ResponseEntity.ok("Marked as read");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error marking notification as read: " + e.getMessage());
        }
    }


    @PostMapping("/api/{id}/check-updates")
    @ResponseBody
    public ResponseEntity<String> checkForUpdates(@PathVariable Long id) {
        try {
            apiService.checkForUpdates(id);
            return ResponseEntity.ok("Update check completed successfully");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error checking for updates: " + e.getMessage());
        }
    }
}