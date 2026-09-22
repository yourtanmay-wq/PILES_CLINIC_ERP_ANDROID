/* ============ PILES CLINIC — ভিতরের কাটা ছবি (V558) ============
   ডাক্তারি বইয়ের মত পাশ থেকে কাটা ছবি: পায়ুনালী, দাঁতের রেখা,
   ভিতরের ও বাইরের পাইলস, ফিস্টুলার নালী, ফিসারের ফাটল।
   ছবিটা ১ নম্বর ছবিতে যা টিক দেওয়া হয়েছে তার সাথেই মিলে যায় —
   ঘড়ির কাঁটার নম্বর দেখেই বোঝা যায় কোনটা কোন পাশে।
   বাইরের কোনো লাইব্রেরি নেই, শুধু canvas 2D।                       */
(function (root) {
  'use strict';

  var CM = 62;                                   // ১ সেমি = কত পিক্সেল
  var VERGE_Y = 0, CANAL_CM = 3.6, DENTATE_CM = 2.0, TOP_CM = 4.6;

  function px(ctx, cx, cy, xcm, ycm) { return [cx + xcm * CM, cy - ycm * CM]; }

  // নালীর অর্ধেক চওড়া — নিচে (মুখে) সরু, উপরে (মলাশয়) চওড়া
  function halfWidth(hcm, piles, side) {
    var d = hcm / CANAL_CM;
    var w = 0.26 + 0.62 * Math.pow(Math.max(0, d), 1.05);
    for (var i = 0; i < piles.length; i++) {
      var p = piles[i];
      if (!p.inner) continue;
      if (pileSide(p) !== side) continue;
      // ফোলাটা গোলগাল — দেওয়ালের গায়ে বসানো অর্ধেক গোলার মত (পাতার মত ছুঁচোলো নয়)
      var R = pileRange(p);
      var t = (hcm - (p.hcm || (DENTATE_CM + 0.55))) / R;
      if (t > -1 && t < 1) w -= p.size * 0.78 * Math.sqrt(1 - t * t);
    }
    return Math.max(0.10, w);
  }
  // ঘড়ির কাঁটা থেকে বোঝা যায় ফোলাটা বাঁ না ডান দেওয়ালে
  function pileSide(p) { return Math.cos(p.a) >= 0 ? 1 : -1; }
  function pileRange(p) { return 0.46 + 0.30 * p.size; }

  // পেশির পুরুত্ব: নিচে (মুখের কাছে) মোটা, উপরে আস্তে আস্তে মিলিয়ে যায়
  function INT_T(h) { return 0.20 + 0.44 * Math.exp(-Math.pow((h - 1.0) / 1.9, 2)); }
  function EXT_T(h) { return INT_T(h) + 0.90 * Math.exp(-Math.pow((h - 0.75) / 1.35, 2)); }

  /* ---- রঙের ধরন ----
     TK বললেন ছবিটা "ভয়ানক" দেখাচ্ছে — রক্ত-মাংসের মত। তাই তিন রকম রং:
     real = বাস্তবের মত · soft = নরম, ভয় লাগে না · line = সাদামাটা নকশা   */
  var PALETTES = {
    real: { fat:'#EFDCB2', fat2:'#E3CB9C', skin:'#B0785C', musc:'#B0554F', musc2:'#8E3C3A',
            extA:'#C4635A', extB:'#9E4744', muco:'#B75C6A', anod:'#D7A79A', lumen:'#3A1A20',
            pile:'#9B2F4A', pileHi:'#C4506B', pileLine:'#7C1F38', dent:'#B07A22',
            bg1:'#FBF6EE', bg2:'#F2E9DC', fiber:'rgba(255,255,255,0.30)', outline:0 },
    soft: { fat:'#F6EDDC', fat2:'#EADFC6', skin:'#C89B80', musc:'#E0A9A2', musc2:'#CE9089',
            extA:'#E7B4AB', extB:'#D2988F', muco:'#DFA3AC', anod:'#EBCDC4', lumen:'#7C6068',
            pile:'#CE8298', pileHi:'#E2A5B4', pileLine:'#A96A7C', dent:'#C0983F',
            bg1:'#FDFAF5', bg2:'#F7F1E7', fiber:'rgba(255,255,255,0.42)', outline:0 },
    scary:{ fat:'#6E2A24', fat2:'#8A3A30', skin:'#7A3A2C', musc:'#8E1F26', musc2:'#5E0F16',
            extA:'#A82A2C', extB:'#6A1218', muco:'#C21F3A', anod:'#B4535A', lumen:'#0B0405',
            pile:'#8E0B2A', pileHi:'#E0335C', pileLine:'#3E040F', dent:'#E8C05A',
            bg1:'#170709', bg2:'#050203', fiber:'rgba(255,140,140,0.20)', outline:0 },
    line: { fat:'#FFFFFF', fat2:'#EFE7D8', skin:'#8A6B58', musc:'#FAEDEA', musc2:'#F2DFDA',
            extA:'#FBF1EE', extB:'#F3E2DD', muco:'#FCEFF1', anod:'#FDF6F3', lumen:'#EFE9E3',
            pile:'#F7E2E8', pileHi:'#FDF2F5', pileLine:'#A45C71', dent:'#B08C3A',
            bg1:'#FFFFFF', bg2:'#FBFAF7', fiber:'rgba(160,120,110,0.35)', outline:1 }
  };
  var C_FAT, C_FAT2, C_SKIN, C_MUSC, C_MUSC2, C_EXTA, C_EXTB,
      C_MUCO, C_ANOD, C_LUMEN, C_PILE, C_PILEHI, C_PILELINE, C_DENT, C_FIBER, OUTLINE, BG1, BG2;
  function applyStyle(st) {
    var q = PALETTES[st.style] || PALETTES.soft;
    C_FAT = q.fat; C_FAT2 = q.fat2; C_SKIN = q.skin; C_MUSC = q.musc; C_MUSC2 = q.musc2;
    C_EXTA = q.extA; C_EXTB = q.extB; C_MUCO = q.muco; C_ANOD = q.anod; C_LUMEN = q.lumen;
    C_PILE = q.pile; C_PILEHI = q.pileHi; C_PILELINE = q.pileLine; C_DENT = q.dent;
    C_FIBER = q.fiber; OUTLINE = q.outline; BG1 = q.bg1; BG2 = q.bg2;
    LABEL_HALO = (st.style === 'scary') ? 'rgba(0,0,0,0.85)' : 'rgba(255,255,255,0.85)';
  }
  // নকশার ধরনে সব কিছুর চারপাশে সরু কালো দাগ পড়ে
  function edge(ctx, col, w) {
    if (!OUTLINE) return;
    ctx.strokeStyle = col || 'rgba(90,64,48,0.65)'; ctx.lineWidth = w || 1.4; ctx.stroke();
  }

  function render(ctx, W, H, st) {
    applyStyle(st);
    var SCARY = SCARY_G = (st.style === 'scary');
    var T = st.t || 0;                       // ০ থেকে ১ — একটা চক্র
    // ধুকধুকানি: ব্যথার তালে ফোলা একটু বড়-ছোট হয়
    var beat = 1 + 0.085 * Math.sin(T * Math.PI * 2);
    var raw = st.piles || [];
    var piles = (T ? raw.map(function (q) {
      return { a: q.a, size: q.size * beat, inner: q.inner, hcm: q.hcm };
    }) : raw);
    var cx = W * 0.5, cy = H * 0.86;
    ctx.clearRect(0, 0, W, H);
    var g;
    if (SCARY) {                       // মাঝখানে আলো, চারপাশে ঘুটঘুটে অন্ধকার
      g = ctx.createRadialGradient(W * 0.5, H * 0.55, W * 0.06, W * 0.5, H * 0.55, W * 0.62);
      g.addColorStop(0, '#3A1012'); g.addColorStop(0.55, BG1); g.addColorStop(1, BG2);
    } else {
      g = ctx.createLinearGradient(0, 0, 0, H);
      g.addColorStop(0, BG1); g.addColorStop(1, BG2);
    }
    ctx.fillStyle = g; ctx.fillRect(0, 0, W, H);

    /* ---- চর্বি ও শরীরের মাংস ----
       ভয় ধরানো ধরনে চারকোনা বাক্স নয় — কিনারা অন্ধকারে মিলিয়ে যায়,
       তাই ছবিটা আঁকা বাক্সের মত না লেগে সত্যিকারের মাংস মনে হয়।       */
    if (SCARY) {
      // ভিতরে লাল মাংস → বাইরে হলদে চর্বি → সবচেয়ে বাইরে বাদামি চামড়া
      var bg = ctx.createRadialGradient(cx, cy - 2.0 * CM, 0.4 * CM, cx, cy - 2.0 * CM, 4.6 * CM);
      bg.addColorStop(0.00, '#8C2A26');
      bg.addColorStop(0.34, '#C08A52');
      bg.addColorStop(0.62, '#A9724B');
      bg.addColorStop(0.84, '#7A4E33');
      bg.addColorStop(1.00, 'rgba(18,8,5,0)');
      ctx.save(); ctx.translate(cx, cy - 2.3 * CM); ctx.scale(1.18, 0.95); ctx.translate(-cx, -(cy - 2.3 * CM));
      ctx.fillStyle = bg;
      ctx.beginPath(); ctx.arc(cx, cy - 2.3 * CM, 4.6 * CM, 0, 6.3); ctx.fill();
      ctx.restore();
    } else {
      ctx.fillStyle = C_FAT;
      ctx.beginPath();
      ctx.moveTo(cx - 3.7 * CM, cy - 5.3 * CM);
      ctx.lineTo(cx + 3.6 * CM, cy - 5.3 * CM);
      ctx.quadraticCurveTo(cx + 3.9 * CM, cy - 1.2 * CM, cx + 2.5 * CM, cy + 0.35 * CM);
      ctx.quadraticCurveTo(cx, cy + 0.95 * CM, cx - 2.5 * CM, cy + 0.35 * CM);
      ctx.quadraticCurveTo(cx - 3.9 * CM, cy - 1.2 * CM, cx - 3.6 * CM, cy - 5.3 * CM);
      ctx.closePath(); ctx.fill(); edge(ctx);
      ctx.strokeStyle = C_FAT2; ctx.lineWidth = 1;
      for (var i = 0; i < 26; i++) {
        var ax = cx + (((i * 37) % 70) / 70 - 0.5) * 7 * CM;
        var ay = cy - 0.3 * CM - ((i * 53) % 45) / 45 * 4.4 * CM;
        ctx.beginPath(); ctx.arc(ax, ay, 5 + (i % 3) * 2, 0, 6.3); ctx.stroke();
      }
    }

    /* ---- দুই পাছা: এটা দেখলেই বোঝা যায় ছবিটা কিসের ---- */
    if (SCARY) {
      [-1, 1].forEach(function (sd) {
        var bxc = cx + sd * 2.95 * CM, byc = cy - 0.15 * CM;
        var cg = ctx.createRadialGradient(bxc - sd * 0.5 * CM, byc - 0.7 * CM, 0.2 * CM, bxc, byc, 2.75 * CM);
        cg.addColorStop(0, '#C79062'); cg.addColorStop(0.55, '#9E6844');
        cg.addColorStop(0.85, '#6B4229'); cg.addColorStop(1, 'rgba(16,7,4,0)');
        ctx.save(); ctx.translate(bxc, byc); ctx.scale(1, 1.05); ctx.translate(-bxc, -byc);
        ctx.fillStyle = cg;
        ctx.beginPath(); ctx.arc(bxc, byc, 2.75 * CM, 0, 6.3); ctx.fill();
        ctx.restore();
      });
      // দুই পাছার মাঝের খাঁজের ছায়া
      var cl = ctx.createLinearGradient(cx - 0.9 * CM, 0, cx + 0.9 * CM, 0);
      cl.addColorStop(0, 'rgba(20,8,6,0)'); cl.addColorStop(0.5, 'rgba(20,8,6,0.55)');
      cl.addColorStop(1, 'rgba(20,8,6,0)');
      ctx.fillStyle = cl; ctx.fillRect(cx - 0.9 * CM, cy - 5.0 * CM, 1.8 * CM, 6.0 * CM);
    }

    /* ---- চামড়ার কিনারা ---- */
    ctx.strokeStyle = C_SKIN; ctx.lineWidth = 7; ctx.lineCap = 'round';
    ctx.beginPath();
    ctx.moveTo(cx - 3.6 * CM, cy - 0.9 * CM);
    ctx.quadraticCurveTo(cx - 2.4 * CM, cy + 0.42 * CM, cx - 0.55 * CM, cy + 0.06 * CM);
    ctx.stroke();
    ctx.beginPath();
    ctx.moveTo(cx + 3.6 * CM, cy - 0.9 * CM);
    ctx.quadraticCurveTo(cx + 2.4 * CM, cy + 0.42 * CM, cx + 0.55 * CM, cy + 0.06 * CM);
    ctx.stroke();

    /* ---- মাংসপেশি: ভিতরের গোল পেশি (internal sphincter) ---- */
    [-1, 1].forEach(function (side) {
      ctx.beginPath();
      wallPath(ctx, cx, cy, piles, side, 0, false, 0, CANAL_CM);
      wallPath(ctx, cx, cy, piles, side, INT_T, true, 0, CANAL_CM);
      ctx.closePath();
      var lg = ctx.createLinearGradient(cx, 0, cx + side * 2.4 * CM, 0);
      lg.addColorStop(0, C_MUSC); lg.addColorStop(1, C_MUSC2);
      ctx.fillStyle = lg; ctx.fill(); edge(ctx);
    });
    /* ---- বাইরের গোল পেশি (external sphincter) — মাংসের বলয়, আঁশের দাগ সহ ---- */
    [-1, 1].forEach(function (side) {
      ctx.save();
      ctx.beginPath();
      wallPath(ctx, cx, cy, piles, side, INT_T, false, 0, 2.9);
      wallPath(ctx, cx, cy, piles, side, EXT_T, true, 0, 2.9);
      ctx.closePath();
      var lg2 = ctx.createLinearGradient(cx + side * 0.6 * CM, 0, cx + side * 2.2 * CM, 0);
      lg2.addColorStop(0, C_EXTA); lg2.addColorStop(1, C_EXTB);
      if (SCARY) { ctx.save(); ctx.shadowColor = 'rgba(0,0,0,0.8)'; ctx.shadowBlur = 22;
                   ctx.fillStyle = lg2; ctx.fill(); ctx.restore(); }
      ctx.fillStyle = lg2; ctx.fill(); edge(ctx);
      ctx.clip();
      ctx.strokeStyle = C_FIBER; ctx.lineWidth = 1.4;   // মাংসের আঁশ
      for (var k = 0; k < 16; k++) {
        var yy = cy - k * 0.18 * CM;
        ctx.beginPath();
        ctx.moveTo(cx + side * 0.4 * CM, yy);
        ctx.lineTo(cx + side * 2.4 * CM, yy - 0.10 * CM);
        ctx.stroke();
      }
      ctx.restore();
    });

    /* ---- নালীর ফাঁকা জায়গা ---- */
    ctx.beginPath();
    wallPath(ctx, cx, cy, piles, -1, 0);
    wallPath(ctx, cx, cy, piles, 1, 0, true);
    ctx.closePath();
    ctx.fillStyle = C_LUMEN; ctx.fill(); edge(ctx, 'rgba(120,80,70,0.7)', 1.6);

    if (SCARY) {
      var glow = ctx.createRadialGradient(cx, cy - 1.4 * CM, 0.2 * CM, cx, cy - 1.4 * CM, 2.6 * CM);
      glow.addColorStop(0, 'rgba(214,40,58,0.30)'); glow.addColorStop(1, 'rgba(214,40,58,0)');
      ctx.fillStyle = glow; ctx.fillRect(0, 0, W, H);
    }
    /* ---- নালীর গায়ের পর্দা: নিচে ফ্যাকাশে, উপরে লাল ---- */
    [-1, 1].forEach(function (side) {
      ctx.save(); ctx.lineWidth = 6; ctx.lineCap = 'round';
      ctx.beginPath(); wallPath(ctx, cx, cy, piles, side, 0, false, 0, DENTATE_CM);
      ctx.strokeStyle = C_ANOD; ctx.stroke();
      ctx.beginPath(); wallPath(ctx, cx, cy, piles, side, 0, false, DENTATE_CM, CANAL_CM);
      ctx.strokeStyle = C_MUCO; ctx.stroke();
      ctx.restore();
    });

    /* ---- ভিতরের পাইলস — দেওয়ালের গা থেকেই ফুলে ওঠে (আলাদা বল নয়) ---- */
    var stackL = 0, stackR = 0;
    piles.forEach(function (p) {
      if (!p.inner) return;
      var side = pileSide(p);
      var n = side > 0 ? stackR++ : stackL++;
      p.hcm = DENTATE_CM + 0.50 + n * 0.62;
      var others = piles.filter(function (q) { return q !== p; });
      var R2 = pileRange(p);
      var h0 = p.hcm - R2, h1 = p.hcm + R2, N = 30, i;
      ctx.beginPath();
      for (i = 0; i <= N; i++) {                       // ফোলা দেওয়ালের কিনারা
        var hh = h0 + (h1 - h0) * (i / N);
        var pt = px(ctx, cx, cy, side * halfWidth(hh, piles, side), hh);
        if (i === 0) ctx.moveTo(pt[0], pt[1]); else ctx.lineTo(pt[0], pt[1]);
      }
      for (i = N; i >= 0; i--) {                       // ফোলা না থাকলে দেওয়াল যেখানে থাকত
        var hb = h0 + (h1 - h0) * (i / N);
        var pb = px(ctx, cx, cy, side * halfWidth(hb, others, side), hb);
        ctx.lineTo(pb[0], pb[1]);
      }
      ctx.closePath();
      var cc = px(ctx, cx, cy, side * halfWidth(p.hcm, piles, side), p.hcm);
      var rg = ctx.createRadialGradient(cc[0] + side * 4, cc[1] - 6, 2, cc[0], cc[1], (0.35 + p.size) * CM);
      rg.addColorStop(0, C_PILEHI); rg.addColorStop(1, C_PILE);
      if (SCARY) { ctx.save(); ctx.shadowColor = 'rgba(0,0,0,0.85)'; ctx.shadowBlur = 26;
                   ctx.shadowOffsetY = 7; ctx.fillStyle = rg; ctx.fill(); ctx.restore(); }
      ctx.fillStyle = rg; ctx.fill();
      ctx.strokeStyle = C_PILELINE; ctx.lineWidth = 1.4; ctx.stroke();
      if (SCARY) gloss(ctx, cc[0] - side * 0.06 * CM, cc[1] - 0.14 * CM, (0.16 + p.size * 0.26) * CM);
      var tx = cx + side * 3.05 * CM;
      ctx.strokeStyle = C_PILELINE; ctx.lineWidth = 1.1;
      ctx.beginPath(); ctx.moveTo(cc[0], cc[1]); ctx.lineTo(tx, cc[1]); ctx.stroke();
      if (p.marked !== false) tag(ctx, tx, cc[1], markText(p.a, st.labelMode));
    });
    /* ---- বাইরের পাইলস — দাঁতের রেখার নিচে, মুখের কাছে ---- */
    piles.forEach(function (p) {
      if (p.inner) return;
      var side = pileSide(p), hh = 0.16;
      var w = halfWidth(hh, piles, side);
      var c = px(ctx, cx, cy, side * (w + 0.30 + p.size * 0.32), hh);
      ctx.beginPath();
      ctx.ellipse(c[0], c[1], (0.20 + p.size * 0.40) * CM, (0.12 + p.size * 0.20) * CM, side * 0.35, 0, 6.3);
      ctx.fillStyle = C_PILE; ctx.fill();
      ctx.strokeStyle = C_PILELINE; ctx.lineWidth = 1.4; ctx.stroke();
      var tx2 = cx + side * 3.05 * CM;
      ctx.strokeStyle = C_PILELINE; ctx.lineWidth = 1.1;
      ctx.beginPath(); ctx.moveTo(c[0] + side * 0.3 * CM, c[1]); ctx.lineTo(tx2, c[1]); ctx.stroke();
      if (p.marked !== false) tag(ctx, tx2, c[1], markText(p.a, st.labelMode));
    });

    /* ---- দাঁতের রেখা ---- */
    var dl = px(ctx, cx, cy, 0, DENTATE_CM);
    ctx.save(); ctx.setLineDash([7, 5]); ctx.strokeStyle = C_DENT; ctx.lineWidth = 2;
    ctx.beginPath(); ctx.moveTo(cx - 3.0 * CM, dl[1]); ctx.lineTo(cx + 3.0 * CM, dl[1]); ctx.stroke();
    ctx.restore();
    label(ctx, cx - 3.62 * CM, dl[1] - 12, 'দাঁতের রেখা · ২ সেমি', '#8A5A12', 'left');

    /* ---- ফিস্টুলার নালী ---- */
    if (st.fistula) {
      var f = st.fistula, sd = f.side || 1;
      var out = px(ctx, cx, cy, sd * (f.outCm || 2.4), -0.25);
      var inn = px(ctx, cx, cy, sd * halfWidth(f.inHcm || DENTATE_CM, piles, sd), f.inHcm || DENTATE_CM);
      ctx.save(); ctx.lineCap = 'round';
      ctx.beginPath();
      ctx.moveTo(out[0], out[1]);
      ctx.quadraticCurveTo(out[0] - sd * 0.5 * CM, inn[1] + 0.5 * CM, inn[0], inn[1]);
      ctx.strokeStyle = '#4A2A16'; ctx.lineWidth = 11; ctx.stroke();
      ctx.strokeStyle = '#E8B23A'; ctx.lineWidth = 5; ctx.setLineDash([9, 6]); ctx.stroke();
      ctx.restore();
      dot(ctx, out[0], out[1], '#8E1B1B', 8);       // বাইরের মুখ
      dot(ctx, inn[0], inn[1], '#F0C24A', 7);       // ভিতরের মুখ
      label(ctx, out[0] + sd * 14, out[1] + 20, 'বাইরের মুখ', '#8E1B1B', sd > 0 ? 'left' : 'right');
      label(ctx, inn[0] - sd * 14, inn[1] - 16, 'ভিতরের মুখ', '#8A5A12', sd > 0 ? 'right' : 'left');
    }

    /* ---- ফিসার (ফাটল) ---- */
    if (st.fissure) {
      var fy = px(ctx, cx, cy, 0, 0.9), sdf = st.fissure.side || -1;
      var w2 = halfWidth(0.9, piles, sdf);
      ctx.save(); ctx.strokeStyle = '#C0392B'; ctx.lineWidth = 4; ctx.lineCap = 'round';
      ctx.beginPath();
      ctx.moveTo(cx + sdf * w2 * CM, fy[1] + 0.5 * CM);
      ctx.lineTo(cx + sdf * (w2 + 0.22) * CM, fy[1]);
      ctx.lineTo(cx + sdf * (w2 + 0.05) * CM, fy[1] - 0.45 * CM);
      ctx.stroke(); ctx.restore();
      label(ctx, cx + sdf * 1.85 * CM, fy[1] - 0.75 * CM, 'ফাটল', '#C0392B', sdf > 0 ? 'left' : 'right');
    }

    /* ---- মাপকাঠি ও নাম ---- */
    if (SCARY) { reseed(); mottle(ctx, cx, cy, W, H, 46); vessels(ctx, cx, cy, piles, 110); }
    if (st.bleed && T) {
      var bt = (T * 1.0) % 1;                              // ফোঁটা কতদূর নেমেছে
      var bh = (DENTATE_CM + 0.5) * (1 - bt) + 0.05 * bt;
      var bs = st.bleed.side || 1;
      var bp = px(ctx, cx, cy, bs * (halfWidth(bh, piles, bs) - 0.06), bh);
      ctx.save();
      ctx.globalAlpha = bt < 0.85 ? 1 : (1 - bt) / 0.15;
      ctx.beginPath();
      ctx.ellipse(bp[0], bp[1], 0.09 * CM, 0.15 * CM, 0, 0, 6.3);
      ctx.fillStyle = '#8E0B14'; ctx.fill();
      ctx.strokeStyle = 'rgba(255,120,120,0.45)'; ctx.lineWidth = 1; ctx.stroke();
      ctx.restore();
    }
    if (SCARY) {
      var vg = ctx.createRadialGradient(W * 0.5, H * 0.55, W * 0.20, W * 0.5, H * 0.55, W * 0.60);
      vg.addColorStop(0, 'rgba(0,0,0,0)'); vg.addColorStop(1, 'rgba(0,0,0,0.72)');
      ctx.fillStyle = vg; ctx.fillRect(0, 0, W, H);
    }
    scaleBar(ctx, W - 58, cy, cy - CANAL_CM * CM);
    if (SCARY && st.grain !== false) grain(ctx, W, H, 26);
    var TXT = SCARY ? '#E9D2C6' : '#5A4030';
    label(ctx, cx, cy + 0.92 * CM, 'পায়ুদ্বার (বাইরের মুখ)', TXT, 'center');
    label(ctx, cx, cy - (TOP_CM + 0.42) * CM, 'মলাশয় (উপরের দিকে)', TXT, 'center');
    label(ctx, cx - 3.62 * CM, cy - 0.95 * CM, 'গোল মাংসপেশি', C_PILELINE, 'left');
  }

  // নালীর এক পাশের দেওয়াল
  function wallPath(ctx, cx, cy, piles, side, off, reverse, h0, h1) {
    var lo = (h0 === undefined) ? 0 : h0, hi = (h1 === undefined) ? CANAL_CM : h1, i, first = true;
    var N = 40;
    for (i = 0; i <= N; i++) {
      var t = reverse ? (N - i) / N : i / N;
      var hh = lo + (hi - lo) * t;
      var o = (typeof off === 'function') ? off(hh) : off;
      var w = halfWidth(hh, piles, side) + o;
      var p = px(ctx, cx, cy, side * w, hh);
      if (first && !reverse) { ctx.lineTo(p[0], p[1]); first = false; }
      else ctx.lineTo(p[0], p[1]);
    }
  }

  // ডাক্তার চাইলে ঘড়ির নম্বরের বদলে সোজা বাংলায় দিক লেখা যায়
  var BANGLA_DIR = { 12:'সামনে', 1:'সামনে-ডান', 2:'সামনে-ডান', 3:'ডান পাশ', 4:'পিছন-ডান',
                     5:'পিছন-ডান', 6:'পিছনে', 7:'পিছন-বাঁ', 8:'পিছন-বাঁ', 9:'বাঁ পাশ',
                     10:'সামনে-বাঁ', 11:'সামনে-বাঁ' };
  function markText(a, mode) {
    var t = clockText(a);
    return (mode === 'bangla') ? BANGLA_DIR[t] : (t + 'টা');
  }

  function clockText(a) {
    var deg = ((a + Math.PI / 2) * 180 / Math.PI + 360) % 360;
    var t = Math.round(deg / 30); if (t === 0) t = 12;
    return t;
  }
  /* ---------- কার্টুন ভাব কাটানোর কাজ ----------
     সমান রঙের চ্যাপ্টা তল দেখলেই কার্টুন লাগে। তাই মাংসের গায়ে
     ছোপ, সরু রক্তের শিরা, আর সব শেষে সূক্ষ্ম দানা বসানো হয় —
     ঠিক যেমন আসল ছবিতে থাকে।                                        */
  var SEED = 20260822;
  function rnd() { SEED = (SEED * 1103515245 + 12345) & 0x7fffffff; return ((SEED >>> 8) & 0xffff) / 65536; }
  function reseed() { SEED = 20260822; }

  // মাংসের গায়ে অসমান ছোপ
  function mottle(ctx, cx, cy, W, H, n) {
    ctx.save(); ctx.globalCompositeOperation = 'overlay';
    for (var i = 0; i < n; i++) {
      var x = cx + (rnd() - 0.5) * 7.4 * CM, y = cy - rnd() * 5.6 * CM;
      var r = (0.25 + rnd() * 1.1) * CM, dark = rnd() < 0.5;
      var g = ctx.createRadialGradient(x, y, 0, x, y, r);
      g.addColorStop(0, dark ? 'rgba(60,10,14,0.30)' : 'rgba(255,180,170,0.22)');
      g.addColorStop(1, 'rgba(0,0,0,0)');
      ctx.fillStyle = g; ctx.beginPath(); ctx.arc(x, y, r, 0, 6.3); ctx.fill();
    }
    ctx.restore();
  }

  // সরু রক্তের শিরা
  function vessels(ctx, cx, cy, piles, n) {
    ctx.save(); ctx.lineCap = 'round';
    for (var i = 0; i < n; i++) {
      var side = rnd() < 0.5 ? -1 : 1;
      var h = 0.15 + rnd() * 3.2;
      var w = halfWidth(h, piles, side) + rnd() * 0.9;
      var p0 = px(ctx, cx, cy, side * w, h);
      ctx.beginPath(); ctx.moveTo(p0[0], p0[1]);
      var x = p0[0], y = p0[1];
      for (var k = 0; k < 3; k++) {
        x += (rnd() - 0.5) * 0.5 * CM; y += (rnd() - 0.35) * 0.4 * CM;
        ctx.lineTo(x, y);
      }
      ctx.strokeStyle = 'rgba(150,18,34,' + (0.10 + rnd() * 0.20).toFixed(2) + ')';
      ctx.lineWidth = 0.6 + rnd() * 1.3;
      ctx.stroke();
    }
    ctx.restore();
  }

  // সব শেষে সূক্ষ্ম দানা — এতেই ছবিটা আঁকা নয়, তোলা মনে হয়
  function grain(ctx, W, H, amt) {
    var img, d, i, n;
    try { img = ctx.getImageData(0, 0, W, H); } catch (e) { return; }
    d = img.data;
    for (i = 0; i < d.length; i += 4) {
      SEED = (SEED * 1103515245 + 12345) & 0x7fffffff;
      n = (((SEED >>> 16) & 255) / 255 - 0.5) * amt;
      d[i]     = clamp255(d[i]     + n * 1.25);
      d[i + 1] = clamp255(d[i + 1] + n * 0.80);
      d[i + 2] = clamp255(d[i + 2] + n * 0.80);
    }
    ctx.putImageData(img, 0, 0);
  }
  function clamp255(v) { return v < 0 ? 0 : (v > 255 ? 255 : v); }

  function gloss(ctx, x, y, r) {
    var gg = ctx.createRadialGradient(x, y, 0, x, y, r);
    gg.addColorStop(0, 'rgba(255,225,225,0.42)'); gg.addColorStop(1, 'rgba(255,235,235,0)');
    ctx.fillStyle = gg;
    ctx.beginPath(); ctx.ellipse(x, y, r, r * 0.72, 0, 0, 6.3); ctx.fill();
  }

  function dot(ctx, x, y, col, r) {
    ctx.beginPath(); ctx.arc(x, y, r, 0, 6.3);
    ctx.fillStyle = col; ctx.fill();
    ctx.lineWidth = 2; ctx.strokeStyle = 'rgba(255,255,255,0.92)'; ctx.stroke();
  }
  var LABEL_HALO = 'rgba(255,255,255,0.85)', SCARY_G = false;
  function label(ctx, x, y, txt, col, al) {
    ctx.font = '600 13px "Noto Sans Bengali", system-ui, sans-serif';
    ctx.textAlign = al || 'left'; ctx.textBaseline = 'middle';
    ctx.lineWidth = 3; ctx.strokeStyle = LABEL_HALO;
    ctx.strokeText(txt, x, y); ctx.fillStyle = col; ctx.fillText(txt, x, y);
  }
  function tag(ctx, x, y, txt) {
    ctx.font = '700 12px "Noto Sans Bengali", system-ui, sans-serif';
    ctx.textAlign = 'center'; ctx.textBaseline = 'middle';
    var w = Math.max(26, ctx.measureText(txt).width + 16), h = 24, r = 12;
    ctx.beginPath();
    ctx.moveTo(x - w / 2 + r, y - h / 2);
    ctx.lineTo(x + w / 2 - r, y - h / 2); ctx.quadraticCurveTo(x + w / 2, y - h / 2, x + w / 2, y - h / 2 + r);
    ctx.lineTo(x + w / 2, y + h / 2 - r); ctx.quadraticCurveTo(x + w / 2, y + h / 2, x + w / 2 - r, y + h / 2);
    ctx.lineTo(x - w / 2 + r, y + h / 2); ctx.quadraticCurveTo(x - w / 2, y + h / 2, x - w / 2, y + h / 2 - r);
    ctx.lineTo(x - w / 2, y - h / 2 + r); ctx.quadraticCurveTo(x - w / 2, y - h / 2, x - w / 2 + r, y - h / 2);
    ctx.closePath();
    ctx.fillStyle = SCARY_G ? 'rgba(20,6,8,0.80)' : 'rgba(255,255,255,0.94)'; ctx.fill();
    ctx.strokeStyle = SCARY_G ? 'rgba(220,150,140,0.65)' : C_PILELINE; ctx.lineWidth = 1.4; ctx.stroke();
    ctx.fillStyle = SCARY_G ? '#F0C9C0' : C_PILELINE; ctx.fillText(txt, x, y);
  }
  function scaleBar(ctx, x, y0, y1) {
    ctx.strokeStyle = SCARY_G ? '#D9C3B4' : '#6A5A48'; ctx.lineWidth = 2;
    ctx.beginPath(); ctx.moveTo(x, y0); ctx.lineTo(x, y1); ctx.stroke();
    for (var c = 0; c <= 3; c++) {
      var yy = y0 - c * CM;
      ctx.beginPath(); ctx.moveTo(x - 6, yy); ctx.lineTo(x + 6, yy); ctx.stroke();
      label(ctx, x - 10, yy, c + ' সেমি', SCARY_G ? '#D9C3B4' : '#6A5A48', 'right');
    }
  }

  root.AnatomySection = { render: render, CM: CM, CANAL_CM: CANAL_CM, DENTATE_CM: DENTATE_CM };
})(typeof window !== 'undefined' ? window : globalThis);
