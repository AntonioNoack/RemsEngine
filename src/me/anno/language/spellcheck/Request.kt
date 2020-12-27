package me.anno.language.spellcheck

class Request(val sentence: String, val key: Any, val callback: (List<Suggestion>) -> Unit)