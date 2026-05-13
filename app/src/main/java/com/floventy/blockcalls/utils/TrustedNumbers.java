package com.floventy.blockcalls.utils;

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

        // Check prefix whitelist
        for (String prefix : TRUSTED_PREFIXES) {
            if (normalized.startsWith(prefix)) return true;
        }

        // Check exact whitelist
        for (String trusted : TRUSTED_EXACT) {
            String normalizedTrusted = normalize(trusted);
            if (normalized.equals(normalizedTrusted)) return true;
        }

        // Also try stripping the Turkish country code (+90 / 90) and re-checking
        String stripped = stripTurkishPrefix(normalized);
        if (!stripped.equals(normalized)) {
            for (String prefix : TRUSTED_PREFIXES) {
                if (stripped.startsWith(prefix)) return true;
            }
            for (String trusted : TRUSTED_EXACT) {
                if (stripped.equals(normalize(trusted))) return true;
            }
        }

        return false;
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
        if (normalized.startsWith("90") && normalized.length() >= 12) {
            return "0" + normalized.substring(2);
        }
        return normalized;
    }
}
