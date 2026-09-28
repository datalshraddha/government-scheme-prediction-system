package com.scheme;

import java.util.*;

/**
 * Rule-based eligibility engine.
 * ELIGIBLE = passes all five rules (age, gender, caste, income, occupation).
 * ALMOST   = fails exactly one rule by a small margin (age within 2 years or income within 25%).
 * Score    = how specifically the scheme targets this person (higher = more relevant).
 */
public final class Matcher {
    public record Result(Scheme s, boolean eligible, int score, List<String> reasons, List<String> gaps) { }

    private Matcher() {}

    static List<Result> match(List<Scheme> all, int age, String gender, String caste, long income, String occ) {
        List<Result> out = new ArrayList<>();
        for (Scheme s : all) {
            List<String> ok = new ArrayList<>(), bad = new ArrayList<>();
            boolean near = true;

            if (age >= s.minAge() && age <= s.maxAge()) ok.add("Age " + age + " fits the " + ageText(s) + " range");
            else { bad.add("Age should be " + ageText(s)); if (Math.max(s.minAge() - age, age - s.maxAge()) > 2) near = false; }

            if (s.gender().equals("ALL")) { /* open to all */ }
            else if (s.gender().equals(gender)) ok.add("Meant for " + gender.toLowerCase() + " applicants");
            else { bad.add("Only for " + s.gender().toLowerCase() + " applicants"); near = false; }

            if (s.castes().equals("ALL")) { /* open to all */ }
            else if (Arrays.asList(s.castes().split(",")).contains(caste)) ok.add("Your category (" + caste + ") qualifies");
            else { bad.add("Only for " + s.castes().replace(",", "/") + " category"); near = false; }

            if (s.maxIncome() == null) { /* no income limit */ }
            else if (income <= s.maxIncome()) ok.add("Income is within " + lakh(s.maxIncome()));
            else { bad.add("Family income should be up to " + lakh(s.maxIncome())); if (income > s.maxIncome() * 1.25) near = false; }

            if (s.occupations().equals("ANY")) { /* any occupation */ }
            else if (Arrays.asList(s.occupations().split(",")).contains(occ)) ok.add("Suits your occupation");
            else { bad.add("Meant for: " + s.occupations().toLowerCase().replace(",", " / ")); near = false; }

            if (bad.isEmpty()) {
                if (ok.size() == 1) ok.add("Open to everyone who meets the age limit");
                int score = 60 + (s.castes().equals("ALL") ? 0 : 15) + (s.gender().equals("ALL") ? 0 : 10)
                        + (s.occupations().equals("ANY") ? 0 : 10)
                        + (s.maxIncome() != null && income <= s.maxIncome() / 2 ? 5 : 0);
                out.add(new Result(s, true, Math.min(score, 100), ok, List.of()));
            } else if (bad.size() == 1 && near) {
                out.add(new Result(s, false, 35, ok, bad));
            }
        }
        out.sort(Comparator.comparing((Result r) -> !r.eligible()).thenComparing(r -> -r.score()).thenComparing(r -> r.s().name()));
        return out;
    }

    private static String ageText(Scheme s) {
        return s.maxAge() >= 100 ? s.minAge() + "+ years" : s.minAge() + " to " + s.maxAge() + " years";
    }

    static String lakh(long v) {
        if (v < 100000) return "Rs. " + v;
        if (v % 100000 == 0) return "Rs. " + (v / 100000) + " lakh";
        return "Rs. " + String.format(Locale.ROOT, "%.2f", v / 100000.0).replaceAll("0+$", "") + " lakh";
    }
}
