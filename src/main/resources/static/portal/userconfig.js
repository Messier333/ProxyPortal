// User configuration for the startpage - update the palette, location, and your preferred tabs, categories, and links

// Define preferred palette for light and dark mode
// Available themes: latte, frappe, mocha, macchiato
const preferredLightTheme = latte;
const preferredDarkTheme = mocha;

let palette = initThemeSystem(preferredLightTheme, preferredDarkTheme);

const rawJson = document.getElementById("portal-config")?.textContent || "{}";
let injected = {};
try {
  injected = JSON.parse(rawJson);
} catch (e) {
  console.error("Invalid portal-config JSON", e);
  injected = {};
}

const injectedTabs = Array.isArray(injected.tabs) ? injected.tabs : [];

function normalizeTabs(tabs) {
  return tabs.map((t) => ({
    name: t.name,
    background_url: t.background_url,
    categories: (t.categories ?? []).map((c) => ({
      name: c.name,
      links: (c.links ?? [])
        .slice()
        .sort((a, b) => (a.sort_order ?? 0) - (b.sort_order ?? 0))
        .map((l) => ({
          name: l.name,
          url: l.url,
          icon: l.icon,
          icon_color: l.icon_color, // 서버가 hex("#a6e3a1") 주면 그대로 사용
          sort_order: l.sort_order,
        })),
    })),
  }));
}

const default_configuration = {
  overrideStorage: true,
  temperature: {
    location: "London",
    scale: "C",
  },
  clock: {
    format: "k:i p",
    icon_color: palette.maroon,
  },
  additionalClocks: [
    {
      label: "UA",
      timezone: "Europe/Kyiv",
      format: "h:i",
      icon_color: palette.peach,
    },
  ],
  search: {
    engines: {
      p: ["https://www.perplexity.ai/search/?q=", "PerplexityAI"],
      g: ["https://google.com/search?q=", "Google"],
    },
    default: "g",
  },
  keybindings: {
    s: "search-bar",
  },
  disabled: [],
  localIcons: true,
  localFonts: true,
  fastlink: "https://www.perplexity.ai",
  openLastVisitedTab: true,

  tabs: normalizeTabs(injectedTabs),
};

const CONFIG = new Config(default_configuration, palette);

const root = document.querySelector(":root");
root.style.setProperty("--bg", palette.mantle);
root.style.setProperty("--accent", palette.blue);
