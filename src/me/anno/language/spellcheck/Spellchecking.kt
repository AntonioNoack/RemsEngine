package me.anno.language.spellcheck

import me.anno.Engine
import me.anno.cache.AsyncCacheData
import me.anno.cache.DualCacheSection
import me.anno.language.Language
import me.anno.utils.InternalAPI

object Spellchecking : DualCacheSection<CharSequence, Language, List<Suggestion>>("Spellchecking") {

    @InternalAPI
    var checkImpl: ((sentence: CharSequence, allowFirstLowercase: Boolean) -> AsyncCacheData<List<Suggestion>>)? = null

    // just which language I personally like best
    var defaultLanguage: Language? = Language.AmericanEnglish

    fun check(sentence: CharSequence, allowFirstLowercase: Boolean): AsyncCacheData<List<Suggestion>> {
        if (Engine.shutdown) return AsyncCacheData.empty() // service isn't running anyway
        return checkImpl?.invoke(sentence, allowFirstLowercase) ?: AsyncCacheData.empty()
    }
}