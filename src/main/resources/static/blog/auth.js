(function () {
  "use strict";

  var currentAuth = null;
  var googleScriptLoading = false;

  function getAuth() {
    return currentAuth;
  }

  function isMaster() {
    return currentAuth && currentAuth.master === true;
  }

  function readCookie(name) {
    var prefix = encodeURIComponent(name) + "=";
    var parts = document.cookie ? document.cookie.split("; ") : [];
    for (var i = 0; i < parts.length; i++) {
      if (parts[i].indexOf(prefix) === 0) return decodeURIComponent(parts[i].substring(prefix.length));
    }
    return null;
  }

  function authHeaders() {
    var headers = {};
    var csrfToken = readCookie("livelab_csrf");
    if (csrfToken) headers["X-CSRF-Token"] = csrfToken;
    return headers;
  }

  function handleGoogleResponse(response) {
    fetch("/api/auth/google", {
      method: "POST",
      credentials: "same-origin",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ credential: response.credential })
    })
    .then(function (res) {
      if (!res.ok) throw new Error("Login failed");
      return res.json();
    })
    .then(function (data) {
      currentAuth = data;
      renderAuthUI();
    })
    .catch(function (err) {
      alert("로그인 실패: " + err.message);
    });
  }

  function logout() {
    fetch("/api/auth/logout", {
      method: "POST",
      credentials: "same-origin",
      headers: authHeaders()
    })
    .then(function (res) {
      if (!res.ok) throw new Error("Logout failed");
      currentAuth = null;
      renderAuthUI();
      loadGoogleSignIn();
    })
    .catch(function (err) {
      alert("로그아웃 실패: " + err.message);
    });
  }

  function renderAuthUI() {
    var container = document.querySelector("[data-auth-area]");
    if (!container) return;

    container.innerHTML = "";
    var auth = getAuth();

    if (auth) {
      var wrap = document.createElement("div");
      wrap.className = "auth-user";

      if (auth.picture) {
        var img = document.createElement("img");
        img.src = auth.picture;
        img.alt = auth.name || "";
        img.className = "auth-avatar";
        img.referrerPolicy = "no-referrer";
        wrap.appendChild(img);
      }

      var nameSpan = document.createElement("span");
      nameSpan.className = "auth-name";
      nameSpan.textContent = auth.name || auth.email;
      wrap.appendChild(nameSpan);

      var logoutBtn = document.createElement("button");
      logoutBtn.className = "btn btn-ghost auth-logout-btn";
      logoutBtn.textContent = "로그아웃";
      logoutBtn.addEventListener("click", logout);
      wrap.appendChild(logoutBtn);
      container.appendChild(wrap);

      if (auth.master
          && !document.querySelector("[data-blog-write-button]")
          && window.location.pathname !== "/blog/write.html") {
        var writeBtn = document.createElement("a");
        writeBtn.className = "btn btn-primary auth-write-btn";
        writeBtn.href = "/blog/write.html";
        writeBtn.textContent = "글쓰기";
        container.appendChild(writeBtn);
      }
    } else {
      var googleDiv = document.createElement("div");
      googleDiv.id = "g_id_signin";
      googleDiv.className = "g_id_signin";
      container.appendChild(googleDiv);

      if (window.google && window.google.accounts) {
        window.google.accounts.id.renderButton(googleDiv, {
          theme: "outline", size: "medium", text: "signin"
        });
      } else {
        var fallback = document.createElement("button");
        fallback.className = "btn btn-ghost auth-google-fallback";
        fallback.textContent = googleScriptLoading ? "로딩 중..." : "관리자";
        fallback.disabled = googleScriptLoading;
        fallback.addEventListener("click", loadGoogleSignIn);
        googleDiv.appendChild(fallback);
      }
    }

    var adminEls = document.querySelectorAll("[data-admin-only]");
    for (var i = 0; i < adminEls.length; i++) {
      adminEls[i].style.display = (auth && auth.master) ? "" : "none";
    }
  }

  function initGoogleSignIn(clientId) {
    if (!clientId || googleScriptLoading || (window.google && window.google.accounts)) {
      renderAuthUI();
      return;
    }
    googleScriptLoading = true;
    renderAuthUI();

    var script = document.createElement("script");
    script.src = "https://accounts.google.com/gsi/client";
    script.async = true;
    script.defer = true;
    script.onload = function () {
      googleScriptLoading = false;
      window.google.accounts.id.initialize({
        client_id: clientId,
        callback: handleGoogleResponse
      });
      renderAuthUI();
    };
    script.onerror = function () {
      googleScriptLoading = false;
      renderAuthUI();
    };
    document.head.appendChild(script);
  }

  function loadGoogleSignIn() {
    fetch("/api/auth/client-id", { credentials: "same-origin" })
      .then(function (res) { return res.json(); })
      .then(function (data) { initGoogleSignIn(data.clientId); })
      .catch(function () { renderAuthUI(); });
  }

  function loadSession() {
    return fetch("/api/auth/me", { credentials: "same-origin" })
      .then(function (res) { return res.ok ? res.json() : null; })
      .catch(function () { return null; });
  }

  function boot() {
    loadSession().then(function (auth) {
      currentAuth = auth;
      renderAuthUI();
      if (!auth) loadGoogleSignIn();
    });
  }

  window.BlogAuth = {
    getAuth: getAuth,
    isMaster: isMaster,
    authHeaders: authHeaders,
    renderAuthUI: renderAuthUI,
    boot: boot
  };

  if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", boot);
  } else {
    boot();
  }
})();
