# Government Scheme Prediction System

Web app that recommends government schemes from a person's age, gender, category (caste), annual family income and occupation.
Stack: Java 17, JDBC, MySQL, JDK built-in HTTP server, HTML, CSS, JavaScript. No Tomcat, no Spring.

## Run
1. Start MySQL. Put your password in `src/main/resources/application.properties` (`db.password=`).
2. Run `com.scheme.Main` (Eclipse: right-click, Run As, Java Application) or `mvn compile exec:java`.
3. Open http://localhost:8080. The database `scheme_db`, its tables and 19 schemes are created automatically.

## How it works
Browser form -> `GET /api/match` -> `Matcher` checks 5 rules per scheme -> JSON -> cards on the page. Every search is saved in `search_log`.
- **Eligible**: passes age, gender, category, income and occupation.
- **Almost eligible**: fails exactly one rule narrowly (age within 2 years or income within 25%); the gap is shown.
- **Score (35 to 100)**: 60 base, +15 category-specific, +10 gender-specific, +10 occupation-specific, +5 if income is under half the limit.

## Structure
`Main` (server + API) · `Db` (JDBC, schema, seed) · `Matcher` (rule engine) · `Scheme` (model) · `resources/web` (UI)

## Limitations and future work
Rules are indicative and hand-entered; no admin panel, login, Marathi language, or state-wise filtering yet. Natural extensions: admin CRUD for schemes, Marathi/Hindi UI, ML ranking from `search_log`.
