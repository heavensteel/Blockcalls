package com.floventy.blockcalls.utils;

import android.content.Context;
import java.util.ArrayList;
import java.util.List;

/**
 * Whitelist of trusted phone numbers and prefixes that should NEVER be blocked,
 * even if a user-defined rule would match them.
 *
 * Categories:
 *  - Turkish banks (444 + 0850 lines)
 *  - Cargo / logistics companies
 *  - E-commerce platforms
 *  - Telecom operators (customer service)
 *  - Government / emergency lines
 *  - Utility companies
 *
 * Matching logic:
 *  - TRUSTED_PREFIXES: number starts with this prefix (digits only, no separators)
 *  - TRUSTED_EXACT:    number matches exactly (digits only)
 */
public class TrustedNumbers {

    public static class TrustedEntry {
        public final String numberOrPattern;
        public final String name;

        public TrustedEntry(String numberOrPattern, String name) {
            this.numberOrPattern = numberOrPattern;
            this.name = name;
        }
    }

    private static List<TrustedEntry> dynamicPrefixes = new ArrayList<>();
    private static List<TrustedEntry> dynamicExact = new ArrayList<>();
    private static final Object lock = new Object();


    // ─── Prefix whitelist ─────────────────────────────────────────────────────
    // A call is trusted if its normalized number STARTS WITH any of these prefixes.

    private static final String[] TRUSTED_PREFIXES = {

        // ── ALL 444-XXXX numbers ──────────────────────────────────────────────
        // 444 is a special Turkish business prefix (banks, cargo, etc.)
        // 7-digit structure: 444 + 4 digits. Spammers do NOT use these lines.
        "444",

        // ── Government emergency & service lines ─────────────────────────────
        "110",  // Yangın / Fire
        "112",  // Acil Yardım / Emergency
        "122",  // Doğal afet / AFAD
        "144",  // Orman yangını
        "155",  // Polis İmdat / Police
        "156",  // Jandarma İmdat
        "157",  // Sahil Güvenlik
        "158",  // Gümrük muhafaza
        "168",  // Sosyal yardım
        "171",  // İntihar önleme hattı
        "174",  // Tütün bırakma hattı
        "176",  // Çevre şikayet hattı
        "177",  // Orman Genel Müdürlüğü
        "179",  // Aile içi şiddet
        "181",  // ALO Gıda
        "182",  // Sağlık Bakanlığı / ALO Doktor
        "183",  // Sosyal hizmetler ALO
        "184",  // Sağlık ihbar hattı
        "185",  // Maliye / Vergi
        "186",  // Çalışma Bakanlığı
        "187",  // Kaçakçılık ihbar
        "188",  // ÇSGB ALO
        "189",  // ALO Yolsuzluk

        // ── PTT ──────────────────────────────────────────────────────────────
        "170",  // PTT genel hat
        "171",  // PTT kargo bilgi

        // ── Telecom operators (customer service) ─────────────────────────────
        // Turkcell
        "05321111111", // placeholder — covered by exact list below
        // These short codes are dialed directly; we cover them in TRUSTED_EXACT.
    };

    // ─── Exact number whitelist ───────────────────────────────────────────────
    // Normalized (digits only, no +, no spaces).

