package me.anno.scripting.visual

class VisualType: NamedVisual() {

    var type = Type.STRING

    enum class Type(val defaultValue: Any?) {
        INT(0L),
        FLOAT(1.0),
        BOOLEAN(false),
        STRING(""),
        NULLABLE(null),
        OBJECT(null),
        GENERIC(null), // can be anything
    }

    override fun getClassName(): String = "VisualType"

}