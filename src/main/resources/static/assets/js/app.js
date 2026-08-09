(function () {
  "use strict";

  const FALLBACK_LABEL = "U10 AWS 운영 페이지 구축 중 · AI-DLC Construction";

  const FALLBACK = {
    currentUnit: 10,
    totalUnits: 11,
    currentLabel: FALLBACK_LABEL,
    units: {
      3: "live",
      4: "live",
      5: "live",
      7: "live",
      8: "live",
      11: "planned",
      10: "in-progress",
    },
  };

  const STATUS_TEXT = {
    planned: "예정",
    "in-progress": "작업 중",
    live: "Live",
  };

  function applyStatus(status) {
    const statusEl = document.querySelector("[data-status]");
    if (statusEl) {
      const label = statusEl.querySelector(".status-label");
      const detail = statusEl.querySelector(".status-detail");
      if (label) {
        label.textContent =
          "Building · Unit " + status.currentUnit + " / " + status.totalUnits;
      }
      if (detail) detail.textContent = status.currentLabel;
    }

    document.querySelectorAll("[data-unit]").forEach(function (card) {
      const unitId = card.getAttribute("data-unit");
      const state = status.units[unitId] || "planned";
      const tag = card.querySelector("[data-tag-status]");
      if (tag) {
        tag.classList.remove("live", "in-progress");
        if (state === "live") tag.classList.add("live");
        if (state === "in-progress") tag.classList.add("in-progress");
        tag.textContent = STATUS_TEXT[state] || STATUS_TEXT.planned;
      }
      // 진행 중이거나 라이브이면 카드를 클릭 가능하게 만든다.
      const link = card.getAttribute("data-link");
      if (link && state !== "planned" && !card.dataset.linked) {
        card.dataset.linked = "1";
        card.style.cursor = "pointer";
        card.addEventListener("click", function () {
          window.location.href = link;
        });
      }
    });
  }

  function loadStatus() {
    fetch("/api/status", { headers: { Accept: "application/json" } })
      .then(function (res) {
        if (!res.ok) throw new Error("status endpoint unavailable");
        return res.json();
      })
      .then(applyStatus)
      .catch(function () {
        applyStatus(FALLBACK);
      });
  }

  function initFadeIn() {
    var els = document.querySelectorAll(".fade-in");
    if (!els.length) return;

    if (!("IntersectionObserver" in window)) {
      els.forEach(function (el) { el.classList.add("visible"); });
      return;
    }

    var observer = new IntersectionObserver(
      function (entries) {
        entries.forEach(function (entry) {
          if (entry.isIntersecting) {
            entry.target.classList.add("visible");
            observer.unobserve(entry.target);
          }
        });
      },
      { threshold: 0.15 }
    );

    els.forEach(function (el) { observer.observe(el); });
  }

  document.addEventListener("DOMContentLoaded", function () {
    loadStatus();
    initFadeIn();
  });
})();
