-- =====================
-- USER (id=1)
-- =====================
MERGE INTO users (id, username, password, role)
    KEY(id)
    VALUES (1, 'admin', '{noop}local', 'ADMIN');

-- =====================
-- TABS
-- =====================
MERGE INTO portal_tabs (id, user_id, name, background_url, sort_order) KEY(id)
    VALUES (1, 1, 'myself', 'src/img/banners/banner_09.gif', 1);

MERGE INTO portal_tabs (id, user_id, name, background_url, sort_order) KEY(id)
    VALUES (2, 1, 'dev', 'src/img/banners/banner_07.gif', 2);

MERGE INTO portal_tabs (id, user_id, name, background_url, sort_order) KEY(id)
    VALUES (3, 1, 'chi ll', 'src/img/banners/banner_08.gif', 3);

-- =====================
-- CATEGORIES
-- =====================
MERGE INTO portal_categories (id, tab_id, name, sort_order) KEY(id)
    VALUES (1, 1, 'bookmarks', 1);
MERGE INTO portal_categories (id, tab_id, name, sort_order) KEY(id)
    VALUES (2, 1, 'workspace', 2);
MERGE INTO portal_categories (id, tab_id, name, sort_order) KEY(id)
    VALUES (3, 1, 'media', 3);

MERGE INTO portal_categories (id, tab_id, name, sort_order) KEY(id)
    VALUES (4, 2, 'development', 1);
MERGE INTO portal_categories (id, tab_id, name, sort_order) KEY(id)
    VALUES (5, 2, 'challenges', 2);
MERGE INTO portal_categories (id, tab_id, name, sort_order) KEY(id)
    VALUES (6, 2, 'resources', 3);

MERGE INTO portal_categories (id, tab_id, name, sort_order) KEY(id)
    VALUES (7, 3, 'social media', 1);
MERGE INTO portal_categories (id, tab_id, name, sort_order) KEY(id)
    VALUES (8, 3, 'gaming', 2);
MERGE INTO portal_categories (id, tab_id, name, sort_order) KEY(id)
    VALUES (9, 3, 'video', 3);

-- =====================
-- LINKS
-- =====================
MERGE INTO portal_links (id, category_id, name, url, icon, icon_color, sort_order) KEY(id)
    VALUES (1, 1, 'raindrop', 'https://app.raindrop.io', 'droplet-bolt', '#a6e3a1', 1);
MERGE INTO portal_links (id, category_id, name, url, icon, icon_color, sort_order) KEY(id)
    VALUES (2, 1, 'musicForProgramming();', 'https://musicforprogramming.net', 'binary-tree', '#fab387', 2);

MERGE INTO portal_links (id, category_id, name, url, icon, icon_color, sort_order) KEY(id)
    VALUES (3, 2, 'gmail', 'https://mail.google.com', 'brand-gmail', '#a6e3a1', 1);
MERGE INTO portal_links (id, category_id, name, url, icon, icon_color, sort_order) KEY(id)
    VALUES (4, 2, 'calendar', 'https://calendar.google.com', 'calendar-filled', '#fab387', 2);
MERGE INTO portal_links (id, category_id, name, url, icon, icon_color, sort_order) KEY(id)
    VALUES (5, 2, 'sheets', 'https://docs.google.com/spreadsheets', 'table', '#f38ba8', 3);
MERGE INTO portal_links (id, category_id, name, url, icon, icon_color, sort_order) KEY(id)
    VALUES (6, 2, 'drive', 'https://drive.google.com/drive/home', 'brand-google-drive', '#89b4fa', 4);

MERGE INTO portal_links (id, category_id, name, url, icon, icon_color, sort_order) KEY(id)
    VALUES (7, 3, 'уп', 'https://www.pravda.com.ua', 'news', '#a6e3a1', 1);
MERGE INTO portal_links (id, category_id, name, url, icon, icon_color, sort_order) KEY(id)
    VALUES (8, 3, 'mil.in.ua', 'https://mil.in.ua', 'badge-filled', '#fab387', 2);
MERGE INTO portal_links (id, category_id, name, url, icon, icon_color, sort_order) KEY(id)
    VALUES (9, 3, 'куток', 'https://kutok.io', 'border-radius', '#f38ba8', 3);
MERGE INTO portal_links (id, category_id, name, url, icon, icon_color, sort_order) KEY(id)
    VALUES (10, 3, 'ґрунт', 'https://grnt.media', 'eye-bolt', '#89b4fa', 4);
MERGE INTO portal_links (id, category_id, name, url, icon, icon_color, sort_order) KEY(id)
    VALUES (11, 3, 'village', 'https://www.village.com.ua', 'home-2', '#cba6f7', 5);

MERGE INTO portal_links (id, category_id, name, url, icon, icon_color, sort_order) KEY(id)
    VALUES (12, 4, 'github', 'https://github.com', 'brand-github', '#a6e3a1', 1);
MERGE INTO portal_links (id, category_id, name, url, icon, icon_color, sort_order) KEY(id)
    VALUES (13, 4, 'stackoverflow', 'https://stackoverflow.com', 'brand-stackoverflow', '#fab387', 2);
