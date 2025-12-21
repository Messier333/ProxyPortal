package com.messier333.proxyportal.portal.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
    name = "portal_categories",
    indexes = @Index(
        name = "idx_portal_categories_tab_sort",
        columnList = "tab_id, sort_order"
    ),
    uniqueConstraints = @UniqueConstraint(
        name = "uq_portal_categories_tab_name",
        columnNames = {"tab_id", "name"}
    )
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PortalCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tab_id", nullable = false)
    private PortalTab tab;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    public static PortalCategory create(PortalTab tab, String name, int sortOrder) {
        PortalCategory category = new PortalCategory();
        category.tab = tab;
        category.name = name;
        category.sortOrder = sortOrder;
        return category;
    }

    public void update(String name, int sortOrder) {
        this.name = name;
        this.sortOrder = sortOrder;
    }
}