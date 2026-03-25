package com.floventy.blockcalls.utils;

import android.util.Log;

import java.util.regex.Pattern;

/**
 * Utility class for matching phone numbers against wildcard patterns.
 *
 * Supported wildcard syntax (only * is a wildcard):
 * "0850*" → blocks numbers STARTING with 0850 (not 0850 itself)
 * "*850*" → blocks any number CONTAINING 850 anywhere
 * "0533 *** 12 12" → spaces are separators; each * matches 1+ digits
 *
 * Spaces and dashes in patterns are treated as separators and ignored.
 * Handles international phone number formats (+90, +44, +1, etc.)
 */
public class PatternMatcher {

    private static final String TAG = "PatternMatcher";

    // Common country codes for variant generation
    private static final String[] COUNTRY_CODES = {
            "90", // Turkey
            "1", // USA/Canada
            "44", // UK
            "49", // Germany
            "33", // France
            "31", // Netherlands
            "39", // Italy
            "34", // Spain
            "61", // Australia
            "81", // Japan
            "82", // South Korea
            "86", // China
            "91", // India
            "7", // Russia
            "380", // Ukraine
            "48", // Poland
            "46", // Sweden
            "47", // Norway
            "45", // Denmark
            "358", // Finland
            "30", // Greece
            "32", // Belgium
            "41", // Switzerland
            "43", // Austria
            "351", // Portugal
            "55", // Brazil
            "52", // Mexico
            "62", // Indonesia
            "66", // Thailand
            "84", // Vietnam
            "63", // Philippines
            "60", // Malaysia
            "65", // Singapore
            "971", // UAE
            "966", // Saudi Arabia
            "20", // Egypt
            "27", // South Africa
            "234", // Nigeria
            "254", // Kenya
    };

    /**
     * Check if a phone number matches a given pattern.
     * Tries multiple normalizations to handle international format differences.
     */
    public static boolean matches(String phoneNumber, String pattern, boolean isWildcard) {
        if (phoneNumber == null || pattern == null) {
            return false;
        }

        // Try direct match first
        if (matchesDirect(phoneNumber, pattern, isWildcard)) {
            Log.d(TAG, "Direct match: " + phoneNumber + " ~ " + pattern);
            return true;
        }

        // Try matching with various international format normalizations
        String[] phoneVariants = generateVariants(phoneNumber);
        String[] patternVariants = generateVariants(pattern);

        for (String phoneVariant : phoneVariants) {
            for (String patternVariant : patternVariants) {
                if (matchesDirect(phoneVariant, patternVariant, isWildcard)) {
                    Log.d(TAG, "Variant match: " + phoneVariant + " ~ " + patternVariant
                            + " (original: " + phoneNumber + " ~ " + pattern + ")");
                    return true;
                }
            }
        }

        Log.d(TAG, "No match: " + phoneNumber + " ~ " + pattern);
        return false;
    }

    /**
     * Direct match without variant generation (avoids recursion).
     *
     * Wildcard rules:
     * Each * in the pattern matches one or more digits (\d+).
     * This means "0850*" does NOT match "0850" itself (requires ≥1 digit after
     * prefix).
     */
    private static boolean matchesDirect(String phoneNumber, String pattern, boolean isWildcard) {
        // Strip all separators; keep only digits and * wildcard
        String normalizedNumber = normalizeNumber(phoneNumber);
        String normalizedPattern = normalizeNumber(pattern);

        if (!isWildcard) {
            return normalizedNumber.equals(normalizedPattern);
        }

        // Build regex: each * → \d+ (one or more digits)
        StringBuilder regexBuilder = new StringBuilder("^");
        for (char c : normalizedPattern.toCharArray()) {
            if (c == '*') {
                regexBuilder.append("\\d+");
            } else {
                regexBuilder.append(Pattern.quote(String.valueOf(c)));
            }
        }
        regexBuilder.append("$");

        try {
            Pattern p = Pattern.compile(regexBuilder.toString());
            return p.matcher(normalizedNumber).matches();
        } catch (Exception e) {
            Log.e(TAG, "Regex error for pattern: " + regexBuilder, e);
            return false;
        }
    }

