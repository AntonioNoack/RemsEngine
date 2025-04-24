package me.anno.ui.editor

import me.anno.config.DefaultConfig
import me.anno.fonts.FontManager
import me.anno.input.Key
import me.anno.language.translation.NameDesc
import me.anno.ui.base.menu.Menu
import me.anno.ui.base.menu.MenuOption
import me.anno.ui.base.text.TextPanel
import me.anno.ui.input.EnumInput
import me.anno.ui.Style
import org.apache.logging.log4j.LogManager
import kotlin.math.max

object FontListMenu {

    private val LOGGER = LogManager.getLogger(FontListMenu::class)

    val lastUsedFonts by lazy { arrayOfNulls<String>(max(0, DefaultConfig["lastUsed.fonts.count", 5])) }

    /**
     * saves the most recently used fonts
     * */
    fun putLastUsedFont(font: String) {
        if (lastUsedFonts.isNotEmpty()) {
            for (i in lastUsedFonts.indices) {
                if (lastUsedFonts[i] == font) return
            }
            for (i in 0 until lastUsedFonts.lastIndex) {
                lastUsedFonts[i] = lastUsedFonts[i + 1]
            }
            lastUsedFonts[lastUsedFonts.lastIndex] = font
        }
    }

    fun createFontInput(oldValue: String, style: Style, onChange: (String) -> Unit): EnumInput {
        val fontList = ArrayList<NameDesc>()
        val oldValueI = NameDesc(oldValue)
        fontList += oldValueI
        fontList += NameDesc(Menu.MENU_SEPARATOR)

        fun sortFavourites() {
            fontList.sortBy { it.name }
            val lastUsedSet = lastUsedFonts.toSet()
            fontList.sortByDescending { if (it.name == Menu.MENU_SEPARATOR) 1 else if (it.name in lastUsedSet) 2 else 0 }
        }

        fontList += FontManager.fontSet
            .filter { it != oldValue }
            .map { NameDesc(it) }

        // Consolas is not correctly centered?
        // todo -> Consolas' width is incorrect...

        return object : EnumInput(
            NameDesc(
                "Font Name",
                "The style of the text",
                "obj.font.name"
            ), oldValueI, fontList,
            style
        ) {
            /**
             * this menu is overridden, so we can set each font name to its respective font :3
             * this could be a bad idea, if the user has thousands of fonts installed
             * */
            override fun onMouseClicked(x: Float, y: Float, button: Key, long: Boolean) {
                if (DefaultConfig["ui.fonts.previewInEnumInput.enable", true]) {
                    val window = Menu.openMenu(windowStack, this.x, this.y,
                        NameDesc("Select the %1", "", "ui.input.enum.menuTitle")
                            .with("%1", nameDesc),
                        options.mapIndexed { index, option ->
                            MenuOption(option) {
                                inputPanel.text = option.name
                                inputPanel.tooltip = option.desc
                                lastIndex = index
                                changeListener(option, index, options)
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
                                    val text = "" +
                                            "the quick brown fox jumps over the lazy dog.\n" +
                                            "THE QUICK BROWN FOX JUMPS OVER THE LAZY DOG.\n" +
                                            panel.text
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
            onChange(it.englishName)
            putLastUsedFont(it.englishName)
            sortFavourites()
        }
    }
}