package me.anno.ui.style

import me.anno.config.DefaultConfig
import me.anno.utils.Color.black
import me.anno.io.utils.StringMap
import me.anno.ui.base.Font
import me.anno.ui.base.components.Padding
import org.apache.logging.log4j.LogManager

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
        return if (value != null) getMaybe(fullName, value, defaultValue)
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

    private fun getValue(name: String, defaultValue: Float) = getValue(name, name, defaultValue)
    private fun getValue(fullName: String, name: String, defaultValue: Float): Float {

        val value = values[name]
        return if (value != null) getMaybe(fullName, value, defaultValue)
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

    private fun getValue(name: String, defaultValue: String) = getValue2(name, defaultValue)
    private fun getValue2(name: String, defaultValue: String): String {
        val value = values[name]
        return if (value != null) value.toString()
        else {

            // warn("Missing config/style/$name")

            val index = name.indexOf('.')
            val index2 = name.indexOf('.', index + 1)
            if (index2 > -1) {
                val lessSpecificName = name.substring(index + 1)
                getValue2(lessSpecificName, defaultValue)
            } else {
                values[name] = defaultValue
                defaultValue
            }

        }
    }

    private fun Int.int4ToInt8(): Int {
        val a = shr(12).and(15)
        val r = shr(8).and(15)
        val g = shr(4).and(15)
        val b = and(15)
        return (a * 0x11).shl(24) + (r * 0x11).shl(16) + (g * 0x11).shl(8) + (b * 0x11)
    }

    private fun getMaybe(fullName: String, value: Any, defaultValue: Int): Int {
        return when (value) {
            is Int -> value
            is Boolean -> if (value) 1 else 0
            is Float -> value.toInt()
            is String -> {
                val hex = value.toIntOrNull(16)
                hex ?: if (value.startsWith("#")) {
                    val colorValue = when (value.length) {
                        9 -> value.substring(1).toIntOrNull()
                        7 -> value.substring(1).toIntOrNull()?.or(black)
                        5 -> value.substring(1).toIntOrNull()?.int4ToInt8()
                        4 -> value.substring(1).toIntOrNull()?.int4ToInt8()?.or(black)
                        else -> null
                    }
                    if (colorValue == null) {
                        values[fullName] = defaultValue
                        defaultValue
                    } else colorValue
                } else {
                    LOGGER.warn("invalid value! $value for int/color")
                    values[fullName] = defaultValue
                    defaultValue
                }
            }
            else -> {
                values[fullName] = defaultValue
                defaultValue
            }
        }
    }

    private fun getMaybe(fullName: String, value: Any, defaultValue: Float): Float {
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
    fun getFont(name: String, defaultValue: Font = DefaultConfig.defaultFont): Font {
        val type = getString("$name.fontName", defaultValue.name)
        val size = getSize("$name.fontSize", defaultValue.size.toInt()).toFloat()
        val isBold = getBoolean("$name.fontBold", defaultValue.isBold)
        val isItalic = getBoolean("$name.fontItalic", defaultValue.isItalic)
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
        return when {
            x == null && z == null -> y
            x == null -> "$y.$z"
            z == null -> "$x.$y"
            else -> "$x.$y.$z"
        }
    }

    fun append(x: String?, y: String?): String {
        return when {
            x == null -> y!!
            y == null -> x
            else -> "$x.$y"
        }
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

    companion object {
        @JvmStatic
        private val LOGGER = LogManager.getLogger(Style::class.java)
    }

}