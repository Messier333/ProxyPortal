const TAB_FLAVOURS = [
  CONFIG.palette.green,
  CONFIG.palette.peach,
  CONFIG.palette.red,
  CONFIG.palette.blue,
  CONFIG.palette.mauve,
];

function getLinkFlavour(index) {
  if (!Number.isInteger(index) || index < 0) return TAB_FLAVOURS[0] ?? CONFIG.palette.green;
  return TAB_FLAVOURS[index % TAB_FLAVOURS.length] ?? CONFIG.palette.green;
}

class Links extends Component {
  constructor() { super(); }

  static getIcon(link, linkIndex) {
    const rawColor = link?.icon_color ?? "";
    const normalizedColor = String(rawColor).trim().toLowerCase();
    const defaultColor = getLinkFlavour(linkIndex);
    const iconColor = normalizedColor === "" || normalizedColor === "#89b4fa"
      ? defaultColor
      : rawColor;

    return link.icon
      ? `<i class="ti ti-${link.icon} link-icon"
            style="color: ${iconColor}"></i>`
      : "";
  }

  static getAll(tabName, tabs) {
    const found = tabs.find((f) => f.name === tabName);
    const categories = found?.categories ?? [];

    return `
      ${categories
        .map(({ name, links }) => {
          const safeLinks = Array.isArray(links) ? links : [];
          return `
          <li class="category-block">
            <h1 class="category-title">${name ?? ""}</h1>
            <div class="links-wrapper">
              ${safeLinks
                .map(
                  (link, linkIndex) => `
                  <div class="link-info">
                    <a class="link" href="${link.url}" target="_blank" rel="noopener noreferrer">
                      ${Links.getIcon(link, linkIndex)}
                      ${link.name ? `<p class="link-name">${link.name}</p>` : ""}
                    </a>
                  </div>`,
                )
                .join("")}
            </div>
          </li>`;
        })
        .join("")}
    `;
  }
}

class Category extends Component {
  constructor() { super(); }

  static getBackgroundStyle(url) {
    if (!url) {
      return `style="--tab-bg: none;"`;
    }

    return `style="--tab-bg: url('${url}');"`;
  }

  static getAll(tabs) {
    const safeTabs = Array.isArray(tabs) ? tabs : [];
    return `
      ${safeTabs
        .map(({ name, background_url }, index) => {
          return `<ul class="${name}" ${Category.getBackgroundStyle(background_url)} ${
            index === 0 ? "active" : ""
          }>
            <div class="banner"></div>
            <div class="links">
              ${Links.getAll(name, safeTabs)}
            </div>
          </ul>`;
        })
        .join("")}
    `;
  }
}

class Tabs extends Component {
  refs = {};

  constructor() {
    super();
    this.tabs = CONFIG.tabs;
  }

  imports() {
    return [
      this.getIconResource("material"),
      this.resources.icons.tabler,
      this.getFontResource("roboto"),
      this.getFontResource("raleway"),
      this.getLibraryResource("awoo"),
    ];
  }

