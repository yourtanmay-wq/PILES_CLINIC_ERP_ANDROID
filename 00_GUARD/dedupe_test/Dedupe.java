import java.util.*;
import java.util.function.Supplier;

/** V494 CloudReadDedupe.kt-এর যুক্তি হুবহু নকল (Gate refcount + sessionTag)। */
class Dedupe {
    static long TTL_MS = 60_000L;
    static long MAX_BYTES = 8L*1024*1024;
    static class Entry { long at; String body; Entry(long a,String b){at=a;body=b;} }
    static class Gate { int users = 0; }
    private final Object lock = new Object();
    private final LinkedHashMap<String,Entry> entries = new LinkedHashMap<>(16,0.75f,true);
    private final HashMap<String,Gate> inFlight = new HashMap<>();
    private long bytes = 0;
    private volatile String sessionTag = "";
    long now = System.currentTimeMillis();

    private String peek(String k){ synchronized(lock){
        Entry e = entries.get(k); if(e==null) return null;
        if(now - e.at > TTL_MS){ entries.remove(k); bytes -= e.body.length(); return null; }
        return e.body; } }
    private void put(String k,String b){ synchronized(lock){
        Entry old = entries.remove(k); if(old!=null) bytes -= old.body.length();
        entries.put(k,new Entry(now,b)); bytes += b.length();
        Iterator<Map.Entry<String,Entry>> it = entries.entrySet().iterator();
        while(bytes > MAX_BYTES && it.hasNext()){ Map.Entry<String,Entry> e=it.next(); bytes-=e.getValue().body.length(); it.remove(); } } }
    private Gate acquireGate(String k){ synchronized(lock){
        Gate g = inFlight.get(k); if(g==null){ g=new Gate(); inFlight.put(k,g); }
        g.users++; return g; } }
    private void releaseGate(String k, Gate g){ synchronized(lock){
        g.users--; if(g.users<=0) inFlight.remove(k); } }
    private String keyFor(String raw){ return sessionTag + "|" + raw; }

    String body(String rawKey, Supplier<String> load){
        String key = keyFor(rawKey);
        try {
            String hit = peek(key); if(hit!=null) return hit;
            Gate g = acquireGate(key);
            try {
                synchronized(g){
                    hit = peek(key); if(hit!=null) return hit;
                    String fresh = load.get();
                    if(fresh!=null) put(key,fresh);
                    return fresh;
                }
            } finally { releaseGate(key,g); }      // ← V494: সব অবস্থাতেই
        } catch(Throwable t){ try { return load.get(); } catch(Throwable t2){ return null; } }
    }
    void setSession(String identity){
        String tag = (identity==null?"":identity).replaceAll("\\D","");
        if(tag.length()>10) tag = tag.substring(tag.length()-10);
        if(tag.equals(sessionTag)) return;
        sessionTag = tag; clear();
    }
    void clear(){ synchronized(lock){ entries.clear(); bytes=0; } }
    int debugSize(){ synchronized(lock){ return entries.size(); } }
    int debugInFlightSize(){ synchronized(lock){ return inFlight.size(); } }
}
