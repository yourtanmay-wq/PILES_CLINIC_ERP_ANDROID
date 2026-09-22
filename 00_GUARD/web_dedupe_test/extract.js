/* V495 — app.js থেকেই dedupe কোডটা বের করে আনে (কপি নয়, আসলটাই)। */
const fs=require('fs'), path=require('path');
const appPath=path.join(__dirname,'..','..','03_NETLIFY_READY','app.js');
const s=fs.readFileSync(appPath,'utf8');
const start=s.indexOf('/* ═══');
const end=s.indexOf("const TABLES=['enquiries'");
if(start<0||end<0||end<start) throw new Error('চিহ্ন পাওয়া যায়নি');
const hook=s.slice(start,end);
if(!hook.includes('__wlv1DedupeInstalled')) throw new Error('dedupe কোড পাওয়া যায়নি');
fs.writeFileSync(path.join(__dirname,'hook_extracted.js'),hook);
console.log('app.js থেকে সরাসরি বের করা হলো:',hook.length,'অক্ষর');
