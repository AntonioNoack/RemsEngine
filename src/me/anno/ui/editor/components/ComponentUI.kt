package me.anno.ui.editor.components

import me.anno.animation.AnimatedProperty
import me.anno.animation.Type
import me.anno.gpu.blending.BlendMode
import me.anno.gpu.blending.blendModes
import me.anno.gpu.texture.Clamping
import me.anno.gpu.texture.Filtering
import me.anno.io.files.FileReference
import me.anno.language.Language
import me.anno.language.translation.NameDesc
import me.anno.objects.Transform
import me.anno.objects.effects.MaskType
import me.anno.objects.effects.ToneMappers
import me.anno.objects.modes.*
import me.anno.studio.rems.RemsStudio
import me.anno.ui.base.Panel
import me.anno.ui.input.*
import me.anno.ui.style.Style
import me.anno.utils.Color.toHexColor
import me.anno.utils.structures.ValueWithDefault
import me.anno.utils.structures.ValueWithDefaultFunc
import org.joml.Quaternionf
import org.joml.Vector2f
import org.joml.Vector3f
import org.joml.Vector4f

object ComponentUI {

    /**
     * creates a panel with the correct input for the type, and sets the default values:
     * title, tool tip text, type, start value
     * callback is used to adjust the value
     * */
    @Suppress("UNCHECKED_CAST") // all casts are checked in all known use-cases ;)
    fun <V> vi(
        self: Transform,
        title: String, ttt: String,
        type: Type?, value: V,
        style: Style, setValue: (V) -> Unit
    ): Panel {
        return when (value) {
            is Boolean -> BooleanInput(title, value, type?.defaultValue as? Boolean ?: false, style)
                .setChangeListener {
                    RemsStudio.largeChange("Set $title to $it") {
                        setValue(it as V)
                    }
                }
                .setIsSelectedListener { self.show(null) }
                .setTooltip(ttt)
            is Int -> IntInput(title, value, type ?: Type.INT, style)
                .setChangeListener {
                    RemsStudio.incrementalChange("Set $title to $it", title) {
                        setValue(it.toInt() as V)
                    }
                }
                .setIsSelectedListener { self.show(null) }
                .setTooltip(ttt)
            is Long -> IntInput(title, value, type ?: Type.LONG, style)
                .setChangeListener {
                    RemsStudio.incrementalChange("Set $title to $it", title) {
                        setValue(it as V)
                    }
                }
                .setIsSelectedListener { self.show(null) }
                .setTooltip(ttt)
            is Float -> FloatInput(title, value, type ?: Type.FLOAT, style)
                .setChangeListener {
                    RemsStudio.incrementalChange("Set $title to $it", title) {
                        setValue(it.toFloat() as V)
                    }
                }
                .setIsSelectedListener { self.show(null) }
                .setTooltip(ttt)
            is Double -> FloatInput(title, value, type ?: Type.DOUBLE, style)
                .setChangeListener {
                    RemsStudio.incrementalChange("Set $title to $it", title) {
                        setValue(it as V)
                    }
                }
                .setIsSelectedListener { self.show(null) }
                .setTooltip(ttt)
            is Vector2f -> VectorInput(style, title, value, type ?: Type.VEC2)
                .setChangeListener { x, y, _, _ ->
                    RemsStudio.incrementalChange("Set $title to ($x,$y)", title) {
                        setValue(Vector2f(x.toFloat(), y.toFloat()) as V)
                    }
                }
                .setIsSelectedListener { self.show(null) }
                .setTooltip(ttt)
            is Vector3f ->
                if (type == Type.COLOR3) {
                    ColorInput(style, title, Vector4f(value, 1f), false, null)
                        .setChangeListener { r, g, b, _ ->
                            RemsStudio.incrementalChange("Set $title to ${Vector3f(r, g, b).toHexColor()}", title) {
                                setValue(Vector3f(r, g, b) as V)
                            }
                        }
                        .setIsSelectedListener { self.show(null) }
                        .setTooltip(ttt)
                } else {
                    VectorInput(style, title, value, type ?: Type.VEC3)
                        .setChangeListener { x, y, z, _ ->
                            RemsStudio.incrementalChange("Set $title to ($x,$y,$z)", title) {
                                setValue(Vector3f(x.toFloat(), y.toFloat(), z.toFloat()) as V)
                            }
                        }
                        .setIsSelectedListener { self.show(null) }
                        .setTooltip(ttt)
                }
            is Vector4f -> {
                if (type == null || type == Type.COLOR) {
                    ColorInput(style, title, value, true, null)
                        .setChangeListener { r, g, b, a ->
                            RemsStudio.incrementalChange("Set $title to ${Vector4f(r, g, b, a).toHexColor()}", title) {
                                setValue(Vector4f(r, g, b, a) as V)
                            }
                        }
                        .setIsSelectedListener { self.show(null) }
                        .setTooltip(ttt)
                } else {
                    VectorInput(style, title, value, type)
                        .setChangeListener { x, y, z, w ->
                            RemsStudio.incrementalChange("Set $title to ($x,$y,$z,$w)", title) {
                                setValue(Vector4f(x.toFloat(), y.toFloat(), z.toFloat(), w.toFloat()) as V)
                            }
                        }
                        .setIsSelectedListener { self.show(null) }
                        .setTooltip(ttt)
                }
            }
            is Quaternionf -> VectorInput(style, title, value, type ?: Type.QUATERNION)
                .setChangeListener { x, y, z, w ->
                    RemsStudio.incrementalChange(title) {
                        setValue(Quaternionf(x, y, z, w) as V)
                    }
                }
                .setIsSelectedListener { self.show(null) }
                .setTooltip(ttt)
            is String -> TextInputML(title, style, value)
                .setChangeListener {
                    RemsStudio.incrementalChange("Set $title to \"$it\"", title) {
                        setValue(it as V)
                    }
                }
                .setIsSelectedListener { self.show(null) }
                .setTooltip(ttt)
            is FileReference -> FileInput(title, style, value)
                .setChangeListener {
                    RemsStudio.incrementalChange("Set $title to \"$it\"", title) {
                        setValue(it as V)
                    }
                }
                .setIsSelectedListener { self.show(null) }
                .setTooltip(ttt)
            is BlendMode -> {
                val values = blendModes.values
                val valueNames = values.map { it to it.naming }
                EnumInput(
                    title, true, valueNames.first { it.first == value }.second.name,
                    valueNames.map { it.second }, style
                )
                    .setChangeListener { name, index, _ ->
                        RemsStudio.incrementalChange("Set $title to $name", title) {
                            setValue(valueNames[index].first as V)
                        }
                    }
                    .setIsSelectedListener { self.show(null) }
                    .setTooltip(ttt)
            }
            is Enum<*> -> {
                val values = when (value) {
                    is LoopingState -> LoopingState.values()
                    is ToneMappers -> ToneMappers.values()
                    is MaskType -> MaskType.values()
                    is Filtering -> Filtering.values()
                    is ArraySelectionMode -> ArraySelectionMode.values()
                    is UVProjection -> UVProjection.values()
                    is Clamping -> Clamping.values()
                    is TransformVisibility -> TransformVisibility.values()
                    is TextRenderMode -> TextRenderMode.values()
                    is Language -> Language.values()
                    else -> throw RuntimeException("Missing enum .values() implementation for UI in Transform.kt for $value")
                }
                val valueNames: List<Pair<Any, NameDesc>> = values.map {
                    it to when (it) {
                        is LoopingState -> it.naming
                        is ToneMappers -> it.naming
                        is MaskType -> it.naming
                        is Filtering -> it.naming
                        is ArraySelectionMode -> it.naming
                        is UVProjection -> it.naming
                        is Clamping -> it.naming
                        is TransformVisibility -> it.naming
                        is TextRenderMode -> it.naming
                        is Language -> it.naming
                        else -> NameDesc(it.name, "", "")
                    }
                }
                EnumInput(
                    title, true, valueNames.first { it.first == value }.second.name,
                    valueNames.map { it.second }, style
                )
                    .setChangeListener { name, index, _ ->
                        RemsStudio.incrementalChange("Set $title to $name") {
                            setValue(values[index] as V)
                        }
                    }
                    .setIsSelectedListener { self.show(null) }
                    .setTooltip(ttt)
            }
            is ValueWithDefaultFunc<*>, is ValueWithDefault<*> -> throw IllegalArgumentException("Must pass value, not ValueWithDefault(Func)!")
            else -> throw RuntimeException("Type $value not yet implemented!")
        }
    }

