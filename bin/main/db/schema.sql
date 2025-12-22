-- =========================
-- USERS
-- =========================
CREATE TABLE IF NOT EXISTS users (
                                     id BIGINT PRIMARY KEY,
                                     username VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL
    );

-- =========================
-- PORTAL_TABS
-- =========================
CREATE TABLE IF NOT EXISTS portal_tabs (
                                           id BIGINT PRIMARY KEY,
                                           user_id BIGINT NOT NULL,
                                           name VARCHAR(255) NOT NULL,
    background_url VARCHAR(512),
    sort_order INTEGER NOT NULL,

    CONSTRAINT fk_portal_tabs_user
    FOREIGN KEY (user_id) REFERENCES users(id)
    );

CREATE INDEX IF NOT EXISTS idx_portal_tabs_user_sort
    ON portal_tabs (user_id, sort_order);

-- =========================
-- PORTAL_CATEGORIES
-- =========================
CREATE TABLE IF NOT EXISTS portal_categories (
                                                 id BIGINT PRIMARY KEY,
                                                 tab_id BIGINT NOT NULL,
                                                 name VARCHAR(255) NOT NULL,
    sort_order INTEGER NOT NULL,

    CONSTRAINT fk_portal_categories_tab
    FOREIGN KEY (tab_id) REFERENCES portal_tabs(id)
    );

CREATE INDEX IF NOT EXISTS idx_portal_categories_tab_sort
    ON portal_categories (tab_id, sort_order);

-- =========================
-- PORTAL_LINKS
-- =========================
CREATE TABLE IF NOT EXISTS portal_links (
                                            id BIGINT PRIMARY KEY,
                                            category_id BIGINT NOT NULL,
                                            name VARCHAR(255) NOT NULL,
    url VARCHAR(1024) NOT NULL,
    icon VARCHAR(255),
    icon_color VARCHAR(50),
    sort_order INTEGER NOT NULL,

    CONSTRAINT fk_portal_links_category
    FOREIGN KEY (category_id) REFERENCES portal_categories(id)
    );

CREATE INDEX IF NOT EXISTS idx_portal_links_category_sort
    ON portal_links (category_id, sort_order);
