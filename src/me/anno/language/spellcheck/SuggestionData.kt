package me.anno.language.spellcheck

import me.anno.cache.CacheData

class SuggestionData(var suggestions: List<Suggestion>?): CacheData {
    override fun destroy() {}
}