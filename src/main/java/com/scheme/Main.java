package com.scheme;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

/** Tiny web server (JDK built-in, no Tomcat needed): serves the HTML/CSS/JS UI and two JSON endpoints. */
public class Main {
    private static final Set<String> GENDERS = Set.of("MALE", "FEMALE", "OTHER");
    private static final Set<String> CASTES = Set.of("GEN", "OBC", "SC", "ST");
    private static final Set<String> JOBS = Set.of("STUDENT", "FARMER", "WORKER", "ENTREPRENEUR", "JOBSEEKER", "OTHER");

    public static void main(String[] args) throws Exception {
        Db.init();
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        server.createContext("/api/match", Main::match);
        server.createContext("/api/stats", ex -> send(ex, 200, "application/json; charset=utf-8",
                ("{\"schemes\":" + Db.count("schemes") + ",\"searches\":" + Db.count("search_log") + "}").getBytes(StandardCharsets.UTF_8)));
        server.createContext("/", Main::files);
        server.start();
        System.out.println("Scheme Predictor running. Open http://localhost:8080 in your browser.");
    }

    private static void match(HttpExchange ex) throws IOException {
        String body;
        int code = 200;
        try {
            Map<String, String> q = query(ex.getRequestURI().getRawQuery());
            int age = Integer.parseInt(q.get("age"));
            long income = Long.parseLong(q.get("income"));
            String gender = q.get("gender"), caste = q.get("caste"), occ = q.get("occupation");
            if (age < 0 || age > 120 || income < 0 || !GENDERS.contains(gender) || !CASTES.contains(caste) || !JOBS.contains(occ)) {
                throw new IllegalArgumentException();
            }
            List<Matcher.Result> results = Matcher.match(Db.schemes(), age, gender, caste, income, occ);
            Db.log(age, gender, caste, income, occ, results.size());
            body = "[" + results.stream().map(Main::toJson).collect(Collectors.joining(",")) + "]";
        } catch (Exception e) {
            code = 400;
            body = "{\"error\":\"Please fill in every field with valid values.\"}";
        }
        send(ex, code, "application/json; charset=utf-8", body.getBytes(StandardCharsets.UTF_8));
    }

    private static String toJson(Matcher.Result r) {
        Scheme s = r.s();
        return "{\"id\":" + s.id() + ",\"name\":" + q(s.name()) + ",\"category\":" + q(s.category()) + ",\"level\":" + q(s.level())
                + ",\"benefits\":" + q(s.benefits()) + ",\"documents\":" + q(s.documents()) + ",\"url\":" + q(s.url())
                + ",\"eligible\":" + r.eligible() + ",\"score\":" + r.score()
                + ",\"reasons\":" + arr(r.reasons()) + ",\"gaps\":" + arr(r.gaps()) + "}";
    }

    private static String arr(List<String> l) {
        return "[" + l.stream().map(Main::q).collect(Collectors.joining(",")) + "]";
    }

    private static String q(String v) {
        if (v == null) return "null";
        StringBuilder b = new StringBuilder("\"");
        for (char ch : v.toCharArray()) {
            switch (ch) {
                case '"' -> b.append("\\\"");
                case '\\' -> b.append("\\\\");
                default -> b.append(ch < 32 ? ' ' : ch);
            }
        }
        return b.append('"').toString();
    }

    private static Map<String, String> query(String raw) {
        Map<String, String> m = new HashMap<>();
        if (raw == null) return m;
        for (String pair : raw.split("&")) {
            String[] kv = pair.split("=", 2);
            m.put(URLDecoder.decode(kv[0], StandardCharsets.UTF_8), kv.length > 1 ? URLDecoder.decode(kv[1], StandardCharsets.UTF_8) : "");
        }
        return m;
    }

    private static void files(HttpExchange ex) throws IOException {
        String p = ex.getRequestURI().getPath();
        if (p.equals("/")) p = "/index.html";
        if (p.contains("..")) { send(ex, 400, "text/plain", "Bad request".getBytes()); return; }
        try (InputStream in = Main.class.getResourceAsStream("/web" + p)) {
            if (in == null) { send(ex, 404, "text/plain", "Not found".getBytes()); return; }
            String type = p.endsWith(".css") ? "text/css; charset=utf-8" : p.endsWith(".js") ? "application/javascript; charset=utf-8" : "text/html; charset=utf-8";
            send(ex, 200, type, in.readAllBytes());
        }
    }

    private static void send(HttpExchange ex, int code, String type, byte[] body) throws IOException {
        ex.getResponseHeaders().set("Content-Type", type);
        ex.sendResponseHeaders(code, body.length);
        try (OutputStream os = ex.getResponseBody()) { os.write(body); }
    }
}
