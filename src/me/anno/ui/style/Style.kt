package me.anno.ui.style

import me.anno.config.DefaultStyle.black
import me.anno.io.utils.StringMap
import me.anno.utils.warn

class Style(val prefix: String?, val suffix: String?){

    var values = StringMap()

    operator fun set(key: String, theme: String, value: Any){
        values["$key.$theme"] = value
    }

    private fun getValue(name: String, defaultValue: Int) = getValue(name, name, defaultValue)
    private fun getValue(fullName: String, name: String, defaultValue: Int): Int {
        val value = values[name]
        return if(value != null) getMaybe(fullName, value, defaultValue)
        else {

            warn("Missing config/style/$name")

            val index = name.indexOf('.')
            val index2 = name.indexOf('.', index+1)
            if(index2 > -1){
                val lessSpecificName = name.substring(index+1)
                getValue(fullName, lessSpecificName, defaultValue)
            } else {
                values[name] = defaultValue
                defaultValue
            }

        }
    }

    private fun getValue(name: String, defaultValue: String) = getValue(name, name, defaultValue)
    private fun getValue(fullName: String, name: String, defaultValue: String): String {
        val value = values[name]
        return if(value != null) value.toString()
        else {

            warn("Missing config/style/$name")

            val index = name.indexOf('.')
            val index2 = name.indexOf('.', index+1)
            if(index2 > -1){
                val lessSpecificName = name.substring(index+1)
                getValue(fullName, lessSpecificName, defaultValue)
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
        return when(value){
            is Int -> value
            is Boolean -> if(value) 1 else 0
            is Float -> value.toInt()
            is String -> {
                val hex = value.toIntOrNull(16)
                hex ?: if(value.startsWith("#")){
                    val colorValue = when(value.length){
                        9 -> value.substring(1).toIntOrNull()
                        7 -> value.substring(1).toIntOrNull()?.or(black)
                        5 -> value.substring(1).toIntOrNull()?.int4ToInt8()
                        4 -> value.substring(1).toIntOrNull()?.int4ToInt8()?.or(black)
                        else -> null
                    }
                    if(colorValue == null){
                        values[fullName] = defaultValue
                        defaultValue
                    } else colorValue
                } else {
                    println("invalid value! $value for int/color")
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

    fun getBoolean(name: String, defaultValue: Boolean) = getValue(getFullName(name), if(defaultValue) 1 else 0) != 0
    fun getSize(name: String, defaultValue: Int): Int = getValue(getFullName(name), defaultValue)
    fun getColor(name: String, defaultValue: Int): Int = getValue(getFullName(name), defaultValue)
    fun getString(name: String, defaultValue: String): String = getValue(getFullName(name), defaultValue)

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
        if(cached != null) return cached
        val child = Style(append(space, prefix), suffix)
        child.values = values
        children[space] = child
        return child
    }

}