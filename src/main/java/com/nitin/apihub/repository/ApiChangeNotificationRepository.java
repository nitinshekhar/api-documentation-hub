package com.nitin.apihub.repository;

import com.nitin.apihub.model.ApiChangeNotification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ApiChangeNotificationRepository extends JpaRepository<ApiChangeNotification, Long> {
    List<ApiChangeNotification> findByApiDocumentationIdOrderByCreatedDateDesc(Long apiDocumentationId);
    List<ApiChangeNotification> findByIsReadOrderByCreatedDateDesc(Boolean isRead);
    long countByIsRead(Boolean isRead);

    // Add these new methods for cleanup
    @Query("SELECT n FROM ApiChangeNotification n WHERE n.createdDate < :cutoffDate AND n.isRead = true")
    List<ApiChangeNotification> findOldReadNotifications(@Param("cutoffDate") LocalDateTime cutoffDate);

    @Query("DELETE FROM ApiChangeNotification n WHERE n.createdDate < :cutoffDate AND n.isRead = true")
    int deleteOldReadNotifications(@Param("cutoffDate") LocalDateTime cutoffDate);

    @Query("SELECT COUNT(n) FROM ApiChangeNotification n WHERE n.createdDate >= :fromDate")
    long countNotificationsSince(@Param("fromDate") LocalDateTime fromDate);
}
