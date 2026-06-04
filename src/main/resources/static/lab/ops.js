(function () {
  "use strict";

  function el(tag, className, text) {
    const node = document.createElement(tag);
    if (className) node.className = className;
    if (text != null) node.textContent = text;
    return node;
  }

  function renderEntry(entry) {
    const article = document.createElement("article");
    article.className = "devlog-entry";
    article.id = entry.slug;

    const header = el("header", "devlog-entry-header");
    header.appendChild(el("h2", "devlog-entry-title", entry.title));

    const pill = el("span", "devlog-cat-pill " + (entry.category || ""),
        (entry.category || "").toUpperCase());
    header.appendChild(pill);

    if (entry.date) header.appendChild(el("span", "devlog-date", entry.date));

    if (entry.tags && entry.tags.length > 0) {
      const tagWrap = el("div", "devlog-tags");
      entry.tags.forEach(function (t) {
        tagWrap.appendChild(el("span", "devlog-tag", t));
      });
      header.appendChild(tagWrap);
    }

    article.appendChild(header);

    const content = el("div", "devlog-content");
    content.innerHTML = entry.htmlContent || "";
    article.appendChild(content);

    return article;
  }

  function renderInto(container, entries) {
    container.innerHTML = "";
    if (entries.length === 0) {
      container.appendChild(el("p", "devlog-loading", "아직 항목이 없습니다."));
      return;
    }
    entries.forEach(function (e) { container.appendChild(renderEntry(e)); });
  }

  async function load() {
    const runbookEl = document.querySelector('[data-cat="runbook"]');
    const incidentEl = document.querySelector('[data-cat="incident"]');
    try {
      const res = await fetch("/api/ops");
      if (!res.ok) throw new Error("ops endpoint not ok");
      const all = await res.json();
      renderInto(runbookEl, all.filter(function (e) { return e.category === "runbook"; }));
      renderInto(incidentEl, all.filter(function (e) { return e.category === "incident"; }));
    } catch (e) {
      const msg = "콘텐츠를 불러오지 못했습니다.";
      runbookEl.innerHTML = "";
      runbookEl.appendChild(el("p", "devlog-loading", msg));
      incidentEl.innerHTML = "";
      incidentEl.appendChild(el("p", "devlog-loading", msg));
    }
  }

  load();
})();
