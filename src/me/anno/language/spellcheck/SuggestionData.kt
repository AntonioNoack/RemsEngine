package me.anno.language.spellcheck

import me.anno.cache.CacheData

class SuggestionData(suggestions: List<Suggestion>?): CacheData<List<Suggestion>?>(suggestions)