MERGE INTO portal_links (id, category_id, name, url, icon, icon_color, sort_order) KEY(id)
    VALUES (14, 4, 'duckdb', 'https://app.motherduck.com', 'file-type-sql', '#f38ba8', 3);
MERGE INTO portal_links (id, category_id, name, url, icon, icon_color, sort_order) KEY(id)
    VALUES (15, 4, 'collab', 'https://colab.research.google.com', 'notebook', '#cba6f7', 4);

MERGE INTO portal_links (id, category_id, name, url, icon, icon_color, sort_order) KEY(id)
    VALUES (16, 5, 'kaggle', 'https://www.kaggle.com', 'brain', '#a6e3a1', 1);
MERGE INTO portal_links (id, category_id, name, url, icon, icon_color, sort_order) KEY(id)
    VALUES (17, 5, 'leetcode', 'https://leetcode.com', 'code-plus', '#fab387', 2);
MERGE INTO portal_links (id, category_id, name, url, icon, icon_color, sort_order) KEY(id)
    VALUES (18, 5, 'exercism', 'https://exercism.org', 'code-minus', '#f38ba8', 3);
MERGE INTO portal_links (id, category_id, name, url, icon, icon_color, sort_order) KEY(id)
    VALUES (19, 5, 'aoc', 'https://adventofcode.com', 'brand-linktree', '#89b4fa', 4);

MERGE INTO portal_links (id, category_id, name, url, icon, icon_color, sort_order) KEY(id)
    VALUES (20, 6, 'dou', 'https://dou.ua', 'brand-prisma', '#a6e3a1', 1);
MERGE INTO portal_links (id, category_id, name, url, icon, icon_color, sort_order) KEY(id)
    VALUES (21, 6, 'hackernews', 'https://news.ycombinator.com', 'brand-redhat', '#fab387', 2);
MERGE INTO portal_links (id, category_id, name, url, icon, icon_color, sort_order) KEY(id)
    VALUES (22, 6, 'uber engineering', 'https://www.uber.com/en-GB/blog/london/engineering', 'brand-uber', '#f38ba8', 3);
MERGE INTO portal_links (id, category_id, name, url, icon, icon_color, sort_order) KEY(id)
    VALUES (23, 6, 'netflix tech blog', 'https://netflixtechblog.com', 'brand-netflix', '#89b4fa', 4);

MERGE INTO portal_links (id, category_id, name, url, icon, icon_color, sort_order) KEY(id)
    VALUES (24, 7, 'telegram', 'https://web.telegram.org', 'brand-telegram', '#a6e3a1', 1);
MERGE INTO portal_links (id, category_id, name, url, icon, icon_color, sort_order) KEY(id)
    VALUES (25, 7, 'facebook', 'https://www.facebook.com', 'brand-facebook', '#fab387', 2);
MERGE INTO portal_links (id, category_id, name, url, icon, icon_color, sort_order) KEY(id)
    VALUES (26, 7, 'reddit', 'https://www.reddit.com/r/unixporn', 'brand-reddit', '#f38ba8', 3);

MERGE INTO portal_links (id, category_id, name, url, icon, icon_color, sort_order) KEY(id)
    VALUES (27, 8, 'infiniteBacklog', 'https://infinitebacklog.net', 'device-gamepad', '#a6e3a1', 1);
MERGE INTO portal_links (id, category_id, name, url, icon, icon_color, sort_order) KEY(id)
    VALUES (28, 8, 'steam', 'https://store.steampowered.com', 'brand-steam', '#fab387', 2);
MERGE INTO portal_links (id, category_id, name, url, icon, icon_color, sort_order) KEY(id)
    VALUES (29, 8, 'epicgames', 'https://store.epicgames.com', 'brand-fortnite', '#f38ba8', 3);
MERGE INTO portal_links (id, category_id, name, url, icon, icon_color, sort_order) KEY(id)
    VALUES (30, 8, 'nintendo', 'https://store.nintendo.co.uk', 'device-nintendo', '#89b4fa', 4);

MERGE INTO portal_links (id, category_id, name, url, icon, icon_color, sort_order) KEY(id)
    VALUES (31, 9, 'anilist', 'https://anilist.co/home', 'brand-funimation', '#a6e3a1', 1);
MERGE INTO portal_links (id, category_id, name, url, icon, icon_color, sort_order) KEY(id)
    VALUES (32, 9, 'youtube', 'https://www.youtube.com', 'brand-youtube', '#fab387', 2);
MERGE INTO portal_links (id, category_id, name, url, icon, icon_color, sort_order) KEY(id)
    VALUES (33, 9, 'patreon', 'https://www.patreon.com', 'brand-patreon', '#f38ba8', 3);
MERGE INTO portal_links (id, category_id, name, url, icon, icon_color, sort_order) KEY(id)
    VALUES (34, 9, 'kyivstar', 'https://tv.kyivstar.ua', 'star-filled', '#89b4fa', 4);
