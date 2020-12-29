package me.anno.ui.editor.config

import me.anno.config.DefaultConfig.style
import me.anno.config.DefaultStyle.black
import me.anno.io.utils.StringMap
import me.anno.objects.animation.Type
import me.anno.studio.rems.RemsStudio.root
import me.anno.ui.base.Panel
import me.anno.ui.base.TextPanel
import me.anno.ui.base.groups.PanelList
import me.anno.ui.input.*
import me.anno.utils.Color.rgba
import me.anno.utils.Color.toHexColor
import me.anno.utils.Color.toVecRGBA
import me.anno.utils.ColorParsing.parseColor
import org.joml.Vector4f

class ContentCreator(
    val fullName: String, val relevantName: String,
    val depth: Int, val groupName: String, val shortName: String, val map: StringMap
) {

    fun createPanels(list: PanelList) {
        val title = TextPanel(shortName, style)
        val pad = title.font.size.toInt()
        when (val value = map[fullName]!!) {
            is Boolean -> {
                list += BooleanInput(shortName, value, style)
                    .setChangeListener { map[fullName] = it }
                    .withPadding(pad, 0, 0, 0)
            }
            /*is Enum<*> -> {
                list += root.VI(shortName, fullName, null, value, style) { map[fullName] = value }
                    .withPadding(pad, 0, 0, 0)
            }*/
            else -> {
                list += title.setTooltip(fullName).withPadding(pad, 0, 0, 0)
                val body: Panel = when (value) {
                    is String -> {
                        when(shortName){
                            "background", "color", "textColor" -> {
                                // todo define size with buttons...
                                ColorInput(style, shortName, (parseColor(value) ?: black).toVecRGBA(), true)
                                    .setChangeListener { r, g, b, a -> map[fullName] = Vector4f(r,g,b,a).toHexColor() }
                                    .noTitle()
                            }
                            else -> {
                                val panel = TextInput(shortName, false, style)
                                panel.setText(value, false)
                                // panel.setText(value)
                                panel.setChangeListener { map[fullName] = it }
                                // panel.changeListener = { map[fullName] = it }
                                panel
                            }
                        }
                    }
                    is Int -> {
                        if (value.shr(24).and(255) > 100) {
                            // a color
                            ColorInput(style, shortName, value.toVecRGBA(), true)
                                .setChangeListener { r, g, b, a -> map[fullName] = rgba(r, g, b, a) }
                                .noTitle()
                        } else {
                            IntInput(shortName, value, style)
                                .setChangeListener { map[fullName] = it.toInt() }
                                .noTitle()
                        }
                    }
                    is Long -> IntInput(shortName, value, style)
                        .setChangeListener { map[fullName] = it }
                        .noTitle()
                    is Float -> FloatInput(shortName, value, Type.FLOAT, style)
                        .setChangeListener { map[fullName] = it.toFloat() }
                        .noTitle()
                    is Double -> FloatInput(shortName, value, Type.DOUBLE, style)
                        .setChangeListener { map[fullName] = it }
                        .noTitle()
                    else -> {
                        println("Missing type implementation ${value.javaClass.simpleName}")
                        root.vi(shortName, fullName, null, value, style) { map[fullName] = value }
                    }
                }
                list += body.withPadding(pad * 2, 0, 0, 0)
            }
        }

    }

}