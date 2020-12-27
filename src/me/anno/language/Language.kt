package me.anno.language

enum class Language(val displayName: String, val code: String) {

    None("None", ""),

    // GermanyGerman("GermanyGerman", "de-de"),
    Arabic("Arabic", "ar"),
    // English("English", "en"),
    AmericanEnglish("American English", "en-US"),
    BritishEnglish("British English", "en-GB"),
    AustralianEnglish("Australian English", "en-AU"),
    CanadianEnglish("Canadian English", "en-CA"),
    NewZealandEnglish("New Zealand English", "en-NZ"),
    SouthAfricanEnglish("South African English", "en-ZA"),
    Persian("Persian", "fa"),
    French("French", "fr"),
    German("German", "de-DE"),

    // GermanyGerman("GermanyGerman", "de-DE"),
    AustrianGerman("Austrian German", "de-AT"),
    SwissGerman("Swiss German", "de-CH"),
    SimpleGerman("Simple German", "de-DE-x-simple-language"),
    Polish("Polish", "pl-PL"),
    Catalan("Catalan", "ca-ES"),
    ValencianCatalan("Valencian Catalan", "ca-ES-valencia"),
    Italian("Italian", "it"),
    Breton("Breton", "br-FR"),
    Dutch("Dutch", "nl"),
    Portuguese("Portuguese", "pt"),
    PortugalPortuguese("Portugal Portuguese", "pt-PT"),
    BrazilianPortuguese("Brazilian Portuguese", "pt-BR"),
    AngolaPortuguese("Angola Portuguese", "pt-AO"),
    MozambiquePortuguese("Mozambique Portuguese", "pt-MZ"),
    Russian("Russian", "ru-RU"),
    Asturian("Asturian", "ast-ES"),
    Belarusian("Belarusian", "be-BY"),
    Chinese("Chinese", "zh-CN"),
    Danish("Danish", "da-DK"),
    Esperanto("Esperanto", "eo"),
    Irish("Irish", "ga-IE"),
    Galician("Galician", "gl-ES"),
    Greek("Greek", "el-GR"),
    Japanese("Japanese", "ja-JP"),
    Khmer("Khmer", "km-KH"),
    Romanian("Romanian", "ro-RO"),
    Slovak("Slovak", "sk-SK"),
    Slovenian("Slovenian", "sl-SI"),
    Spanish("Spanish", "es"),
    Swedish("Swedish", "sv"),
    Tamil("Tamil", "ta-IN"),
    Tagalog("Tagalog", "tl-PH"),
    Ukrainian("Ukrainian", "uk-UA");

    companion object {
        fun get(code: String) = values()
            .firstOrNull { it.code.equals(code, true) }
            ?: AmericanEnglish
    }

}