import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

public class T {
  static int pass=0, fail=0;
  static void ck(String name, boolean ok, String got){
    if(ok){pass++; System.out.println("  ✅ "+name+"  →  "+got);}
    else {fail++; System.out.println("  ❌ "+name+"  →  "+got);}
  }
  public static void main(String[] a) throws Exception {
    System.out.println("── ১. একসঙ্গে ৮টা একই অনুরোধ (Follow-up-এর ৩ ট্যাব × ২ পথ) ──");
    Dedupe d = new Dedupe();
    AtomicInteger hits = new AtomicInteger();
    CountDownLatch go = new CountDownLatch(1);
    ExecutorService ex = Executors.newFixedThreadPool(8);
    List<Future<String>> fs = new ArrayList<>();
    for(int i=0;i<8;i++) fs.add(ex.submit(() -> { go.await();
        return d.body("URL/patients?select=id&limit=5000", () -> {
            hits.incrementAndGet(); try{Thread.sleep(120);}catch(Exception e){} return "[{\"id\":\"p1\"}]"; }); }));
    go.countDown();
    Set<String> outs = new HashSet<>(); for(Future<String> f: fs) outs.add(f.get());
    ex.shutdown();
    ck("নেটে গেল মাত্র একবার", hits.get()==1, "network hit = "+hits.get()+" (আগে হত ৮)");
    ck("আটজনই একই উত্তর পেল", outs.size()==1, "আলাদা উত্তর = "+outs.size());

    System.out.println("\n── ২. ৬০ সেকেন্ডের ভিতরে বারবার (onCreate→onResume→LiveRefresh) ──");
    Dedupe d2 = new Dedupe(); AtomicInteger h2 = new AtomicInteger();
    for(int i=0;i<5;i++) d2.body("U", () -> { h2.incrementAndGet(); return "[]"; });
    ck("৫ বার চাওয়া, নেটে একবার", h2.get()==1, "network hit = "+h2.get());
    d2.now += 61_000;                       // ৬১ সেকেন্ড পরে
    d2.body("U", () -> { h2.incrementAndGet(); return "[]"; });
    ck("৬০ সেকেন্ড পেরোলে আবার নেটে যায়", h2.get()==2, "network hit = "+h2.get());

    System.out.println("\n── ৩. নিজে কিছু সেভ করার পরে (upsert → clear) ──");
    Dedupe d3 = new Dedupe(); AtomicInteger h3 = new AtomicInteger();
    d3.body("U", () -> { h3.incrementAndGet(); return "[1]"; });
    d3.clear();                              // লেখার পরে
    d3.body("U", () -> { h3.incrementAndGet(); return "[2]"; });
    ck("সেভের পরে টাটকা তথ্যই আসে", h3.get()==2, "network hit = "+h3.get());

    System.out.println("\n── ৪. নেট আটকালে (খাতার সারি B446) ──");
    Dedupe d4 = new Dedupe(); AtomicInteger h4 = new AtomicInteger();
    String r1 = d4.body("U", () -> { h4.incrementAndGet(); return null; });   // ব্যর্থ
    String r2 = d4.body("U", () -> { h4.incrementAndGet(); return "[{\"amount\":400}]"; });
    ck("ব্যর্থ পড়া জমা হয় না", h4.get()==2, "network hit = "+h4.get());
    ck("পরের চেষ্টায় আসল টাকা ফেরে", r1==null && r2.contains("400"), "১ম="+r1+" ২য়="+r2);
    ck("ব্যর্থতা cache-এ ঢোকেনি", d4.debugSize()==1, "জমা = "+d4.debugSize());

    System.out.println("\n── ৫. আলাদা অনুরোধ আলাদাই থাকে ──");
    Dedupe d5 = new Dedupe(); AtomicInteger h5 = new AtomicInteger();
    d5.body("A", () -> { h5.incrementAndGet(); return "[a]"; });
    d5.body("B", () -> { h5.incrementAndGet(); return "[b]"; });
    ck("দুটো আলাদা URL = দুটো পড়া", h5.get()==2, "network hit = "+h5.get());

    System.out.println("\n── ৬. মেমরির সীমা ──");
    Dedupe d6 = new Dedupe(); StringBuilder big=new StringBuilder();
    for(int i=0;i<3_000_000;i++) big.append('x');
    for(int i=0;i<4;i++){ final int k=i; d6.body("K"+k, () -> big.toString()); }
    ck("৮ MB-র বেশি জমে না", d6.debugSize()<=3, "জমা = "+d6.debugSize()+" টি (৩ MB × ৪ চাওয়া হয়েছিল)");

    System.out.println("\n── ৭. ছবি আর নামে না (SafeWideColumns) ──");
    String p = SafeWide.forTable("patients","id,name,mobile");
    ck("তালিকার পড়ায় photo নেই", !p.contains("photo"), p);
    ck("medicalHistory-ও নেই", !p.contains("medicalHistory"), "ঘর সংখ্যা = "+p.split(",").length);
    ck("চাওয়া ঘর সবই আছে", p.contains("id")&&p.contains("name")&&p.contains("mobile"), "id·name·mobile ✓");
    String q = SafeWide.forTable("patients","id,photo");
    ck("কেউ ইচ্ছে করে photo চাইলে সেটা থাকে", q.contains("photo"), q);
    String f2 = SafeWide.forTable("followups","id,stage");
    ck("followups-এ photo ও history নেই", !f2.contains("photo")&&!f2.contains("history"), f2);
    ck("অচেনা টেবিলে আগের আচরণ", SafeWide.forTable("address_tags","id")==null, "null ⇒ select=* আগের মতোই");

    System.out.println("\n═══════════════════════════════");
    System.out.println("পাশ: "+pass+"   ব্যর্থ: "+fail);
    if(fail>0) System.exit(1);
  }
}
