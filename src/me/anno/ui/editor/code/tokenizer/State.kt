package me.anno.ui.editor.code.tokenizer

class State(val indent0: Int, var indentDepth: Int, var next: (Stream, State) -> TokenType)