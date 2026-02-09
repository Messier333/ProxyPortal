// Statusbar component for tab navigation and widgets

class Statusbar extends Component {
  externalRefs = {};

  // ⚠️ 이 프레임워크는 refs를 자동으로 DOM으로 치환할 수 있음
  refs = {
    categories: ".categories ul",
    tabs: "#tabs ul li",
    indicator: ".indicator",
    fastlink: ".fastlink",
  };

  // ✅ querySelector용 "고정 selector" (절대 DOM으로 바뀌지 않음)
  staticSelectors = {
    tabs: "#tabs ul li",
    indicator: ".indicator",
    fastlink: ".fastlink",
  };

  dom = {
    tabs: null,       // NodeList
    indicator: null,  // Element|null
    fastlink: null,   // Element|null
  };

  currentTabIndex = 0;
  lastArrowNavAt = 0;

  constructor() {
    super();
    this.setDependencies();
  }

  setDependencies() {
    this.externalRefs = {
      categories: this.parentNode.querySelectorAll(this.refs.categories),
    };
  }

  imports() {
    return [
      this.getFontResource("roboto"),
      this.getIconResource("material"),
      this.getLibraryResource("awoo"),
    ];
  }

  style() {
    return `
      *:not(:defined) { display: none; }
      #tabs, #tabs .widgets, #tabs ul li:last-child { position: absolute; }
      #tabs { width: 100%; height: 100%; }
      #tabs ul {
        counter-reset: tabs; height: 100%; position: relative;
        list-style: none; margin-left: 1em;
      }
      #tabs ul li:not(:last-child)::after {
        content: counter(tabs, decimal);
        counter-increment: tabs;
        display: flex; width: 100%; height: 100%;
        position: relative; align-items: center;
        text-align: center; justify-content: center;
      }
      #tabs ul li:not(:last-child) {
        width: 35px; text-align: center;
        font: 300 12px 'Roboto', sans-serif;
        color: ${CONFIG.palette.text};
        padding: 6px 0; transition: all .1s;
        cursor: pointer; line-height: 0; height: 100%;
      }
      #tabs ul li:not(:last-child):hover { background: ${CONFIG.palette.surface0}; }
      #tabs ul li:last-child {
        --flavour: var(--accent);
        width: 35px; height: 3px;
        background: var(--flavour);
        bottom: 0; transition: all .3s;
      }
      #tabs ul li[active]:not(:last-child) {
        color: ${CONFIG.palette.text}; font-size: 13px; padding: 6px 0;
      }
      #tabs ul li[active]:nth-child(2) ~ li:last-child { margin: 0 0 0 35px; }
      #tabs ul li[active]:nth-child(3) ~ li:last-child { margin: 0 0 0 70px; }
      #tabs ul li[active]:nth-child(4) ~ li:last-child { margin: 0 0 0 105px; }
      #tabs ul li[active]:nth-child(5) ~ li:last-child { margin: 0 0 0 140px; }

      #tabs ul li[active]:nth-child(1) ~ li:last-child { --flavour: ${CONFIG.palette.green}; }
      #tabs ul li[active]:nth-child(2) ~ li:last-child { --flavour: ${CONFIG.palette.peach}; }
      #tabs ul li[active]:nth-child(3) ~ li:last-child { --flavour: ${CONFIG.palette.red}; }
      #tabs ul li[active]:nth-child(4) ~ li:last-child { --flavour: ${CONFIG.palette.blue}; }
      #tabs ul li[active]:nth-child(5) ~ li:last-child { --flavour: ${CONFIG.palette.mauve}; }

      .widgets { right: 0; margin: auto; height: 32px; color: #fff; font-size: 12px; }
      .widget { position: relative; height: 100%; padding: 0 1em; }
      .widget.time-widget { min-width: max-content; }
      .widget:first-child { padding-left: 2em; }
      .widget:last-child { padding-right: 2em; }
      .widget:hover { cursor: pointer; background: rgba(255, 255, 255, .05); }

      #tabs > cols {
        position: relative;
        grid-template-columns: [chat-tab] 35px [tabs] auto [widgets] auto;
      }
      #tabs .time span { font-weight: 400; }
      #tabs i { font-size: 14pt !important; }

      .widget:not(:first-child)::before {
        content: '';
        position: absolute; display: block; left: 0;
        height: calc(100% - 15px); width: 1px;
        background: rgb(255 255 255 / 10%);
      }

      .fastlink {
        border: 0; background: ${CONFIG.palette.mantle};
        color: ${CONFIG.palette.green}; cursor: pointer;
        border-radius: 5px 15px 15px 5px;
      }
      .fastlink:hover { filter: brightness(1.2); }
      .fastlink-icon { width: 70%; }

      .logout-widget, .dashboard-widget {
        background: transparent; border: 0;
        font: 300 9pt 'Roboto', sans-serif;
        color: ${CONFIG.palette.text};
        letter-spacing: .5px; padding: 10;
      }

      .search-widget {
        background: transparent;
        border: 0;
        color: ${CONFIG.palette.text};
        display: flex;
        align-items: center;
        justify-content: center;
        width: 32px;
        min-width: 32px;
        padding: 0;
        border-radius: 4px;
      }

      .search-widget:first-child {
        padding-left: 0;
      }

      .search-widget .material-icons {
        font-size: 16px !important;
        line-height: 1;
      }
    `;
  }

