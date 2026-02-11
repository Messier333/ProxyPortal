package com.messier333.proxyportal.portal.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.messier333.proxyportal.portal.entity.PortalCategory;

public interface PortalCategoryRepository extends JpaRepository<PortalCategory, Long> {

    Optional<PortalCategory> findByIdAndTabUserUsername(Long id, String tabUserUsername);
    boolean existsByTabIdAndNameIgnoreCase(Long tabId, String name);
    List<PortalCategory> findAllByTabIdOrderBySortOrderAscIdAsc(Long tabId);
    void deleteByTabUserUsername(String username);
}
