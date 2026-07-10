(function () {
  "use strict";

  var AUTH_KEY = "blog-auth";

  function getAuth() {
    try {
      var raw = localStorage.getItem(AUTH_KEY);
      if (!raw) return null;
      var auth = JSON.parse(raw);
      // 간이 만료 확인 (JWT 디코딩 없이 저장 시간 기준)
      if (auth.expiresAt && Date.now() > auth.expiresAt) {
        localStorage.removeItem(AUTH_KEY);
        return null;
      }
      return auth;
    } catch (e) {
      localStorage.removeItem(AUTH_KEY);
      return null;
    }
  }

  function setAuth(data) {
    data.expiresAt = Date.now() + 23 * 60 * 60 * 1000; // 23시간
    localStorage.setItem(AUTH_KEY, JSON.stringify(data));
  }

  function clearAuth() {
    localStorage.removeItem(AUTH_KEY);
  }

  function getToken() {
    var auth = getAuth();
    return auth ? auth.token : null;
  }

  function isMaster() {
    var auth = getAuth();
    return auth && auth.master === true;
  }

  function authHeaders() {
    var token = getToken();
    var headers = {};
    if (token) headers["Authorization"] = "Bearer " + token;
    return headers;
  }

  // Google Sign-In 콜백
  function handleGoogleResponse(response) {
    fetch("/api/auth/google", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ credential: response.credential })
    })
    .then(function (res) {
      if (!res.ok) throw new Error("Login failed");
      return res.json();
    })
    .then(function (data) {
      setAuth(data);
      renderAuthUI();
    })
    .catch(function (err) {
      alert("로그인 실패: " + err.message);
    });
  }

  // 로그아웃
  function logout() {
    clearAuth();
    renderAuthUI();
  }

  // 인증 UI 렌더링
  function renderAuthUI() {
    var container = document.querySelector("[data-auth-area]");
    if (!container) return;

    container.innerHTML = "";
    var auth = getAuth();

    if (auth) {
      // 로그인 상태 — 프로필 + 로그아웃
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

      // 마스터이면 글쓰기 버튼. 페이지가 별도 버튼 자리를 제공하면 중복 생성하지 않는다.
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
      // 비로그인 — Google Sign-In 버튼
      var googleDiv = document.createElement("div");
      googleDiv.id = "g_id_signin";
      googleDiv.className = "g_id_signin";
      googleDiv.setAttribute("data-type", "standard");
      googleDiv.setAttribute("data-size", "medium");
      googleDiv.setAttribute("data-theme", "outline");
      googleDiv.setAttribute("data-text", "signin");
      googleDiv.setAttribute("data-shape", "rectangular");
      googleDiv.setAttribute("data-logo_alignment", "left");
      container.appendChild(googleDiv);

      if (window.google && window.google.accounts) {
        // Google Identity Services가 로드된 후 렌더
        window.google.accounts.id.renderButton(googleDiv, {
          theme: "outline", size: "medium", text: "signin"
        });
      } else {
        // GIS 미로드 시 폴백 버튼
        var fallback = document.createElement("button");
        fallback.className = "btn btn-ghost auth-google-fallback";
        fallback.textContent = "관리자";
        fallback.addEventListener("click", function () {
          fallback.textContent = "로딩 중...";
          fallback.disabled = true;
          boot();
        });
        googleDiv.appendChild(fallback);
      }
    }

    // 수정/삭제 버튼 토글
    var adminEls = document.querySelectorAll("[data-admin-only]");
    for (var i = 0; i < adminEls.length; i++) {
      adminEls[i].style.display = (auth && auth.master) ? "" : "none";
    }
  }

  // Google Identity Services 초기화
  function initGoogleSignIn(clientId) {
    if (!clientId) return;

    var script = document.createElement("script");
    script.src = "https://accounts.google.com/gsi/client";
    script.async = true;
    script.defer = true;
    script.onload = function () {
      window.google.accounts.id.initialize({
        client_id: clientId,
        callback: handleGoogleResponse
      });
      renderAuthUI();
    };
    document.head.appendChild(script);
  }

  // 클라이언트 ID를 서버에서 가져와서 초기화
  function boot() {
    fetch("/api/auth/client-id")
      .then(function (res) { return res.json(); })
      .then(function (data) {
        if (data.clientId) {
          initGoogleSignIn(data.clientId);
        }
      })
      .catch(function () {
        // 인증 설정 안 되어 있으면 무시
      });

    // 이미 로그인 상태면 UI 바로 렌더
    renderAuthUI();
  }

  // 글로벌 노출
  window.BlogAuth = {
    getAuth: getAuth,
    getToken: getToken,
    isMaster: isMaster,
    authHeaders: authHeaders,
    renderAuthUI: renderAuthUI,
    boot: boot
  };

  // DOM ready 시 자동 부트
  if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", boot);
  } else {
    boot();
  }
})();
