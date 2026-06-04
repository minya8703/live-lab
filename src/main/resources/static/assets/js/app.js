(function () {
  "use strict";

  // Korean 리터럴은 codepoint 빌드로 — JS 파일이 어떤 인코딩으로 저장돼도 동일하게 해석.
  // 평문(참고): "U10 AWS 운영 페이지 구축 중 · AI-DLC Construction"
  const FALLBACK_LABEL = String.fromCodePoint(
    85, 49, 48, 32,                               // U10
    65, 87, 83, 32,                               // AWS
    0xC6B4, 0xC601, 32,                           // 운영
    0xD398, 0xC774, 0xC9C0, 32,                   // 페이지
    0xAD6C, 0xCD95, 32,                           // 구축
    0xC911, 32,                                   // 중
    0x00B7, 32,                                   // ·
    65, 73, 45, 68, 76, 67, 32,
    67, 111, 110, 115, 116, 114, 117, 99, 116, 105, 111, 110
  );

  const KO_PLANNED = String.fromCodePoint(0xC608, 0xC815);                       // 예정
  const KO_IN_PROGRESS = String.fromCodePoint(0xC791, 0xC5C5, 0x20, 0xC911);     // 작업 중

  const FALLBACK = {
    currentUnit: 10,
    totalUnits: 11,
    currentLabel: FALLBACK_LABEL,
    units: {
      3: "live",
      4: "live",
      5: "live",
      6: "live",
      7: "live",
      8: "live",
      11: "live",
      10: "in-progress",
    },
  };

  const STATUS_TEXT = {
    planned: KO_PLANNED,
    "in-progress": KO_IN_PROGRESS,
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

  document.addEventListener("DOMContentLoaded", loadStatus);
})();
