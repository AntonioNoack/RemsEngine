package me.anno.ui.editor.config

import me.anno.config.DefaultConfig.style
import me.anno.io.files.FileReference
import me.anno.io.utils.StringMap
import me.anno.language.translation.NameDesc
import me.anno.ui.Panel
import me.anno.ui.base.components.AxisAlignment
import me.anno.ui.base.groups.PanelContainer.Companion.withPadding
import me.anno.ui.base.groups.PanelList
import me.anno.ui.base.menu.Menu
import me.anno.ui.base.text.TextPanel
import me.anno.ui.editor.FontListMenu.createFontInput
import me.anno.ui.input.BooleanInput
import me.anno.ui.input.ColorInput
import me.anno.ui.input.FileInput
import me.anno.ui.input.FloatInput
import me.anno.ui.input.IntInput
import me.anno.ui.input.NumberType
import me.anno.ui.input.TextInput
import me.anno.utils.Color.a
import me.anno.utils.Color.black
import me.anno.utils.Color.rgba
import me.anno.utils.Color.toHexColor
import me.anno.utils.Color.toVecRGBA
import me.anno.utils.ColorParsing.parseColor
import me.anno.utils.types.Strings.camelCaseToTitle
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
        title.addRightClickListener {
            Menu.ask(list.windowStack, NameDesc("Delete/Reset Value?")) {
                map.remove(fullName)
                title.text = "// $shortTitle"
            }
        }
        val pad = title.font.sizeInt
        val value = map[fullName]!!
        if (value is Boolean) {
            list += createInputPanel(list)!!
                .withPadding(pad, 0, 0, 0)
        } else {
            list += title.setTooltip(fullName).withPadding(pad, 0, 0, 0)
            val body: Panel = createInputPanel(list) ?: return
            // else takes up comically lot space
            body.forAllPanels { it.alignmentX = AxisAlignment.MIN }
            list += body.withPadding(pad * 2, 0, 0, 0)
        }
    }

    fun createInputPanel(panelForWindowStack: Panel): Panel? {
        val shortTitle = shortName.camelCaseToTitle()
        return when (val value = map[fullName]!!) {
            is Boolean -> {
                val title = TextPanel(shortTitle, style)
                title.addRightClickListener {
                    Menu.ask(panelForWindowStack.windowStack, NameDesc("Delete/Reset Value?")) {
                        map.remove(fullName)
                        title.text = "// $shortTitle"
                    }
                }
                BooleanInput(NameDesc(shortTitle), value, false, style)
                    .setChangeListener { map[fullName] = it }
                    .withPadding(title.font.sizeInt, 0, 0, 0)
            }
            is String -> {
                if (fullName.contains("fontName") || fullName == "font") {
                    // there is only a certain set of values available
                    createFontInput(value, style) {
                        map[fullName] = it
                    }
                } else {
                    when (shortName) {
                        "background", "color", "textColor" -> {
                            ColorInput(
                                NameDesc.EMPTY,
                                "",
                                (parseColor(value) ?: black).toVecRGBA(),
                                true,
                                style
                            )
                                .setChangeListener { r, g, b, a, _ ->
                                    map[fullName] = Vector4f(r, g, b, a).toHexColor()
                                }
                        }
                        else -> {
                            TextInput(NameDesc.EMPTY, "", false, style)
                                .setValue(value, -1, false)
                                .addChangeListener { map[fullName] = it }
                                .setPlaceholder("Value")
                        }
                    }
                }
            }
            is FileReference -> {
                FileInput(NameDesc.EMPTY, style, value, emptyList())
                    .apply { base.setPlaceholder(shortTitle) }
                    .addChangeListener { map[fullName] = it }
            }
            is Int -> {
                if (value.a() > 100 || "Color" in fullName || fullName.endsWith("background")) {
                    // a color
                    ColorInput(NameDesc.EMPTY, "", value.toVecRGBA(), true, style)
                        .setChangeListener { r, g, b, a, _ -> map[fullName] = rgba(r, g, b, a) }
                } else {
                    IntInput(NameDesc.EMPTY, "", value, style)
                        .setChangeListener { map[fullName] = it.toInt() }
                        .setPlaceholder("Value")
                }
            }
            is Long -> IntInput(NameDesc.EMPTY, "", value, style)
                .setChangeListener { map[fullName] = it }
                .setPlaceholder("Value")
            is Float -> FloatInput(NameDesc.EMPTY, "", value, NumberType.FLOAT, style)
                .setChangeListener { map[fullName] = it.toFloat() }
                .setPlaceholder("Value")
            is Double -> FloatInput(NameDesc.EMPTY, "", value, NumberType.DOUBLE, style)
                .setChangeListener { map[fullName] = it }
                .setPlaceholder("Value")
            else -> {
                LOGGER.warn("Missing type implementation ${value::class}")
                // ComponentUI.vi(null, shortTitle, fullName, null, value, style) { map[fullName] = value }
                return null
            }
        }
    }
}