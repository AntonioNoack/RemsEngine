package me.anno.engine

import me.anno.animation.Type
import me.anno.ecs.Component
import me.anno.ecs.annotations.Range.Companion.maxByte
import me.anno.ecs.annotations.Range.Companion.maxDouble
import me.anno.ecs.annotations.Range.Companion.maxFloat
import me.anno.ecs.annotations.Range.Companion.maxInt
import me.anno.ecs.annotations.Range.Companion.maxLong
import me.anno.ecs.annotations.Range.Companion.maxShort
import me.anno.ecs.annotations.Range.Companion.minByte
import me.anno.ecs.annotations.Range.Companion.minDouble
import me.anno.ecs.annotations.Range.Companion.minFloat
import me.anno.ecs.annotations.Range.Companion.minInt
import me.anno.ecs.annotations.Range.Companion.minLong
import me.anno.ecs.annotations.Range.Companion.minShort
import me.anno.io.serialization.CachedProperty
import me.anno.language.translation.NameDesc
import me.anno.ui.base.Panel
import me.anno.ui.base.menu.Menu
import me.anno.ui.base.menu.MenuOption
import me.anno.ui.base.text.TextPanel
import me.anno.ui.input.*
import me.anno.ui.style.Style
import me.anno.utils.Maths
import me.anno.utils.strings.StringHelper
import me.anno.utils.strings.StringHelper.titlecase
import org.joml.*
import org.joml.Math.toDegrees
import org.joml.Math.toRadians

object ComponentUI {