    private static final String[] TRUSTED_EXACT = {

        // ════════════════════════════════════════════════════════════════════
        // BANKALAR / BANKS
        // ════════════════════════════════════════════════════════════════════

        // Ziraat Bankası
        "4440100", "08502200100",
        // Halkbank
        "4440400", "08502220400",
        // Vakıfbank
        "4440724", "08502220724",
        // Garanti BBVA
        "4440333", "08502220333",
        // İş Bankası
        "4440444", "08507240444",
        // Yapı Kredi
        "4448444", "08502500444",
        // Akbank
        "4442525", "08502502525",
        // Denizbank
        "4440800", "08502220800",
        // TEB (Türk Ekonomi Bankası)
        "4440832", "08502000832",
        // QNB Finansbank
        "4440111", "08502220111",
        // ING Bank
        "4440946", "08502220946",
        // HSBC Türkiye
        "4440472", "08502110472",
        // Şekerbank
        "4440735", "08502220735",
        // Odea Bank
        "08502226332",
        // Burgan Bank
        "4442800",
        // Alternatifbank
        "4447900",
        // Fibabanka
        "4443422",
        // Türkiye Finans (Katılım)
        "4447365",
        // Kuveyt Türk
        "4440123",
        // Albaraka Türk
        "4446663",
        // Ziraat Katılım
        "4441203",
        // Vakıf Katılım
        "4441724",
        // Emlak Katılım
        "4441361",
        // Odeabank
        "4446332",
        // Aktif Bank
        "4445588",
        // Türkiye İhracat Kredi Bankası (Eximbank)
        "03122040000",
        // Merkez Bankası
        "03124507777",

        // ════════════════════════════════════════════════════════════════════
        // KARGO / CARGO & LOGISTICS
        // ════════════════════════════════════════════════════════════════════

        // Yurtiçi Kargo
        "4449999", "08503330999",
        // PTT Kargo
        "4441788", "08504441788",
        // Aras Kargo
        "4442727", "08504552727",
        // MNG Kargo
        "4440664", "08504550664",
        // Sürat Kargo
        "4447872", "08503440544",
        // UPS Türkiye
        "4440877", "08502220877",
        // DHL Türkiye
        "4440040", "08502150040",
        // FedEx Türkiye
        "4440033",
        // Sendeo (eski Horoz Lojistik)
        "4440543",
        // Borusan Lojistik
        "4440011",
        // AGT Kargo
        "4444428",
        // Cainiao / Aliexpress Türkiye
        "08505322246",

        // ════════════════════════════════════════════════════════════════════
        // E-TİCARET / E-COMMERCE
        // ════════════════════════════════════════════════════════════════════

        // Trendyol
        "08503330555",
        // Hepsiburada
        "08502524000",
        // n11
        "08505324200",
        // Amazon Türkiye
        "08505327000",
        // GittiGidiyor
        "4448484",
        // Çiçeksepeti
        "08504441515",
        // Morhipo
        "08502220676",
        // Boyner
        "08502020400",
        // LC Waikiki
        "08502220522",
        // Getir
        "08502558374",
        // Yemeksepeti
        "08504441444",
        // Migros Sanal Market
        "4440505",

        // ════════════════════════════════════════════════════════════════════
        // TELEKOM OPERATÖRLERİ / TELECOM OPERATORS
        // ════════════════════════════════════════════════════════════════════

        // Turkcell
        "532", "05320000532",
        "4440532",
        // Vodafone
        "4440542", "08502420000",
        // Türk Telekom / Turkcell
        "4440121", "08504440121",
        // Netgsm
        "08502220850",

        // ════════════════════════════════════════════════════════════════════
        // SİGORTA / INSURANCE
        // ════════════════════════════════════════════════════════════════════

        // Anadolu Sigorta
        "4440274",
        // Allianz Sigorta
        "4440900",
        // Güneş Sigorta
        "4440486",
        // Mapfre Sigorta
        "4440627",
        // Türkiye Sigorta
        "4447575",
        // AXA Sigorta
        "4440292",
        // Sompo Sigorta
        "4440440",
        // Ray Sigorta
        "4440729",
        // HDI Sigorta
        "4440434",
        // Zurich Sigorta
        "4440998",

        // ════════════════════════════════════════════════════════════════════
        // ENERJİ / UTILITY COMPANIES
        // ════════════════════════════════════════════════════════════════════

        // BEDAŞ (İstanbul Avrupa elektrik)
        "08503330060",
        // AYEDAŞ (İstanbul Anadolu elektrik)
        "08503330061",
        // BAŞKENTGAZ
        "4440033",
        // İGDAŞ (İstanbul Gaz)
        "4440023",
        // BOTAŞ
        "4440110",
        // TEDAŞ
        "4442020",

        // ════════════════════════════════════════════════════════════════════
        // SAĞLIK / HEALTH
        // ════════════════════════════════════════════════════════════════════

        // Acıbadem Hastaneleri
        "4440155",
        // Medical Park
        "08502220400",
        // Medicana
        "4440222",
        // Memorial Hastaneleri
        "4440 161", // normalized: 44400161
        "44400161",
        // Liv Hospital
        "08504440548",
        // Türkiye Hastaneleri
        "08502000000",

        // ════════════════════════════════════════════════════════════════════
        // DEVLET KURUMLARI / GOVERNMENT SERVICES
        // ════════════════════════════════════════════════════════════════════

        // SGK (Sosyal Güvenlik Kurumu)
        "4701010",
        // Vergi Dairesi ALO
        "1890",
        // Belediye hizmet hattı
        "153",
        // Su kesintisi ihbar
        "185",
        // İŞKUR
        "08504750575",
        // E-devlet yardım
        "4704555",
        // Emniyet (ihbar dışı)
        "4704045",

        // ════════════════════════════════════════════════════════════════════
        // HAVAYOLLARI / AIRLINES
        // ════════════════════════════════════════════════════════════════════

        // Türk Hava Yolları (THY)
        "4440849",
        // Pegasus
        "4440737",
        // SunExpress
        "08502225566",
        // AnadoluJet
        "4440487",
    };

