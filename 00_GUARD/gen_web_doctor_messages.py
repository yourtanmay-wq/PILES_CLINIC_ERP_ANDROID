# -*- coding: utf-8 -*-
"""
🔧 ডাক্তারের ৪টে বার্তা — ফোনের কোড থেকে **ওয়েবের কোড বানানোর যন্ত্র** (V733)

TK-নির্দেশ: *"ফোনের লেখা থেকে যন্ত্র দিয়ে ওয়েবেরটা বানাবেন, হাতে টাইপ করবেন না।"*

কেন যন্ত্র দিয়ে:
  · হাতে টাইপ করলে একটা অক্ষর এদিক-ওদিক হলেই ডাক্তারের কাছে ভুল বার্তা যেত।
  · এখন লেখার **একমাত্র উৎস** `DoctorMessage.kt`। ওয়েবেরটা সেখান থেকেই তৈরি।
  · `--check` দিলে মিলিয়ে দেখে — দুটো আলাদা হলে গার্ড ফেল করে।

কেন এটা নিরাপদ:
  ⛔ বার্তা-বানানো ১১টা ফাংশনেই **একটাও if/when/লুপ নেই** (গুনে দেখা) —
     সবগুলো শুধু `sb.append(...)`-এর সরল সারি। তাই যন্ত্রে অনুবাদ নির্ভুল।
  ⛔ যদি কখনো কেউ ওখানে শর্ত যোগ করে, এই যন্ত্র **থেমে গিয়ে জানাবে**
     (নিচের UNKNOWN-এক্সপ্রেশন যাচাই), চুপচাপ ভুল কোড বানাবে না।
"""
import io, re, sys, json

KT   = "02_ANDROID_SOURCE_CODE/PilesClinicApp/app/src/main/java/com/tkbiswas/pilesclinic/native/DoctorMessage.kt"
BR   = "02_ANDROID_SOURCE_CODE/PilesClinicApp/app/src/main/java/com/tkbiswas/pilesclinic/print/BranchInfo.kt"
JS   = "03_NETLIFY_READY/app.js"
BEGIN = "/* ==== WLV1_DOCMSG_BEGIN (যন্ত্রে তৈরি — হাতে বদলাবেন না) ==== */"
END   = "/* ==== WLV1_DOCMSG_END ==== */"

ESC = {'n':'\n','t':'\t','r':'\r','"':'"','\\':'\\',"'":"'",'$':'$','b':'\b'}
def unesc(s):
    r, i, n = [], 0, len(s)
    while i < n:
        if s[i] == '\\' and i+1 < n:
            c = s[i+1]
            if c == 'u':
                r.append(chr(int(s[i+2:i+6], 16))); i += 6; continue
            r.append(ESC.get(c, c)); i += 2; continue
        r.append(s[i]); i += 1
    return ''.join(r)

def jstr(s):
    """পাইথনের লেখা → JS-এর একক-উদ্ধৃতির literal।"""
    out = s.replace('\\', '\\\\').replace("'", "\\'")
    return "'" + out.replace('\n', '\\n').replace('\t', '\\t').replace('\r', '\\r') + "'"

def strip_comments(t):
    """কমেন্ট বাদ — কিন্তু **লেখার ভিতরের** // (যেমন https://) ছোঁয় না।"""
    out, i, n, instr = [], 0, len(t), False
    while i < n:
        c = t[i]
        if instr:
            if c == '\\': out.append(t[i:i+2]); i += 2; continue
            if c == '"': instr = False
            out.append(c); i += 1; continue
        if c == '"': instr = True; out.append(c); i += 1; continue
        if c == '/' and i+1 < n and t[i+1] == '/':
            j = t.find('\n', i); i = j if j >= 0 else n; continue
        if c == '/' and i+1 < n and t[i+1] == '*':
            j = t.find('*/', i+2); i = (j+2) if j >= 0 else n; continue
        out.append(c); i += 1
    return ''.join(out)

SRC = io.open(KT, encoding='utf-8').read()
BSRC = io.open(BR, encoding='utf-8').read()

# ── ১. ব্রাঞ্চের তথ্য (ঠিকানা · ফোন · ক্লিনিকের নাম) ────────────────
CONSTS = dict(re.findall(r'private const val (\w+)\s*=\s*"((?:[^"\\]|\\.)*)"', BSRC))
def cval(tok):
    tok = tok.strip()
    if tok.startswith('"'): return unesc(tok[1:-1])
    return CONSTS[tok]

