(function () {
  "use strict";

  const tocEl = document.querySelector("[data-toc]");
  const entriesEl = document.querySelector("[data-entries]");

  function el(tag, className, text) {
    const node = document.createElement(tag);
    if (className) node.className = className;
    if (text != null) node.textContent = text;
    return node;
  }

  function renderToc(entries) {
    tocEl.innerHTML = "";
    entries.forEach(function (entry) {
      const li = document.createElement("li");
      const link = document.createElement("a");
      link.href = "#" + entry.slug;
      if (entry.unit != null) {
        const unitTag = el("span", "toc-unit", "U" + entry.unit);
        link.appendChild(unitTag);
      }
      link.appendChild(document.createTextNode(entry.title));
      li.appendChild(link);
      tocEl.appendChild(li);
    });
  }

  function renderEntries(entries) {
    entriesEl.innerHTML = "";
    if (entries.length === 0) {
      entriesEl.appendChild(el("p", "devlog-loading", "회고 항목이 없습니다."));
      return;
    }
    entries.forEach(function (entry) {
      const article = document.createElement("article");
      article.className = "devlog-entry";
      article.id = entry.slug;

      const header = el("header", "devlog-entry-header");
      const title = el("h2", "devlog-entry-title", entry.title);
      header.appendChild(title);

      if (entry.unit != null) {
        header.appendChild(el("span", "devlog-unit-badge", "U" + entry.unit));
      }
      if (entry.date) {
        header.appendChild(el("span", "devlog-date", entry.date));
      }
      if (entry.tags && entry.tags.length > 0) {
        const tagWrap = el("div", "devlog-tags");
        entry.tags.forEach(function (t) {
          tagWrap.appendChild(el("span", "devlog-tag", t));
        });
        header.appendChild(tagWrap);
      }

      article.appendChild(header);

      // 서버에서 렌더된 HTML — commonmark-java 의 출력은 안전하다고 가정.
      const content = el("div", "devlog-content");
      content.innerHTML = entry.htmlContent || "";
      article.appendChild(content);

      entriesEl.appendChild(article);
    });
  }

  async function load() {
    try {
      const res = await fetch("/api/devlog");
      if (!res.ok) throw new Error("devlog endpoint not ok");
      const entries = await res.json();
      renderToc(entries);
      renderEntries(entries);
    } catch (e) {
      entriesEl.innerHTML = "";
      entriesEl.appendChild(el("p", "devlog-loading", "회고를 불러오지 못했습니다."));
    }
  }

  load();
})();
