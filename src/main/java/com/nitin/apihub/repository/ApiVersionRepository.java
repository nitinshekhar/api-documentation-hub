package com.nitin.apihub.repository;

import com.nitin.apihub.model.ApiVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ApiVersionRepository extends JpaRepository<ApiVersion, Long> {
    List<ApiVersion> findByApiDocumentationIdOrderByCreatedDateDesc(Long apiDocumentationId);
    Optional<ApiVersion> findByApiDocumentationIdAndIsCurrent(Long apiDocumentationId, Boolean isCurrent);
    Optional<ApiVersion> findByApiDocumentationIdAndVersionNumber(Long apiDocumentationId, String versionNumber);

   // void save(ApiVersion historicalVersion);
}