  template() {
    return `
      <div id="tabs">
        <cols>
          <button class="+ fastlink" type="button">
            <img class="fastlink-icon" src="portal/src/img/favicon.png"/>
          </button>
          <ul class="- indicator"></ul>
          <div class="+ widgets col-end">
            <button class="+ widget search-widget" type="button" title="Search (S)"><i class="material-icons">search</i></button>
            <button class="+ widget dashboard-widget" type="button" title="Dashboard">Dashboard</button>
            <button class="+ widget logout-widget" type="button" title="Logout">Logout</button>
            <current-time class="+ widget time-widget"></current-time>
            <weather-forecast class="+ widget weather"></weather-forecast>
          </div>
        </cols>
      </div>
    `;
  }

  // selector/element 모두 안전 처리
  getEl(selOrEl, fallbackSelector) {
    if (!selOrEl) return this.shadow.querySelector(fallbackSelector);
    if (selOrEl instanceof Element) return selOrEl;
    if (typeof selOrEl === "string") return this.shadow.querySelector(selOrEl);
    return this.shadow.querySelector(fallbackSelector);
  }

  // NodeList가 필요할 때
  getAll(selOrEls, fallbackSelector) {
    if (!selOrEls) return this.shadow.querySelectorAll(fallbackSelector);
    if (typeof selOrEls === "string") return this.shadow.querySelectorAll(selOrEls);
    if (typeof selOrEls.forEach === "function") return selOrEls; // NodeList/Array
    return this.shadow.querySelectorAll(fallbackSelector);
  }

  cacheDom() {
    this.dom.fastlink = this.getEl(this.refs.fastlink, this.staticSelectors.fastlink);
    this.dom.indicator = this.getEl(this.refs.indicator, this.staticSelectors.indicator);

    // ✅ tabs는 refs가 li element로 바뀔 수도 있으니 "항상 고정 selector"로 잡는다
    this.dom.tabs = this.shadow.querySelectorAll(this.staticSelectors.tabs);
  }

  createTabs() {
    if (!this.dom.indicator) return;

    const categoriesCount = this.externalRefs.categories?.length ?? 0;

    this.dom.indicator.innerHTML = "";

    for (let i = 0; i <= categoriesCount; i++) {
      this.dom.indicator.innerHTML += `<li tab-index=${i} ${i === 0 ? "active" : ""}></li>`;
    }

    // ✅ li가 추가됐으니 다시 고정 selector로 재캐싱
    this.dom.tabs = this.shadow.querySelectorAll(this.staticSelectors.tabs);
  }

