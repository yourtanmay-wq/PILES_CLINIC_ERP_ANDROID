import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

public class T2 {
  static int pass=0, fail=0;
  static void ck(String n, boolean ok, String got){
    if(ok){pass++; System.out.println("  ✅ "+n+"  →  "+got);} else {fail++; System.out.println("  ❌ "+n+"  →  "+got);} }

  public static void main(String[] a) throws Exception {

    System.out.println("── ১. Memory Leak (TK-যাচাই ১) ──");
    Dedupe d = new Dedupe();
    for(int i=0;i<20000;i++){ final int k=i; d.body("URL/p?id="+k, () -> "[]"); }
    ck("২০,০০০ আলাদা URL-এর পরে inFlight খালি", d.debugInFlightSize()==0, "inFlight = "+d.debugInFlightSize());
    // ব্যর্থতা ও exception-এর পথেও
    for(int i=0;i<5000;i++){ final int k=i; d.body("F/"+k, () -> null); }
    ck("৫,০০০ ব্যর্থ পড়ার পরেও খালি", d.debugInFlightSize()==0, "inFlight = "+d.debugInFlightSize());
    for(int i=0;i<2000;i++){ final int k=i;
      try { d.body("E/"+k, () -> { throw new RuntimeException("boom"); }); } catch(Throwable t){} }
    ck("২,০০০ exception-এর পরেও খালি", d.debugInFlightSize()==0, "inFlight = "+d.debugInFlightSize());
    // থ্রেড দিয়ে
    ExecutorService ex = Executors.newFixedThreadPool(16);
    List<Future<?>> fs = new ArrayList<>();
    for(int i=0;i<4000;i++){ final int k=i;
      fs.add(ex.submit(() -> d.body("T/"+(k%400), () -> "[x]"))); }
    for(Future<?> f: fs) f.get(); ex.shutdown();
    ck("১৬ থ্রেডে ৪,০০০ কাজের পরেও খালি", d.debugInFlightSize()==0, "inFlight = "+d.debugInFlightSize());

    System.out.println("\n── ২. একসঙ্গে ৮টা একই অনুরোধ (ক্ষমতা অটুট) ──");
    Dedupe d1 = new Dedupe(); AtomicInteger h1 = new AtomicInteger();
    CountDownLatch go = new CountDownLatch(1);
    ExecutorService e1 = Executors.newFixedThreadPool(8);
    List<Future<String>> r1 = new ArrayList<>();
    for(int i=0;i<8;i++) r1.add(e1.submit(() -> { go.await();
      return d1.body("SAME", () -> { h1.incrementAndGet(); try{Thread.sleep(120);}catch(Exception e){} return "[ok]"; }); }));
    go.countDown(); Set<String> outs=new HashSet<>(); for(Future<String> f: r1) outs.add(f.get()); e1.shutdown();
    ck("নেটে গেল ১ বার", h1.get()==1, "network hit = "+h1.get()+" (আগে ৮)");
    ck("সবাই একই উত্তর", outs.size()==1, "আলাদা উত্তর = "+outs.size());
    ck("কাজ শেষে inFlight খালি", d1.debugInFlightSize()==0, "inFlight = "+d1.debugInFlightSize());

    System.out.println("\n── ৩. Login / Logout / User switch (TK-যাচাই ২) ──");
    Dedupe d2 = new Dedupe(); AtomicInteger h2 = new AtomicInteger();
    d2.setSession("+919800000001");                       // স্টাফ ক লগইন
    String uA = d2.body("URL/payments", () -> { h2.incrementAndGet(); return "[{\"branch\":\"Kishanganj\"}]"; });
    d2.setSession(null);                                  // লগআউট
    ck("লগআউটে সব জমানো মুছে যায়", d2.debugSize()==0, "জমা = "+d2.debugSize());
    d2.setSession("+919800000002");                       // স্টাফ খ লগইন
    String uB = d2.body("URL/payments", () -> { h2.incrementAndGet(); return "[{\"branch\":\"Falakata\"}]"; });
    ck("দ্বিতীয় জন নতুন করে নেট থেকে পায়", h2.get()==2, "network hit = "+h2.get());
    ck("আগের জনের তথ্য পায়নি", uB.contains("Falakata") && !uB.contains("Kishanganj"), uB);
    // লগআউট ছাড়াই সরাসরি বদল (জোর করে সাইন-আউট)
    Dedupe d3 = new Dedupe(); AtomicInteger h3 = new AtomicInteger();
    d3.setSession("9800000001");
    d3.body("U", () -> { h3.incrementAndGet(); return "[ক]"; });
    d3.setSession("9800000002");                          // clear() বাদ পড়লেও
    String x = d3.body("U", () -> { h3.incrementAndGet(); return "[খ]"; });
    ck("পরিচয় বদলালে চাবিও আলাদা", h3.get()==2 && x.equals("[খ]"), "network hit = "+h3.get()+", পেল "+x);
    d3.setSession("9800000002");
    ck("একই পরিচয় আবার দিলে অকারণে মুছে না", d3.debugSize()==1, "জমা = "+d3.debugSize());

    System.out.println("\n── ৪. Cache expiry ও অন্য Staff-এর বদল (TK-যাচাই) ──");
    Dedupe d4 = new Dedupe(); AtomicInteger h4 = new AtomicInteger();
    final String[] server = { "[{\"amount\":100}]" };
    d4.body("U", () -> { h4.incrementAndGet(); return server[0]; });
    server[0] = "[{\"amount\":100},{\"amount\":500}]";     // অন্য স্টাফ নতুন পেমেন্ট দিল
    String stale = d4.body("U", () -> { h4.incrementAndGet(); return server[0]; });
    ck("৬০ সেকেন্ডের ভিতরে জমানোটাই (নেটে যায় না)", h4.get()==1, "network hit = "+h4.get());
    d4.now += 60_001;                                     // সর্বোচ্চ নির্ধারিত সময় পার
    String fresh = d4.body("U", () -> { h4.incrementAndGet(); return server[0]; });
    ck("৬০ সেকেন্ডের মধ্যেই অন্য স্টাফের বদল দেখা যায়", fresh.contains("500"), "পেল = "+fresh);
    ck("তখন নেটে যায়", h4.get()==2, "network hit = "+h4.get());

    System.out.println("\n═══════════════════════════════");
    System.out.println("পাশ: "+pass+"   ব্যর্থ: "+fail);
    if(fail>0) System.exit(1);
  }
}
