package com.messier333.proxyportal.portal.repository.impl;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import com.messier333.proxyportal.common.config.QueryDslConfig;
import com.messier333.proxyportal.portal.dto.response.PortalCategoriesResponse;
import com.messier333.proxyportal.portal.dto.response.PortalTabsResponse;
import com.messier333.proxyportal.portal.entity.PortalCategory;
import com.messier333.proxyportal.portal.entity.PortalLink;
import com.messier333.proxyportal.portal.entity.PortalTab;
import com.messier333.proxyportal.portal.repository.PortalCategoryRepository;
import com.messier333.proxyportal.portal.repository.PortalLinkRepository;
import com.messier333.proxyportal.portal.repository.PortalTabRepository;
import com.messier333.proxyportal.user.entity.Role;
import com.messier333.proxyportal.user.entity.User;
import com.messier333.proxyportal.user.repository.UserRepository;

@DataJpaTest
@Import({QueryDslConfig.class, PortalQueryRepositoryImpl.class})
class PortalQueryRepositoryImplTest {

    @Autowired
    private PortalQueryRepositoryImpl portalQueryRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PortalTabRepository portalTabRepository;

    @Autowired
    private PortalCategoryRepository portalCategoryRepository;

    @Autowired
    private PortalLinkRepository portalLinkRepository;

    @SuppressWarnings("null")
    @Test
    void findTabsByUsername_shouldBuildHierarchyAndKeepSortOrder() {
        User alice = userRepository.save(User.createUser("alice", "ENC", Role.USER));
        userRepository.save(User.createUser("bob", "ENC", Role.USER));

        PortalTab tabA = portalTabRepository.save(PortalTab.createTab(alice, "Tab-A", 1));
        tabA.setBackgroundUrl("/uploads/a.png");
        PortalTab tabB = portalTabRepository.save(PortalTab.createTab(alice, "Tab-B", 2));
        portalTabRepository.save(PortalTab.createTab(alice, "Tab-C", 3)); // no categories
        PortalCategory catA1 = portalCategoryRepository.save(PortalCategory.create(tabA, "Cat-A1", 1));
        portalCategoryRepository.save(PortalCategory.create(tabA, "Cat-A2", 2)); // no links
        PortalCategory catB1 = portalCategoryRepository.save(PortalCategory.create(tabB, "Cat-B1", 1));

        portalLinkRepository.save(PortalLink.create(catA1, "L2", "https://l2.com", "i2", "#222", 2));
        portalLinkRepository.save(PortalLink.create(catA1, "L1", "https://l1.com", "i1", "#111", 1));
        portalLinkRepository.save(PortalLink.create(catB1, "LB1", "https://lb1.com", "ib1", "#333", 1));

        PortalTabsResponse result = portalQueryRepository.findTabsByUsername("alice");

        assertThat(result.tabs()).hasSize(3);
        assertThat(result.tabs().get(0).name()).isEqualTo("Tab-A");
        assertThat(result.tabs().get(0).backgroundUrl()).isEqualTo("/uploads/a.png");
        assertThat(result.tabs().get(1).name()).isEqualTo("Tab-B");
        assertThat(result.tabs().get(2).name()).isEqualTo("Tab-C");
        assertThat(result.tabs().get(2).categories()).isEmpty();

        assertThat(result.tabs().get(0).categories()).extracting("name").containsExactly("Cat-A1", "Cat-A2");
        assertThat(result.tabs().get(0).categories().get(0).links()).extracting("name").containsExactly("L1", "L2");
        assertThat(result.tabs().get(0).categories().get(1).links()).isEmpty();
        assertThat(result.tabs().get(1).categories().get(0).links()).extracting("name").containsExactly("LB1");
    }

    @SuppressWarnings("null")
    @Test
    void findTabsByUsername_shouldReturnEmptyWhenUserHasNoTabs() {
        userRepository.save(User.createUser("alice", "ENC", Role.USER));

        PortalTabsResponse result = portalQueryRepository.findTabsByUsername("alice");

        assertThat(result.tabs()).isEmpty();
    }

    @SuppressWarnings("null")
    @Test
    void findCategoriesByUsername_shouldFlattenAllCategories() {
        User alice = userRepository.save(User.createUser("alice", "ENC", Role.USER));
        PortalTab tab1 = portalTabRepository.save(PortalTab.createTab(alice, "Tab-1", 1));
        PortalTab tab2 = portalTabRepository.save(PortalTab.createTab(alice, "Tab-2", 2));

        PortalCategory cat1 = portalCategoryRepository.save(PortalCategory.create(tab1, "Cat-1", 1));
        PortalCategory cat2 = portalCategoryRepository.save(PortalCategory.create(tab2, "Cat-2", 1));
        portalLinkRepository.save(PortalLink.create(cat1, "L1", "https://l1.com", "i1", "#111", 1));
        portalLinkRepository.save(PortalLink.create(cat2, "L2", "https://l2.com", "i2", "#222", 1));

        PortalCategoriesResponse result = portalQueryRepository.findCategoriesByUsername("alice");

        assertThat(result.categories()).extracting("name").containsExactly("Cat-1", "Cat-2");
        assertThat(result.categories().stream().map(category -> category.links().size()))
                .isEqualTo(List.of(1, 1));
    }
}