branches = []
for m in re.finditer(r'val [A-Z_]+ = BranchInfo\((.*?)\n    \)', BSRC, re.S):
    blk = strip_comments(m.group(1))
    g = lambda k: re.search(k + r'\s*=\s*("(?:[^"\\]|\\.)*"|\w+)', blk).group(1)
    branches.append({
        'id': cval(g('id')), 'displayName': cval(g('displayName')),
        'clinicName': cval(g('clinicName')), 'addressLine': cval(g('addressLine')),
        'phoneLine': cval(g('phoneLine')),
    })
assert len(branches) == 5, "ব্রাঞ্চ ৫টা হওয়ার কথা, পাওয়া গেল %d" % len(branches)

# ── ২. চেম্বারের দিন · ম্যাপ · ফেসবুক (introBranchExtra) ─────────────
extra = {}
blk = SRC[SRC.index('private val introBranchExtra'):]
blk = blk[:blk.index('\n    )')]
# প্রতিটা সারি "id" to IntroBranchExtra( ... ) — বন্ধনী গুনে শেষ খুঁজি,
# তাই মাঝের কমেন্ট বা ভিতরের বন্ধনীতে ভুল হয় না।
for m in re.finditer(r'"(\w+)" to IntroBranchExtra\(', blk):
    bid = m.group(1); j = m.end(); depth = 1; instr = False
    while j < len(blk):
        c = blk[j]
        if instr:
            if c == '\\': j += 2; continue
            if c == '"': instr = False
        else:
            if c == '"': instr = True
            elif c == '(': depth += 1
            elif c == ')':
                depth -= 1
                if depth == 0: break
        j += 1
    body = strip_comments(blk[m.end():j])
    d = {}
    for k, v in re.findall(r'(\w+)\s*=\s*"((?:[^"\\]|\\.)*)"', body):
        d[k] = unesc(v)
    extra[bid] = d
assert len(extra) == 5, "introBranchExtra ৫টা হওয়ার কথা, পাওয়া গেল %d" % len(extra)

WEBSITE = unesc(re.search(r'INTRO_WEBSITE\s*=\s*"((?:[^"\\]|\\.)*)"', SRC).group(1))

# ── ৩. append-এর সারি বের করা ────────────────────────────────────────
def body_of(name):
    m = re.search(r'private fun %s\(' % re.escape(name), SRC)
    if not m: raise SystemExit('ফাংশন পাওয়া গেল না: ' + name)
    i = SRC.index('{', m.end()); depth = 0; j = i
    while j < len(SRC):
        if SRC[j] == '{': depth += 1
        elif SRC[j] == '}':
            depth -= 1
            if depth == 0: break
        j += 1
    return SRC[i:j]

def appends(text):
    """`sb.append(X)` গুলো ক্রমে — X হুবহু Kotlin-এর লেখা।"""
    out, i = [], 0
    while True:
        k = text.find('.append(', i)
        if k < 0: break
        j = k + len('.append('); depth = 1; instr = False; start = j
        while j < len(text):
            c = text[j]
            if instr:
                if c == '\\': j += 2; continue
                if c == '"': instr = False
            else:
                if c == '"': instr = True
                elif c == '(': depth += 1
                elif c == ')':
                    depth -= 1
                    if depth == 0: break
            j += 1
        out.append(text[start:j].strip()); i = j + 1
    return out