  style() {
    return `
      status-bar {
        bottom: -70px;
        height: 32px;
        background: ${CONFIG.palette.base};
        border-radius: 4px;
        box-shadow: 0 10px 20px rgba(0, 0, 0, .25);
      }

      :root { --topbar-h: 48px; }

      #panels, #panels ul {
        position: absolute;
      }

      .nav { color: #fff; }

      #panels {
        border-radius: 5px 0 0 5px;
        width: min(92vw, 1200px);
        max-width: 1200px;
        aspect-ratio: 8 / 3;
        height: auto;
        right: 0;
        left: 0;
        top: 0;
        bottom: 0;
        margin: auto;
        box-shadow: 0 5px 10px rgba(0, 0, 0, .2);
        background: ${CONFIG.palette.base};
      }

      .categories {
        width: 100%;
        height: 100%;
        overflow: hidden;
        position: relative;
        border-radius: 10px 0 0 10px;
      }

      .categories ul {
        --panelbg: transparent;
        --flavour: var(--accent);

        width: 100%;
        height: 100%;
        right: 100%;
        background-color: ${CONFIG.palette.base};
        transition: all .6s;

        display: flex;
        flex-direction: row;
      }

      .categories ul:nth-child(1) { --flavour: ${CONFIG.palette.green}; }
      .categories ul:nth-child(2) { --flavour: ${CONFIG.palette.peach}; }
      .categories ul:nth-child(3) { --flavour: ${CONFIG.palette.red}; }
      .categories ul:nth-child(4) { --flavour: ${CONFIG.palette.blue}; }
      .categories ul:nth-child(5) { --flavour: ${CONFIG.palette.mauve}; }

      .categories ul[active] {
        right: 0;
        z-index: 1;
      }

      .categories ul .banner {
        width: 30%;
        height: 100%;
        position: relative;
        flex: 0 0 30%;
        background-image: var(--tab-bg);
        background-repeat: no-repeat;
        background-size: cover;
        background-position: center center;
      }

      .categories ul .links {
        --links-pad-x: clamp(48px, 6.2vw, 72px);
        --links-pad-top: clamp(20px, 3vh, 30px);
        --links-pad-bottom: clamp(16px, 2.4vh, 24px);

        position: relative;
        flex: 1;
        height: 100%;
        width: 70%;
        background: ${CONFIG.palette.base};

        overflow-y: hidden;
        overflow-x: hidden;
        box-sizing: border-box;
        scrollbar-gutter: auto;
        display: grid;
        align-content: center;

        padding: var(--links-pad-top) var(--links-pad-x) var(--links-pad-bottom);
        box-shadow: inset -1px 0 var(--flavour);

        scroll-behavior: smooth;
        overscroll-behavior: contain;
      }

.categories ul .links::-webkit-scrollbar {
  width: 6px;
}

.categories ul .links::-webkit-scrollbar-track {
  background: transparent;
}

.categories ul .links::-webkit-scrollbar-thumb {
  background: ${CONFIG.palette.surface0};
  border-radius: 999px;
}

.categories ul .links::-webkit-scrollbar-thumb:hover {
  background: ${CONFIG.palette.overlay0};
}

.categories ul .links::-webkit-scrollbar-button {
  display: none !important;
  width: 0 !important;
  height: 0 !important;
}

.categories ul .links.has-overflow {
  overflow-y: auto;
  scrollbar-gutter: stable;
  scrollbar-width: thin;
  scrollbar-color: ${CONFIG.palette.surface0} transparent;
  align-content: start;

  padding-right: calc(var(--links-pad-x) - 12px);
}

      .categories ul .links li { list-style: none; }

      .categories ul .links .category-block:not(:last-child) {
        box-shadow: 0 1px 0 ${CONFIG.palette.text};
        padding: 0 0 .72em 0;
        margin-bottom: 1.5em;
      }
      .categories ul .links:not(.has-overflow) > .category-block {
        transform: translateY(-14px);
      }
      .categories ul .links .category-block:not(:last-child) .links-wrapper {
        margin-bottom: .34em;
      }
      .categories ul .links .category-title {
        position: sticky;
        top: var(--topbar-h); 
        z-index: 5;

        color: ${CONFIG.palette.text};
        opacity: 0.55;
        font-size: 13px;
        margin: 0 0 .9em 0;
        font-weight: 600;
        letter-spacing: 1px;
        text-transform: uppercase;
        font-family: 'Raleway', sans-serif;

        background: ${CONFIG.palette.base};
        padding: .3em 0;
      }

      .categories ul .links .link {
        color: ${CONFIG.palette.text};
        text-decoration: none;
        font: 700 18px 'Roboto', sans-serif;
        transition: all .2s;
        display: inline-flex;
        align-items: center;
        padding: .35em .65em;
        background: ${CONFIG.palette.mantle};
        box-shadow: 0 4px ${CONFIG.palette.mantle}, 0 5px 10px rgb(0 0 0 / 20%);
        border-radius: 8px;
        will-change: transform;
      }

      .categories .link-info { display: inline-flex; }

      .categories ul .links .link:hover {
        transform: translate(0, 4px);
        box-shadow: 0 0 rgba(0, 0, 0, 0.25), 0 0 0 rgba(0, 0, 0, .5), 0 -0px 5px rgba(0, 0, 0, .1);
        color: var(--flavour);
      }

      .categories ul::after {
        content: attr(class);
        position: absolute;
        display: flex;
        text-transform: uppercase;
        writing-mode: vertical-rl;
        text-orientation: upright;
        white-space: nowrap;
        overflow-wrap: break-word;
        width: 25px;
        height: 250px;
        max-height: calc(100% - 2em);
        padding: 1em;
        margin: auto;
        border-radius: 5px;
        box-shadow: inset 0 0 0 2px var(--flavour);
        left: calc(15% - 42.5px);
        bottom: 0;
        top: 0;
        background: linear-gradient(to top, rgb(50 48 47 / 90%), transparent);
        color: var(--flavour);
        letter-spacing: 1px;
        font: 500 clamp(14px, 1.6vw, 20px) 'Nunito', sans-serif;
        line-height: 1.05;
                justify-content: center;
        align-items: center;
        backdrop-filter: blur(3px);
        pointer-events: none;
        overflow: hidden;
      }

      .categories .link-icon { font-size: 27px; color: ${CONFIG.palette.text}; }
      .categories .link-icon + .link-name { margin-left: 10px; }
      .categories .link-name {
        max-width: 12ch;
        white-space: nowrap;
        overflow: hidden;
        text-overflow: ellipsis;
      }
      .categories .links-wrapper {
        display: flex;
        flex-wrap: wrap;
        gap: .5em;
      }

      .ti {
        animation: fadeInAnimation ease .5s;
        animation-iteration-count: 1;
        animation-fill-mode: forwards;
        height: 27px;
        width: 27px;
      }

      @keyframes fadeInAnimation {
        0% { opacity: 0; }
        100% { opacity: 1; }
      }

    `;
  }

  template() {
    return `
      <div id="links" class="-">
        <div id="panels">
          <div class="categories">
            ${Category.getAll(this.tabs)}
            <search-bar></search-bar>
          </div>
          <status-bar class="!-"></status-bar>
        </div>
      </div>
    `;
  }

  syncLinksOverflow() {
    const containers = this.shadow.querySelectorAll(".categories ul .links");
    containers.forEach((container) => {
      const hasOverflow = container.scrollHeight - container.clientHeight > 10;
      container.classList.toggle("has-overflow", hasOverflow);
    });
  }

  connectedCallback() {
    this.render().then(() => {
      this.syncLinksOverflow();
      requestAnimationFrame(() => this.syncLinksOverflow());
      setTimeout(() => this.syncLinksOverflow(), 300);

      this.handleResize = () => this.syncLinksOverflow();
      window.addEventListener("resize", this.handleResize);
    });
  }

  disconnectedCallback() {
    if (this.handleResize) {
      window.removeEventListener("resize", this.handleResize);
    }
  }
}
