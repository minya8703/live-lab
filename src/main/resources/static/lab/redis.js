(function () {
  "use strict";

  const categorySelect = document.getElementById("category");
  const iterationsInput = document.getElementById("iterations");
  const runNoCacheBtn = document.getElementById("runNoCache");
  const runCachedBtn = document.getElementById("runCached");
  const evictBtn = document.getElementById("evict");
  const comparison = document.getElementById("comparison");
  const comparisonText = comparison.querySelector("[data-comparison]");

  const panels = {
    noCache: document.getElementById("resultNoCache"),
    cached: document.getElementById("resultCached"),
  };

  // 두 패널의 Y축 비교를 위해 마지막 측정값을 보관 → 같은 스케일로 렌더링.
  const lastRun = { noCache: null, cached: null };

  function setButtonsDisabled(disabled) {
    runNoCacheBtn.disabled = disabled;
    runCachedBtn.disabled = disabled;
    evictBtn.disabled = disabled;
  }

  function setPanelStatus(panel, text, kind) {
    const status = panel.querySelector(".result-status");
    status.textContent = text;
    status.classList.remove("running", "done");
    if (kind) status.classList.add(kind);
  }

  async function loadCategories() {
    try {
      const res = await fetch("/api/redis-demo/categories");
      if (!res.ok) throw new Error("categories endpoint not ok");
      const list = await res.json();
      categorySelect.innerHTML = "";
      list.forEach(function (c) {
        const opt = document.createElement("option");
        opt.value = c;
        opt.textContent = c;
        categorySelect.appendChild(opt);
      });
      categorySelect.disabled = false;
    } catch (e) {
      categorySelect.innerHTML = '<option>로드 실패</option>';
    }
  }

  function maxOfBoth() {
    let m = 0;
    [lastRun.noCache, lastRun.cached].forEach(function (r) {
      if (r && r.maxMs > m) m = r.maxMs;
    });
    return m || 1;
  }

  function renderChart(panel, result) {
    const chart = panel.querySelector("[data-chart]");
    chart.innerHTML = "";
    const max = maxOfBoth();
    result.timingsMs.forEach(function (ms, idx) {
      const bar = document.createElement("div");
      bar.className = "bar";
      // 캐시 실행의 첫 막대는 캐시 미스 — 색상으로 구분.
      if (result.cached && idx === 0 && ms > result.avgMs * 2) {
        bar.classList.add("cold");
      }
      const pct = Math.max(1, (ms / max) * 100);
      bar.style.height = pct + "%";
      bar.title = ms.toFixed(2) + " ms" + (bar.classList.contains("cold") ? " (cache miss)" : "");
      chart.appendChild(bar);
    });
  }

  function renderStats(panel, result) {
    const stats = panel.querySelector("[data-stats]");
    stats.hidden = false;
    stats.querySelector('[data-stat="iterations"]').textContent = result.iterations;
    stats.querySelector('[data-stat="avg"]').textContent = result.avgMs.toFixed(2) + " ms";
    stats.querySelector('[data-stat="min"]').textContent = result.minMs.toFixed(2) + " ms";
    stats.querySelector('[data-stat="max"]').textContent = result.maxMs.toFixed(2) + " ms";
  }

  function renderComparison() {
    if (!lastRun.noCache || !lastRun.cached) {
      comparison.hidden = true;
      return;
    }
    const ratio = lastRun.noCache.avgMs / lastRun.cached.avgMs;
    comparison.hidden = false;
    comparisonText.innerHTML =
      "평균 응답 시간 비교 — 캐시 사용이 <strong>" +
      ratio.toFixed(1) +
      "배</strong> 빠릅니다. (DB " +
      lastRun.noCache.avgMs.toFixed(2) +
      " ms → 캐시 " +
      lastRun.cached.avgMs.toFixed(2) +
      " ms)";
  }

  async function run(cached) {
    const iterations = parseInt(iterationsInput.value, 10);
    const category = categorySelect.value;
    if (!iterations || iterations < 1 || iterations > 200) {
      alert("반복 횟수는 1~200 사이여야 합니다.");
      return;
    }
    const panel = cached ? panels.cached : panels.noCache;
    const key = cached ? "cached" : "noCache";

    setButtonsDisabled(true);
    setPanelStatus(panel, "실행 중...", "running");
    panel.querySelector("[data-chart]").innerHTML = "";

    try {
      const url =
        "/api/redis-demo/run?cache=" +
        (cached ? "true" : "false") +
        "&iterations=" +
        iterations +
        "&category=" +
        encodeURIComponent(category);
      const res = await fetch(url);
      const body = await res.json();
      if (!res.ok) {
        setPanelStatus(panel, "실패", "");
        alert(body.error || "요청 실패");
        return;
      }
      lastRun[key] = body;
      // 두 패널 모두 같은 Y스케일로 재렌더링
      renderChart(panels.noCache, lastRun.noCache || emptyResult());
      renderChart(panels.cached, lastRun.cached || emptyResult());
      renderStats(panel, body);
      setPanelStatus(panel, "완료", "done");
      renderComparison();
    } catch (e) {
      setPanelStatus(panel, "오류", "");
      alert("네트워크 오류로 실행에 실패했습니다.");
    } finally {
      setButtonsDisabled(false);
    }
  }

  function emptyResult() {
    return { timingsMs: [], avgMs: 0, minMs: 0, maxMs: 0, cached: false };
  }

  async function evict() {
    setButtonsDisabled(true);
    try {
      const res = await fetch("/api/redis-demo/evict", { method: "POST" });
      if (!res.ok) {
        alert("캐시 비우기 실패");
        return;
      }
      lastRun.cached = null;
      renderChart(panels.cached, emptyResult());
      panels.cached.querySelector("[data-stats]").hidden = true;
      setPanelStatus(panels.cached, "비워짐", "");
      renderComparison();
    } catch (e) {
      alert("네트워크 오류로 캐시 비우기에 실패했습니다.");
    } finally {
      setButtonsDisabled(false);
    }
  }

  runNoCacheBtn.addEventListener("click", function () { run(false); });
  runCachedBtn.addEventListener("click", function () { run(true); });
  evictBtn.addEventListener("click", evict);

  loadCategories();
})();
