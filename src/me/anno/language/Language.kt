package me.anno.language

import me.anno.language.translation.NameDesc

/**
 * languages from LanguageTool for spellchecking;
 * could be good enough for most game dev projects;
 * if you need more languages to be added here, just write me :)
 * */
enum class Language(prettyName: String, val code: String) {

    Arabic("Arabic", "ar"),
    // English("English", "en"),
    AmericanEnglish("English (American)", "en-US"),
    BritishEnglish("English (British)", "en-GB"),
    AustralianEnglish("English (Australia)", "en-AU"),
    CanadianEnglish("English (Canada)", "en-CA"),
    NewZealandEnglish("English (New Zealand)", "en-NZ"),
    SouthAfricanEnglish("English (South Africa)", "en-ZA"),
    Persian("Persian", "fa"),
    French("French", "fr"),
    German("German (Germany)", "de-DE"),

    // GermanyGerman("GermanyGerman", "de-DE"),
    AustrianGerman("German (Austria)", "de-AT"),
    SwissGerman("German (Switzerland)", "de-CH"),
    SimpleGerman("German (Simple)", "de-DE-x-simple-language"),
    Polish("Polish", "pl-PL"),
    Catalan("Catalan", "ca-ES"),
    ValencianCatalan("Valencian Catalan", "ca-ES-valencia"),
    Italian("Italian", "it"),
    Breton("Breton", "br-FR"),
    Dutch("Dutch", "nl"),
    // Portuguese("Portuguese", "pt"),
    PortugalPortuguese("Portuguese (Portugal)", "pt-PT"),
    BrazilianPortuguese("Portuguese (Brazilian)", "pt-BR"),
    AngolaPortuguese("Portuguese (Angola)", "pt-AO"),
    MozambiquePortuguese("Portuguese (Mozambique)", "pt-MZ"),
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

    val nameDesc = NameDesc(prettyName, "", "lang.$code")

    override fun toString() = nameDesc.name

    companion object {
        @JvmStatic
        fun get(code: String) = entries
            .firstOrNull { it.code.equals(code, true) }
            ?: AmericanEnglish
    }

}