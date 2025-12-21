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
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
    name = "portal_links",
    indexes=@Index(
        name="idx_portal_links_category_sort",
        columnList="category_id, sort_order"
    )
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PortalLink {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch= FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private PortalCategory category;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String url;

    @Column(length = 200)
    private String icon;

    @Column(name = "icon_color", nullable = false)
    private String iconColor;

    @Column(name = "sort_order")
    private int sortOrder;

        public static PortalLink create(
            PortalCategory category,
            String name,
            String url,
            String icon,
            String iconColor,
            int sortOrder
    ) {
        PortalLink link = new PortalLink();
        link.category = category;
        link.name = name;
        link.url = url;
        link.icon = icon;
        link.iconColor = iconColor;
        link.sortOrder = sortOrder;
        return link;
    }

    public void update(String name, String url, String icon, String iconColor, int sortOrder) {
        this.name = name;
        this.url = url;
        this.icon = icon;
        this.iconColor = iconColor;
        this.sortOrder = sortOrder;
    }
}
