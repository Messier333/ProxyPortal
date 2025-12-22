package com.messier333.proxyportal.portal.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.messier333.proxyportal.portal.entity.PortalTab;

import java.util.Optional;

public interface PortalTabRepository extends JpaRepository<PortalTab, Long> {

    Optional<PortalTab> findByIdAndUserUsername(Long id, String userUsername);
}
