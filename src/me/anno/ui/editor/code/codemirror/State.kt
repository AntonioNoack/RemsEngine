package me.anno.ui.editor.code.codemirror

class State(val indent0: Int, var indentDepth: Int, var cur: (Stream, State) -> TokenType)