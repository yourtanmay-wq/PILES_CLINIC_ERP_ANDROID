# V225 — Test report

- Project inventory: Android source, web app/public site, SQL, assets, `.git`, rollback and notes present.
- Android embedded web and Netlify web: byte-identical (`app.js`, `styles.css`).
- Guard: passed before release; release run recorded separately.
- Logic tests: 41 passed, 0 failed.
- XML/Kotlin static checks: handled by project guard.
- Android build: attempted, but this Linux workspace has no cached Gradle 8.5 and its network cannot reach the Gradle download server. Therefore no new APK/build success is claimed here. The received V224 report states its prior V223 source built successfully; V225's changed Kotlin expression is covered by guard/static checks.
- Live Supabase and physical-device tests: not claimed; owner will perform these.

## Important database note

SQL files are included but are not auto-executed. Take a live backup and run only the documented safe order. Existing duplicate Official Patient IDs must be resolved before creating the unique index.
