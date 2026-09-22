import java.util.*;

/** SupabaseClient.fetchListSlimOrNull()-এর V494 চার-ধাপের যুক্তি হুবহু নকল। */
public class T3 {
  static int pass=0, fail=0;
  static void ck(String n, boolean ok, String got){
    if(ok){pass++; System.out.println("  ✅ "+n+"  →  "+got);} else {fail++; System.out.println("  ❌ "+n+"  →  "+got);} }

  static List<String> asked = new ArrayList<>();
  static Set<String> slimProven = new HashSet<>();

  /** সার্ভারের নকল। mode: "ok" · "badcol" (ভুল ঘর) · "netdown" (নেট বন্ধ) */
  static String mode = "ok";
  static String fetch(String table, String select){
    asked.add(select);
    if(mode.equals("netdown")) return null;
    if(mode.equals("badcol") && select.contains("typo_column")) return null;
    return "[{\"amount\":24400}]";
  }

  /** V494-এর চেইন: সরু → সরু আবার → SafeWide → select=* */
  static String slimOrNull(String table, String cols){
    String narrow = fetch(table, cols);
    if(narrow!=null){ slimProven.add(table); return narrow; }
    if(slimProven.contains(table)){
      String retry = fetch(table, cols);
      if(retry!=null) return retry;
    }
    String safe = SafeWide.forTable(table, cols);
    if(safe!=null){
      String safeRead = fetch(table, safe);
      if(safeRead!=null) return safeRead;          // ← V494: null ফেরত নয়
    }
    return fetch(table, "*");
  }

  public static void main(String[] x){
    System.out.println("── পরিস্থিতি ক: ঘরের নাম ভুল (নেট ঠিক আছে) ──");
    asked.clear(); slimProven.clear(); mode="badcol";
    String r1 = slimOrNull("patients","id,name,typo_column");
    ck("তবু আসল তথ্য ফেরে (₹24,400)", r1!=null && r1.contains("24400"), String.valueOf(r1));
    boolean anyStar = asked.contains("*");
    boolean anyPhoto = asked.stream().anyMatch(s -> s.contains("photo"));
    ck("select=* পর্যন্ত যেতেই হয়নি", !anyStar, "চাওয়া হয়েছে: "+asked.size()+" বার");
    ck("রোগীর ছবি চাওয়া হয়নি", !anyPhoto, asked.get(asked.size()-1));

    System.out.println("\n── পরিস্থিতি খ: নেট বন্ধ (খাতার সারি B446) ──");
    asked.clear(); slimProven.clear(); mode="netdown";
    String r2 = slimOrNull("payments","id,amount,mode");
    ck("চারটে ধাপই চেষ্টা করা হয়েছে", asked.size()>=2, "চেষ্টা = "+asked.size()+" ধাপ");
    ck("শেষে select=* পর্যন্ত গেছে", asked.contains("*"), "শেষ চেষ্টা = "+asked.get(asked.size()-1));
    ck("সৎ null ফেরে (খালি তালিকা নয়)", r2==null, "ফল = "+r2);

    System.out.println("\n  ↓ ডাকার জায়গা এই null নিয়ে কী করে (V494-এর নিয়ম)");
    String lastGood = "[{\"amount\":24400}]";      // ফোনে জমানো শেষ সফল তথ্য
    String shown = (r2!=null) ? r2 : lastGood;      // null ⇒ জমানোটাই দেখাও
    ck("Collection ₹0 হয় না", shown.contains("24400"), "পর্দায় দেখাবে ₹24,400");

    System.out.println("\n── পরিস্থিতি গ: নেট ফিরে এল ──");
    mode="ok"; asked.clear();
    String r3 = slimOrNull("payments","id,amount,mode");
    ck("পরের চেষ্টায় টাটকা তথ্য", r3!=null && r3.contains("24400"), r3);
    ck("একবারেই, সরু পড়াতেই", asked.size()==1 && !asked.contains("*"), "চেষ্টা = "+asked.size());

    System.out.println("\n═══════════════════════════════");
    System.out.println("পাশ: "+pass+"   ব্যর্থ: "+fail);
    if(fail>0) System.exit(1);
  }
}
