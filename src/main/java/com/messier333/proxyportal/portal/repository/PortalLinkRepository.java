package com.messier333.proxyportal.portal.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.messier333.proxyportal.portal.entity.PortalLink;

import java.util.List;
import java.util.Optional;

public interface PortalLinkRepository extends JpaRepository<PortalLink, Long> {
    boolean existsByCategoryIdAndNameIgnoreCase(Long categoryId, String name);
    boolean existsByCategoryIdAndUrlIgnoreCase(Long categoryId, String url);
    List<PortalLink> findAllByCategoryIdOrderBySortOrderAscIdAsc(Long categoryId);
    Optional<PortalLink> findByIdAndCategoryTabUserUsername(Long id, String username);
    void deleteByCategoryTabUserUsername(String username);
}
