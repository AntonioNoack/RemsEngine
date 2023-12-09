package me.anno.ui.editor.code.tokenizer

enum class TokenType {

    COMMENT,
    VARIABLE,
    VARIABLE2,
    VARIABLE3,
    KEYWORD,
    BUILTIN,
    STRING,
    STRING2,
    NUMBER,
    OPERATOR,
    META,
    TAG,
    PROPERTY,
    ATTRIBUTE,
    ATOM, // atomic variable = constant
    DEFINE,
    PUNCTUATION,
    ERROR,
    QUALIFIER,
    BRACKET,
    UNKNOWN;

    companion object {
        val values2 = values()
    }
}