/* Home page: refreshes the three hero figures from the REST API every 30 seconds. */
(function () {
  'use strict';
  function refresh() {
    fetch('/api/public/stats', { headers: { 'Accept': 'application/json' } })
      .then(function (r) { return r.ok ? r.json() : null; })
      .then(function (data) {
        if (!data) { return; }
        Object.keys(data).forEach(function (key) {
          var el = document.querySelector('[data-stat="' + key + '"]');
          if (el) { el.textContent = data[key]; }
        });
      })
      .catch(function () { /* keep the server-rendered numbers */ });
  }
  setInterval(refresh, 30000);
})();
