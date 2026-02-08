const Dashboard = (() => {
  const qs = (sel) => document.querySelector(sel);

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

  function useNpmLink(btn) {
    const name = btn?.dataset?.name ?? "";
    const url = btn?.dataset?.url ?? "";

    const nameEl = qs("#linkName");
    const urlEl = qs("#linkUrl");
    if (nameEl) nameEl.value = name;
    if (urlEl) urlEl.value = url;

    // 모바일에서 사이드바 열려있으면 닫아주기
    const sb = qs("#sidebar");
    if (sb && sb.classList.contains("is-open")) sb.classList.remove("is-open");

    toast("NPM 링크를 폼에 채웠어요");
    // 입력 포커스
    if (urlEl) urlEl.focus();
  }

  function clearLinkForm() {
    const nameEl = qs("#linkName");
    const urlEl = qs("#linkUrl");
    const tabEl = qs("#linkTabId");
    const catEl = qs("#linkCategoryId");
    if (nameEl) nameEl.value = "";
    if (urlEl) urlEl.value = "";
    if (tabEl) tabEl.selectedIndex = 0;
    if (catEl) catEl.selectedIndex = 0;
    toast("링크 폼 초기화");
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

  function bind() {
    const ham = qs("#hamburger");
    if (ham) ham.addEventListener("click", toggleSidebar);

    const clearBtn = qs("#clearLinkForm");
    if (clearBtn) clearBtn.addEventListener("click", clearLinkForm);
    bindFilePickers();

    // 바깥 클릭 시 모바일 사이드바 닫기
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
