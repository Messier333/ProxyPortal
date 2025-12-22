

class Links extends Component {
  constructor() { super(); }

  static getIcon(link) {
    const defaultColor = CONFIG.palette.base;

    return link.icon
      ? `<i class="ti ti-${link.icon} link-icon"
            style="color: ${link.icon_color ?? defaultColor}"></i>`
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
                  (link) => `
                  <div class="link-info">
                    <a class="link" href="${link.url}" target="_blank" rel="noopener noreferrer">
                      ${Links.getIcon(link)}
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
    if (!url) return "";
    return `style="background-image: url(${url}); background-repeat: no-repeat; background-size: contain;"`;
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
        width: 90%;
        max-width: 1200px;
        height: 450px;
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
        background: ${CONFIG.palette.base} url("../img/bg-1.gif") repeat left;
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
        background: transparent;
      }

      .categories ul .links {
        position: relative;
        flex: 1;
        height: 100%;
        width: 70%;
        background: ${CONFIG.palette.base};

        overflow-y: auto;
        overflow-x: hidden;
        box-sizing: border-box;
        scrollbar-gutter: stable;

        padding: 5%;
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

.categories ul .links {
  scrollbar-width: thin;
  scrollbar-color: ${CONFIG.palette.surface0} transparent;

  padding: 5%; 
  padding-right: calc(5% - 14px); 
}

      .categories ul .links li { list-style: none; }

      .categories ul .links .category-block:not(:last-child) {
        box-shadow: 0 1px 0 ${CONFIG.palette.text};
        padding: 0 0 .5em 0;
        margin-bottom: 1.5em;
      }

      .categories ul .links .category-title {
        position: sticky;
        top: var(--topbar-h); 
        z-index: 5;

        color: ${CONFIG.palette.text};
        opacity: 0.55;
        font-size: 13px;
        margin: 0 0 1em 0;
        font-weight: 600;
        letter-spacing: 1px;
        text-transform: uppercase;
        font-family: 'Raleway', sans-serif;

        background: ${CONFIG.palette.base};
        padding: .4em 0;
      }

      .categories ul .links .link {
        color: ${CONFIG.palette.text};
        text-decoration: none;
        font: 700 18px 'Roboto', sans-serif;
        transition: all .2s;
        display: inline-flex;
        align-items: center;
        padding: .4em .7em;
        background: ${CONFIG.palette.mantle};
        box-shadow: 0 4px ${CONFIG.palette.mantle}, 0 5px 10px rgb(0 0 0 / 20%);
        border-radius: 8px;
        margin-bottom: .7em;
        will-change: transform;
      }

      .categories .link-info { display: inline-flex; }
      .categories .link-info:not(:last-child) { margin-right: .5em; }

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
        overflow-wrap: break-word;
        width: 25px;
        height: 250px;
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
        font: 500 30px 'Nunito', sans-serif;
        text-align: center;
        flex-wrap: wrap;
        word-break: break-all;
        align-items: center;
        backdrop-filter: blur(3px);
        pointer-events: none;
      }

      .categories .link-icon { font-size: 27px; color: ${CONFIG.palette.text}; }
      .categories .link-icon + .link-name { margin-left: 10px; }
      .categories .links-wrapper { display: flex; flex-wrap: wrap; }

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

  connectedCallback() {
    this.render();
  }
}
