package com.messier333.proxyportal.portal.entity;

import com.messier333.proxyportal.user.entity.User;

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

@Entity
@Getter
@Table(
    name = "portal_tabs",
    indexes = @Index( 
        name = "idx_portal_tabs_user_sort", columnList = "user_id, sort_order" 
    ),
    uniqueConstraints= @UniqueConstraint(
        name = "uk_user_tabname",
        columnNames = {"user_id", "name"}
    )
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PortalTab {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable=false)
    private String name;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "background_url")
    private String backgroundUrl;

    public static PortalTab createTab(User user, String name, int sortOrder) {
        PortalTab tab = new PortalTab();
        tab.user = user;
        tab.name = name;
        tab.sortOrder = sortOrder;
        return tab;
    }

    public void update(String name, int sortOrder) {
        this.name = name;
        this.sortOrder = sortOrder;
    }

    public void setSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }

    public void setBackgroundUrl(String backgroundUrl) {
        this.backgroundUrl = backgroundUrl;
    }
}
