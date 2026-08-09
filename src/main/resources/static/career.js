(function () {
"use strict";

// Project card toggle
function careerToggle(card) {
  card.classList.toggle('open');
  card.setAttribute('aria-expanded', card.classList.contains('open') ? 'true' : 'false');
}

// Image modal
function openCareerImgModal(src, caption) {
  var modal = document.getElementById('careerImgModal');
  document.getElementById('careerImgModalSrc').src = src;
  document.getElementById('careerImgModalCaption').textContent = caption;
  modal.classList.add('active');
  document.body.style.overflow = 'hidden';
}

function closeCareerImgModal() {
  document.getElementById('careerImgModal').classList.remove('active');
  document.body.style.overflow = '';
}

document.addEventListener('click', function (event) {
  var imageButton = event.target.closest('.c-evidence-btn[data-image-src]');
  if (imageButton) {
    openCareerImgModal(imageButton.dataset.imageSrc, imageButton.dataset.imageCaption || '');
    return;
  }

  if (event.target.closest('.c-img-modal-close')) {
    closeCareerImgModal();
    return;
  }

  var modal = document.getElementById('careerImgModal');
  if (event.target === modal) {
    closeCareerImgModal();
    return;
  }

  var card = event.target.closest('.c-project-card');
  if (card && !event.target.closest('a, button')) careerToggle(card);
});

document.addEventListener('keydown', function (event) {
  var card = event.target.closest('.c-project-card');
  if (card && !event.target.closest('a, button') && (event.key === 'Enter' || event.key === ' ')) {
    event.preventDefault();
    careerToggle(card);
  }
});

// ESC key closes modal
document.addEventListener('keydown', function (e) {
  if (e.key === 'Escape') closeCareerImgModal();
});

// Intersection Observer for fade-in
var careerObserver = new IntersectionObserver(function (entries) {
  entries.forEach(function (e) {
    if (e.isIntersecting) e.target.classList.add('visible');
  });
}, { threshold: 0.08, rootMargin: '0px 0px -40px 0px' });

document.querySelectorAll('.c-fade-in').forEach(function (el) {
  careerObserver.observe(el);
});

// Email masking
var user = 'minya8703';
var domain = 'gmail.com';
var addr = user + '@' + domain;
var heroEmail = document.getElementById('career-hero-email');
if (heroEmail) { heroEmail.href = 'mailto:' + addr; }

})();