    /**
     * Generate phone number / pattern variants for cross-format matching.
     *
     * Examples:
     * +905551234567 → [905551234567, 05551234567, 5551234567]
     * 05551234567 → [05551234567, 5551234567, 905551234567, ...]
     * 0850* → [0850*, 850*, 90850*, ...]
     */
    private static String[] generateVariants(String input) {
        String normalized = normalizeNumber(input);
        // Keep + to detect international prefix (but strip wildcards for this check)
        String digitsOnly = input != null ? input.replaceAll("[^0-9+]", "") : "";

        java.util.List<String> variants = new java.util.ArrayList<>();
        variants.add(normalized);

        // Input starts with +XX country code (e.g., +905551234567)
        if (digitsOnly.startsWith("+")) {
            String withoutPlus = digitsOnly.substring(1); // e.g., "905551234567"
            variants.add(withoutPlus);

            // Try stripping each known country code to generate local format
            for (String code : COUNTRY_CODES) {
                if (withoutPlus.startsWith(code) && withoutPlus.length() > code.length()) {
                    String localPart = withoutPlus.substring(code.length()); // e.g., "5551234567"
                    variants.add("0" + localPart); // e.g., "05551234567"
                    variants.add(localPart); // e.g., "5551234567"
                    // Only match the longest country code (don't break, in case of overlaps)
                }
            }
        }
        // Input starts with 0 (local format like 05551234567 or 0850*)
        else if (normalized.startsWith("0") && normalized.length() > 1) {
            String withoutZero = normalized.substring(1); // e.g., "5551234567" or "850*"
            variants.add(withoutZero);

            // Add international variants with all country codes
            for (String code : COUNTRY_CODES) {
                variants.add(code + withoutZero); // e.g., "905551234567" or "90850*"
            }
        }
        // Input starts directly with digits (might be country code without +)
        else if (!normalized.isEmpty() && Character.isDigit(normalized.charAt(0))) {
            // Try to detect if it starts with a country code
            for (String code : COUNTRY_CODES) {
                if (normalized.startsWith(code) && normalized.length() > code.length() + 5) {
                    String localPart = normalized.substring(code.length());
                    variants.add("0" + localPart);
                    variants.add(localPart);
                }
            }
        }

        // Return unique variants preserving insertion order
        java.util.LinkedHashSet<String> unique = new java.util.LinkedHashSet<>(variants);
        return unique.toArray(new String[0]);
    }

    /**
     * Normalize a phone number or pattern.
     * Strips everything except digits and the * wildcard character.
     * Spaces, dashes, parentheses, etc. are treated as visual separators.
     *
     * Examples:
     * "+90 555 123 45 67" → "905551234567"
     * "0533 *** 12 12" → "0533***1212"
     * "0850*" → "0850*"
     */
    private static String normalizeNumber(String input) {
        if (input == null)
            return "";
        return input.replaceAll("[^0-9*]", "");
    }

    /**
     * Returns true if the pattern contains the * wildcard character.
     * Spaces and dashes are NOT wildcards — they are separators.
     */
    public static boolean isWildcardPattern(String pattern) {
        return pattern != null && pattern.contains("*");
    }

    /**
     * Validates that a pattern is properly formatted.
     * Accepts:
     * - Digits (0–9)
     * - Wildcard *
     * - Visual separators: spaces, dashes (stripped before matching)
     * - Optional leading +
     */
    public static boolean isValidPattern(String pattern) {
        if (pattern == null || pattern.trim().isEmpty()) {
            return false;
        }

        // Strip visual separators; keep digits, * and leading +
        String normalized = pattern.replaceAll("[^0-9+*]", "");

        if (normalized.isEmpty()) {
            return false;
        }

        // Must be: optional +, then one or more digits or *
        return normalized.matches("[+]?[0-9*]+");
    }
}
