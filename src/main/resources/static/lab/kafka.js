(function () {
  "use strict";

  const POLL_INTERVAL_MS = 500;
  const MAX_CHART_BARS = 60;

  const countInput = document.getElementById("count");
  const publishBtn = document.getElementById("publish");
  const resetBtn = document.getElementById("reset");
  const progressBar = document.getElementById("progressBar");
  const chartEl = document.getElementById("throughputChart");
  const throughputStatus = document.querySelector("[data-throughput-status]");

  const statEls = {
    attempted: document.querySelector('[data-stat="attempted"]'),
    acknowledged: document.querySelector('[data-stat="acknowledged"]'),
    publishFailed: document.querySelector('[data-stat="publishFailed"]'),
    success: document.querySelector('[data-stat="success"]'),
    dlt: document.querySelector('[data-stat="dlt"]'),
    throughput: document.querySelector('[data-stat="throughput"]'),
    elapsed: document.querySelector('[data-stat="elapsed"]'),
  };

  let pollHandle = null;
  let history = []; // { successDelta, dltDelta, throughput }
  let prevSnapshot = { success: 0, dlt: 0 };

  function setBusy(busy) {
    publishBtn.disabled = busy;
    resetBtn.disabled = busy;
    countInput.disabled = busy;
  }

  function setStatus(text, kind) {
    throughputStatus.textContent = text;
    throughputStatus.classList.remove("running", "done");
    if (kind) throughputStatus.classList.add(kind);
  }

  function renderChart() {
    chartEl.innerHTML = "";
    if (history.length === 0) return;
    const maxValue = history.reduce(function (m, h) { return Math.max(m, h.throughput); }, 1);
    history.slice(-MAX_CHART_BARS).forEach(function (h) {
      const bar = document.createElement("div");
      bar.className = "bar";
      const pct = Math.max(1, (h.throughput / maxValue) * 100);
      bar.style.height = pct + "%";
      bar.title = h.throughput.toFixed(1) + " msgs/sec";
      if (h.dltDelta > 0 && h.successDelta === 0) bar.classList.add("cold");
      chartEl.appendChild(bar);
    });
  }

  function renderStats(snap) {
    statEls.attempted.textContent = snap.attempted.toLocaleString();
    statEls.acknowledged.textContent = snap.acknowledged.toLocaleString();
    statEls.publishFailed.textContent = snap.publishFailed.toLocaleString();
    statEls.success.textContent = snap.success.toLocaleString();
    statEls.dlt.textContent = snap.dlt.toLocaleString();
    statEls.throughput.textContent = snap.throughputPerSec > 0 ? snap.throughputPerSec.toFixed(1) : "-";
    statEls.elapsed.textContent = snap.elapsedMs > 0 ? (snap.elapsedMs / 1000).toFixed(2) + "s" : "-";

    const completed = snap.success + snap.dlt + snap.publishFailed;
    const pct = snap.attempted > 0 ? Math.min(100, (completed / snap.attempted) * 100) : 0;
    progressBar.style.width = pct + "%";
  }

  async function pollOnce() {
    try {
      const res = await fetch("/api/kafka-demo/status");
      const snap = await res.json();
      if (!res.ok) return null;
      const successDelta = snap.success - prevSnapshot.success;
      const dltDelta = snap.dlt - prevSnapshot.dlt;
      const throughput = ((successDelta + dltDelta) * 1000) / POLL_INTERVAL_MS;
      history.push({ successDelta: successDelta, dltDelta: dltDelta, throughput: throughput });
      prevSnapshot = { success: snap.success, dlt: snap.dlt };
      renderStats(snap);
      renderChart();
      return snap;
    } catch (e) {
      return null;
    }
  }

  function stopPolling() {
    if (pollHandle) {
      clearInterval(pollHandle);
      pollHandle = null;
    }
  }

  async function publish() {
    const count = parseInt(countInput.value, 10);
    if (!count || count < 1 || count > 10000) {
      alert("발행 메시지 수는 1~10000 사이여야 합니다.");
      return;
    }

    history = [];
    prevSnapshot = { success: 0, dlt: 0 };
    renderChart();
    setBusy(true);
    setStatus("실행 중...", "running");

    try {
      const res = await fetch("/api/kafka-demo/publish?count=" + count, { method: "POST" });
      const initial = await res.json();
      if (!res.ok) {
        alert(initial.error || "발행 실패");
        setBusy(false);
        setStatus("실패", "");
        return;
      }
      renderStats(initial);

      // 폴링 시작
      let stableTicks = 0;
      pollHandle = setInterval(async function () {
        const snap = await pollOnce();
        if (!snap) return;
        const completed = snap.success + snap.dlt + snap.publishFailed;
        if (snap.attempted > 0 && completed >= snap.attempted) {
          stableTicks++;
          // 같은 값이 2틱 연속 유지되면 종료
          if (stableTicks >= 2) {
            stopPolling();
            setBusy(false);
            setStatus("완료", "done");
          }
        } else {
          stableTicks = 0;
        }
      }, POLL_INTERVAL_MS);
    } catch (e) {
      stopPolling();
      setBusy(false);
      setStatus("오류", "");
      alert("네트워크 오류로 발행에 실패했습니다.");
    }
  }

  async function reset() {
    setBusy(true);
    try {
      await fetch("/api/kafka-demo/reset", { method: "POST" });
      history = [];
      prevSnapshot = { success: 0, dlt: 0 };
      renderStats({ attempted: 0, acknowledged: 0, publishFailed: 0, success: 0, dlt: 0, throughputPerSec: 0, elapsedMs: 0 });
      renderChart();
      setStatus("리셋됨", "");
    } catch (e) {
      // 무시
    } finally {
      setBusy(false);
    }
  }

  publishBtn.addEventListener("click", publish);
  resetBtn.addEventListener("click", reset);
})();