# Kotlin এক্সপ্রেশন → JS এক্সপ্রেশন (স্পষ্ট তালিকা; বাইরের কিছু এলে থেমে যাবে)
XMAP = {
    'dr': 'dr', 'pt': 'pt', 'dateText': 'dateText', 'visitDate': 'visitDate',
    'treatment': 'treatment', 'bloodTest': 'bloodTest', 'nextVisit': 'nextVisit',
    'areaLine': 'areaLine', 'INTRO_WEBSITE': 'W.site',
    'paymentDate.trim()': 'S(paymentDate)', 'mode.trim()': 'S(mode)',
    'mode.trim().ifBlank { blank }': '(S(mode)||blank)',
    'paymentDate.trim().ifBlank { blank }': '(S(paymentDate)||blank)',
    'visitDate.trim().ifBlank { blank }': '(S(visitDate)||blank)',
    'head(branch)': 'H(b,"bn")', 'foot(branch)': 'F(b,"bn")',
    'headHi(branch)': 'H(b,"hi")', 'footHi(branch)': 'F(b,"hi")',
    'headEn(branch)': 'H(b,"en")', 'footEn(branch)': 'F(b,"en")',
    'referralAmountLine(amount, blank)': 'AMT(amount,blank)',
    'referralRefLine(mode, referenceNo, blank)': 'REF(mode,referenceNo,blank)',
    'info.displayName': 'b.displayName', 'info.clinicName': 'b.clinicName',
    'info.addressLine': 'b.addressLine', 'info.phoneLine': 'b.phoneLine',
    'extra.bnName': 'x.bnName', 'extra.hiName': 'x.hiName',
    'extra.bnDays': 'x.bnDays', 'extra.hiDays': 'x.hiDays', 'extra.enDays': 'x.enDays',
    'extra.mapLink': 'x.mapLink', 'extra.facebookLink': 'x.facebookLink',
}
def toJs(expr):
    if expr.startswith('"') and expr.endswith('"'):
        return jstr(unesc(expr[1:-1]))
    if expr in XMAP: return XMAP[expr]
    raise SystemExit("⛔ অচেনা এক্সপ্রেশন — যন্ত্র থামল, হাতে দেখুন: " + expr)

def joinJs(exprs):
    return '\n    ' + '\n  + '.join(toJs(e) for e in exprs)

# চারটে বার্তা × তিন ভাষা
FN = {}
for name in ['introBn','introHi','arrivedBn','arrivedHi','arrivedEn',
             'detailsBn','detailsHi','detailsEn','referralPaidBn','referralPaidHi','referralPaidEn']:
    t = body_of(name)
    if re.search(r'\n\s+(if|when|for|while)\s*[\(\{]', t):
        raise SystemExit("⛔ %s-এ শর্ত/লুপ ঢুকেছে — যন্ত্র থামল, হাতে দেখুন।" % name)
    FN[name] = appends(t)

# intro-র লক করা টেমপ্লেট: when(lang) → bn / hi / else(en)
IT = body_of('introLockedTemplate')
seg = {}
for key, start in [('bn', '"bn" -> {'), ('hi', '"hi" -> {'), ('en', 'else -> {')]:
    i = IT.index(start) + len(start); depth = 1; j = i
    while j < len(IT):
        if IT[j] == '{': depth += 1
        elif IT[j] == '}':
            depth -= 1
            if depth == 0: break
        j += 1
    seg[key] = appends(IT[i:j])

