(function () {
  "use strict";

  const FALLBACK = {
    units: {
      3: "live",
      4: "live",
      5: "live",
      7: "live",
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
        label.textContent = "Live · 4 demos available";
      }
      if (detail) detail.textContent = "Kafka · Redis · AI Q&A · 운영 회고";
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