  setEvents() {
    // 탭 클릭
    if (this.dom.tabs && this.dom.tabs.length > 0) {
      this.dom.tabs.forEach((tab) => {
        tab.onclick = ({ target }) => this.handleTabChange(target);
      });
    }

    document.onkeydown = (e) => this.handleKeyPress(e);

    this.dom.fastlink?.addEventListener("click", () => {
      if (CONFIG.config.fastlink) window.location.href = CONFIG.config.fastlink;
    });

    if (CONFIG.openLastVisitedTab) {
      window.onbeforeunload = () => this.saveCurrentTab();
    }

    this.shadow.querySelector(".search-widget")?.addEventListener("click", (e) => {
      e.stopPropagation();
      const search = RenderedComponents["search-bar"];
      if (!search || !search.refs?.search) return;

      if (search.refs.search.classList.contains("active")) search.deactivate();
      else search.activate();
    });

    this.shadow.querySelector(".dashboard-widget")?.addEventListener("click", (e) => {
      e.stopPropagation();
      window.location.href = "/dashboard";
    });

    this.shadow.querySelector(".logout-widget")?.addEventListener("click", async (e) => {
      e.stopPropagation();

      const tokenMeta = document.querySelector('meta[name="_csrf"]');
      const headerMeta = document.querySelector('meta[name="_csrf_header"]');

      const token = tokenMeta?.getAttribute("content");
      const header = headerMeta?.getAttribute("content");

      if (!token || !header) {
        console.error("CSRF meta not found. Check index.html meta tags.");
        return;
      }

      const res = await fetch("/logout", {
        method: "POST",
        headers: { [header]: token },
        credentials: "same-origin",
      });

      if (res.ok || res.status === 302) {
        window.location.href = "/login";
      } else {
        console.error("Logout failed:", res.status);
      }
    });
  }

  saveCurrentTab() {
    localStorage.lastVisitedTab = this.currentTabIndex;
  }

  openLastVisitedTab() {
    if (!CONFIG.openLastVisitedTab) return;
    this.activateByKey(Number(localStorage.lastVisitedTab ?? 0));
  }

  handleTabChange(tab) {
    const li = tab?.closest?.("li") ?? tab;
    this.activateByKey(Number(li.getAttribute("tab-index")));
  }

  handleKeyPress(event) {
    if (!event) return;
    const { key } = event;

    // Ignore global tab navigation while user is typing in an editable control.
    const active = document.activeElement;
    const tag = active?.tagName?.toLowerCase?.() ?? "";
    const isTypingTarget =
      tag === "input" ||
      tag === "textarea" ||
      tag === "select" ||
      Boolean(active?.isContentEditable);
    if (isTypingTarget) return;

    // If search overlay is open, arrow keys are used by the search component.
    const search = RenderedComponents["search-bar"];
    if (search?.refs?.search?.classList?.contains("active")) return;

    const categoriesLength = this.externalRefs.categories?.length ?? 0;

    if (key === "ArrowRight" || key === "ArrowLeft") {
      if (categoriesLength <= 0) return;
      // Key-repeat can fire much faster than panel transitions, causing jitter.
      if (event.repeat) {
        const now = Date.now();
        if (now - this.lastArrowNavAt < 120) return;
        this.lastArrowNavAt = now;
      } else {
        this.lastArrowNavAt = Date.now();
      }
      event.preventDefault();

      const direction = key === "ArrowRight" ? 1 : -1;
      const nextIndex = (this.currentTabIndex + direction + categoriesLength) % categoriesLength;
      this.activateByKey(nextIndex);
      return;
    }

    if (Number.isInteger(parseInt(key)) && key <= categoriesLength) {
      this.activateByKey(key - 1);
    }
  }

  activateByKey(key) {
    if (key < 0) return;
    if (!this.dom.tabs || this.dom.tabs.length === 0) return;
    if (key >= this.dom.tabs.length) return;

    this.currentTabIndex = key;

    this.activate(this.dom.tabs, this.dom.tabs[key]);

    if (this.externalRefs.categories && this.externalRefs.categories.length > key) {
      this.activate(this.externalRefs.categories, this.externalRefs.categories[key]);
    }
  }

  activate(target, item) {
    if (!target || typeof target.forEach !== "function") return;
    if (!item) return;
    target.forEach((i) => i.removeAttribute("active"));
    item.setAttribute("active", "");
  }

  connectedCallback() {
    this.render().then(() => {
      this.cacheDom();
      this.createTabs();
      this.setEvents();
      this.openLastVisitedTab();
    });
  }
}
