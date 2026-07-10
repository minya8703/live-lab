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
    var d = dateStr.substring(0, 10).split("-");
    return d[0] + "년 " + parseInt(d[1]) + "월 " + parseInt(d[2]) + "일";
  }

  function parseTags(tagsStr) {
    if (!tagsStr) return [];
    return tagsStr.split(",").map(function (t) { return t.trim(); }).filter(Boolean);
  }

  // 태그 필터
  function renderTagFilter(posts) {
    if (!filterEl) return;
    filterEl.innerHTML = "";
    var tagCount = {};
    posts.forEach(function (p) {
      parseTags(p.tags).forEach(function (t) {
        tagCount[t] = (tagCount[t] || 0) + 1;
      });
    });
    if (Object.keys(tagCount).length === 0) return;

    var allBtn = el("button", "velog-filter-btn" + (activeTag === null ? " active" : ""), "전체");
    allBtn.addEventListener("click", function () { activeTag = null; renderFiltered(); });
    filterEl.appendChild(allBtn);

    Object.keys(tagCount).sort().forEach(function (tag) {
      var btn = el("button", "velog-filter-btn" + (activeTag === tag ? " active" : ""), tag);
      btn.addEventListener("click", function () { activeTag = tag; renderFiltered(); });
      filterEl.appendChild(btn);
    });
  }

  // velog 스타일 카드
  function renderCard(post) {
    var card = el("article", "velog-card");

    var link = document.createElement("a");
    link.href = "/blog/post.html?slug=" + encodeURIComponent(post.slug);
    link.className = "velog-card-link";

    // 썸네일
    if (post.thumbnailUrl) {
      var thumbWrap = el("div", "velog-thumb-wrap");
      var img = document.createElement("img");
      img.className = "velog-thumb";
      img.src = post.thumbnailUrl;
      img.alt = post.title;
      img.loading = "lazy";
      thumbWrap.appendChild(img);
      link.appendChild(thumbWrap);
    }

    // 본문 영역
    var body = el("div", "velog-card-body");

    body.appendChild(el("h2", "velog-title", post.title));

    if (post.summary) {
      body.appendChild(el("p", "velog-summary", post.summary));
    }

    // 태그
    var tags = parseTags(post.tags);
    if (tags.length > 0) {
      var tagWrap = el("div", "velog-tags");
      tags.forEach(function (tag) {
        var tagEl = el("span", "velog-tag", tag);
        tagEl.addEventListener("click", function (e) {
          e.preventDefault();
          e.stopPropagation();
          activeTag = tag;
          renderFiltered();
        });
        tagWrap.appendChild(tagEl);
      });
      body.appendChild(tagWrap);
    }

    link.appendChild(body);
    card.appendChild(link);

    // 하단 메타
    var footer = el("div", "velog-card-footer");
    var dateSpan = el("span", "velog-date", formatDate(post.createdAt));
    footer.appendChild(dateSpan);
    card.appendChild(footer);

    return card;
  }

  function renderList(posts) {
    postsEl.innerHTML = "";
    if (posts.length === 0) {
      postsEl.appendChild(el("p", "blog-loading", "아직 작성된 글이 없습니다."));
      return;
    }
    posts.forEach(function (post) {
      postsEl.appendChild(renderCard(post));
    });
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
