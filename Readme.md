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
   c. ApiChangeNotification.java - JPA entity for api change notification
   d. ChangeType.java - Enum for change types
   e. SourceType.java - Enum for source types
   f. dto/UpdateApiRequest.java - DTO for updating API requests
3. Repository: ApiDocumentationRepository.java - Data access layer
   a. ApiChangeNotificationRepository
   b. ApiDocumentationRepository
   c. ApiVersionRepository
4. Service: ApiDocumentationService.java - Business logic
5. Controller: ApiDocumentationController.java - Web endpoints
6. Config: SwaggerConfig.java - Swagger/OpenAPI configuration
7. ScheduledTasks.java - Scheduled Task for Auto Updates for URL based API's
8. Resources - Static Style css, configurations and templates
8. Templates: Thymeleaf templates for the web interface.
   a. layout.html
   b. index.html
   c. add-api.html
   d. api-detail.html
   e. edit-api.html
   f. notification.html
   g. version-history.html

---

## Technologies Used

*   **Spring Boot**: Framework for building the application.
*   **Spring Data JPA**: For database interaction and ORM.
*   **Thymeleaf**: Server-side Java template engine for web applications.
*   **H2 Database**: In-memory relational database for development and testing.
*   **SpringDoc OpenAPI UI**: For generating and serving Swagger UI.
*   **Jackson Dataformat YAML**: For YAML processing.
*   **SnakeYAML**: YAML parser and emitter.
*   **Bootstrap 5**: Front-end framework for responsive design.
*   **Maven**: Build automation tool.
*   **Java 21**: Programming language.

## Prerequisites

*   **Java Development Kit (JDK) 21** or higher.
*   **Maven 3.x** or higher.

## Configuration

The application can be configured using `application.properties` or `application.yml` files located in `src/main/resources`.

*   **Server Port**: `server.port=8080` (default)
*   **H2 Console**:
    *   `spring.h2.console.enabled=true`
    *   `spring.h2.console.path=/h2-console`
*   **Swagger UI**:
    *   `springdoc.api-docs.path=/api-docs`
    *   `springdoc.swagger-ui.path=/swagger-ui.html`
*   **Scheduled Updates**:
    *   `app.scheduling.enabled=true` (enable/disable scheduled tasks)
    *   `app.auto-update.interval=3600000` (interval in milliseconds, default 1 hour)
*   **Notification Retention**:
    *   `app.notification.retention-days=30` (number of days to retain notifications)
*   **HTTP Client**:
    *   `app.http.connection-timeout=10000` (connection timeout in milliseconds)
    *   `app.http.read-timeout=30000` (read timeout in milliseconds)
    *   `app.http.max-redirects=5` (maximum HTTP redirects)

## Getting Started

To run the project locally, follow these steps:

1.  **Clone the repository**:
    ```bash
    git clone <repository_url>
    cd api-documentation-hub
    ```
2.  **Build the project**:
    ```bash
    mvn clean install
    ```
3.  **Run the application**:
    ```bash
    mvn spring-boot:run
    ```
    The application will start on `http://localhost:8080` (or your configured port).

4.  **Access the application**:
    *   **Home Page**: `http://localhost:8080`
    *   **Swagger UI**: `http://localhost:8080/swagger-ui.html`
    *   **H2 Console**: `http://localhost:8080/h2-console` (JDBC URL: `jdbc:h2:mem:apihub`, Username: `sa`, Password: ` `)