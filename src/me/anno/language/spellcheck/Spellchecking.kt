package me.anno.language.spellcheck

import me.anno.cache.CacheSection
import me.anno.language.Language
import me.anno.utils.InternalAPI

object Spellchecking : CacheSection("Spellchecking") {

    @InternalAPI
    var checkImpl: ((sentence: CharSequence, allowFirstLowercase: Boolean, async: Boolean) -> List<Suggestion>?)? = null

    var defaultLanguage = Language.AmericanEnglish

    fun check(sentence: CharSequence, allowFirstLowercase: Boolean, async: Boolean = true): List<Suggestion>? {
        return checkImpl?.invoke(sentence, allowFirstLowercase, async)
    }
}