    /**
     * creates a panel with the correct input for the type, and sets the default values:
     * title, tool tip text, type, start value
     * modifies the AnimatedProperty-Object, so no callback is needed
     * */
    fun vi(
        self: Transform,
        title: String, ttt: String, values: AnimatedProperty<*>, style: Style
    ): Panel {
        val time = self.lastLocalTime
        val sl = { self.show(values) }
        return when (val value = values[time]) {
            is Int -> IntInput(title, values, 0, time, style)
                .setChangeListener {
                    RemsStudio.incrementalChange("Set $title to $it", title) {
                        self.putValue(values, it.toInt(), false)
                    }
                }
                .setIsSelectedListener(sl)
                .setTooltip(ttt)
            is Long -> IntInput(title, values, 0, time, style)
                .setChangeListener {
                    RemsStudio.incrementalChange("Set $title to $it", title) {
                        self.putValue(values, it, false)
                    }
                }
                .setIsSelectedListener { self.show(values) }
                .setTooltip(ttt)
            is Float -> FloatInput(title, values, 0, time, style)
                .setChangeListener {
                    RemsStudio.incrementalChange("Set $title to $it", title) {
                        self.putValue(values, it.toFloat(), false)
                    }
                }
                .setIsSelectedListener(sl)
                .setTooltip(ttt)
            is Double -> FloatInput(title, values, 0, time, style)
                .setChangeListener {
                    RemsStudio.incrementalChange("Set $title to $it", title) {
                        self.putValue(values, it, false)
                    }
                }
                .setIsSelectedListener(sl)
                .setTooltip(ttt)
            is Vector2f -> VectorInput(title, values, time, style)
                .setChangeListener { x, y, _, _ ->
                    RemsStudio.incrementalChange("Set $title to ($x,$y)", title) {
                        self.putValue(values, Vector2f(x.toFloat(), y.toFloat()), false)
                    }
                }
                .setIsSelectedListener(sl)
                .setTooltip(ttt)
            is Vector3f ->
                if (values.type == Type.COLOR3) {
                    ColorInput(style, title, Vector4f(value, 1f), false, values)
                        .setChangeListener { r, g, b, _ ->
                            RemsStudio.incrementalChange("Set $title to ${Vector3f(r, g, b).toHexColor()}", title) {
                                self.putValue(values, Vector3f(r, g, b), false)
                            }
                        }
                        .setIsSelectedListener(sl)
                        .setTooltip(ttt)
                } else {
                    VectorInput(title, values, time, style)
                        .setChangeListener { x, y, z, _ ->
                            RemsStudio.incrementalChange("Set $title to ($x,$y,$z)", title) {
                                self.putValue(values, Vector3f(x.toFloat(), y.toFloat(), z.toFloat()), false)
                            }
                        }
                        .setIsSelectedListener(sl)
                        .setTooltip(ttt)
                }
            is Vector4f -> {
                if (values.type == Type.COLOR) {
                    ColorInput(style, title, value, true, values)
                        .setChangeListener { r, g, b, a ->
                            RemsStudio.incrementalChange("Set $title to ${Vector4f(r, g, b, a).toHexColor()}", title) {
                                self.putValue(values, Vector4f(r, g, b, a), false)
                            }
                        }
                        .setIsSelectedListener(sl)
                        .setTooltip(ttt)
                } else {
                    VectorInput(title, values, time, style)
                        .setChangeListener { x, y, z, w ->
                            RemsStudio.incrementalChange("Set $title to ($x,$y,$z,$w)", title) {
                                self.putValue(
                                    values,
                                    Vector4f(x.toFloat(), y.toFloat(), z.toFloat(), w.toFloat()),
                                    false
                                )
                            }
                        }
                        .setIsSelectedListener(sl)
                        .setTooltip(ttt)
                }
            }
            is String -> TextInputML(title, style, value)
                .setChangeListener {
                    RemsStudio.incrementalChange("Set $title to $it") {
                        self.putValue(values, it, false)
                    }
                }
                .setIsSelectedListener(sl)
                .setTooltip(ttt)
            is Quaternionf -> VectorInput(title, values, time, style)
                .setChangeListener { x, y, z, w ->
                    RemsStudio.incrementalChange("Set $title to ($x,$y,$z,$w)", title) {
                        self.putValue(values, Quaternionf(x, y, z, w), false)
                    }
                }
                .setIsSelectedListener(sl)
                .setTooltip(ttt)
            else -> throw RuntimeException("Type $value not yet implemented!")
        }
    }

}