package com.messier333.proxyportal.portal.repository.impl;

import com.messier333.proxyportal.portal.dto.response.CategoryResponse;
import com.messier333.proxyportal.portal.dto.response.LinkResponse;
import com.messier333.proxyportal.portal.dto.response.PortalTabsResponse;
import com.messier333.proxyportal.portal.dto.response.TabResponse;
import com.messier333.proxyportal.portal.entity.PortalRow;
import com.messier333.proxyportal.portal.entity.QPortalCategory;
import com.messier333.proxyportal.portal.entity.QPortalLink;
import com.messier333.proxyportal.portal.entity.QPortalTab;
import com.messier333.proxyportal.portal.repository.PortalQueryRepository;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
@RequiredArgsConstructor
public class PortalQueryRepositoryImpl implements PortalQueryRepository {

    private final JPAQueryFactory queryFactory;
    @Override
    public PortalTabsResponse findTabsByUsername(String username) {
        QPortalTab tab = QPortalTab.portalTab;
        QPortalCategory category = QPortalCategory.portalCategory;
        QPortalLink link = QPortalLink.portalLink;

        List<PortalRow> rows = queryFactory
                .select(Projections.constructor(
                        PortalRow.class,
                        tab.id, tab.name, tab.backgroundUrl, tab.sortOrder,
                        category.id, category.name, category.sortOrder,
                        link.id, link.name, link.url, link.icon, link.iconColor, link.sortOrder
                ))
                .from(tab)
                .leftJoin(category).on(category.tab.eq(tab))
                .leftJoin(link).on(link.category.eq(category))
                .where(tab.user.username.eq(username))
                .orderBy(
                        tab.sortOrder.asc(), tab.id.asc(),
                        category.sortOrder.asc(), category.id.asc(),
                        link.sortOrder.asc(), link.id.asc()
                )
                .fetch();

        Map<Long, TabBuilder> tabMap = new LinkedHashMap<>();

        for (PortalRow r : rows) {
            TabBuilder tb = tabMap.computeIfAbsent(r.tabId(), id ->
                    new TabBuilder(id, r.tabName(), r.tabBg(), r.tabSort())
            );

            if (r.categoryId() != null) {
                CategoryBuilder cb = tb.categoryMap.computeIfAbsent(r.categoryId(), cid ->
                        new CategoryBuilder(cid, r.categoryName(), r.categorySort())
                );

                if (r.linkId() != null) {
                    cb.links.add(new LinkResponse(
                            r.linkId(),
                            r.linkName(),
                            r.linkUrl(),
                            r.linkIcon(),
                            r.linkIconColor(),
                            r.linkSort()
                    ));
                }
            }
        }
        List<TabResponse> tabs = tabMap.values().stream()
                .map(TabBuilder::build)
                .toList();

        return new PortalTabsResponse(tabs);
    }

    private static class TabBuilder {
        final Long id;
        final String name;
        final String bg;
        final Integer sort;
        final Map<Long, CategoryBuilder> categoryMap = new LinkedHashMap<>();
        TabBuilder(Long id, String name, String bg, Integer sort) {
            this.id = id;
            this.name = name;
            this.bg = bg;
            this.sort = sort;
        }

        TabResponse build() {
            List<CategoryResponse> categories = categoryMap.values().stream()
                    .map(CategoryBuilder::build)
                    .toList();
            return new TabResponse(id, name, bg, sort, categories);
        }
    }
    private static class CategoryBuilder {
        final Long id;
        final String name;
        final Integer sort;
        final List<LinkResponse> links = new ArrayList<>();

        CategoryBuilder(Long id, String name, Integer sort) {
            this.id = id;
            this.name = name;
            this.sort = sort;
        }

        CategoryResponse build() {
            return new CategoryResponse(id, name, links, sort);
        }
    }
}
