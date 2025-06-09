package com.nitin.apihub.task;

import com.nitin.apihub.model.ApiDocumentation;
import com.nitin.apihub.service.ApiDocumentationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
//@EnableScheduling
@ConditionalOnProperty(name = "app.scheduling.enabled", havingValue = "true", matchIfMissing = true)
public class ScheduledTasks {

    @Autowired
    private ApiDocumentationService apiDocumentationService;
    private static final Logger logger = LoggerFactory.getLogger(ScheduledTasks.class);

    @Scheduled(fixedRateString = "${app.auto-update.interval:86400000}") // Check every day
    public void checkForApiUpdates() {
        logger.info("Starting scheduled API update check...");
        List<ApiDocumentation> apis = apiDocumentationService.getApisForAutoCheck();

        int successCount = 0;
        int errorCount = 0;

        for (ApiDocumentation api : apis) {
            try {
                apiDocumentationService.checkForUpdates(api.getId());
                successCount++;
            } catch (Exception e) {
                errorCount++;
                logger.error("Failed to auto-update API "+ api.getName());
            }
        }
        logger.info("Completed API update check: " + successCount + " successful, " + errorCount + " failed");
    }

    @Scheduled(cron = "0 0 2 * * ?") // Daily at 2 AM
    public void cleanupOldNotifications() {
        logger.info("Starting notification cleanup...");
        try {
            apiDocumentationService.cleanupOldNotifications();
        } catch (Exception e) {
            logger.error("Error during scheduled notification cleanup: " + e.getMessage());
        }
    }

    @Scheduled(cron = "0 0 3 * * SUN") // Weekly on Sunday at 3 AM
    public void generateWeeklyStats() {
        logger.info("Generating weekly statistics...");
        try {
            Map<String, Long> stats = apiDocumentationService.getNotificationStats();
            logger.info("Weekly Stats: " + stats);
            // @TODO Further these could be saved to database or send via email
        } catch (Exception e) {
            logger.error("Error generating weekly stats: " + e.getMessage());
        }
    }
}