    // ─── Public API ───────────────────────────────────────────────────────────

    /**
     * Initializes the dynamic whitelists by reading the cached JSON file.
     */
    public static void initialize(Context context) {
        synchronized (lock) {
            try {
                java.io.File file = new java.io.File(context.getFilesDir(), "safe_lists_cache.json");
                if (file.exists()) {
                    java.io.BufferedReader br = new java.io.BufferedReader(new java.io.FileReader(file));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) {
                        sb.append(line).append('\n');
                    }
                    br.close();

                    org.json.JSONObject root = new org.json.JSONObject(sb.toString());
                    
                    org.json.JSONArray prefixesJson = root.optJSONArray("prefixes");
                    List<TrustedEntry> prefixes = new ArrayList<>();
                    if (prefixesJson != null) {
                        for (int i = 0; i < prefixesJson.length(); i++) {
                            org.json.JSONObject obj = prefixesJson.optJSONObject(i);
                            if (obj != null) {
                                String pattern = obj.optString("pattern");
                                String name = obj.optString("name");
                                if (!pattern.isEmpty()) {
                                    prefixes.add(new TrustedEntry(pattern, name));
                                }
                            } else {
                                String pattern = prefixesJson.optString(i);
                                if (!pattern.isEmpty()) {
                                    prefixes.add(new TrustedEntry(pattern, getHardcodedNameFor(pattern, true)));
                                }
                            }
                        }
                    }

                    org.json.JSONArray exactJson = root.optJSONArray("exact");
                    List<TrustedEntry> exact = new ArrayList<>();
                    if (exactJson != null) {
                        for (int i = 0; i < exactJson.length(); i++) {
                            org.json.JSONObject obj = exactJson.optJSONObject(i);
                            if (obj != null) {
                                String num = obj.optString("number");
                                String name = obj.optString("name");
                                if (!num.isEmpty()) {
                                    exact.add(new TrustedEntry(num, name));
                                }
                            } else {
                                String num = exactJson.optString(i);
                                if (!num.isEmpty()) {
                                    exact.add(new TrustedEntry(num, getHardcodedNameFor(num, false)));
                                }
                            }
                        }
                    }

                    dynamicPrefixes = prefixes;
                    dynamicExact = exact;
                    android.util.Log.d("TrustedNumbers", "Loaded from cache: " + dynamicPrefixes.size() + " prefixes, " + dynamicExact.size() + " exact numbers.");
                }
            } catch (Exception e) {
                android.util.Log.w("TrustedNumbers", "Failed to load dynamic safe list from cache", e);
            }
        }
    }

    /**
     * Sets the dynamic whitelists in RAM. Called after successful download.
     */
    public static void setDynamicLists(List<TrustedEntry> prefixes, List<TrustedEntry> exact) {
        synchronized (lock) {
            dynamicPrefixes = prefixes != null ? prefixes : new ArrayList<>();
            dynamicExact = exact != null ? exact : new ArrayList<>();
            android.util.Log.d("TrustedNumbers", "Dynamic safe lists updated: " + dynamicPrefixes.size() + " prefixes, " + dynamicExact.size() + " exact numbers.");
        }
    }

    /**
     * Returns true if the phone number is in the trusted whitelist.
     * Normalized (digits only) comparison.
     *
     * @param phoneNumber The incoming phone number (raw format).
     * @return true if the number should NEVER be blocked.
     */
    public static boolean isTrusted(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.isEmpty()) return false;

        String normalized = normalize(phoneNumber);
        if (normalized.isEmpty()) return false;

        // Generate all format variants to compare against trusted lists:
        // 1. normalized as-is          e.g. "908502220400" or "4440400"
        // 2. with-0 stripped           e.g. "08502220400"  (mobile/0850 local format)
        // 3. raw stripped (no 0)       e.g. "4440400"      (444-XXXX business lines)
        java.util.List<String> variants = new java.util.ArrayList<>();
        variants.add(normalized);
        if (normalized.startsWith("90") && normalized.length() >= 9) {
            String withoutCC = normalized.substring(2); // e.g. "4440400" or "8502220400"
            variants.add("0" + withoutCC);              // e.g. "04440400" or "08502220400"
            if (!variants.contains(withoutCC)) {
                variants.add(withoutCC);                // e.g. "4440400" — key for 444 lines
            }
        }

        for (String v : variants) {
            // Dynamic prefix list
            synchronized (lock) {
                if (dynamicPrefixes != null) {
                    for (TrustedEntry entry : dynamicPrefixes) {
                        if (v.startsWith(normalize(entry.numberOrPattern))) return true;
                    }
                }
                // Dynamic exact list
                if (dynamicExact != null) {
                    for (TrustedEntry entry : dynamicExact) {
                        if (v.equals(normalize(entry.numberOrPattern))) return true;
                    }
                }
            }
            // Hardcoded prefix list
            for (String prefix : TRUSTED_PREFIXES) {
                if (v.startsWith(prefix)) return true;
            }
            // Hardcoded exact list
            for (String trusted : TRUSTED_EXACT) {
                if (v.equals(normalize(trusted))) return true;
            }
        }

        return false;
    }

    /**
     * Returns a combined list of all trusted entries (dynamic downloaded list + local hardcoded fallbacks).
     */
    public static List<TrustedEntry> getAllTrustedEntries() {
        List<TrustedEntry> list = new ArrayList<>();
        synchronized (lock) {
            if (dynamicPrefixes != null && !dynamicPrefixes.isEmpty()) {
                list.addAll(dynamicPrefixes);
            }
            if (dynamicExact != null && !dynamicExact.isEmpty()) {
                list.addAll(dynamicExact);
            }
        }

        // If the dynamic list is empty, populate from hardcoded lists
        if (list.isEmpty()) {
            for (String prefix : TRUSTED_PREFIXES) {
                list.add(new TrustedEntry(prefix, getHardcodedNameFor(prefix, true)));
            }
            for (String exact : TRUSTED_EXACT) {
                list.add(new TrustedEntry(exact, getHardcodedNameFor(exact, false)));
            }
        }
        return list;
    }

    private static String getHardcodedNameFor(String number, boolean isPrefix) {
        if (isPrefix) {
            switch (number) {
                case "444": return "Kurumsal Hatlar";
                case "110": return "İtfaiye";
                case "112": return "Acil Çağrı Merkezi";
                case "122": return "AFAD";
                case "144": return "Sosyal Yardım";
                case "155": return "Polis İmdat";
                case "156": return "Jandarma İmdat";
                case "157": return "Göç İdaresi";
                case "158": return "Sahil Güvenlik";
                case "168": return "Sosyal Yardım / Kızılay";
                case "171": return "Sigara Bırakma Hattı";
                case "174": return "Alo Gıda";
                case "176": return "Kültür Turizm İhbar";
                case "177": return "Orman Yangın İhbar";
                case "179": return "Valilik / Kaymakamlık";
                case "181": return "Çevre ve Şehircilik";
                case "182": return "MHRS Hastane Randevu";
                case "183": return "Sosyal Hizmetler ALO";
                case "184": return "Sağlık İhbar Hattı";
                case "185": return "Belediye Su Arıza";
                case "186": return "Elektrik Arıza";
                case "187": return "Doğalgaz Arıza";
                case "188": return "Cenaze Hizevleri";
                case "189": return "Vergi Danışma";
                case "170": return "PTT / SGK Danışma";
                case "05321111111": return "Müşteri Hizmetleri";
                default: return "Güvenli Numara (Ön Tanımlı)";
            }
        } else {
            if (number.equals("4440100") || number.equals("08502200100")) return "Ziraat Bankası";
            if (number.equals("4440400") || number.equals("08502220400")) return "Halkbank";
            if (number.equals("4440724") || number.equals("08502220724")) return "VakıfBank";
            if (number.equals("4440333") || number.equals("08502220333")) return "Garanti BBVA";
            if (number.equals("4440444") || number.equals("08507240444")) return "İş Bankası";
            if (number.equals("4448444") || number.equals("08502500444")) return "Yapı Kredi";
            if (number.equals("4442525") || number.equals("08502502525")) return "Akbank";
            if (number.equals("4440800") || number.equals("08502220800")) return "DenizBank";
            if (number.equals("4440832") || number.equals("08502000832")) return "TEB (Türk Ekonomi Bankası)";
            if (number.equals("4440111") || number.equals("08502220111")) return "QNB Finansbank";
            if (number.equals("4440946") || number.equals("08502220946")) return "ING Bank";
            if (number.equals("4440472") || number.equals("08502110472")) return "HSBC Türkiye";
            if (number.equals("4440735") || number.equals("08502220735")) return "Şekerbank";
            
            if (number.equals("4449999") || number.equals("08503330999")) return "Yurtiçi Kargo";
            if (number.equals("4441788") || number.equals("08504441788")) return "PTT Kargo";
            if (number.equals("4442727") || number.equals("08504552727") || number.equals("4442552")) return "Aras Kargo";
            if (number.equals("4440664") || number.equals("08504550664") || number.equals("08502220606")) return "MNG Kargo";
            if (number.equals("4447872") || number.equals("08503440544") || number.equals("08502020202")) return "Sürat Kargo";
            
            if (number.equals("532") || number.equals("05320000532") || number.equals("4440532") || number.equals("05325320000") || number.equals("05327552222")) return "Turkcell";
            if (number.equals("542") || number.equals("4440542") || number.equals("08502420000") || number.equals("05425420000") || number.equals("05425425425")) return "Vodafone";
            if (number.equals("4440121") || number.equals("08504440121") || number.equals("4441444") || number.equals("500") || number.equals("4445444") || number.equals("4440375") || number.equals("4441375")) return "Türk Telekom";
            if (number.equals("08503330555") || number.equals("02123310200")) return "Trendyol";
            if (number.equals("08502524000")) return "Hepsiburada";
            if (number.equals("08505324200") || number.equals("08503330011")) return "n11";
            if (number.equals("08505327000")) return "Amazon Türkiye";
            if (number.equals("08502558374") || number.equals("08505325050")) return "Getir";
            if (number.equals("08504441444") || number.equals("4445445")) return "Yemeksepeti";
            if (number.equals("4440505") || number.equals("08502004000")) return "Migros";
            
            return "Güvenli Numara";
        }
    }

    /**
     * Returns the institution name for a trusted pattern/number, or null if unknown or generic.
     */
    public static String getTrustedNameFor(String patternOrNumber) {
        if (patternOrNumber == null || patternOrNumber.isEmpty()) return null;

        // Clean wildcard characters like * if present in the pattern
        String cleanInput = patternOrNumber.replace("*", "").trim();
        String cleaned = getCleanTurkishNumber(cleanInput);
        if (cleaned.isEmpty()) return null;

        synchronized (lock) {
            if (dynamicExact != null) {
                for (TrustedEntry entry : dynamicExact) {
                    if (getCleanTurkishNumber(entry.numberOrPattern).equals(cleaned)) {
                        return entry.name;
                    }
                }
            }
            if (dynamicPrefixes != null) {
                for (TrustedEntry entry : dynamicPrefixes) {
                    if (getCleanTurkishNumber(entry.numberOrPattern).equals(cleaned)) {
                        return entry.name;
                    }
                }
            }
        }

        // Check fallback lists
        for (String exact : TRUSTED_EXACT) {
            if (getCleanTurkishNumber(exact).equals(cleaned)) {
                String name = getHardcodedNameFor(exact, false);
                if (name != null && !name.equals("Güvenli Numara")) {
                    return name;
                }
            }
        }

        for (String prefix : TRUSTED_PREFIXES) {
            String cleanPrefix = getCleanTurkishNumber(prefix);
            if (cleanPrefix.equals(cleaned) || cleaned.startsWith(cleanPrefix)) {
                String name = getHardcodedNameFor(prefix, true);
                if (name != null && !name.equals("Güvenli Numara (Ön Tanımlı)") && !name.equals("Kurumsal Hatlar")) {
                    return name;
                }
            }
        }

        return null;
    }

    /**
     * Standardizes Turkish numbers by stripping country codes (90 / +90) and prefixing 0 for area codes.
     */
    public static String getCleanTurkishNumber(String number) {
        if (number == null) return "";
        String normalized = number.replaceAll("[^0-9]", "");
        if (normalized.startsWith("90") && normalized.length() >= 9) {
            String stripped = normalized.substring(2);
            if (stripped.startsWith("5") || stripped.startsWith("8") || stripped.startsWith("2") || stripped.startsWith("3") || stripped.startsWith("4")) {
                return "0" + stripped;
            }
            return stripped;
        }
        return normalized;
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    /** Strip all non-digit characters. */
    private static String normalize(String input) {
        return input == null ? "" : input.replaceAll("[^0-9]", "");
    }

    /**
     * If the number starts with 90 (Turkish country code without +),
     * return the version without the country code.
     * E.g. "905321234567" → "05321234567"
     */
    private static String stripTurkishPrefix(String normalized) {
        // >= 9: covers 2-digit country code (90) + 7-digit Turkish business numbers (e.g. 4440400)
        // as well as longer mobile numbers (12 digits total)
        if (normalized.startsWith("90") && normalized.length() >= 9) {
            return "0" + normalized.substring(2);
        }
        return normalized;
    }
}
