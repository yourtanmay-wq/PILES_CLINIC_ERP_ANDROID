/* ============ PILES CLINIC — ছবির উপরে দাগ (V558) ============
   TK-এর নিয়ম: ডাক্তার রোগীকে বোঝানোর সময় *যে কোনো* ছবির উপরেই
   দাগ দিতে পারবেন — বইয়ের ছবি, আমাদের আঁকা ছবি, যেটাই হোক।
   দাগগুলো ছবির শতকরা হিসেবে জমা হয় (যেমন x=42%, y=63%), তাই
   ছোট ফোন, বড় ফোন, ওয়েব — সব জায়গায় একই জায়গায় বসে।
   ছবি বদলালে দাগও সেই ছবির নিজের দাগ থাকে।                      */
(function (root) {
  'use strict';

  var COLORS = { pile: '#D81E3F', tract: '#F0A400', arrow: '#1B6FD8', ring: '#12A05A', pen: '#111111' };

  /* --------- জমা হওয়ার চেহারা: এক লাইনে, নতুন ঘর লাগে না ---------
     pic=pic1|pile:42.1,63.4,বড়|tract:20,80;28,72;36,66|ring:55,40,9|note=... */
  function format(picKey, marks, note) {
    var out = ['pic=' + picKey];
    for (var i = 0; i < marks.length; i++) {
      var m = marks[i];
      if (m.kind === 'tract' || m.kind === 'pen') {
        out.push(m.kind + ':' + m.pts.map(function (p) { return n1(p[0]) + ',' + n1(p[1]); }).join(';'));
      } else if (m.kind === 'bulge') {
        out.push('bulge:' + n1(m.x) + ',' + n1(m.y) + ',' + n1(m.r) + ',' + n1(m.s));
      } else if (m.kind === 'ring') {
        out.push('ring:' + n1(m.x) + ',' + n1(m.y) + ',' + n1(m.r));
      } else if (m.kind === 'arrow') {
        out.push('arrow:' + n1(m.x) + ',' + n1(m.y) + ',' + n1(m.x2) + ',' + n1(m.y2));
      } else {
        out.push('pile:' + n1(m.x) + ',' + n1(m.y) + ',' + (m.label || ''));
      }
    }
    if (note) out.push('note=' + note.replace(/[|]/g, '/'));
    return out.join('|');
  }
  function n1(v) { return Math.round(v * 10) / 10; }

  function parse(saved) {
    var res = { pic: '', marks: [], note: '' };
    if (!saved) return res;
    var parts = String(saved).split('|');
    for (var i = 0; i < parts.length; i++) {
      var t = parts[i].trim(); if (!t) continue;
      if (t.indexOf('pic=') === 0) { res.pic = t.slice(4); continue; }
      if (t.indexOf('note=') === 0) { res.note = t.slice(5); continue; }
      var c = t.indexOf(':'); if (c < 0) continue;
      var kind = t.slice(0, c), body = t.slice(c + 1);
      if (kind === 'tract' || kind === 'pen') {
        var pts = body.split(';').map(function (s) {
          var a = s.split(','); return [parseFloat(a[0]), parseFloat(a[1])];
        }).filter(function (p) { return !isNaN(p[0]) && !isNaN(p[1]); });
        if (pts.length > 1) res.marks.push({ kind: kind, pts: pts });
      } else if (kind === 'bulge') {
        var g2 = body.split(',');
        res.marks.push({ kind: 'bulge', x: +g2[0], y: +g2[1], r: +g2[2], s: +g2[3] });
      } else if (kind === 'ring') {
        var r = body.split(',');
        res.marks.push({ kind: 'ring', x: +r[0], y: +r[1], r: +r[2] });
      } else if (kind === 'arrow') {
        var q = body.split(',');
        res.marks.push({ kind: 'arrow', x: +q[0], y: +q[1], x2: +q[2], y2: +q[3] });
      } else if (kind === 'pile') {
        var p2 = body.split(',');
        res.marks.push({ kind: 'pile', x: +p2[0], y: +p2[1], label: p2.slice(2).join(',') });
      }
    }
    return res;
  }

  /* --------- ছবির মাংস ফুলিয়ে তোলা ----------
     TK-এর দরকার: শুধু দাগ নয় — যে মাংসটা বেড়ে গেছে, তার উপরে আঙুল
     রেখে টান দিলে ছবির ওই মাংসটাই সত্যি সত্যি ফুলে উঠবে। এটা দাগ আঁকা
     নয়, ছবির ওই জায়গাটাকেই ফুলিয়ে দেওয়া (bulge) — আঙুল যত টানবেন
     তত বড়। উল্টো দিকে টানলে আবার ছোট হয়ে যাবে।
     শুধু যতটুকু জায়গায় ফোলা, ততটুকু অংশই হিসাব করা হয় — তাই ফোনেও
     আঙুলের সাথে সাথেই চলে।                                          */
  function bulge(ctx, W, H, b) {
    var cxp = b.x * W / 100, cyp = b.y * H / 100, R = b.r * W / 100;
    var st = Math.max(-0.85, Math.min(0.85, b.s || 0.45));
    if (R < 2 || st === 0) return;
    var x0 = Math.max(0, Math.floor(cxp - R)), y0 = Math.max(0, Math.floor(cyp - R));
    var x1 = Math.min(W, Math.ceil(cxp + R)), y1 = Math.min(H, Math.ceil(cyp + R));
    var w = x1 - x0, h = y1 - y0;
    if (w < 2 || h < 2) return;
    var src, dst;
    try { src = ctx.getImageData(x0, y0, w, h); } catch (e) { return; }
    dst = ctx.createImageData(w, h);
    var sd = src.data, dd = dst.data, ix, iy;
    for (iy = 0; iy < h; iy++) {
      for (ix = 0; ix < w; ix++) {
        var dx = (x0 + ix) - cxp, dy = (y0 + iy) - cyp;
        var d = Math.sqrt(dx * dx + dy * dy), o = (iy * w + ix) * 4;
        if (d >= R) { copyPx(sd, o, dd, o); continue; }
        var t = d / R, k = 1 - t * t;
        var f = 1 - st * k * k;                       // ভিতর থেকে টেনে বাইরে ঠেলা
        sample(sd, w, h, cxp - x0 + dx * f, cyp - y0 + dy * f, dd, o);
      }
    }
    ctx.putImageData(dst, x0, y0);

    // ফোলা মাংস রক্ত জমে গাঢ় হয়, আর উপরটা ভেজা-চকচকে থাকে
    var g = ctx.createRadialGradient(cxp, cyp, 0, cxp, cyp, R);
    g.addColorStop(0, 'rgba(158,18,44,0.42)'); g.addColorStop(0.7, 'rgba(124,12,38,0.22)');
    g.addColorStop(1, 'rgba(120,15,40,0)');
    ctx.fillStyle = g; ctx.beginPath(); ctx.arc(cxp, cyp, R, 0, 6.3); ctx.fill();
    var hg = ctx.createRadialGradient(cxp - R * 0.28, cyp - R * 0.30, 0, cxp - R * 0.28, cyp - R * 0.30, R * 0.55);
    hg.addColorStop(0, 'rgba(255,235,235,0.34)'); hg.addColorStop(1, 'rgba(255,235,235,0)');
    ctx.fillStyle = hg; ctx.beginPath(); ctx.arc(cxp - R * 0.28, cyp - R * 0.30, R * 0.55, 0, 6.3); ctx.fill();
  }
  function copyPx(sd, so, dd, dofs) {
    dd[dofs] = sd[so]; dd[dofs + 1] = sd[so + 1]; dd[dofs + 2] = sd[so + 2]; dd[dofs + 3] = sd[so + 3];
  }
  function sample(sd, w, h, fx, fy, dd, o) {
    var x = Math.max(0, Math.min(w - 1.001, fx)), y = Math.max(0, Math.min(h - 1.001, fy));
    var xi = x | 0, yi = y | 0, ax = x - xi, ay = y - yi;
    var i00 = (yi * w + xi) * 4, i10 = i00 + 4, i01 = i00 + w * 4, i11 = i01 + 4;
    for (var c = 0; c < 4; c++) {
      var top = sd[i00 + c] * (1 - ax) + sd[i10 + c] * ax;
      var bot = sd[i01 + c] * (1 - ax) + sd[i11 + c] * ax;
      dd[o + c] = top * (1 - ay) + bot * ay;
    }
  }

  // আঙুল কতটা টেনেছে → ফোলা কত বড়
  function bulgeFromDrag(startPct, nowPct, W, H) {
    var dx = (nowPct[0] - startPct[0]), dy = (nowPct[1] - startPct[1]);
    var pull = Math.sqrt(dx * dx + dy * dy);
    return { x: startPct[0], y: startPct[1],
             r: Math.max(3, Math.min(26, 4 + pull * 1.35)),
             s: Math.max(0.12, Math.min(0.80, 0.16 + pull * 0.055)) };
  }

  /* --------- ছবির উপরে আঁকা --------- */
  function draw(ctx, W, H, marks, opts) {
    opts = opts || {};
    var s = Math.min(W, H) / 100;                 // সব মাপ ছবির অনুপাতে
    for (var b = 0; b < marks.length; b++) {      // মাংস ফোলানো আগে, দাগ পরে
      if (marks[b].kind === 'bulge') bulge(ctx, W, H, marks[b]);
    }
    ctx.lineCap = 'round'; ctx.lineJoin = 'round';
    for (var i = 0; i < marks.length; i++) {
      var m = marks[i];
      if (m.kind === 'tract' || m.kind === 'pen') {
        var w = (m.kind === 'tract' ? 2.2 : 1.4) * s;
        stroke(ctx, m.pts, W, H, 'rgba(0,0,0,0.45)', w + 1.6 * s);
        stroke(ctx, m.pts, W, H, m.kind === 'tract' ? COLORS.tract : COLORS.pen, w,
               m.kind === 'tract' ? [3.4 * s, 2.4 * s] : null);
        if (m.kind === 'tract' && opts.showCm) {
          var last = m.pts[m.pts.length - 1];
          chip(ctx, last[0] * W / 100 + 3 * s, last[1] * H / 100, tractCm(m.pts, opts) + ' সেমি', COLORS.tract, s);
        }
      } else if (m.kind === 'ring') {
        ctx.beginPath();
        ctx.ellipse(m.x * W / 100, m.y * H / 100, m.r * W / 100, m.r * W / 100 * 0.82, 0, 0, 6.3);
        ctx.strokeStyle = 'rgba(0,0,0,0.4)'; ctx.lineWidth = 2.8 * s; ctx.stroke();
        ctx.strokeStyle = COLORS.ring; ctx.lineWidth = 1.6 * s; ctx.stroke();
      } else if (m.kind === 'arrow') {
        arrow(ctx, m.x * W / 100, m.y * H / 100, m.x2 * W / 100, m.y2 * H / 100, s);
      } else if (m.kind === 'pile') {
        var x = m.x * W / 100, y = m.y * H / 100;
        ctx.beginPath(); ctx.arc(x, y, 2.4 * s, 0, 6.3);
        ctx.fillStyle = COLORS.pile; ctx.fill();
        ctx.strokeStyle = '#fff'; ctx.lineWidth = 1.1 * s; ctx.stroke();
        if (m.label) chip(ctx, x + 3.4 * s, y, m.label, COLORS.pile, s);
      }
    }
  }

  function stroke(ctx, pts, W, H, col, w, dash) {
    ctx.save();
    if (dash) ctx.setLineDash(dash);
    ctx.beginPath();
    for (var i = 0; i < pts.length; i++) {
      var x = pts[i][0] * W / 100, y = pts[i][1] * H / 100;
      if (i === 0) ctx.moveTo(x, y); else ctx.lineTo(x, y);
    }
    ctx.strokeStyle = col; ctx.lineWidth = w; ctx.stroke();
    ctx.restore();
  }
  function arrow(ctx, x1, y1, x2, y2, s) {
    var a = Math.atan2(y2 - y1, x2 - x1), hl = 4.2 * s;
    ctx.beginPath(); ctx.moveTo(x1, y1); ctx.lineTo(x2, y2);
    ctx.strokeStyle = 'rgba(0,0,0,0.4)'; ctx.lineWidth = 3.2 * s; ctx.stroke();
    ctx.strokeStyle = COLORS.arrow; ctx.lineWidth = 1.8 * s; ctx.stroke();
    ctx.beginPath();
    ctx.moveTo(x2, y2);
    ctx.lineTo(x2 - hl * Math.cos(a - 0.42), y2 - hl * Math.sin(a - 0.42));
    ctx.lineTo(x2 - hl * Math.cos(a + 0.42), y2 - hl * Math.sin(a + 0.42));
    ctx.closePath(); ctx.fillStyle = COLORS.arrow; ctx.fill();
  }
  function chip(ctx, x, y, txt, col, s) {
    ctx.font = '700 ' + (3.1 * s).toFixed(1) + 'px "Noto Sans Bengali", system-ui, sans-serif';
    ctx.textAlign = 'left'; ctx.textBaseline = 'middle';
    var w = ctx.measureText(txt).width + 2.6 * s, h = 5.2 * s, r = 2.6 * s;
    ctx.beginPath();
    ctx.moveTo(x + r, y - h / 2); ctx.lineTo(x + w - r, y - h / 2);
    ctx.quadraticCurveTo(x + w, y - h / 2, x + w, y); ctx.quadraticCurveTo(x + w, y + h / 2, x + w - r, y + h / 2);
    ctx.lineTo(x + r, y + h / 2); ctx.quadraticCurveTo(x, y + h / 2, x, y);
    ctx.quadraticCurveTo(x, y - h / 2, x + r, y - h / 2);
    ctx.closePath();
    ctx.fillStyle = 'rgba(255,255,255,0.94)'; ctx.fill();
    ctx.strokeStyle = col; ctx.lineWidth = 0.9 * s; ctx.stroke();
    ctx.fillStyle = col; ctx.fillText(txt, x + 1.3 * s, y + 0.2 * s);
  }

  /* নালীর লম্বা — ছবির গায়ে দেওয়া মাপকাঠি অনুযায়ী (cmPerPct) */
  function tractCm(pts, opts) {
    var k = (opts && opts.cmPerPct) || 0.09, sum = 0;
    for (var i = 1; i < pts.length; i++) {
      var dx = pts[i][0] - pts[i - 1][0], dy = pts[i][1] - pts[i - 1][1];
      sum += Math.sqrt(dx * dx + dy * dy);
    }
    return Math.round(sum * k * 10) / 10;
  }

  root.AnatomyMark = { format: format, parse: parse, draw: draw, tractCm: tractCm,
                       bulge: bulge, bulgeFromDrag: bulgeFromDrag, COLORS: COLORS };
})(typeof window !== 'undefined' ? window : globalThis);
