package me.anno.ui.editor.config

import me.anno.animation.Type
import me.anno.config.DefaultConfig.style
import me.anno.io.files.FileReference
import me.anno.io.utils.StringMap
import me.anno.ui.Panel
import me.anno.ui.base.groups.PanelList
import me.anno.ui.base.text.TextPanel
import me.anno.ui.editor.FontListMenu.createFontInput
import me.anno.ui.input.*
import me.anno.utils.Color.black
import me.anno.utils.Color.rgba
import me.anno.utils.Color.toHexColor
import me.anno.utils.Color.toVecRGBA
import me.anno.utils.ColorParsing.parseColor
import me.anno.utils.strings.StringHelper.camelCaseToTitle
import org.apache.logging.log4j.LogManager
import org.joml.Vector4f

class ContentCreator(
    val fullName: String, val relevantName: String,
    val depth: Int, val groupName: String,
    val shortName: String, val map: StringMap
) {

    companion object {
        @JvmStatic
        private val LOGGER = LogManager.getLogger(ContentCreator::class)
    }

    fun createPanels(list: PanelList) {
        val shortTitle = shortName.camelCaseToTitle()
        val title = TextPanel(shortTitle, style)
        val pad = title.font.sizeInt
        when (val value = map[fullName]!!) {
            is Boolean -> {
                list += BooleanInput(shortTitle, value, false, style)
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
                        if (fullName.contains("fontName") || fullName == "font") {
                            // there is only a certain set of values available
                            createFontInput(value, style) {
                                map[fullName] = it
                            }
                        } else {
                            when (shortName) {
                                "background", "color", "textColor" -> {
                                    ColorInput(style, "", "", (parseColor(value) ?: black).toVecRGBA(), true)
                                        .setChangeListener { r, g, b, a, _ ->
                                            map[fullName] = Vector4f(r, g, b, a).toHexColor()
                                        }
                                }
                                else -> {
                                    TextInput("", "", false, style)
                                        .setValue(value, -1, false)
                                        .addChangeListener { map[fullName] = it }
                                }
                            }
                        }
                    }
                    is FileReference -> {
                        FileInput("", style, value, emptyList())
                            .setChangeListener { map[fullName] = it }
                    }
                    is Int -> {
                        if (value.shr(24).and(255) > 100) {
                            // a color
                            ColorInput(style, "", "", value.toVecRGBA(), true)
                                .setChangeListener { r, g, b, a, _ -> map[fullName] = rgba(r, g, b, a) }
                        } else {
                            IntInput("", "", value, style)
                                .setChangeListener { map[fullName] = it.toInt() }
                        }
                    }
                    is Long -> IntInput("", "", value, style)
                        .setChangeListener { map[fullName] = it }
                    is Float -> FloatInput("", "", value, Type.FLOAT, style)
                        .setChangeListener { map[fullName] = it.toFloat() }
                    is Double -> FloatInput("", "", value, Type.DOUBLE, style)
                        .setChangeListener { map[fullName] = it }
                    else -> {
                        LOGGER.warn("Missing type implementation ${value.javaClass.simpleName}")
                        // ComponentUI.vi(null, shortTitle, fullName, null, value, style) { map[fullName] = value }
                        return
                    }
                }
                list += body.withPadding(pad * 2, 0, 0, 0)
            }
        }
    }
}