    /**
     * a massive function, which provides editors for all types of variables
     * */
    fun createUI(name: String, property: CachedProperty, c: Component, style: Style): Panel? {

        if (property.hideInInspector) return null
        val range = property.range
        val title = StringHelper.splitCamelCase(name.titlecase())
        val ttt = "" // we could use annotations for that :)
        when (val value = property.getter.call(c)) {// todo better, not sample-depending check for the type
            // native types
            is Boolean -> return BooleanInput(title, ttt, value, false, style)
                // reset listener / option to reset it
                .apply {
                    setResetListener { c.resetProperty(name) as Boolean }
                    askForReset(name, c) { setValue(it as Boolean, false) }
                    setChangeListener {
                        property.set(c, it)
                        c.changedPropertiesInInstance.add(name)
                    }
                }
            // todo char
            // todo unsigned types
            is Byte -> {
                val type = Type(c.getDefaultValue(name) as Byte,
                    { Maths.clamp(it.toLong(), range.minByte().toLong(), range.maxByte().toLong()).toByte() }, { it })
                return IntInput(style, title, type, null, 0)
                    .apply {
                        setValue(value.toInt(), false)
                        askForReset(name, c) { setValue((it as Byte).toInt(), false) }
                        setResetListener { c.resetProperty(name).toString() }
                        setChangeListener {
                            property.set(c, it.toByte())
                            c.changedPropertiesInInstance.add(name)
                        }
                    }
            }
            is Short -> {
                val type = Type(c.getDefaultValue(name) as Short,
                    { Maths.clamp(it.toLong(), range.minShort().toLong(), range.maxShort().toLong()).toShort() },
                    { it })
                return IntInput(style, title, type, null, 0)
                    .apply {
                        setValue(value.toInt(), false)
                        askForReset(name, c) { setValue((it as Short).toInt(), false) }
                        setResetListener { c.resetProperty(name).toString() }
                        setChangeListener {
                            property.set(c, it.toShort())
                            c.changedPropertiesInInstance.add(name)
                        }
                    }
            }
            is Int -> {
                val type = Type(c.getDefaultValue(name) as Int,
                    { Maths.clamp(it.toLong(), range.minInt().toLong(), range.maxInt().toLong()).toInt() }, { it })
                return IntInput(style, title, type, null, 0)
                    .apply {
                        setValue(value, false)
                        askForReset(name, c) { setValue(it as Int, false) }
                        setResetListener { c.resetProperty(name).toString() }
                        setChangeListener {
                            property.set(c, it.toInt())
                            c.changedPropertiesInInstance.add(name)
                        }
                    }
            }
            is Long -> {
                val type = Type(c.getDefaultValue(name) as Long,
                    { Maths.clamp(it.toLong(), range.minLong(), range.maxLong()) }, { it })
                return IntInput(style, title, type, null, 0)
                    .apply {
                        setValue(value, false)
                        askForReset(name, c) { setValue(it as Long, false) }
                        setResetListener { c.resetProperty(name).toString() }
                        setChangeListener {
                            property.set(c, it.toInt())
                            c.changedPropertiesInInstance.add(name)
                        }
                    }

            }
            is Float -> {
                val type = Type(c.getDefaultValue(name) as Float,
                    { Maths.clamp(it as Float, range.minFloat(), range.maxFloat()) }, { it })
                return FloatInput(style, title, type, null, 0)
                    .apply {
                        setValue(value, false)
                        setResetListener { c.resetProperty(name).toString() }
                        askForReset(name, c) { setValue(it as Float, false) }
                        setChangeListener {
                            property.set(c, it.toFloat())
                            c.changedPropertiesInInstance.add(name)
                        }
                    }
            }
            is Double -> {
                val type = Type(c.getDefaultValue(name) as Double,
                    { Maths.clamp(it as Double, range.minDouble(), range.maxDouble()) }, { it })
                return FloatInput(style, title, type, null, 0)
                    .apply {
                        setValue(value, false)
                        setResetListener { c.resetProperty(name).toString() }
                        askForReset(name, c) { setValue(it as Double, false) }
                        setChangeListener {
                            property.set(c, it)
                            c.changedPropertiesInInstance.add(name)
                        }
                    }
            }
            is String -> {
                return TextInput(title, style, value)
                    .apply {
                        setResetListener { c.resetProperty(name).toString() }
                        askForReset(name, c) { setValue(it as String, false) }
                        setChangeListener {
                            property.set(c, it)
                            c.changedPropertiesInInstance.add(name)
                        }
                    }
            }

            // float vectors
            // todo ranges for vectors (?)
            is Vector2f -> {
                return VectorInput(style, title, value, Type.VEC2)
                    .apply {
                        // todo reset listener
                        askForReset(name, c) { setValue(it as Vector2f, false) }
                        setChangeListener { x, y, _, _ ->
                            value.set(x, y)
                        }
                    }
            }
            is Vector3f -> {
                return VectorInput(style, title, value, Type.VEC3)
                    .apply {
                        // todo reset listener
                        askForReset(name, c) { setValue(it as Vector3f, false) }
                        setChangeListener { x, y, z, _ ->
                            value.set(x, y, z)
                        }
                    }
            }
            is Vector4f -> {
                return VectorInput(style, title, value, Type.VEC4)
                    .apply {
                        // todo reset listener
                        askForReset(name, c) { setValue(it as Vector4f, false) }
                        setChangeListener { x, y, z, w ->
                            value.set(x, y, z, w)
                        }
                    }
            }
            is Vector2d -> {
                return VectorInput(style, title, value, Type.VEC2D)
                    .apply {
                        // todo reset listener
                        askForReset(name, c) { setValue(it as Vector2d, false) }
                        setChangeListener { x, y, _, _ ->
                            value.set(x, y)
                        }
                    }
            }
            is Vector3d -> {
                return VectorInput(style, title, value, Type.VEC3D)
                    .apply {
                        // todo reset listener
                        askForReset(name, c) { setValue(it as Vector3d, false) }
                        setChangeListener { x, y, z, _ ->
                            value.set(x, y, z)
                        }
                    }
            }
            is Vector4d -> {
                return VectorInput(style, title, value, Type.VEC4D)
                    .apply {
                        // todo reset listener
                        askForReset(name, c) { setValue(it as Vector4d, false) }
                        setChangeListener { x, y, z, w ->
                            value.set(x, y, z, w)
                        }
                    }
            }

            // int vectors
            is Vector2i -> {
                // todo correct types
                val type = Type(Vector2i(), 2)
                return VectorIntInput(style, title, value, type)
                    .apply {
                        // todo reset listener
                        askForReset(name, c) { setValue(it as Vector2i, false) }
                        setChangeListener { x, y, _, _ ->
                            value.set(x, y)
                        }
                    }
            }
            is Vector3i -> {
                val type = Type(Vector3i(), 3)
                return VectorIntInput(style, title, value, type)
                    .apply {
                        // todo reset listener
                        askForReset(name, c) { setValue(it as Vector3i, false) }
                        setChangeListener { x, y, z, _ ->
                            value.set(x, y, z)
                        }
                    }
            }
            is Vector4i -> {
                val type = Type(Vector4i(), 4)
                return VectorIntInput(style, title, value, type)
                    .apply {
                        // todo reset listener
                        askForReset(name, c) { setValue(it as Vector4i, false) }
                        setChangeListener { x, y, z, w ->
                            value.set(x, y, z, w)
                        }
                    }
            }

            // todo quaternions
            // todo annotation/switch for edit mode: raw input, or via euler angles?
            is Quaternionf -> {
                val d2 = toDegrees(1.0).toFloat()
                return VectorInput(style, title, value.getEulerAnglesXYZ(Vector3f()).mul(d2), Type.ROT_YXZ)
                    .apply {
                        // todo reset listener
                        askForReset(name, c) {
                            it as Quaternionf
                            val vec = it.getEulerAnglesXYZ(Vector3f()).mul(d2)
                            setValue(vec, false)
                        }
                        setChangeListener { x, y, z, _ ->
                            value.set(
                                Quaternionf().rotateYXZ(
                                    toRadians(y).toFloat(),
                                    toRadians(x).toFloat(),
                                    toRadians(z).toFloat()
                                )
                            )
                        }
                    }
            }
            is Quaterniond -> {
                val d2 = toDegrees(1.0)
                return VectorInput(style, title, value.getEulerAnglesXYZ(Vector3d()).mul(d2), Type.ROT_YXZ)
                    .apply {
                        // todo reset listener
                        askForReset(name, c) {
                            it as Quaterniond
                            val vec = it.getEulerAnglesXYZ(Vector3d()).mul(d2)
                            setValue(vec, false)
                        }
                        setChangeListener { x, y, z, _ ->
                            value.set(Quaterniond().rotateYXZ(toRadians(y), toRadians(x), toRadians(z)))
                        }
                    }
            }

            // todo matrices

            // todo edit native arrays (byte/short/int/float/...) as images

            // todo nice ui inputs for array types and maps
            // todo sort list/map by key or property of the users choice
            // todo array & list inputs of any kind
            // todo map inputs: a list of pairs of key & value
            // todo tables for structs?
            // todo show changed values with bold font
            // todo ISaveable-s
            // is ISaveable -> { list all child properties }
            else -> {
                return TextPanel("?? $title", style)
            }
        }

    }

    private fun Panel.askForReset(name: String, c: Component, callback: (Any?) -> Unit): Panel {
        setOnClickListener { _, _, button, _ ->
            if (button.isRight) {
                // todo option to edit the parent... how will that work?
                Menu.openMenu(listOf(MenuOption(NameDesc("Reset")) {
                    callback(c.resetProperty(name))
                }))
            }
        }
        return this
    }

    fun Any?.toLong(): Long {
        return when (this) {
            is Byte -> toLong()
            is Short -> toLong()
            is Int -> toLong()
            is Long -> toLong()
            else -> throw RuntimeException()
        }
    }

}