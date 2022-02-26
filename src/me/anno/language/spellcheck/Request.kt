package me.anno.language.spellcheck

class Request(val sentence: CharSequence, val callback: (List<Suggestion>) -> Unit)