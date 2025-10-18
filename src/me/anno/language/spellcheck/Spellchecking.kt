package me.anno.language.spellcheck

import me.anno.Engine
import me.anno.cache.Promise
import me.anno.cache.DualCacheSection
import me.anno.language.Language
import me.anno.utils.InternalAPI

object Spellchecking : DualCacheSection<CharSequence, Language, List<Suggestion>>("Spellchecking") {

    @InternalAPI
    var checkImpl: ((sentence: CharSequence, allowFirstLowercase: Boolean) -> Promise<List<Suggestion>>)? = null

    // just which language I personally like best
    var defaultLanguage: Language? = Language.AmericanEnglish

    fun check(sentence: CharSequence, allowFirstLowercase: Boolean): Promise<List<Suggestion>> {
        if (Engine.shutdown) return Promise.empty() // service isn't running anyway
        return checkImpl?.invoke(sentence, allowFirstLowercase) ?: Promise.empty()
    }
}