(function () {
  "use strict";

  var postsEl = document.querySelector("[data-posts]");
  var pagingEl = document.querySelector("[data-paging]");
  var currentPage = 0;
  var PAGE_SIZE = 12;

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

  function renderTags(tagsStr) {
    if (!tagsStr) return null;
    var tags = tagsStr.split(",").map(function (t) { return t.trim(); }).filter(Boolean);
    if (tags.length === 0) return null;
    var container = el("div", "blog-card-tags");
    tags.forEach(function (tag) {
      container.appendChild(el("span", "blog-tag", tag));
    });
    return container;
  }

  function renderCard(post) {
    var card = el("article", "blog-card");
    var link = document.createElement("a");
    link.href = "/blog/post.html?slug=" + encodeURIComponent(post.slug);
    link.style.border = "none";

    if (post.thumbnailUrl) {
      var img = document.createElement("img");
      img.className = "blog-card-thumb";
      img.src = post.thumbnailUrl;
      img.alt = post.title;
      img.loading = "lazy";
      link.appendChild(img);
    } else {
      link.appendChild(el("div", "blog-card-thumb-placeholder", "BLOG"));
    }

    var body = el("div", "blog-card-body");
    body.appendChild(el("h3", "blog-card-title", post.title));
    if (post.summary) {
      body.appendChild(el("p", "blog-card-summary", post.summary));
    }

    var meta = el("div", "blog-card-meta");
    meta.appendChild(el("span", "blog-card-date", formatDate(post.createdAt)));
    var tags = renderTags(post.tags);
    if (tags) meta.appendChild(tags);
    body.appendChild(meta);

    link.appendChild(body);
    card.appendChild(link);
    return card;
  }

  function renderPaging(data) {
    pagingEl.innerHTML = "";
    if (data.totalPages <= 1) return;

    for (var i = 0; i < data.totalPages; i++) {
      var btn = el("button", "blog-page-btn", String(i + 1));
      if (i === data.number) btn.classList.add("active");
      btn.dataset.page = i;
      btn.addEventListener("click", function () {
        loadPage(parseInt(this.dataset.page));
      });
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
        postsEl.innerHTML = "";
        if (!data.content || data.content.length === 0) {
          postsEl.appendChild(el("p", "blog-loading", "아직 작성된 글이 없습니다."));
          return;
        }
        data.content.forEach(function (post) {
          postsEl.appendChild(renderCard(post));
        });
        renderPaging(data);
      })
      .catch(function () {
        postsEl.innerHTML = "";
        postsEl.appendChild(el("p", "blog-loading", "블로그를 불러오지 못했습니다."));
      });
  }

  loadPage(0);
})();
