package me.anno.objects.text

import me.anno.animation.AnimatedProperty
import me.anno.config.DefaultConfig
import me.anno.fonts.FontManager
import me.anno.input.MouseButton
import me.anno.language.translation.NameDesc
import me.anno.studio.rems.RemsStudio
import me.anno.studio.rems.Selection
import me.anno.ui.base.buttons.TextButton
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.base.menu.Menu
import me.anno.ui.base.menu.MenuOption
import me.anno.ui.base.text.TextPanel
import me.anno.ui.editor.SettingCategory
import me.anno.ui.editor.color.spaces.HSLuv
import me.anno.ui.input.BooleanInput
import me.anno.ui.input.EnumInput
import me.anno.ui.style.Style
import org.apache.logging.log4j.LogManager
import org.joml.Vector3f
import org.joml.Vector4f

object TextInspector {

    private val LOGGER = LogManager.getLogger(TextInspector::class)

    fun createFontInput(oldValue: String, style: Style, onChange: (String) -> Unit): EnumInput {
        val fontList = ArrayList<NameDesc>()
        fontList += NameDesc(oldValue)
        fontList += NameDesc(Menu.menuSeparator)

        fun sortFavourites() {
            fontList.sortBy { it.name }
            val lastUsedSet = Text.lastUsedFonts.toHashSet()
            fontList.sortByDescending { if (it.name == Menu.menuSeparator) 1 else if (it.name in lastUsedSet) 2 else 0 }
        }

        FontManager.requestFontList { systemFonts ->
            synchronized(fontList) {
                fontList += systemFonts
                    .filter { it != oldValue }
                    .map { NameDesc(it) }
            }
        }

        // todo Consolas is not correctly centered?

        // todo general favourites for all enum types?
        // todo at least a generalized form to make it simpler?

        return object : EnumInput(
            "Font Name",
            "The style of the text",
            "obj.font.name",
            oldValue, fontList,
            style
        ) {
            /**
             * this menu is overridden, so we can set each font name to its respective font :3
             * this could be a bad idea, if the user has thousands of fonts installed
             * */
            override fun onMouseClicked(x: Float, y: Float, button: MouseButton, long: Boolean) {
                if (DefaultConfig["ui.fonts.previewInEnumInput.enable", true]) {
                    val window = Menu.openMenu(
                        this.x, this.y,
                        NameDesc("Select the %1", "", "ui.input.enum.menuTitle")
                            .with("%1", title),
                        options.mapIndexed { index, option ->
                            MenuOption(option) {
                                inputPanel.text = option.name
                                inputPanel.tooltip = option.desc
                                lastIndex = index
                                changeListener(option.name, index, options)
                            }
                        })
                    if (window != null) {
                        LOGGER.warn("Looking up all fonts, engine might lag")
                        val fontNames = fontList.map { it.englishName }.toSet()
                        window.panel.forAllPanels { panel ->
                            if (panel is TextPanel && panel.text in fontNames) {
                                if (DefaultConfig["ui.fonts.previewInEnumInput.direct", false]) {
                                    // this is expensive
                                    panel.font = panel.font.withName(panel.text)
                                    panel.tooltip = panel.text
                                } else {
                                    val text = "The quick brown fox jumps over the lazy dog."
                                    val clone = panel.clone()
                                    panel.tooltipPanel = clone
                                    clone.font = panel.font
                                        .withName(panel.text)
                                        .withSize(panel.font.size * 2)
                                    clone.text = text
                                }
                            }
                        }
                    }
                } else super.onMouseClicked(x, y, button, long)
            }
        }.setChangeListener { it, _, _ ->
            onChange(it)
            Text.putLastUsedFont(it)
            sortFavourites()
        }
    }

