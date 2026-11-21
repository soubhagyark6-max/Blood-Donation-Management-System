/* LifeLink - small helpers (no framework). */
(function () {
  'use strict';

  // Bootstrap client-side validation (server-side validation is always enforced too).
  document.querySelectorAll('form.needs-validation').forEach(function (form) {
    form.addEventListener('submit', function (event) {
      if (!form.checkValidity()) {
        event.preventDefault();
        event.stopPropagation();
      }
      form.classList.add('was-validated');
    });
  });

  // Auto-hide success alerts after a few seconds.
  document.querySelectorAll('.alert-auto').forEach(function (el) {
    setTimeout(function () {
      el.classList.remove('show');
      setTimeout(function () { el.remove(); }, 400);
    }, 6000);
  });

  // Re-open a modal after a server-side validation error (data-open-modal on <body>).
  var openModal = document.body.getAttribute('data-open-modal');
  if (openModal && window.bootstrap) {
    var el = document.getElementById(openModal);
    if (el) { new bootstrap.Modal(el).show(); }
  }

  // Confirmation dialog for dangerous actions: <form data-confirm="Are you sure?">
  document.querySelectorAll('form[data-confirm]').forEach(function (form) {
    form.addEventListener('submit', function (e) {
      if (!window.confirm(form.getAttribute('data-confirm'))) { e.preventDefault(); }
    });
  });

  // Print button: <button data-print>
  document.querySelectorAll('[data-print]').forEach(function (b) {
    b.addEventListener('click', function () { window.print(); });
  });

  // QR codes: <div class="qr" data-qr="LL-XXXX">
  if (window.QRCode) {
    document.querySelectorAll('.qr[data-qr]').forEach(function (div) {
      new QRCode(div, { text: div.getAttribute('data-qr'), width: 112, height: 112, colorDark: '#0a2342', colorLight: '#ffffff' });
    });
  }
})();

/* Chart helpers used by the dashboards. */
window.LL = window.LL || {};
LL.colors = {
  red: '#c8102e', navy: '#123862', light: '#8aa4c8', rose: '#f28ca0', amber: '#f0a30a', green: '#1b9e5a', grey: '#9aa7b8'
};
LL.palette = ['#c8102e', '#123862', '#f0a30a', '#1b9e5a', '#6d4aa8', '#0b8aa4', '#e2602b', '#7d8a9c'];
LL.chart = function (id, config) {
  var el = document.getElementById(id);
  if (!el || !window.Chart) { return null; }
  Chart.defaults.font.family = "'Inter', sans-serif";
  Chart.defaults.color = '#4a5a70';
  config.options = Object.assign({ responsive: true, maintainAspectRatio: false }, config.options || {});
  return new Chart(el, config);
};
