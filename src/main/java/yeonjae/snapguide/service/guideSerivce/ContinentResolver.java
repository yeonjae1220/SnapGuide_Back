package yeonjae.snapguide.service.guideSerivce;

import java.util.HashMap;
import java.util.Map;

/**
 * ISO 3166-1 alpha-2 국가 코드 → 대륙 코드/이름 매핑.
 * 대륙 단위 집계(zoom out 최상위)에서 사용한다. 매핑에 없는 코드는 "OTHER"로 처리.
 */
public final class ContinentResolver {

    private ContinentResolver() {}

    private static final Map<String, String> CODE_TO_CONTINENT = new HashMap<>();

    private static final Map<String, String> CONTINENT_NAMES = Map.of(
            "AF", "Africa",
            "AS", "Asia",
            "EU", "Europe",
            "NA", "North America",
            "SA", "South America",
            "OC", "Oceania",
            "AN", "Antarctica",
            "OTHER", "Other"
    );

    static {
        put("AS", "KR", "JP", "CN", "TW", "HK", "MO", "MN", "IN", "ID", "TH", "VN", "PH",
                "MY", "SG", "KH", "LA", "MM", "BN", "BD", "BT", "NP", "LK", "MV", "PK",
                "AF", "KZ", "KG", "TJ", "TM", "UZ", "IR", "IQ", "SY", "LB", "IL", "PS",
                "JO", "SA", "AE", "QA", "BH", "KW", "OM", "YE", "TR", "GE", "AM", "AZ", "CY");
        put("EU", "GB", "IE", "FR", "DE", "NL", "BE", "LU", "ES", "PT", "IT", "CH", "AT",
                "GR", "SE", "NO", "FI", "DK", "IS", "PL", "CZ", "SK", "HU", "RO", "BG",
                "HR", "SI", "RS", "BA", "ME", "MK", "AL", "UA", "BY", "RU", "EE", "LV",
                "LT", "MD", "MT", "LI", "MC", "SM", "VA", "AD");
        put("NA", "US", "CA", "MX", "GT", "BZ", "SV", "HN", "NI", "CR", "PA", "CU", "JM",
                "HT", "DO", "BS", "BB", "TT", "PR");
        put("SA", "BR", "AR", "CL", "CO", "PE", "VE", "EC", "BO", "PY", "UY", "GY", "SR", "GF");
        put("AF", "EG", "MA", "DZ", "TN", "LY", "SD", "ET", "KE", "TZ", "UG", "RW", "NG",
                "GH", "CI", "SN", "ML", "CM", "CD", "CG", "AO", "ZM", "ZW", "MZ", "BW",
                "ZA", "LS", "SZ", "MG", "MU", "SC", "GA", "GN", "BF", "BJ", "TG", "NE",
                "TD", "SO", "DJ", "ER", "MW");
        put("OC", "AU", "NZ", "FJ", "PG", "NC", "PF", "SB", "VU", "WS", "TO", "KI", "FM",
                "MH", "PW", "NR", "TV");
        put("AN", "AQ");
    }

    private static void put(String continent, String... codes) {
        for (String code : codes) {
            CODE_TO_CONTINENT.put(code, continent);
        }
    }

    /** 국가 코드를 대륙 코드로 변환. 알 수 없는 코드는 "OTHER". */
    public static String continentCode(String countryCode) {
        if (countryCode == null) {
            return "OTHER";
        }
        return CODE_TO_CONTINENT.getOrDefault(countryCode.toUpperCase(), "OTHER");
    }

    /** 대륙 코드를 표시 이름으로 변환. */
    public static String continentName(String continentCode) {
        return CONTINENT_NAMES.getOrDefault(continentCode, "Other");
    }
}
