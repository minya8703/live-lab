// Project card toggle
function careerToggle(card) {
  card.classList.toggle('open');
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
(function () {
  var user = 'minya8703';
  var domain = 'gmail.com';
  var addr = user + '@' + domain;
  var heroEmail = document.getElementById('career-hero-email');
  if (heroEmail) { heroEmail.href = 'mailto:' + addr; }
})();
