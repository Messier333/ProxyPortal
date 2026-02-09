const Dashboard = (() => {
  const qs = (sel) => document.querySelector(sel);
  const STORAGE_KEYS = {
    categoryDraft: "dashboard.manage.categoryDraft",
    linkDraft: "dashboard.manage.linkDraft",
  };
  const DUPLICATE_MESSAGES = {
    tab: "이미 같은 이름의 탭이 있습니다.",
    category: "이미 같은 이름의 카테고리가 있습니다.",
    linkName: "이미 같은 이름의 링크가 있습니다.",
    linkUrl: "이미 같은 URL 링크가 있습니다.",
  };
  const TABLER_ICON_CSS_PATH = "/portal/src/css/tabler-icons.min.css";
  const DEFAULT_ICON_NAME = "link";
  const DEFAULT_ICON_COLOR = "#89b4fa";
  const ICON_RENDER_LIMIT_DEFAULT = 120;
  const ICON_RENDER_LIMIT_SEARCH = 280;
  const FEATURED_ICON_NAMES = [
    "link",
    "brand-github",
    "brand-youtube",
    "brand-gmail",
    "calendar-filled",
    "brand-google-drive",
    "table",
    "brand-stackoverflow",
    "brand-telegram",
    "brand-facebook",
    "brand-reddit",
    "brand-steam",
  ];
  const FALLBACK_ICON_NAMES = [
    ...FEATURED_ICON_NAMES,
    "brand-google",
    "brand-aws",
    "brand-apple",
    "brand-discord",
    "brand-notion",
    "world",
    "search",
    "device-desktop",
    "server",
    "database",
    "cloud",
    "mail",
  ];
  const DOMAIN_STOPWORDS = new Set([
    "www", "com", "co", "net", "org", "io", "kr", "jp", "uk", "de", "edu", "gov",
  ]);

  let allTablerIcons = dedupeIconNames(FALLBACK_ICON_NAMES);
  let isIconSearchManual = false;

  function toast(msg) {
    const t = qs("#toast");
    const m = qs("#toastMsg");
    if (!t || !m) return;
    m.textContent = msg;
    t.hidden = false;
    clearTimeout(toast._timer);
    toast._timer = setTimeout(() => (t.hidden = true), 1600);
  }

  function toggleSidebar() {
    const sb = qs("#sidebar");
    if (!sb) return;
    sb.classList.toggle("is-open");
  }

  function clearLinkForm() {
    const nameEl = qs("#linkName");
    const urlEl = qs("#linkUrl");
    const tabEl = qs("#linkTabId");
    const catEl = qs("#linkCategoryId");
    const iconEl = qs("#linkIcon");
    const iconColorEl = qs("#linkIconColor");
    const iconSearchEl = qs("#linkIconSearch");
    if (nameEl) nameEl.value = "";
    if (urlEl) urlEl.value = "";
    if (tabEl) tabEl.selectedIndex = 0;
    if (catEl) catEl.selectedIndex = 0;
    if (iconEl) iconEl.value = "";
    if (iconColorEl) iconColorEl.value = DEFAULT_ICON_COLOR;
    if (iconSearchEl) iconSearchEl.value = "";
    isIconSearchManual = false;
    renderIconPicker();
    applyLinkVisualDefaults();
    filterLinkCategoriesByTab();
    sessionStorage.removeItem(STORAGE_KEYS.linkDraft);
    toast("링크 폼 초기화");
  }

  function withProtocol(url) {
    const trimmed = (url ?? "").trim();
    if (!trimmed) return "";
    if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) return trimmed;
    if (trimmed.startsWith("//")) return `https:${trimmed}`;
    return `https://${trimmed}`;
  }

  function suggestNameFromUrl(url) {
    const normalized = parseNormalizedUrl(url);
    if (!normalized) return "";
    try {
      const parsed = new URL(normalized);
      return (parsed.hostname || "").replace(/^www\./, "");
    } catch (_) {
      return "";
    }
  }

  function useNpmLink(rawUrl) {
    const urlEl = qs("#linkUrl");
    const nameEl = qs("#linkName");
    if (!urlEl) return;

    const safeUrl = withProtocol(rawUrl);
    if (!safeUrl) return;

    urlEl.value = safeUrl;
    if (nameEl && !nameEl.value.trim()) {
      const suggestedName = suggestNameFromUrl(safeUrl);
      if (suggestedName) nameEl.value = suggestedName;
    }
    applyLinkVisualDefaults();
    applySuggestedIconSearch(true);
    toast("NPM 링크를 링크 등록 폼에 채웠습니다.");
  }

  function parseNormalizedUrl(url) {
    const trimmed = (url ?? "").trim();
    if (!trimmed) return "";
    if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) return trimmed.toLowerCase();
    return `https://${trimmed}`.toLowerCase();
  }

  function normalizeIconName(iconName) {
    const value = (iconName ?? "").trim();
    return value || DEFAULT_ICON_NAME;
  }

  function iconLabel(iconName) {
    const normalized = normalizeIconName(iconName);
    return normalized.split("-").map((word) => {
      if (!word) return "";
      if (/^\d+$/.test(word)) return word;
      return word.charAt(0).toUpperCase() + word.slice(1);
    }).join(" ");
  }

  function dedupeIconNames(iconNames) {
    const unique = new Set();
    iconNames.forEach((name) => {
      const normalized = normalizeIconName(name);
      if (/^[a-z0-9-]+$/.test(normalized)) unique.add(normalized);
    });
    return Array.from(unique).sort((a, b) => a.localeCompare(b));
  }

  function extractIconNamesFromCss(cssText) {
    const regex = /\.ti-([a-z0-9-]+)(?::before|::before)\s*\{/g;
    const icons = [];
    let match;
    while ((match = regex.exec(cssText)) !== null) {
      icons.push(match[1]);
    }
    return dedupeIconNames(icons);
  }

  function updateIconCount(totalCount, renderedCount, hasQuery) {
    const countEl = qs("#linkIconCount");
    if (!countEl) return;

    if (totalCount === 0) {
      countEl.textContent = "아이콘을 찾지 못했습니다.";
      return;
    }

    if (hasQuery) {
      countEl.textContent = `검색 결과 ${totalCount}개 중 ${renderedCount}개 표시`;
      return;
    }

    const message = `전체 ${totalCount}개 중 ${renderedCount}개 표시`;
    countEl.textContent = totalCount > renderedCount ? `${message} (검색어 입력 권장)` : message;
  }

  function buildIconRenderSource(query) {
    const normalizedQuery = (query ?? "").trim().toLowerCase();
    if (!normalizedQuery) {
      const featured = FEATURED_ICON_NAMES.filter((name) => allTablerIcons.includes(name));
      const featuredSet = new Set(featured);
      const rest = allTablerIcons.filter((name) => !featuredSet.has(name));
      return [...featured, ...rest];
    }
    const tokens = normalizedQuery
      .split(/[\s,]+/)
      .map((token) => token.trim())
      .filter((token) => token.length > 0);
    if (!tokens.length) return allTablerIcons;
    return allTablerIcons.filter((name) => tokens.some((token) => name.includes(token)));
  }

  function extractSearchTokensFromText(value) {
    return String(value ?? "")
      .toLowerCase()
      .split(/[^a-z0-9-]+/)
      .map((token) => token.trim())
      .filter((token) => token.length > 0);
  }

  function extractSearchTokensFromUrl(value) {
    const normalized = parseNormalizedUrl(value);
    if (!normalized) return [];
    try {
      const url = new URL(normalized);
      return String(url.hostname ?? "")
        .toLowerCase()
        .split(".")
        .map((token) => token.trim())
        .filter((token) => token.length > 0 && !DOMAIN_STOPWORDS.has(token) && !/^\d+$/.test(token));
    } catch (_) {
      return [];
    }
  }

  function buildSuggestedIconQuery() {
    const linkNameEl = qs("#linkName");
    const linkUrlEl = qs("#linkUrl");
    const nameTokens = extractSearchTokensFromText(linkNameEl?.value);
    const urlTokens = extractSearchTokensFromUrl(linkUrlEl?.value);
    const tokens = Array.from(new Set([...nameTokens, ...urlTokens])).slice(0, 6);
    return tokens.join(" ");
  }

  function applySuggestedIconSearch(force = false) {
    const searchEl = qs("#linkIconSearch");
    if (!searchEl) return;
    if (isIconSearchManual && !force) return;
    searchEl.value = buildSuggestedIconQuery();
    renderIconPicker();
  }

  function renderIconPicker() {
    const pickerEl = qs("#linkIconPicker");
    const searchEl = qs("#linkIconSearch");
    const iconEl = qs("#linkIcon");
    if (!pickerEl || !searchEl || !iconEl) return;

    const query = searchEl.value ?? "";
    const hasQuery = query.trim().length > 0;
    const source = buildIconRenderSource(query);
    const limit = hasQuery ? ICON_RENDER_LIMIT_SEARCH : ICON_RENDER_LIMIT_DEFAULT;
    const visibleIcons = source.slice(0, limit);

    const selectedIcon = normalizeIconName(iconEl.value);
    if (selectedIcon && !visibleIcons.includes(selectedIcon)) {
      visibleIcons.unshift(selectedIcon);
    }

    pickerEl.textContent = "";
    if (visibleIcons.length === 0) {
      const emptyEl = document.createElement("div");
      emptyEl.className = "iconpick__empty";
      emptyEl.textContent = "검색 결과가 없습니다.";
      pickerEl.appendChild(emptyEl);
      updateIconCount(source.length, 0, hasQuery);
      syncIconPickerState();
      return;
    }

    const fragment = document.createDocumentFragment();
    visibleIcons.forEach((iconName) => {
      const button = document.createElement("button");
      button.type = "button";
      button.className = "iconpick__item";
      button.dataset.icon = iconName;
      button.dataset.label = iconLabel(iconName);

      const iconNode = document.createElement("i");
      iconNode.className = `ti ti-${iconName}`;

      const labelNode = document.createElement("span");
      labelNode.textContent = iconName;

      button.appendChild(iconNode);
      button.appendChild(labelNode);
      button.addEventListener("click", () => {
        iconEl.value = iconName;
        syncIconPickerState();
      });
      fragment.appendChild(button);
    });
    pickerEl.appendChild(fragment);

    updateIconCount(source.length, visibleIcons.length, hasQuery);
    syncIconPickerState();
  }

  async function hydrateIconCatalog() {
    try {
      const response = await fetch(TABLER_ICON_CSS_PATH, { cache: "force-cache" });
      if (!response.ok) return;
      const cssText = await response.text();
      const parsedIcons = extractIconNamesFromCss(cssText);
      if (!parsedIcons.length) return;

      allTablerIcons = dedupeIconNames([...FEATURED_ICON_NAMES, ...parsedIcons]);
      renderIconPicker();
      syncIconPickerState();
    } catch (_) {
      // Fallback icons are already loaded.
    }
  }

  function applyLinkVisualDefaults() {
    const iconEl = qs("#linkIcon");
    const iconColorEl = qs("#linkIconColor");
    if (!iconEl || !iconColorEl) return;

    const normalizedIcon = normalizeIconName(iconEl.value);
    if (iconEl.value !== normalizedIcon) {
      iconEl.value = normalizedIcon;
    }
    if (!iconColorEl.value || !iconColorEl.value.trim()) {
      iconColorEl.value = DEFAULT_ICON_COLOR;
    }
    syncIconPickerState();
  }

  function syncIconPickerState() {
    const pickerEl = qs("#linkIconPicker");
    const iconEl = qs("#linkIcon");
    if (!pickerEl || !iconEl) return;

    const selectedIcon = normalizeIconName(iconEl.value);
    const items = Array.from(pickerEl.querySelectorAll(".iconpick__item"));
    items.forEach((item) => {
      const isActive = item.dataset.icon === selectedIcon;
      item.classList.toggle("is-active", isActive);
    });
  }

  function bindIconPicker() {
    const searchEl = qs("#linkIconSearch");
    const iconEl = qs("#linkIcon");
    if (!searchEl || !iconEl) return;
    searchEl.addEventListener("input", () => {
      isIconSearchManual = searchEl.value.trim().length > 0;
      renderIconPicker();
    });

    if (!iconEl.value) iconEl.value = DEFAULT_ICON_NAME;
    renderIconPicker();
    hydrateIconCatalog();
  }

  function normalizeName(value) {
    return (value ?? "").trim().toLowerCase();
  }

  function normalizeUrl(value) {
    return parseNormalizedUrl(value);
  }

  function isTabNameDuplicate() {
    const tabNameEl = qs("#tabName");
    const tabSelectEl = qs("#linkTabId");
    if (!tabNameEl || !tabSelectEl) return false;

    const target = normalizeName(tabNameEl.value);
    if (!target) return false;

    return Array.from(tabSelectEl.options)
      .filter((option) => option.value !== "")
      .some((option) => normalizeName(option.textContent) === target);
  }

  function isCategoryNameDuplicate() {
    const tabEl = qs("#categoryTabId");
    const categoryNameEl = qs("#categoryName");
    const categorySelectEl = qs("#linkCategoryId");
    if (!tabEl || !categoryNameEl || !categorySelectEl) return false;

    const tabId = tabEl.value;
    const categoryName = normalizeName(categoryNameEl.value);
    if (!tabId || !categoryName) return false;

    return Array.from(categorySelectEl.options)
      .filter((option) => option.value !== "" && option.dataset.tabId === tabId)
      .some((option) => normalizeName(option.dataset.categoryName || option.textContent) === categoryName);
  }

  function isLinkDuplicate() {
    const categoryEl = qs("#linkCategoryId");
    const linkNameEl = qs("#linkName");
    const linkUrlEl = qs("#linkUrl");
    if (!categoryEl || !linkNameEl || !linkUrlEl) return { duplicateName: false, duplicateUrl: false };

    const categoryId = categoryEl.value;
    if (!categoryId) return { duplicateName: false, duplicateUrl: false };

    const targetName = normalizeName(linkNameEl.value);
    const targetUrl = normalizeUrl(linkUrlEl.value);
    const rows = Array.from(document.querySelectorAll(`.linkrow[data-category-id="${categoryId}"]`));

    const duplicateName = targetName
      ? rows.some((row) => normalizeName(row.dataset.linkName) === targetName)
      : false;
    const duplicateUrl = targetUrl
      ? rows.some((row) => normalizeUrl(row.dataset.linkUrl) === targetUrl)
      : false;

    return { duplicateName, duplicateUrl };
  }

  function saveCategoryDraft() {
    const tabEl = qs("#categoryTabId");
    if (!tabEl) return;
    sessionStorage.setItem(
      STORAGE_KEYS.categoryDraft,
      JSON.stringify({ tabId: tabEl.value }),
    );
  }

  function restoreCategoryDraft() {
    const raw = sessionStorage.getItem(STORAGE_KEYS.categoryDraft);
    const tabEl = qs("#categoryTabId");
    const nameEl = qs("#categoryName");
    if (!tabEl || !nameEl) return;
    nameEl.value = "";
    if (!raw) return;

    try {
      const draft = JSON.parse(raw);
      if (draft?.tabId && Array.from(tabEl.options).some((opt) => opt.value === draft.tabId)) {
        tabEl.value = draft.tabId;
      }
      nameEl.value = "";
    } catch (_) {
      sessionStorage.removeItem(STORAGE_KEYS.categoryDraft);
      nameEl.value = "";
    }
  }

  function saveLinkDraft() {
    const tabEl = qs("#linkTabId");
    const catEl = qs("#linkCategoryId");
    if (!tabEl || !catEl) return;
    sessionStorage.setItem(
      STORAGE_KEYS.linkDraft,
      JSON.stringify({
        tabId: tabEl.value,
        categoryId: catEl.value,
      }),
    );
  }

  function filterLinkCategoriesByTab() {
    const tabEl = qs("#linkTabId");
    const catEl = qs("#linkCategoryId");
    if (!tabEl || !catEl) return;

    const selectedTabId = tabEl.value;
    const options = Array.from(catEl.options).filter((opt) => opt.value !== "");

    options.forEach((option) => {
      const tabId = option.dataset.tabId;
      const isMatch = selectedTabId && tabId === selectedTabId;
      option.hidden = !isMatch;
      option.disabled = !isMatch;
    });

    if (!selectedTabId) {
      catEl.value = "";
      return;
    }

    const selectedOption = catEl.selectedOptions[0];
    if (!selectedOption || selectedOption.disabled) {
      catEl.value = "";
    }
  }

  function restoreLinkDraft() {
    const raw = sessionStorage.getItem(STORAGE_KEYS.linkDraft);
    const tabEl = qs("#linkTabId");
    const catEl = qs("#linkCategoryId");
    const nameEl = qs("#linkName");
    const urlEl = qs("#linkUrl");
    if (!tabEl || !catEl || !nameEl || !urlEl) return;
    nameEl.value = "";
    urlEl.value = "";

    if (!raw) {
      filterLinkCategoriesByTab();
      return;
    }

    try {
      const draft = JSON.parse(raw);
      if (draft?.tabId && Array.from(tabEl.options).some((opt) => opt.value === draft.tabId)) {
        tabEl.value = draft.tabId;
      }
      filterLinkCategoriesByTab();
      if (draft?.categoryId && Array.from(catEl.options).some((opt) => opt.value === draft.categoryId && !opt.disabled)) {
        catEl.value = draft.categoryId;
      }
    } catch (_) {
      sessionStorage.removeItem(STORAGE_KEYS.linkDraft);
      filterLinkCategoriesByTab();
    }
  }

  function bindFilePickers() {
    const fileInputs = document.querySelectorAll(".filepick__input");

    fileInputs.forEach((input) => {
      input.addEventListener("change", () => {
        const wrap = input.closest(".filepick");
        const nameEl = wrap?.querySelector(".filepick__name");
        if (!nameEl) return;

        const emptyLabel = nameEl.dataset.emptyLabel || "선택된 파일 없음";
        const fileName = input.files && input.files.length > 0 ? input.files[0].name : emptyLabel;
        nameEl.textContent = fileName;
      });
    });
  }

  function getCsrfConfig() {
    const token = document.querySelector('meta[name="_csrf"]')?.getAttribute("content");
    const header = document.querySelector('meta[name="_csrf_header"]')?.getAttribute("content");
    return { token, header };
  }

  function getSortAfterElement(container, y) {
    const items = Array.from(container.querySelectorAll(".sortitem:not(.is-dragging)"));
    let closest = { offset: Number.NEGATIVE_INFINITY, element: null };

    items.forEach((item) => {
      const box = item.getBoundingClientRect();
      const offset = y - box.top - box.height / 2;
      if (offset < 0 && offset > closest.offset) {
        closest = { offset, element: item };
      }
    });

    return closest.element;
  }

  function getSortIds(container) {
    return Array.from(container.querySelectorAll(".sortitem"))
      .map((item) => item.dataset.id)
      .filter((id) => id);
  }

  async function postSortOrder(container) {
    const endpoint = container.dataset.sortEndpoint;
    const ids = getSortIds(container);
    if (!endpoint || ids.length === 0) return;

    const params = new URLSearchParams();
    params.set("ids", ids.join(","));
    if (container.dataset.parentId) {
      params.set("parentId", container.dataset.parentId);
    }

    const headers = {
      "Content-Type": "application/x-www-form-urlencoded;charset=UTF-8",
    };
    const { token, header } = getCsrfConfig();
    if (token && header) headers[header] = token;

    const response = await fetch(endpoint, {
      method: "POST",
      headers,
      credentials: "same-origin",
      body: params.toString(),
    });
    if (!response.ok) {
      throw new Error(`Sort save failed (${response.status})`);
    }
  }

  function bindSortableLists() {
    const sortLists = Array.from(document.querySelectorAll(".sortlist"));
    if (!sortLists.length) return;

    sortLists.forEach((container) => {
      container.querySelectorAll(".sortitem").forEach((item) => {
        item.draggable = false;
        const handle = item.querySelector(".sort-handle");
        if (handle) handle.draggable = true;
      });

      let draggingItem = null;
      let beforeOrder = "";

      container.addEventListener("dragstart", (event) => {
        const handle = event.target.closest(".sort-handle");
        const item = handle?.closest(".sortitem");
        if (!item) {
          event.preventDefault();
          return;
        }
        draggingItem = item;
        beforeOrder = getSortIds(container).join(",");
        item.classList.add("is-dragging");
        event.dataTransfer.effectAllowed = "move";
        event.dataTransfer.setData("text/plain", item.dataset.id || "");
      });

      container.addEventListener("dragover", (event) => {
        if (!draggingItem) return;
        event.preventDefault();
        const afterElement = getSortAfterElement(container, event.clientY);
        if (!afterElement) {
          container.appendChild(draggingItem);
        } else if (afterElement !== draggingItem) {
          container.insertBefore(draggingItem, afterElement);
        }
      });

      container.addEventListener("drop", (event) => {
        if (!draggingItem) return;
        event.preventDefault();
      });

      container.addEventListener("dragend", async () => {
        if (!draggingItem) return;
        draggingItem.classList.remove("is-dragging");
        draggingItem = null;

        const afterOrder = getSortIds(container).join(",");
        if (beforeOrder === afterOrder) return;

        container.classList.add("is-saving");
        try {
          await postSortOrder(container);
          toast("순서가 저장되었습니다.");
        } catch (_) {
          toast("순서 저장 실패, 새로고침 후 다시 시도하세요.");
          window.location.reload();
        } finally {
          container.classList.remove("is-saving");
        }
      });
    });
  }

  function bindNpmLinkPicker() {
    document.querySelectorAll(".js-use-npm-link").forEach((button) => {
      button.addEventListener("click", () => {
        useNpmLink(button.dataset.npmUrl || "");
      });
    });
  }

  function bind() {
    const ham = qs("#hamburger");
    if (ham) ham.addEventListener("click", toggleSidebar);

    const clearBtn = qs("#clearLinkForm");
    if (clearBtn) clearBtn.addEventListener("click", clearLinkForm);

    const tabForm = qs("#tabForm");
    if (tabForm) {
      tabForm.addEventListener("submit", (event) => {
        if (!isTabNameDuplicate()) return;
        event.preventDefault();
        toast(DUPLICATE_MESSAGES.tab);
      });
    }

    const categoryForm = qs("#categoryForm");
    const categoryTabEl = qs("#categoryTabId");
    if (categoryTabEl) categoryTabEl.addEventListener("change", saveCategoryDraft);
    if (categoryForm) {
      categoryForm.addEventListener("submit", (event) => {
        if (isCategoryNameDuplicate()) {
          event.preventDefault();
          toast(DUPLICATE_MESSAGES.category);
          return;
        }
        saveCategoryDraft();
      });
    }

    const linkForm = qs("#linkForm");
    const linkTabEl = qs("#linkTabId");
    const linkCategoryEl = qs("#linkCategoryId");
    const linkNameEl = qs("#linkName");
    const linkUrlEl = qs("#linkUrl");
    if (linkTabEl) {
      linkTabEl.addEventListener("change", () => {
        filterLinkCategoriesByTab();
        saveLinkDraft();
      });
    }
    if (linkCategoryEl) linkCategoryEl.addEventListener("change", saveLinkDraft);
    if (linkNameEl) linkNameEl.addEventListener("input", () => applySuggestedIconSearch());
    if (linkUrlEl) linkUrlEl.addEventListener("input", () => applySuggestedIconSearch());
    bindIconPicker();
    if (linkForm) {
      linkForm.addEventListener("submit", (event) => {
        const { duplicateName, duplicateUrl } = isLinkDuplicate();
        if (duplicateName) {
          event.preventDefault();
          toast(DUPLICATE_MESSAGES.linkName);
          return;
        }
        if (duplicateUrl) {
          event.preventDefault();
          toast(DUPLICATE_MESSAGES.linkUrl);
          return;
        }
        saveLinkDraft();
      });
    }

    restoreCategoryDraft();
    restoreLinkDraft();
    applyLinkVisualDefaults();
    applySuggestedIconSearch();
    const serverToastMessage = qs("#serverToastMessage");
    const message = serverToastMessage?.dataset?.message;
    if (message) toast(message);

    bindFilePickers();
    bindSortableLists();
    bindNpmLinkPicker();

    document.addEventListener("click", (e) => {
      const sb = qs("#sidebar");
      const ham2 = qs("#hamburger");
      if (!sb || !sb.classList.contains("is-open")) return;
      const insideSidebar = sb.contains(e.target);
      const onHamburger = ham2 && ham2.contains(e.target);
      if (!insideSidebar && !onHamburger) sb.classList.remove("is-open");
    });
  }

  document.addEventListener("DOMContentLoaded", bind);

  return { useNpmLink };
})();

window.Dashboard = Dashboard;
