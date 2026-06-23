package app.downify.data

import java.util.Locale

data class Country(
    val iso: String,       // ISO 3166-1 alpha-2, ör. "TR"
    val name: String,      // Görünen ad
    val dialCode: String   // ör. "+90"
) {
    /** ISO kodundan bayrak emojisi üretir (regional indicator semboller). */
    val flag: String
        get() = iso.uppercase().map { 0x1F1E6 - 'A'.code + it.code }
            .joinToString("") { String(Character.toChars(it)) }
}

object Countries {
    /** Cihaz bölgesine göre varsayılan ülke (bulunamazsa Türkiye). */
    fun deviceDefault(): Country {
        val region = Locale.getDefault().country
        return all.firstOrNull { it.iso == region } ?: all.first { it.iso == "TR" }
    }

    val all: List<Country> = listOf(
        Country("TR", "Türkiye", "+90"),
        Country("US", "United States", "+1"),
        Country("GB", "United Kingdom", "+44"),
        Country("DE", "Deutschland", "+49"),
        Country("FR", "France", "+33"),
        Country("NL", "Nederland", "+31"),
        Country("ES", "España", "+34"),
        Country("IT", "Italia", "+39"),
        Country("PT", "Portugal", "+351"),
        Country("BE", "Belgique", "+32"),
        Country("CH", "Schweiz", "+41"),
        Country("AT", "Österreich", "+43"),
        Country("SE", "Sverige", "+46"),
        Country("NO", "Norge", "+47"),
        Country("DK", "Danmark", "+45"),
        Country("FI", "Suomi", "+358"),
        Country("IE", "Ireland", "+353"),
        Country("PL", "Polska", "+48"),
        Country("CZ", "Česko", "+420"),
        Country("GR", "Ελλάδα", "+30"),
        Country("RO", "România", "+40"),
        Country("HU", "Magyarország", "+36"),
        Country("UA", "Україна", "+380"),
        Country("RU", "Россия", "+7"),
        Country("AZ", "Azərbaycan", "+994"),
        Country("GE", "საქართველო", "+995"),
        Country("CY", "Κύπρος", "+357"),
        Country("BG", "България", "+359"),
        Country("RS", "Srbija", "+381"),
        Country("HR", "Hrvatska", "+385"),
        Country("SA", "السعودية", "+966"),
        Country("AE", "الإمارات", "+971"),
        Country("QA", "قطر", "+974"),
        Country("KW", "الكويت", "+965"),
        Country("BH", "البحرين", "+973"),
        Country("OM", "عُمان", "+968"),
        Country("JO", "الأردن", "+962"),
        Country("LB", "لبنان", "+961"),
        Country("EG", "مصر", "+20"),
        Country("MA", "المغرب", "+212"),
        Country("DZ", "الجزائر", "+213"),
        Country("TN", "تونس", "+216"),
        Country("IL", "ישראל", "+972"),
        Country("IR", "ایران", "+98"),
        Country("IQ", "العراق", "+964"),
        Country("PK", "Pakistan", "+92"),
        Country("IN", "India", "+91"),
        Country("BD", "বাংলাদেশ", "+880"),
        Country("LK", "Sri Lanka", "+94"),
        Country("CN", "中国", "+86"),
        Country("JP", "日本", "+81"),
        Country("KR", "대한민국", "+82"),
        Country("ID", "Indonesia", "+62"),
        Country("MY", "Malaysia", "+60"),
        Country("SG", "Singapore", "+65"),
        Country("TH", "ไทย", "+66"),
        Country("VN", "Việt Nam", "+84"),
        Country("PH", "Philippines", "+63"),
        Country("AU", "Australia", "+61"),
        Country("NZ", "New Zealand", "+64"),
        Country("CA", "Canada", "+1"),
        Country("MX", "México", "+52"),
        Country("BR", "Brasil", "+55"),
        Country("AR", "Argentina", "+54"),
        Country("CL", "Chile", "+56"),
        Country("CO", "Colombia", "+57"),
        Country("PE", "Perú", "+51"),
        Country("VE", "Venezuela", "+58"),
        Country("ZA", "South Africa", "+27"),
        Country("NG", "Nigeria", "+234"),
        Country("KE", "Kenya", "+254"),
        Country("GH", "Ghana", "+233"),
        Country("ET", "Ethiopia", "+251"),
        Country("KZ", "Қазақстан", "+7"),
        Country("UZ", "Oʻzbekiston", "+998"),
        Country("TM", "Türkmenistan", "+993"),
        Country("KG", "Кыргызстан", "+996"),
        Country("TJ", "Тоҷикистон", "+992"),
    )
}
