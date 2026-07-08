(function () {
  "use strict";

  var profileEl = document.querySelector("[data-profile]");
  var projectsEl = document.querySelector("[data-projects]");
  var experienceEl = document.querySelector("[data-experience]");
  var techEl = document.querySelector("[data-tech]");

  function el(tag, className, text) {
    var node = document.createElement(tag);
    if (className) node.className = className;
    if (text != null) node.textContent = text;
    return node;
  }

  function renderProfile(section) {
    if (!section || !section.htmlContent) return;
    profileEl.innerHTML = "";
    var div = el("div", "devlog-content");
    div.innerHTML = section.htmlContent;
    profileEl.appendChild(div);
  }

  function renderProjects(projects) {
    projectsEl.innerHTML = "";
    if (!projects || projects.length === 0) {
      projectsEl.appendChild(el("p", "devlog-loading", "프로젝트 데이터가 없습니다."));
      return;
    }
    projects.forEach(function (proj) {
      var card = document.createElement("article");
      card.className = "career-project-card";
      card.id = proj.key;

      var header = el("header", "devlog-entry-header");
      header.appendChild(el("h3", "devlog-entry-title", proj.title));
      card.appendChild(header);

      var content = el("div", "devlog-content");
      content.innerHTML = proj.htmlContent || "";
      card.appendChild(content);

      projectsEl.appendChild(card);
    });
  }

  function renderExperience(items) {
    experienceEl.innerHTML = "";
    if (!items || items.length === 0) {
      experienceEl.appendChild(el("p", "devlog-loading", "경력 데이터가 없습니다."));
      return;
    }
    items.forEach(function (exp) {
      var card = document.createElement("article");
      card.className = "career-exp-card";
      card.id = exp.key;

      var header = el("header", "devlog-entry-header");
      header.appendChild(el("h3", "devlog-entry-title", exp.title));
      card.appendChild(header);

      var content = el("div", "devlog-content");
      content.innerHTML = exp.htmlContent || "";
      card.appendChild(content);

      experienceEl.appendChild(card);
    });
  }

  function renderTech(section) {
    if (!section || !section.htmlContent) return;
    techEl.innerHTML = section.htmlContent;
  }

  async function load() {
    try {
      var res = await fetch("/api/career");
      if (!res.ok) throw new Error("career endpoint not ok");
      var data = await res.json();

      renderProfile(data.profile);
      renderProjects(data.projects);
      renderExperience(data.experience);
      renderTech(data.techStack);
    } catch (e) {
      projectsEl.innerHTML = "";
      projectsEl.appendChild(el("p", "devlog-loading", "경력 데이터를 불러오지 못했습니다."));
    }
  }

  load();
})();