def emit():
    L = []
    A = L.append
    A(BEGIN)
    A("/* 🔒 V733 — এই পুরো অংশটা `00_GUARD/gen_web_doctor_messages.py` **যন্ত্রে**")
    A("   বানিয়েছে, উৎস = ফোনের `DoctorMessage.kt` (লেখার একমাত্র উৎস)।")
    A("   ⛔ এখানে হাতে একটা অক্ষরও বদলাবেন না — বদলালে গার্ড [৯.২১] ফেল করবে।")
    A("   ⛔ লেখা বদলাতে হলে ফোনের DoctorMessage.kt বদলান, তারপর:")
    A("        python3 00_GUARD/gen_web_doctor_messages.py --write   */")
    A("var WLV1_DOCMSG_BRANCH=" + json.dumps(
        {b['id']: b for b in branches}, ensure_ascii=False, sort_keys=True) + ";")
    A("var WLV1_DOCMSG_EXTRA=" + json.dumps(extra, ensure_ascii=False, sort_keys=True) + ";")
    A("var WLV1_DOCMSG_SITE=" + jstr(WEBSITE) + ";")
    A("""function wlv1DocBranch(branch){
  var k=String(branch||'').trim().toLowerCase().replace(/[-_]/g,' ');
  var all=WLV1_DOCMSG_BRANCH;
  for(var id in all){ if(!Object.prototype.hasOwnProperty.call(all,id))continue;
    var b=all[id];
    if(String(b.displayName).toLowerCase()===k||String(id).replace(/_/g,' ')===k)return b; }
  return all['kishanganj'];
}
function wlv1DocExtra(branch){ var b=wlv1DocBranch(branch);
  return WLV1_DOCMSG_EXTRA[b.id]||WLV1_DOCMSG_EXTRA['kishanganj']; }
function wlv1DocName(raw,fallback){
  var t=String(raw||'').trim(); return (t?t:String(fallback||'').trim()).toUpperCase(); }""")
    A("""function wlv1DocMsgBuild(kind,lang,branch,o){
  o=o||{};
  var b=wlv1DocBranch(branch), x=wlv1DocExtra(branch), W={site:WLV1_DOCMSG_SITE};
  var dr=wlv1DocName(o.doctorName,o.doctorMobile), pt=wlv1DocName(o.patientName,o.patientMobile);
  var dateText=String(o.dateText||''), visitDate=String(o.visitDate||'');
  var treatment=String(o.treatment||''), bloodTest=String(o.bloodTest||''), nextVisit=String(o.nextVisit||'');
  var amount=Number(o.amount||0), paymentDate=String(o.paymentDate||''), mode=String(o.mode||'');
  var referenceNo=String(o.referenceNo||''), blank='______';
  var areaLine=(String(o.doctorArea||'').trim()==='')?'':(String(o.doctorArea).trim()+'\\n');
  function S(v){ return String(v||'').trim(); }
  function H(b,l){ return (l==='hi')? (b.clinicName+'\\n'+b.addressLine+'\\n\\u092b\\u094b\\u0928: '+b.phoneLine)
                 : (l==='en')? (b.clinicName+'\\n'+b.addressLine+'\\nPhone: '+b.phoneLine)
                 : (b.clinicName+'\\n'+b.addressLine+'\\n\\u09ab\\u09cb\\u09a8: '+b.phoneLine); }
  function F(b,l){ var s=(l==='hi')?'\\u0938\\u0938\\u094d\\u0928\\u0947\\u0939,':(l==='en')?'Regards,':'\\u09b8\\u09ac\\u09bf\\u09a8\\u09af\\u09bc\\u09c7,';
    return s+'\\nTK BISWAS\\nFounder & Consultant\\n'+b.clinicName+' \\u00b7 '+b.displayName; }
  function AMT(a,bl){ return a>0? Math.round(a).toLocaleString('en-IN') : bl; }
  function REF(m,r,bl){ return (S(m).toLowerCase()!=='online')? 'Not Applicable' : (S(r)||bl); }
  switch(kind+'|'+lang){""")
    order = [('intro','bn','__INTRO_BN__'), ('intro','hi','__INTRO_HI__'), ('intro','en','__INTRO_EN__'),
             ('arrived','bn','arrivedBn'), ('arrived','hi','arrivedHi'), ('arrived','en','arrivedEn'),
             ('details','bn','detailsBn'), ('details','hi','detailsHi'), ('details','en','detailsEn'),
             ('referralPaid','bn','referralPaidBn'), ('referralPaid','hi','referralPaidHi'),
             ('referralPaid','en','referralPaidEn')]
    for kind, lang, fn in order:
        exprs = seg[lang] if fn.startswith('__INTRO') else FN[fn]
        A("    case %s: return (%s\n    );" % (jstr(kind + '|' + lang), joinJs(exprs)))
    A("  }\n  return '';\n}\nwindow['wlv1DocMsgBuild']=wlv1DocMsgBuild;")
    A(END)
    return '\n'.join(L) + '\n'

BLOCK = emit()

if __name__ == '__main__':
    mode = sys.argv[1] if len(sys.argv) > 1 else '--print'
    if mode == '--print':
        print(BLOCK)
    elif mode == '--check':
        js = io.open(JS, encoding='utf-8').read()
        if BEGIN not in js or END not in js:
            print('❌ app.js-এ ডাক্তার-বার্তার অংশটাই নেই'); sys.exit(1)
        cur = js[js.index(BEGIN): js.index(END) + len(END)] + '\n'
        if cur.strip() != BLOCK.strip():
            print('❌ ফোন ও ওয়েবের ডাক্তার-বার্তা আলাদা হয়ে গেছে।')
            print('   ঠিক করতে চালান: python3 00_GUARD/gen_web_doctor_messages.py --write')
            sys.exit(1)
        print('✅ ফোন ও ওয়েবের ডাক্তার-বার্তা হুবহু এক')
    elif mode == '--write':
        js = io.open(JS, encoding='utf-8').read()
        if BEGIN in js and END in js:
            js = js[:js.index(BEGIN)] + BLOCK.rstrip('\n') + js[js.index(END) + len(END):]
        else:
            raise SystemExit('⛔ app.js-এ বসানোর জায়গা (BEGIN/END চিহ্ন) নেই')
        io.open(JS, 'w', encoding='utf-8').write(js)
        print('✅ app.js-এ বসানো হলো')
