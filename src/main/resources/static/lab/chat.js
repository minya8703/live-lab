(function () {
  "use strict";

  const conversation = document.getElementById("conversation");
  const form = document.getElementById("composer");
  const textarea = document.getElementById("message");
  const sendBtn = document.getElementById("send");

  function el(tag, className, textContent) {
    const node = document.createElement(tag);
    if (className) node.className = className;
    if (textContent != null) node.textContent = textContent;
    return node;
  }

  function appendUser(text) {
    const msg = el("article", "msg msg-user");
    msg.textContent = text;
    conversation.appendChild(msg);
    msg.scrollIntoView({ behavior: "smooth", block: "end" });
  }

  function appendBot(text, sources, grounded) {
    const msg = el("article", "msg msg-bot");
    msg.appendChild(el("div", "msg-answer", text));

    const evidence = el("div", "msg-evidence");
    if (grounded && Array.isArray(sources) && sources.length > 0) {
      evidence.appendChild(el("span", "evidence-label", "검증된 근거"));
      sources.forEach(function (source) {
        evidence.appendChild(el("code", "evidence-source", source));
      });
    } else {
      evidence.appendChild(el("span", "evidence-label evidence-missing", "근거 확인 불가 · 답변 보류"));
    }
    msg.appendChild(evidence);
    conversation.appendChild(msg);
    msg.scrollIntoView({ behavior: "smooth", block: "end" });
  }

  function appendError(text) {
    const msg = el("article", "msg msg-error");
    msg.textContent = text;
    conversation.appendChild(msg);
    msg.scrollIntoView({ behavior: "smooth", block: "end" });
  }

  function appendThinking() {
    const msg = el("article", "msg msg-bot thinking");
    msg.textContent = "생각 중";
    conversation.appendChild(msg);
    msg.scrollIntoView({ behavior: "smooth", block: "end" });

    // 5초 / 15초 경과 시 안내 문구 자동 갱신 (Gemini 혼잡 등 장시간 대기 대응)
    const startTime = Date.now();
    msg._timer = setInterval(function () {
      const elapsed = Date.now() - startTime;
      if (elapsed > 15000) {
        msg.textContent = "AI 모델 혼잡 — 조금만 더 기다려 주세요";
      } else if (elapsed > 5000) {
        msg.textContent = "응답 지연 — 자동 재시도 중";
      }
    }, 1000);

    return msg;
  }

  function removeThinking(thinking) {
    if (!thinking) return;
    if (thinking._timer) clearInterval(thinking._timer);
    thinking.remove();
  }

  async function ask(question) {
    const thinking = appendThinking();
    sendBtn.disabled = true;
    try {
      const res = await fetch("/api/chat", {
        method: "POST",
        headers: { "Content-Type": "application/json", Accept: "application/json" },
        body: JSON.stringify({ message: question }),
      });
      const body = await res.json().catch(function () {
        return {};
      });
      removeThinking(thinking);
      if (!res.ok) {
        appendError(body.error || "응답 처리에 실패했습니다.");
        return;
      }
      appendBot(body.answer || "(빈 응답)", body.sources, body.grounded === true);
    } catch (e) {
      removeThinking(thinking);
      appendError("네트워크 오류로 응답을 받지 못했습니다.");
    } finally {
      sendBtn.disabled = false;
    }
  }

  form.addEventListener("submit", function (ev) {
    ev.preventDefault();
    const text = textarea.value.trim();
    if (!text) return;
    appendUser(text);
    textarea.value = "";
    ask(text);
  });

  // Enter 키 처리:
  //  - Enter 2번 연속 → 전송 (텍스트가 \n으로 끝나는 상태에서 다시 Enter)
  //  - Shift + Enter → 줄바꿈 (기본 동작 유지)
  //  - Ctrl/Cmd + Enter → 즉시 전송 (파워 유저용)
  //  - 한글 IME 조합 중에는 모든 처리 스킵 (글자 깨짐 방지)
  textarea.addEventListener("keydown", function (ev) {
    if (ev.isComposing || ev.keyCode === 229) return;

    if (ev.key === "Enter" && (ev.metaKey || ev.ctrlKey)) {
      ev.preventDefault();
      form.requestSubmit();
      return;
    }

    if (ev.key === "Enter" && !ev.shiftKey) {
      if (textarea.value.endsWith("\n")) {
        ev.preventDefault();
        textarea.value = textarea.value.replace(/\n+$/, "");
        form.requestSubmit();
      }
    }
  });

  document.querySelectorAll(".suggest").forEach(function (btn) {
    btn.addEventListener("click", function () {
      const q = btn.getAttribute("data-q");
      if (!q) return;
      textarea.value = q;
      form.requestSubmit();
    });
  });
})();
