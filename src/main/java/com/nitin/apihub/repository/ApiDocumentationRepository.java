package com.nitin.apihub.repository;

import com.nitin.apihub.model.ApiDocumentation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ApiDocumentationRepository extends JpaRepository<ApiDocumentation, Long> {

    List<ApiDocumentation> findByActiveTrue();

    @Query("SELECT a FROM ApiDocumentation a WHERE a.active = true ORDER BY a.name ASC")
    List<ApiDocumentation> findActiveApisSortedByName();

    List<ApiDocumentation> findByNameContainingIgnoreCaseAndActiveTrue(String name);
}
