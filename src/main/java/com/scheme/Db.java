package com.scheme;

import java.io.InputStream;
import java.sql.*;
import java.util.*;

/** All JDBC code: connection, table creation, seed data, queries. */
public final class Db {
    private static final Properties P = new Properties();
    static {
        try (InputStream in = Db.class.getResourceAsStream("/application.properties")) { P.load(in); }
        catch (Exception e) { throw new ExceptionInInitializerError(e); }
    }
    private Db() {}

    private static String cfg(String k) {
        String env = System.getenv(k.toUpperCase().replace('.', '_'));
        return env != null ? env : P.getProperty(k);
    }
    static Connection conn() throws SQLException {
        return DriverManager.getConnection(cfg("db.url"), cfg("db.user"), cfg("db.password"));
    }

    static void init() throws SQLException {
        try (Connection c = conn(); Statement st = c.createStatement()) {
            st.execute("CREATE TABLE IF NOT EXISTS schemes (id INT AUTO_INCREMENT PRIMARY KEY, name VARCHAR(150) NOT NULL, " +
                    "category VARCHAR(40) NOT NULL, level VARCHAR(20) NOT NULL, benefits VARCHAR(300) NOT NULL, " +
                    "min_age INT NOT NULL, max_age INT NOT NULL, gender VARCHAR(10) NOT NULL, castes VARCHAR(30) NOT NULL, " +
                    "max_income INT NULL, occupations VARCHAR(60) NOT NULL, documents VARCHAR(300), url VARCHAR(200))");
            st.execute("CREATE TABLE IF NOT EXISTS search_log (id BIGINT AUTO_INCREMENT PRIMARY KEY, age INT, gender VARCHAR(10), " +
                    "caste VARCHAR(10), income BIGINT, occupation VARCHAR(20), matches INT, created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
        }
        if (count("schemes") == 0) seed();
    }

    static int count(String table) {
        try (Connection c = conn(); Statement st = c.createStatement(); ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM " + table)) {
            rs.next();
            return rs.getInt(1);
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    static List<Scheme> schemes() {
        List<Scheme> list = new ArrayList<>();
        try (Connection c = conn(); Statement st = c.createStatement(); ResultSet rs = st.executeQuery("SELECT * FROM schemes ORDER BY id")) {
            while (rs.next()) {
                list.add(new Scheme(rs.getInt("id"), rs.getString("name"), rs.getString("category"), rs.getString("level"),
                        rs.getString("benefits"), rs.getInt("min_age"), rs.getInt("max_age"), rs.getString("gender"),
                        rs.getString("castes"), (Integer) rs.getObject("max_income"), rs.getString("occupations"),
                        rs.getString("documents"), rs.getString("url")));
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
        return list;
    }

    static void log(int age, String gender, String caste, long income, String occ, int matches) {
        String sql = "INSERT INTO search_log(age,gender,caste,income,occupation,matches) VALUES(?,?,?,?,?,?)";
        try (Connection c = conn(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, age); ps.setString(2, gender); ps.setString(3, caste);
            ps.setLong(4, income); ps.setString(5, occ); ps.setInt(6, matches);
            ps.executeUpdate();
        } catch (SQLException e) { System.err.println("Could not log search: " + e.getMessage()); }
    }

    private static Object[] r(Object... v) { return v; }

    /** Indicative criteria taken from public scheme guidelines. Verify on the official site before applying. */
    private static void seed() throws SQLException {
        Object[][] rows = {
            r("Post Matric Scholarship for SC Students", "Education", "Central", "Covers tuition fees and a monthly maintenance allowance for post-matric courses", 16, 35, "ALL", "SC", 250000, "STUDENT", "Caste certificate, income certificate, admission proof, bank passbook, Aadhaar", "https://scholarships.gov.in"),
            r("Post Matric Scholarship for ST Students", "Education", "Central", "Covers tuition fees and a monthly maintenance allowance for post-matric courses", 16, 35, "ALL", "ST", 250000, "STUDENT", "Caste certificate, income certificate, admission proof, bank passbook, Aadhaar", "https://scholarships.gov.in"),
            r("PM YASASVI Scholarship for OBC Students", "Education", "Central", "Scholarship for OBC, EBC and DNT students from Class 9 up to higher education", 13, 25, "ALL", "OBC", 250000, "STUDENT", "Caste certificate, income certificate, previous marksheet, bank passbook, Aadhaar", "https://scholarships.gov.in"),
            r("Central Sector Scholarship Scheme", "Education", "Central", "Up to Rs. 12,000 a year for graduation and Rs. 20,000 for post-graduation for meritorious students", 17, 25, "ALL", "ALL", 450000, "STUDENT", "Class 12 marksheet, income certificate, admission proof, bank passbook, Aadhaar", "https://scholarships.gov.in"),
            r("PM-KISAN Samman Nidhi", "Agriculture", "Central", "Rs. 6,000 a year in three instalments, sent directly to the bank account", 18, 100, "ALL", "ALL", null, "FARMER", "Land records, Aadhaar, bank passbook, mobile number", "https://pmkisan.gov.in"),
            r("Kisan Credit Card", "Agriculture", "Central", "Low-interest short-term credit for crops and farm expenses", 18, 75, "ALL", "ALL", null, "FARMER", "Land records, Aadhaar, PAN, passport photo, bank account", "https://pmkisan.gov.in"),
            r("PM Matru Vandana Yojana", "Women and Child", "Central", "Cash incentive of Rs. 5,000 for the first living child, paid in instalments", 19, 45, "FEMALE", "ALL", null, "ANY", "Aadhaar, bank account, mother and child protection card, ID proof", "https://www.myscheme.gov.in"),
            r("Sukanya Samriddhi Yojana", "Savings", "Central", "High-interest savings account for a girl child's education and marriage", 0, 10, "FEMALE", "ALL", null, "ANY", "Girl's birth certificate, parent ID and address proof", "https://www.indiapost.gov.in"),
            r("PM Ujjwala Yojana", "Household", "Central", "Deposit-free LPG connection for women from low-income households", 18, 100, "FEMALE", "ALL", 100000, "ANY", "Aadhaar, ration card or BPL proof, bank passbook, passport photo", "https://www.pmuy.gov.in"),
            r("PM Awas Yojana (Urban) 2.0", "Housing", "Central", "Financial help to buy, build or rent an affordable home", 18, 70, "ALL", "ALL", 900000, "ANY", "Aadhaar, income proof, bank account, property or rent papers", "https://pmay-urban.gov.in"),
            r("Ayushman Bharat PM-JAY", "Health", "Central", "Free treatment up to Rs. 5 lakh per family per year in listed hospitals", 0, 100, "ALL", "ALL", 250000, "ANY", "Aadhaar, ration card, family ID (eligibility is checked against official lists)", "https://pmjay.gov.in"),
            r("Ayushman Vay Vandana", "Health", "Central", "Health cover up to Rs. 5 lakh a year for every senior citizen aged 70 and above", 70, 120, "ALL", "ALL", null, "ANY", "Aadhaar, age proof", "https://pmjay.gov.in"),
            r("Atal Pension Yojana", "Pension", "Central", "Guaranteed monthly pension of Rs. 1,000 to Rs. 5,000 after age 60", 18, 40, "ALL", "ALL", null, "ANY", "Aadhaar, savings bank account, mobile number", "https://www.jansuraksha.gov.in"),
            r("PM Shram Yogi Maan-dhan", "Pension", "Central", "Assured pension of Rs. 3,000 a month after age 60 for unorganised workers", 18, 40, "ALL", "ALL", 180000, "WORKER", "Aadhaar, savings bank account, mobile number", "https://maandhan.in"),
            r("PM Mudra Yojana", "Business", "Central", "Collateral-free loans up to Rs. 10 lakh for small businesses", 18, 65, "ALL", "ALL", null, "ENTREPRENEUR", "Aadhaar, PAN, business plan, address proof, bank statements", "https://www.mudra.org.in"),
            r("PM Kaushal Vikas Yojana", "Skills", "Central", "Free short-term skill training with a certificate and placement support", 15, 45, "ALL", "ALL", null, "JOBSEEKER,STUDENT", "Aadhaar, bank account, education certificate", "https://www.skillindiadigital.gov.in"),
            r("Indira Gandhi National Old Age Pension", "Pension", "Central", "Monthly pension for senior citizens from low-income households", 60, 120, "ALL", "ALL", 100000, "ANY", "Age proof, BPL card or income certificate, Aadhaar, bank passbook", "https://nsap.nic.in"),
            r("Mukhyamantri Majhi Ladki Bahin Yojana", "Women and Child", "Maharashtra", "Rs. 1,500 a month for eligible women of Maharashtra", 21, 65, "FEMALE", "ALL", 250000, "ANY", "Aadhaar, domicile or ration card proof, income proof, bank passbook", "https://ladakibahin.maharashtra.gov.in"),
            r("PM Suraksha Bima Yojana", "Insurance", "Central", "Accident insurance cover of Rs. 2 lakh for Rs. 20 a year", 18, 70, "ALL", "ALL", null, "ANY", "Aadhaar, savings bank account", "https://www.jansuraksha.gov.in")
        };
        String sql = "INSERT INTO schemes(name,category,level,benefits,min_age,max_age,gender,castes,max_income,occupations,documents,url) VALUES(?,?,?,?,?,?,?,?,?,?,?,?)";
        try (Connection c = conn(); PreparedStatement ps = c.prepareStatement(sql)) {
            for (Object[] row : rows) {
                for (int i = 0; i < row.length; i++) ps.setObject(i + 1, row[i]);
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }
}
