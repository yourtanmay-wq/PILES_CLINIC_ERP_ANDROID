#!/usr/bin/env python3
"""
🔵 V1293 (১০.০৯.২০২৬) — তালিকা ৪১১-⑫ (ক), TK: *"ক করুন, সাবধানে"*

কম্পিউটার-অ্যাপ (03_NETLIFY_READY) **সত্যিই ব্রাউজারে চালিয়ে** দেখার পাহারা —
নকল সার্ভার (Supabase-এ কিছু যায় না, আসল তথ্য ছোঁয়া হয় না)।

যা দেখে (tests.js):
  A  localStorage-এর পুরনো টেবিল IndexedDB-তে সরে (V1287)     B  ১৪০০০ সারি reload-এর পরেও থাকে
  C  IndexedDB না থাকলে পুরনো localStorage-পথ                  D  অন্য ট্যাবের লেখা দেখা যায়
  E  মাস্টার-সেশনে boot, ড্যাশবোর্ড, page-error ০
  F1–F5  মোছা-চিহ্ন তালিকা: প্রথমবার পুরো · অপরিবর্তিত হলে বাদ · নতুন চিহ্নে আবার ·
         reload-এর পরে জমা কপি · চিহ্ন তোলা হলে আবার (V1292)

ব্যবহার:  python3 00_GUARD/web_browser_test/run.py        (ওয়েব ফাইল বদলালে বাধ্যতামূলক)
node-প্যাকেজ প্রজেক্টের **বাইরে** (~/.cache/tk_web_test) থাকে — ZIP বড় হয় না।
"""
import glob, os, subprocess, sys

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(os.path.dirname(HERE))
WORK = os.environ.get("TK_WEB_TEST_DIR", os.path.expanduser("~/.cache/tk_web_test"))

def ensure_node_modules():
    os.makedirs(WORK, exist_ok=True)
    need = [p for p in ("playwright", "@supabase/supabase-js") if not os.path.isdir(os.path.join(WORK, "node_modules", p))]
    if not need: return
    print("node-প্যাকেজ নামছে (একবারই):", ", ".join(need))
    if not os.path.exists(os.path.join(WORK, "package.json")):
        subprocess.run(["npm", "init", "-y"], cwd=WORK, capture_output=True)
    env = dict(os.environ, PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD="1")
    r = subprocess.run(["npm", "i", "playwright@1.58.2", "@supabase/supabase-js@2", "--no-audit", "--no-fund"], cwd=WORK, env=env, capture_output=True, text=True)
    if r.returncode: sys.exit("npm install ব্যর্থ:\n" + r.stderr[-800:])

def chromium_path():
    cands = sorted(glob.glob("/opt/pw-browsers/chromium-*/chrome-linux/chrome")) + sorted(glob.glob(os.path.expanduser("~/.cache/ms-playwright/chromium-*/chrome-linux/chrome")))
    return cands[-1] if cands else ""

def main():
    ensure_node_modules()
    sdk = os.path.join(WORK, "node_modules", "@supabase", "supabase-js", "dist", "umd", "supabase.js")
    if not os.path.exists(sdk): sys.exit("supabase-js UMD পাওয়া গেল না: " + sdk)
    env = dict(os.environ, NODE_PATH=os.path.join(WORK, "node_modules"), TK_WEB_ROOT=os.path.join(ROOT, "03_NETLIFY_READY"),
               TK_SDK_PATH=sdk, TK_CHROME=chromium_path(), TK_UDATA=os.path.join(WORK, "udata"))
    r = subprocess.run(["node", os.path.join(HERE, "tests.js")], env=env, cwd=WORK, text=True)
    if r.returncode:
        print("\n❌ FAIL — ওয়েব-অ্যাপের ব্রাউজার-পরীক্ষা পাশ হয়নি; TK-কে ফাইল/ভার্সন দেওয়া যাবে না।")
        sys.exit(1)
    print("\n✅ PASS — ব্রাউজার-পরীক্ষা সব পাশ (03_NETLIFY_READY)।")

if __name__ == "__main__":
    main()
