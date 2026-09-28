package com.scheme;

public record Scheme(int id, String name, String category, String level, String benefits,
                     int minAge, int maxAge, String gender, String castes, Integer maxIncome,
                     String occupations, String documents, String url) { }
