(function () {
  "use strict";

  var postEl = document.querySelector("[data-post]");

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

  function getSlug() {
    var params = new URLSearchParams(window.location.search);
    return params.get("slug");
  }

  function renderPost(post) {
    postEl.innerHTML = "";
    document.title = post.title + " · 민야령 Backend Live Lab";

    // header
    var header = el("header", "blog-post-header");
    header.appendChild(el("h1", "blog-post-title", post.title));

    var meta = el("div", "blog-post-meta");
    meta.appendChild(el("span", "blog-post-date", formatDate(post.createdAt)));
    if (post.tags) {
      var tagsContainer = el("div", "blog-card-tags");
      post.tags.split(",").map(function (t) { return t.trim(); }).filter(Boolean)
        .forEach(function (tag) {
          tagsContainer.appendChild(el("span", "blog-tag", tag));
        });
      meta.appendChild(tagsContainer);
    }
    header.appendChild(meta);
    postEl.appendChild(header);

    // body
    var body = el("div", "blog-post-body");
    body.innerHTML = post.htmlContent;
    postEl.appendChild(body);

    // nav
    var nav = el("div", "blog-post-nav");
    var backBtn = document.createElement("a");
    backBtn.className = "btn btn-ghost";
    backBtn.href = "/blog.html";
    backBtn.textContent = "← 목록으로";
    backBtn.style.border = "1px solid var(--border)";
    nav.appendChild(backBtn);

    // 마스터 전용 수정/삭제 버튼
    if (window.BlogAuth && window.BlogAuth.isMaster()) {
      var editBtn = document.createElement("a");
      editBtn.className = "btn btn-ghost";
      editBtn.href = "/blog/write.html?edit=" + encodeURIComponent(post.slug);
      editBtn.textContent = "수정";
      editBtn.style.border = "1px solid var(--border)";
      nav.appendChild(editBtn);

      var deleteBtn = document.createElement("button");
      deleteBtn.className = "btn btn-ghost";
      deleteBtn.textContent = "삭제";
      deleteBtn.style.border = "1px solid var(--border)";
      deleteBtn.style.color = "#dc2626";
      deleteBtn.addEventListener("click", function () {
        if (!confirm("정말 삭제하시겠습니까?")) return;
        fetch("/api/blog/" + encodeURIComponent(post.slug), {
          method: "DELETE",
          headers: window.BlogAuth.authHeaders()
        })
        .then(function (res) {
          if (!res.ok) throw new Error("Delete failed");
          alert("삭제되었습니다.");
          window.location.href = "/blog.html";
        })
        .catch(function (err) { alert("삭제 실패: " + err.message); });
      });
      nav.appendChild(deleteBtn);
    }

    postEl.appendChild(nav);
  }

  var slug = getSlug();
  if (!slug) {
    postEl.innerHTML = "";
    postEl.appendChild(el("p", "blog-loading", "잘못된 접근입니다."));
    return;
  }

  fetch("/api/blog/" + encodeURIComponent(slug))
    .then(function (res) {
      if (!res.ok) throw new Error("Not found");
      return res.json();
    })
    .then(renderPost)
    .catch(function () {
      postEl.innerHTML = "";
      postEl.appendChild(el("p", "blog-loading", "글을 찾을 수 없습니다."));
    });
})();
