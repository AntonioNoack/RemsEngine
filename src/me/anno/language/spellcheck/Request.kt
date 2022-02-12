package me.anno.language.spellcheck

class Request(val sentence: CharSequence, val key: Any, val callback: (List<Suggestion>) -> Unit)