#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
⛔🔒 TK-এর ১ নম্বর নিয়মের **স্থায়ী পাহারা** (৩০.০৮.২০২৬)

TK বহুবার বলেছেন — *"দু-তিন লাইনের বেশি বলা যাবে না"*, আর আমি বহুবার
প্রতিশ্রুতি দিয়ে আবার ভুলে গেছি। TK: *"আমি এটার পার্মানেন্ট সলিউশন চাই"*।

⇒ এটা Stop-হুক। উত্তর শেষ হওয়ার আগে **মেশিন নিজে গুনে দেখে**। বেশি হলে
   উত্তরটা আটকে দেয় এবং ছোট করে আবার লিখতে বাধ্য করে।

যা গোনা হয় না (এগুলো TK নিজেই চান):
  · ``` কোড/SQL ব্লক        · | টেবিল |         · খালি লাইন
  · TK নিজে "বিস্তারিত / প্লান / ব্লু প্রিন্ট / লিস্ট" চাইলে পুরো যাচাই বন্ধ
"""
import json, os, re, sys

LIMIT = 3
LONG_OK = ("বিস্তারিত", "প্লান", "প্ল্যান", "ব্লু প্রিন্ট", "ব্লুপ্রিন্ট",
           "লিস্ট", "তালিকা", "সব বলুন", "detail")


def last_turn(path):
    """শেষ assistant-বার্তা, আর তার আগের user-বার্তা।"""
    assistant, user = "", ""
    try:
        with open(path, encoding="utf-8") as fh:
            rows = [json.loads(l) for l in fh if l.strip()]
    except Exception:
        return "", ""
    for row in reversed(rows):
        msg = row.get("message") or {}
        role = msg.get("role") or row.get("type")
        content = msg.get("content")
        text = ""
        if isinstance(content, str):
            text = content
        elif isinstance(content, list):
            text = "\n".join(c.get("text", "") for c in content
                             if isinstance(c, dict) and c.get("type") == "text")
        if not text.strip():
            continue
        if role == "assistant" and not assistant:
            assistant = text
        elif role == "user" and assistant:
            user = text
            break
    return assistant, user


def visible_lines(text):
    text = re.sub(r"```.*?```", "", text, flags=re.S)      # কোড/SQL ব্লক বাদ
    out = []
    for line in text.split("\n"):
        s = line.strip()
        if not s or s.startswith("|") or set(s) <= set("-—= "):
            continue                                        # টেবিল ও খালি লাইন বাদ
        out.append(s)
    return out


def main():
    try:
        data = json.load(sys.stdin)
    except Exception:
        return
    if data.get("stop_hook_active"):
        return                                              # একবারের বেশি আটকাব না
    path = data.get("transcript_path") or ""
    if not path or not os.path.exists(path):
        return
    reply, ask = last_turn(path)
    if not reply:
        return
    if any(w in ask for w in LONG_OK):
        return                                              # TK নিজেই লম্বা চেয়েছেন
    lines = visible_lines(reply)
    if len(lines) <= LIMIT:
        return
    print(json.dumps({
        "decision": "block",
        "reason": ("⛔ TK-এর ১ নম্বর নিয়ম ভাঙছে — উত্তরে %d লাইন, সর্বোচ্চ %d।\n"
                   "উত্তরটা আবার লিখুন, শুধু এই তিনটে: কী হলো · ঝুঁকি আছে কিনা · "
                   "এরপর কী। ফাইলের নাম · লাইন নম্বর · কোড · প্রক্রিয়ার বর্ণনা "
                   "কিচ্ছু নয়।") % (len(lines), LIMIT)
    }, ensure_ascii=False))


if __name__ == "__main__":
    main()
