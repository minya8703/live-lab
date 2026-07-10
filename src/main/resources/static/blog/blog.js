(function () {
  "use strict";

  var postsEl = document.querySelector("[data-posts]");
  var pagingEl = document.querySelector("[data-paging]");
  var filterEl = document.querySelector("[data-tag-filter]");
  var currentPage = 0;
  var PAGE_SIZE = 20;
  var allPosts = [];
  var activeTag = null;

  function el(tag, cls, text) {
    var node = document.createElement(tag);
    if (cls) node.className = cls;
    if (text != null) node.textContent = text;
    return node;
  }

  function formatDate(dateStr) {
    if (!dateStr) return "";
    return dateStr.substring(0, 10);
  }

  function parseTags(tagsStr) {
    if (!tagsStr) return [];
    return tagsStr.split(",").map(function (t) { return t.trim(); }).filter(Boolean);
  }

  // 태그 필터 렌더링
  function renderTagFilter(posts) {
    if (!filterEl) return;
    filterEl.innerHTML = "";
    var tagCount = {};
    posts.forEach(function (p) {
      parseTags(p.tags).forEach(function (t) {
        tagCount[t] = (tagCount[t] || 0) + 1;
      });
    });

    var allBtn = el("button", "blog-filter-btn" + (activeTag === null ? " active" : ""), "전체");
    allBtn.addEventListener("click", function () { activeTag = null; renderFiltered(); });
    filterEl.appendChild(allBtn);

    Object.keys(tagCount).sort().forEach(function (tag) {
      var btn = el("button", "blog-filter-btn" + (activeTag === tag ? " active" : ""), tag + " (" + tagCount[tag] + ")");
      btn.addEventListener("click", function () { activeTag = tag; renderFiltered(); });
      filterEl.appendChild(btn);
    });
  }

  // 리스트 렌더링
  function renderList(posts) {
    postsEl.innerHTML = "";
    if (posts.length === 0) {
      postsEl.appendChild(el("p", "blog-loading", "아직 작성된 글이 없습니다."));
      return;
    }

    var table = document.createElement("table");
    table.className = "blog-table";

    var thead = document.createElement("thead");
    var tr = document.createElement("tr");
    tr.appendChild(el("th", "blog-th-date", "날짜"));
    tr.appendChild(el("th", "blog-th-title", "제목"));
    tr.appendChild(el("th", "blog-th-tags", "카테고리"));
    thead.appendChild(tr);
    table.appendChild(thead);

    var tbody = document.createElement("tbody");
    posts.forEach(function (post) {
      var row = document.createElement("tr");
      row.className = "blog-row";
      row.addEventListener("click", function () {
        window.location.href = "/blog/post.html?slug=" + encodeURIComponent(post.slug);
      });

      var dateCell = el("td", "blog-cell-date", formatDate(post.createdAt));
      row.appendChild(dateCell);

      var titleCell = document.createElement("td");
      titleCell.className = "blog-cell-title";
      var titleLink = document.createElement("a");
      titleLink.href = "/blog/post.html?slug=" + encodeURIComponent(post.slug);
      titleLink.className = "blog-title-link";
      titleLink.textContent = post.title;
      titleCell.appendChild(titleLink);
      if (post.summary) {
        titleCell.appendChild(el("span", "blog-cell-summary", post.summary));
      }
      row.appendChild(titleCell);

      var tagCell = document.createElement("td");
      tagCell.className = "blog-cell-tags";
      parseTags(post.tags).forEach(function (tag) {
        var tagSpan = el("span", "blog-tag", tag);
        tagSpan.addEventListener("click", function (e) {
          e.stopPropagation();
          activeTag = tag;
          renderFiltered();
        });
        tagCell.appendChild(tagSpan);
      });
      row.appendChild(tagCell);

      tbody.appendChild(row);
    });
    table.appendChild(tbody);
    postsEl.appendChild(table);
  }

  function renderFiltered() {
    var filtered = activeTag
      ? allPosts.filter(function (p) { return parseTags(p.tags).indexOf(activeTag) !== -1; })
      : allPosts;
    renderTagFilter(allPosts);
    renderList(filtered);
  }

  function renderPaging(data) {
    pagingEl.innerHTML = "";
    if (data.totalPages <= 1) return;
    for (var i = 0; i < data.totalPages; i++) {
      var btn = el("button", "blog-page-btn", String(i + 1));
      if (i === data.number) btn.classList.add("active");
      btn.dataset.page = i;
      btn.addEventListener("click", function () { loadPage(parseInt(this.dataset.page)); });
      pagingEl.appendChild(btn);
    }
  }

  function loadPage(page) {
    currentPage = page;
    postsEl.innerHTML = "";
    postsEl.appendChild(el("p", "blog-loading", "불러오는 중..."));

    fetch("/api/blog?page=" + page + "&size=" + PAGE_SIZE)
      .then(function (res) {
        if (!res.ok) throw new Error("Failed");
        return res.json();
      })
      .then(function (data) {
        allPosts = data.content || [];
        renderFiltered();
        renderPaging(data);
      })
      .catch(function () {
        postsEl.innerHTML = "";
        postsEl.appendChild(el("p", "blog-loading", "블로그를 불러오지 못했습니다."));
      });
  }

  loadPage(0);
})();
