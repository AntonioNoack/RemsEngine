package me.anno.ui

import me.anno.fonts.Font
import me.anno.fonts.FontStats
import me.anno.io.utils.StringMap
import me.anno.ui.base.components.Padding
import me.anno.utils.ColorParsing
import me.anno.utils.types.Strings

/**
 * Advanced map for Panel styling; properties will be inherited from its parent.
 * */
class Style(val prefix: String?, val suffix: String?) {

    var values = StringMap()

    operator fun set(key: String, theme: String, value: Any) {
        values["$key.$theme"] = value
    }

    private fun getValue(name: String, defaultValue: Int) = getValue(name, name, defaultValue)
    private fun getValue(fullName: String, name: String, defaultValue: Int): Int {

        val value = values[name]
        return if (value != null) getColorMaybe(fullName, value, defaultValue)
        else {

            // LOGGER.warn("Missing config/style/$name")

            val index = name.indexOf('.')
            val index2 = name.indexOf('.', index + 1)
            if (index2 > -1) {
                val lessSpecificName = name.substring(index + 1)
                getValue(fullName, lessSpecificName, defaultValue)
            } else {
                values[name] = defaultValue
                defaultValue
            }
        }
    }

    private fun getValue(name: String, defaultValue: Float): Float = getValue(name, name, defaultValue)
    private fun getValue(fullName: String, name: String, defaultValue: Float): Float {

        val value = values[name]
        return if (value != null) getFloatMaybe(fullName, value, defaultValue)
        else {

            // LOGGER.warn("Missing config/style/$name")

            val index = name.indexOf('.')
            val index2 = name.indexOf('.', index + 1)
            if (index2 > -1) {
                val lessSpecificName = name.substring(index + 1)
                getValue(fullName, lessSpecificName, defaultValue)
            } else {
                values[name] = defaultValue
                defaultValue
            }
        }
    }

    private fun getValue(name: String, defaultValue: String): String {
        val value = values[name]
        return if (value != null) value.toString()
        else {

            // warn("Missing config/style/$name")

            val index = name.indexOf('.')
            val index2 = name.indexOf('.', index + 1)
            if (index2 > -1) {
                val lessSpecificName = name.substring(index + 1)
                getValue(lessSpecificName, defaultValue)
            } else {
                values[name] = defaultValue
                defaultValue
            }
        }
    }

    private fun getColorMaybe(fullName: String, value: Any, defaultValue: Int): Int {
        return when (value) {
            is Int -> value
            is Boolean -> if (value) 1 else 0
            is Float -> value.toInt()
            is String -> {
                val colorValue = value.toIntOrNull(16)
                    ?: ColorParsing.parseColor(value)
                if (colorValue == null) {
                    values[fullName] = defaultValue
                    defaultValue
                } else colorValue
            }
            else -> {
                values[fullName] = defaultValue
                defaultValue
            }
        }
    }

    private fun getFloatMaybe(fullName: String, value: Any, defaultValue: Float): Float {
        return when (value) {
            is Int -> value.toFloat()
            is Boolean -> if (value) 1f else 0f
            is Float -> value
            is String -> value.toFloatOrNull() ?: defaultValue
            else -> {
                values[fullName] = defaultValue
                defaultValue
            }
        }
    }

    fun getBoolean(name: String, defaultValue: Boolean) = getValue(getFullName(name), if (defaultValue) 1 else 0) != 0
    fun getInt(name: String, defaultValue: Int): Int = getValue(getFullName(name), defaultValue)
    fun getSize(name: String, defaultValue: Int): Int = getValue(getFullName(name), defaultValue)
    fun getSize(name: String, defaultValue: Float): Float = getValue(getFullName(name), defaultValue)
    fun getColor(name: String, defaultValue: Int): Int = getValue(getFullName(name), defaultValue)
    fun getString(name: String, defaultValue: String): String = getValue(getFullName(name), defaultValue)
    fun getFont(name: String): Font {
        val type = getString("$name.fontName", "Verdana")
        val size = getSize("$name.fontSize", FontStats.getDefaultFontSize()).toFloat()
        val isBold = getBoolean("$name.fontBold", false)
        val isItalic = getBoolean("$name.fontItalic", false)
        return Font(type, size, isBold, isItalic)
    }

    fun getPadding(name: String, defaultSize: Int): Padding {
        val defaultSize2 = getSize(name, defaultSize)
        return Padding(
            getSize("$name.left", defaultSize2),
            getSize("$name.top", defaultSize2),
            getSize("$name.right", defaultSize2),
            getSize("$name.bottom", defaultSize2)
        )
    }

    fun getFullName(name: String): String {
        return append(prefix, name, suffix)
    }

    private fun append(x: String?, y: String, z: String?): String {
        return append(append(x, y), z)
    }

    fun append(x: String?, y: String?): String {
        return Strings.append(x, ".", y)!!
    }

    val children = HashMap<String, Style>()

    fun getStyle(name: String): Style {
        val child = Style(prefix, append(name, suffix))
        child.values = values
        return child
    }

    fun getChild(space: String): Style {
        val cached = children[space]
        if (cached != null) return cached
        val child = Style(append(prefix, space), suffix)
        child.values = values
        children[space] = child
        return child
    }
}