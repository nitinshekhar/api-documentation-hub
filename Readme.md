APIDocumentationHub is a Spring Boot application that serves as a centralized repository for API documentation. 
The application includes: 
1. Core Features 
   a. Multiple Input Methods: * Upload Swagger/OpenAPI files (JSON/YAML) 
   b. Add APIs by providing external URLs 
   c. Update API's by similar methods
   d. Maintain version of API's and sends notification
   e. Automatic content fetching and normalization 
2. Management Capabilities: 
   a. View all APIs in a searchable directory 
   b. Detailed API documentation viewer with Swagger UI * Refresh external URLs to get latest versions 
   c. Soft delete functionality * Search and filter APIs 
3. Modern Web Interface: 
   a. Bootstrap 5 responsive design 
   b. Interactive Swagger UI integration 
   c. Clean, professional interface 
   d. Real-time search functionality 
4. Additional Features 
   a. Automatic scheduled updates for external APIs 
   b. Real-time change notifications 
   c. Enhanced search capabilities 
   d.Better security for external URL fetching

Project Structure 
1. Main Application: ApiDocumentationHubApplication.java 
2. Model: JPA Entity for storing data
   a. ApiDocumentation.java - JPA entity for storing API metadata 
   b. ApiVersion.java - JPA entity for storing API Version data
   c. ApiChangeNotification - JPA entity for api change notification
3. Repository: ApiDocumentationRepository.java - Data access layer
   a. ApiChangeNotificationRepository
   b. ApiDocumentationRepository
   c. ApiVersionRepository
4. Service: ApiDocumentationService.java - Business logic
5. Controller: ApiDocumentationController.java - Web endpoints
6. ScheduledTasks - Scheduled Task for Auto Updates for URL based API's
7. Resources - Static Style css, configurations and templates
8. Templates: Thymeleaf templates for the web interface.
   a. layout.html
   b. index.html
   c. add-api.html
   d. api-detail.html
   e. Notification.html
   f. version-history.html

Database : H2database.

To run the project 
The project has a pom file that can be used to compile, build and run the project
