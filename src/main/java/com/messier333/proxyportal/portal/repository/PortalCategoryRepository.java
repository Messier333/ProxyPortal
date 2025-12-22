package com.messier333.proxyportal.portal.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.messier333.proxyportal.portal.entity.PortalCategory;

import java.util.Optional;

public interface PortalCategoryRepository extends JpaRepository<PortalCategory, Long> {

    Optional<PortalCategory> findByIdAndTabUserUsername(Long id, String tabUserUsername);
}
