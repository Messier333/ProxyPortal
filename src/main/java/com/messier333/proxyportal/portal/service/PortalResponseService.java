package com.messier333.proxyportal.portal.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.messier333.proxyportal.portal.dto.PortalConfigResponse;
import com.messier333.proxyportal.portal.dto.PortalConfigResponse.CategoryDto;
import com.messier333.proxyportal.portal.dto.PortalConfigResponse.LinkDto;
import com.messier333.proxyportal.portal.dto.PortalConfigResponse.TabDto;

@Service
public class PortalResponseService {
    
    public PortalConfigResponse getPortalCategories() {
        List<TabDto> tabs = List.of(
                new TabDto(
                        "myself",
                        "portal/src/img/banners/banner_09.gif",
                        1, List.of(
                                new CategoryDto(
                                        "bookmarks",
                                        List.of(
                                                new LinkDto("raindrop", "https://app.raindrop.io", "droplet-bolt", "#a6e3a1", 1),
                                                new LinkDto("musicForProgramming();", "https://musicforprogramming.net", "binary-tree", "#fab387", 2)
                                        ), 1
                                ),
                                new CategoryDto(
                                        "workspace",
                                        List.of(
                                                new LinkDto("gmail", "https://mail.google.com", "brand-gmail", "#a6e3a1", 1),
                                                new LinkDto("calendar", "https://calendar.google.com", "calendar-filled", "#fab387", 2),
                                                new LinkDto("sheets", "https://docs.google.com/spreadsheets", "table", "#f38ba8", 3),
                                                new LinkDto("drive", "https://drive.google.com/drive/home", "brand-google-drive", "#89b4fa", 4)
                                        ), 2
                                )
                        )
                ),
                new TabDto(
                        "dev",
                        "portal/src/img/banners/banner_07.gif",
                        2, List.of(
                                new CategoryDto(
                                        "development",
                                        List.of(
                                                new LinkDto("github", "https://github.com", "brand-github", "#a6e3a1", 1),
                                                new LinkDto("stackoverflow", "https://stackoverflow.com", "brand-stackoverflow", "#fab387", 2),
                                                new LinkDto("duckdb", "https://app.motherduck.com", "file-type-sql", "#f38ba8", 3),
                                                new LinkDto("collab", "https://colab.research.google.com", "notebook", "#cba6f7", 4)
                                        ), 1
                                )
                        )
                ),
                                new TabDto(
                        "dev",
                        "portal/src/img/banners/banner_07.gif",
                        3, List.of(
                                new CategoryDto(
                                        "development",
                                        List.of(
                                                new LinkDto("github", "https://github.com", "brand-github", "#a6e3a1", 1),
                                                new LinkDto("stackoverflow", "https://stackoverflow.com", "brand-stackoverflow", "#fab387", 2),
                                                new LinkDto("duckdb", "https://app.motherduck.com", "file-type-sql", "#f38ba8", 3),
                                                new LinkDto("collab", "https://colab.research.google.com", "notebook", "#cba6f7", 4)
                                        ), 1
                                )
                        )
                ),
                                new TabDto(
                        "dev",
                        "portal/src/img/banners/banner_07.gif",
                        2, List.of(
                                new CategoryDto(
                                        "development",
                                        List.of(
                                                new LinkDto("github", "https://github.com", "brand-github", "#a6e3a1", 1),
                                                new LinkDto("stackoverflow", "https://stackoverflow.com", "brand-stackoverflow", "#fab387", 2),
                                                new LinkDto("duckdb", "https://app.motherduck.com", "file-type-sql", "#f38ba8", 3),
                                                new LinkDto("collab", "https://colab.research.google.com", "notebook", "#cba6f7", 4)
                                        ), 1
                                )
                        )
                ),
                                new TabDto(
                        "dev",
                        "portal/src/img/banners/banner_07.gif",
                        2, List.of(
                                new CategoryDto(
                                        "development",
                                        List.of(
                                                new LinkDto("github", "https://github.com", "brand-github", "#a6e3a1", 1),
                                                new LinkDto("stackoverflow", "https://stackoverflow.com", "brand-stackoverflow", "#fab387", 2),
                                                new LinkDto("duckdb", "https://app.motherduck.com", "file-type-sql", "#f38ba8", 3),
                                                new LinkDto("collab", "https://colab.research.google.com", "notebook", "#cba6f7", 4)
                                        ), 1
                                )
                        )
                )
        );

        return new PortalConfigResponse(tabs);
    }
}
