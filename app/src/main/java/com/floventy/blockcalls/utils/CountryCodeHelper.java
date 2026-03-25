package com.floventy.blockcalls.utils;

import android.content.Context;
import android.telephony.TelephonyManager;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Helper class for country codes and flags.
 */
public class CountryCodeHelper {

    private static final String TAG = "CountryCodeHelper";

    public static class CountryCode {
        public final String code;
        public final String name;
        public final String flag;
        public final String dialCode;

        public CountryCode(String code, String name, String flag, String dialCode) {
            this.code = code;
            this.name = name;
            this.flag = flag;
            this.dialCode = dialCode;
        }

        @Override
        public String toString() {
            return flag + "  " + name + " (+" + dialCode + ")";
        }
    }

    /**
     * Get the user's current country code from SIM or network.
     */
    public static String getUserCountryCode(Context context) {
        try {
            TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            if (tm != null) {
                // Try SIM country first
                String countryCode = tm.getSimCountryIso();
                if (countryCode != null && !countryCode.isEmpty()) {
                    return countryCode.toUpperCase();
                }
                // Fallback to network country
                countryCode = tm.getNetworkCountryIso();
                if (countryCode != null && !countryCode.isEmpty()) {
                    return countryCode.toUpperCase();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting country code", e);
        }
        // Default to Turkey if detection fails
        return "TR";
    }

    /**
     * Get list of popular countries for blocking international calls.
     */
    public static List<CountryCode> getPopularCountries() {
        List<CountryCode> countries = new ArrayList<>();

        countries.add(new CountryCode("TR", "Türkiye", "🇹🇷", "90"));
        countries.add(new CountryCode("US", "United States", "🇺🇸", "1"));
        countries.add(new CountryCode("GB", "United Kingdom", "🇬🇧", "44"));
        countries.add(new CountryCode("RU", "Russia", "🇷🇺", "7"));
        countries.add(new CountryCode("DE", "Germany", "🇩🇪", "49"));
        countries.add(new CountryCode("FR", "France", "🇫🇷", "33"));
        countries.add(new CountryCode("IT", "Italy", "🇮🇹", "39"));
        countries.add(new CountryCode("ES", "Spain", "🇪🇸", "34"));
        countries.add(new CountryCode("NL", "Netherlands", "🇳🇱", "31"));
        countries.add(new CountryCode("PL", "Poland", "🇵🇱", "48"));
        countries.add(new CountryCode("UA", "Ukraine", "🇺🇦", "380"));
        countries.add(new CountryCode("IN", "India", "🇮🇳", "91"));
        countries.add(new CountryCode("CN", "China", "🇨🇳", "86"));
        countries.add(new CountryCode("JP", "Japan", "🇯🇵", "81"));
        countries.add(new CountryCode("KR", "South Korea", "🇰🇷", "82"));
        countries.add(new CountryCode("AU", "Australia", "🇦🇺", "61"));
        countries.add(new CountryCode("BR", "Brazil", "🇧🇷", "55"));
        countries.add(new CountryCode("MX", "Mexico", "🇲🇽", "52"));
        countries.add(new CountryCode("SA", "Saudi Arabia", "🇸🇦", "966"));
        countries.add(new CountryCode("AE", "UAE", "🇦🇪", "971"));
        countries.add(new CountryCode("EG", "Egypt", "🇪🇬", "20"));
        countries.add(new CountryCode("ZA", "South Africa", "🇿🇦", "27"));

        return countries;
    }

    /**
     * Find country by ISO code.
     */
    public static CountryCode findCountryByCode(String isoCode) {
        for (CountryCode country : getPopularCountries()) {
            if (country.code.equalsIgnoreCase(isoCode)) {
                return country;
            }
        }
        return null;
    }

    /**
     * Extract country dial code from pattern (e.g., "+90*" → "90").
     */
    public static String extractDialCodeFromPattern(String pattern) {
        if (pattern == null || !pattern.startsWith("+")) {
            return null;
        }

        String normalized = pattern.substring(1).replaceAll("[^0-9]", "");

        // Check against known dial codes
        for (CountryCode country : getPopularCountries()) {
            if (normalized.startsWith(country.dialCode)) {
                return country.dialCode;
            }
        }

        return null;
    }

    /**
     * Get flag emoji for a pattern (e.g., "+90*" → "🇹🇷").
     */
    public static String getFlagForPattern(String pattern) {
        String dialCode = extractDialCodeFromPattern(pattern);
        if (dialCode == null) {
            return "";
        }

        for (CountryCode country : getPopularCountries()) {
            if (country.dialCode.equals(dialCode)) {
                return country.flag;
            }
        }

        return "";
    }
}
