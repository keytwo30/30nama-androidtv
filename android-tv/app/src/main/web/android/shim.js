/* Android TV shim: stands in for Tizen's webapis/tizen objects.
   - fetch() to other origins goes through native OkHttp (no CORS, TV user agent)
   - webapis.avplay is backed by a native ExoPlayer drawn under the transparent WebView
   - remote keys the WebView cannot see (Back, media keys) are injected as Tizen key codes */
(function () {
  var NN = window.NativeNet, NP = window.NativePlayer, NA = window.NativeApp;
  if (!NN) return;

  /* ---------- fetch bridge ---------- */
  var origFetch = window.fetch.bind(window), pending = {}, seq = 0;
  window.fetch = function (input, init) {
    var url = typeof input === 'string' ? input : (input && input.url) || String(input);
    var abs; try { abs = new URL(url, location.href); } catch (e) { return origFetch(input, init); }
    if (abs.origin === location.origin || !/^https?:$/.test(abs.protocol)) return origFetch(input, init);
    init = init || {};
    var headers = {}, h = init.headers;
    if (h) {
      if (typeof h.forEach === 'function' && !(h instanceof Array)) h.forEach(function (v, k) { headers[k] = v; });
      else for (var k in h) if (h.hasOwnProperty(k)) headers[k] = String(h[k]);
    }
    var body = init.body == null ? '' : String(init.body);
    return new Promise(function (res, rej) {
      var id = ++seq; pending[id] = { res: res, rej: rej };
      NN.request(id, (init.method || 'GET').toUpperCase(), abs.href, JSON.stringify(headers), body);
    });
  };
  window.__nnDone = function (id, status, headersJson) {
    var p = pending[id]; if (!p) return; delete pending[id];
    if (status <= 0) { p.rej(new TypeError('Network request failed')); return; }
    var text = NN.take(id) || '';
    p.res(new Response(status === 204 || status === 304 ? null : text, { status: status, headers: JSON.parse(headersJson || '{}') }));
  };

  /* ---------- webapis.avplay -> ExoPlayer ---------- */
  var L = {}, st = { cur: 0, dur: 0, onOk: null, onErr: null };
  window.__av = {
    prepared: function (dur) { st.dur = dur; var f = st.onOk; st.onOk = st.onErr = null; if (f) f(); },
    error: function (msg) {
      var f = st.onErr; st.onOk = st.onErr = null;
      if (f) f(msg); else if (L.onerror) L.onerror(msg);
    },
    time: function (cur, dur) { st.cur = cur; if (dur > 0) st.dur = dur; if (L.oncurrentplaytime) L.oncurrentplaytime(cur); },
    buffering: function (on) { var f = on ? L.onbufferingstart : L.onbufferingcomplete; if (f) f(); },
    ended: function () { if (L.onstreamcompleted) L.onstreamcompleted(); }
  };
  window.webapis = {
    avplay: {
      open: function (url) { st.cur = 0; st.dur = 0; NP.open(String(url)); },
      setDisplayRect: function () {},
      setDisplayMethod: function () {},
      setListener: function (l) { L = l || {}; },
      prepareAsync: function (ok, err) { st.onOk = ok; st.onErr = err; NP.prepare(); },
      play: function () { NP.play(); },
      pause: function () { NP.pause(); },
      seekTo: function (ms) { st.cur = ms; NP.seekTo(Math.floor(ms)); },
      getDuration: function () { return st.dur; },
      getCurrentTime: function () { return st.cur; },
      getTotalTrackInfo: function () { try { return JSON.parse(NP.tracks()); } catch (e) { return []; } },
      setSelectTrack: function (type, index) { if (type === 'AUDIO') NP.selectAudio(index); },
      stop: function () { NP.stop(); },
      close: function () { NP.close(); }
    },
    productinfo: { getDuid: function () { return ''; } }
  };

  /* ---------- tizen ---------- */
  window.tizen = {
    application: { getCurrentApplication: function () { return { exit: function () { NA.exit(); }, hide: function () { NA.exit(); } }; } },
    tvinputdevice: { registerKey: function () {}, unregisterKey: function () {} }
  };

  /* ---------- injected remote keys ---------- */
  window.__key = function (code) {
    var e = new KeyboardEvent('keydown', { bubbles: true, cancelable: true });
    Object.defineProperty(e, 'keyCode', { get: function () { return code; } });
    Object.defineProperty(e, 'which', { get: function () { return code; } });
    (document.activeElement || document.body).dispatchEvent(e);
  };
  // Chrome reports media keys with its own codes; map them to Tizen's
  var MAP = { 179: 10252, 178: 413, 176: 417, 177: 412 };
  document.addEventListener('keydown', function (e) {
    var m = MAP[e.keyCode]; if (!m) return;
    e.stopImmediatePropagation(); e.preventDefault(); window.__key(m);
  }, true);

  var css = document.createElement('style');
  css.textContent = '#avplayObj{display:none!important}body.playing #app *{visibility:hidden!important}';
  document.head.appendChild(css);
})();