    fun Text.createInspectorWithoutSuperImpl(
        list: PanelListY,
        style: Style,
        getGroup: (title: String, description: String, dictSubPath: String) -> SettingCategory
    ) {

        list += vi("Text", "", "", text, style)
        /*list += TextInputML("Text", style, text[lastLocalTime])
            .setChangeListener {
                RemsStudio.incrementalChange("text") {
                    getSelfWithShadows().forEach { c ->
                        c.putValue(c.text, it, true)
                    }
                }
            }
            .setIsSelectedListener { show(null) }*/


        val fontGroup = getGroup("Font", "", "font")
        fontGroup += createFontInput(font.name, style) {
            RemsStudio.largeChange("Change Font to '$it'") {
                getSelfWithShadows().forEach { c -> c.font = c.font.withName(it) }
            }
            invalidate()
        }.setIsSelectedListener { show(null) }

        fontGroup += BooleanInput("Italic", font.isItalic, false, style)
            .setChangeListener {
                RemsStudio.largeChange("Italic: $it") {
                    getSelfWithShadows().forEach { c -> c.font = c.font.withItalic(it) }
                }
                invalidate()
            }
            .setIsSelectedListener { show(null) }
        fontGroup += BooleanInput("Bold", font.isBold, false, style)
            .setChangeListener {
                RemsStudio.largeChange("Bold: $it") {
                    getSelfWithShadows().forEach { c -> c.font = c.font.withBold(it) }
                }
                invalidate()
            }
            .setIsSelectedListener { show(null) }

        val alignGroup = getGroup("Alignment", "", "alignment")
        fun align(title: String, value: AnimatedProperty<*>) {
            // , xAxis: Boolean, set: (self: Text, AxisAlignment) -> Unit
            alignGroup += vi(title, "", "", value, style)
            /* operator fun AxisAlignment.get(x: Boolean) = if (x) xName else yName
             alignGroup += EnumInput(
                 title, true,
                 value[xAxis],
                 AxisAlignment.values().map { NameDesc(it[xAxis]) }, style
             )
                 .setIsSelectedListener { show(null) }
                 .setChangeListener { name, _, _ ->
                     val alignment = AxisAlignment.values().first { it[xAxis] == name }
                     RemsStudio.largeChange("Set $title to $name") {
                         getSelfWithShadows().forEach { set(it, alignment) }
                     }
                     invalidate()
                 }*/
        }

        align("Text Alignment", textAlignment)//, true)// { self, it -> self.textAlignment = it }
        align("Block Alignment X", blockAlignmentX)//, true)// { self, it -> self.blockAlignmentX = it }
        align("Block Alignment Y", blockAlignmentY)//, false)// { self, it -> self.blockAlignmentY = it }

        val spaceGroup = getGroup("Spacing", "", "spacing")
        // make this element separable from the parent???
        spaceGroup += vi(
            "Character Spacing",
            "Space between individual characters",
            "text.characterSpacing",
            null, relativeCharSpacing, style
        ) {
            RemsStudio.incrementalChange("char space") { relativeCharSpacing = it }
            invalidate()
        }
        spaceGroup += vi(
            "Line Spacing",
            "How much lines are apart from each other",
            "text.lineSpacing",
            relativeLineSpacing, style
        )
        spaceGroup += vi(
            "Tab Size", "Relative tab size, in widths of o's", "text.tabSpacing",
            Text.tabSpaceType, relativeTabSize, style
        ) {
            RemsStudio.incrementalChange("tab size") { relativeTabSize = it }
            invalidate()
        }
        spaceGroup += vi(
            "Line Break Width",
            "How broad the text shall be, at maximum; < 0 = no limit", "text.widthLimit",
            Text.lineBreakType, lineBreakWidth, style
        ) {
            RemsStudio.incrementalChange("line break width") { lineBreakWidth = it }
            invalidate()
        }

        val ops = getGroup("Operations", "", "operations")
        ops += TextButton("Create Shadow", false, style)
            .addLeftClickListener {
                // such a mess is the result of copying colors from the editor ;)
                val signalColor = Vector4f(HSLuv.toRGB(Vector3f(0.000f, 0.934f, 0.591f)), 1f)
                val shadow = clone() as Text
                shadow.name = "Shadow"
                shadow.comment = "Keep \"shadow\" in the name for automatic property inheritance"
                // this avoids user reports, from people, who can't see their shadow
                // making something black should be simple
                shadow.color.set(signalColor)
                shadow.position.set(Vector3f(0.01f, -0.01f, -0.001f))
                shadow.relativeLineSpacing = relativeLineSpacing // evil ;)
                RemsStudio.largeChange("Add Text Shadow") { addChild(shadow) }
                Selection.selectTransform(shadow)
            }

        val rpgEffects = getGroup("RPG Effects", "", "rpg-effects")
        rpgEffects += vi("Start Cursor", "The first character index to be drawn", startCursor, style)
        rpgEffects += vi("End Cursor", "The last character index to be drawn; -1 = unlimited", endCursor, style)

        val outline = getGroup("Outline", "", "outline")
        outline.setTooltip("Needs Rendering Mode = SDF or Merged SDF")
        outline += vi(
            "Rendering Mode",
            "Mesh: Sharp, Signed Distance Fields: with outline", "text.renderingMode",
            null, renderingMode, style
        ) { renderingMode = it }
        outline += vi("Color 1", "First Outline Color", "outline.color1", outlineColor0, style)
        outline += vi("Color 2", "Second Outline Color", "outline.color2", outlineColor1, style)
        outline += vi("Color 3", "Third Outline Color", "outline.color3", outlineColor2, style)
        outline += vi("Widths", "[Main, 1st, 2nd, 3rd]", "outline.widths", outlineWidths, style)
        outline += vi(
            "Smoothness", "How smooth the edge is, [Main, 1st, 2nd, 3rd]", "outline.smoothness",
            outlineSmoothness, style
        )
        outline += vi(
            "Depth", "For non-merged SDFs to join close characters correctly; needs a distance from the background",
            "outline.depth", outlineDepth, style
        )
        outline += vi("Rounded Corners", "Makes corners curvy", "outline.roundCorners", null, roundSDFCorners, style) {
            roundSDFCorners = it
            invalidate()
        }

        val shadows = getGroup("Shadow", "", "shadow")
        shadows += vi("Color", "", "shadow.color", shadowColor, style)
        shadows += vi("Offset", "", "shadow.offset", shadowOffset, style)
        shadows += vi("Smoothness", "", "shadow.smoothness", shadowSmoothness, style)


    }

}