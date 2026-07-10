(function () {
  "use strict";

  var titleEl = document.querySelector("[data-title]");
  var slugEl = document.querySelector("[data-slug]");
  var summaryEl = document.querySelector("[data-summary]");
  var tagsEl = document.querySelector("[data-tags]");
  var thumbnailEl = document.querySelector("[data-thumbnail]");
  var contentEl = document.querySelector("[data-content]");
  var previewEl = document.querySelector("[data-preview]");
  var draftBtn = document.querySelector("[data-btn-draft]");
  var publishBtn = document.querySelector("[data-btn-publish]");
  var editorTitle = document.querySelector("[data-editor-title]");

  // 수정 모드 감지
  var params = new URLSearchParams(window.location.search);
  var editSlug = params.get("edit");

  function getHeaders(contentType) {
    var h = window.BlogAuth ? window.BlogAuth.authHeaders() : {};
    if (contentType) h["Content-Type"] = contentType;
    return h;
  }

  // 마크다운 프리뷰 (서버 렌더링 없이 간이 변환)
  var previewTimer = null;
  contentEl.addEventListener("input", function () {
    clearTimeout(previewTimer);
    previewTimer = setTimeout(updatePreview, 300);
  });

  function updatePreview() {
    var md = contentEl.value;
    var html = md
      .replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;")
      .replace(/^### (.+)$/gm, "<h3>$1</h3>")
      .replace(/^## (.+)$/gm, "<h2>$1</h2>")
      .replace(/^# (.+)$/gm, "<h1>$1</h1>")
      .replace(/\*\*(.+?)\*\*/g, "<strong>$1</strong>")
      .replace(/`([^`]+)`/g, "<code>$1</code>")
      .replace(/!\[([^\]]*)\]\(([^)]+)\)/g, '<img src="$2" alt="$1" style="max-width:100%;border-radius:8px" />')
      .replace(/\[([^\]]+)\]\(([^)]+)\)/g, '<a href="$2">$1</a>')
      .replace(/^---$/gm, "<hr>")
      .replace(/\n\n/g, "</p><p>")
      .replace(/\n/g, "<br>");
    previewEl.innerHTML = "<p>" + html + "</p>";
  }

  // 이미지 업로드
  var fileInput = document.querySelector("[data-file-input]");
  var uploadBtn = document.querySelector("[data-btn-upload]");
  var uploadStatus = document.querySelector("[data-upload-status]");
  var uploadCount = 0;
  var uploadTotal = 0;

  function showUploadStatus(current, total) {
    if (total === 0) {
      uploadStatus.textContent = "";
      return;
    }
    uploadStatus.textContent = "업로드 중... (" + current + "/" + total + ")";
  }

  function uploadFile(file) {
    if (!window.BlogAuth || !window.BlogAuth.isMaster()) {
      alert("로그인이 필요합니다.");
      return Promise.reject();
    }
    var formData = new FormData();
    formData.append("file", file);

    return fetch("/api/blog/upload", {
      method: "POST",
      headers: window.BlogAuth.authHeaders(),
      body: formData
    })
    .then(function (res) {
      if (!res.ok) throw new Error("Upload failed");
      return res.json();
    })
    .then(function (data) {
      var pos = contentEl.selectionStart;
      var text = contentEl.value;
      var imgMd = "\n![image](" + data.url + ")\n";
      contentEl.value = text.substring(0, pos) + imgMd + text.substring(pos);
      contentEl.focus();
      contentEl.selectionStart = contentEl.selectionEnd = pos + imgMd.length;
      updatePreview();

      // 첫 번째 이미지를 자동 썸네일 설정
      if (!thumbnailEl.value.trim()) {
        thumbnailEl.value = data.url;
      }

      return data.url;
    })
    .catch(function (err) {
      alert("이미지 업로드 실패: " + err.message);
    });
  }

  // 다중 이미지 순차 업로드
  function uploadFiles(files) {
    var imageFiles = [];
    for (var i = 0; i < files.length; i++) {
      if (files[i].type.startsWith("image/")) imageFiles.push(files[i]);
    }
    if (imageFiles.length === 0) return;

    uploadTotal = imageFiles.length;
    uploadCount = 0;

    function next() {
      if (uploadCount >= imageFiles.length) {
        showUploadStatus(0, 0);
        return;
      }
      uploadCount++;
      showUploadStatus(uploadCount, uploadTotal);
      uploadFile(imageFiles[uploadCount - 1]).then(next);
    }
    next();
  }

  // 버튼 클릭 → 파일 선택
  uploadBtn.addEventListener("click", function () { fileInput.click(); });
  fileInput.addEventListener("change", function () {
    if (fileInput.files.length > 0) uploadFiles(fileInput.files);
    fileInput.value = "";
  });

  // 드래그앤드롭
  var dropOverlay = null;
  contentEl.addEventListener("dragover", function (e) {
    e.preventDefault();
    if (!dropOverlay) {
      dropOverlay = document.createElement("div");
      dropOverlay.className = "blog-drop-overlay";
      dropOverlay.textContent = "이미지를 여기에 놓으세요";
      document.body.appendChild(dropOverlay);
    }
  });

  document.addEventListener("dragleave", function (e) {
    if (e.relatedTarget === null && dropOverlay) {
      dropOverlay.remove();
      dropOverlay = null;
    }
  });

  contentEl.addEventListener("drop", function (e) {
    e.preventDefault();
    if (dropOverlay) { dropOverlay.remove(); dropOverlay = null; }
    uploadFiles(e.dataTransfer.files);
  });

  // Ctrl+V 이미지 붙여넣기
  contentEl.addEventListener("paste", function (e) {
    var items = e.clipboardData.items;
    for (var i = 0; i < items.length; i++) {
      if (items[i].type.startsWith("image/")) {
        e.preventDefault();
        uploadFile(items[i].getAsFile());
        return;
      }
    }
  });

  // 저장
  function save(published) {
    if (!window.BlogAuth || !window.BlogAuth.isMaster()) {
      alert("로그인이 필요합니다.");
      return;
    }
    if (!titleEl.value.trim()) { alert("제목을 입력해주세요."); return; }
    if (!contentEl.value.trim()) { alert("본문을 입력해주세요."); return; }

    var body = {
      title: titleEl.value.trim(),
      slug: slugEl.value.trim() || null,
      summary: summaryEl.value.trim() || null,
      content: contentEl.value,
      thumbnailUrl: thumbnailEl.value.trim() || null,
      tags: tagsEl.value.trim() || null,
      published: published
    };

    var isEdit = !!editSlug;
    var url = isEdit ? "/api/blog/" + encodeURIComponent(editSlug) : "/api/blog";
    var method = isEdit ? "PUT" : "POST";

    fetch(url, {
      method: method,
      headers: getHeaders("application/json"),
      body: JSON.stringify(body)
    })
    .then(function (res) {
      if (!res.ok) throw new Error("Save failed");
      return res.json();
    })
    .then(function (data) {
      if (published) {
        window.location.href = "/blog.html";
      } else {
        alert("임시저장되었습니다.");
        if (!isEdit) {
          window.location.href = "/blog/write.html?edit=" + encodeURIComponent(data.slug);
        }
      }
    })
    .catch(function (err) {
      alert("저장 실패: " + err.message);
    });
  }

  draftBtn.addEventListener("click", function () { save(false); });
  publishBtn.addEventListener("click", function () { save(true); });

  // 수정 모드: 기존 글 로드
  if (editSlug) {
    editorTitle.textContent = "글 수정";
    fetch("/api/blog/" + encodeURIComponent(editSlug))
      .then(function (res) {
        if (!res.ok) throw new Error("Not found");
        return res.json();
      })
      .then(function (post) {
        titleEl.value = post.title || "";
        slugEl.value = post.slug || "";
        summaryEl.value = post.summary || "";
        tagsEl.value = post.tags || "";
        thumbnailEl.value = post.thumbnailUrl || "";
        contentEl.value = post.content || "";
        updatePreview();
      })
      .catch(function () {
        alert("글을 불러오지 못했습니다.");
      });
  }
})();
