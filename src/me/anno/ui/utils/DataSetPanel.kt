package me.anno.ui.utils

import me.anno.engine.inspector.CachedProperty
import me.anno.engine.inspector.CachedReflections
import me.anno.engine.inspector.IProperty
import me.anno.engine.ui.input.ComponentUI
import me.anno.io.saveable.Saveable
import me.anno.language.translation.NameDesc
import me.anno.ui.Panel
import me.anno.ui.Style
import me.anno.ui.base.buttons.TextButton
import me.anno.ui.base.components.AxisAlignment
import me.anno.ui.base.groups.TablePanel
import me.anno.ui.base.menu.Menu
import me.anno.ui.base.menu.Menu.openMenu
import me.anno.ui.base.menu.Menu.openMenuByPanels
import me.anno.ui.base.menu.MenuOption
import me.anno.ui.base.text.TextPanel
import me.anno.ui.input.BooleanInput
import me.anno.utils.types.Booleans.toInt
import me.anno.utils.types.Strings.camelCaseToTitle
import org.apache.logging.log4j.LogManager

class DataSetPanel(
    val values: ArrayList<Saveable>,
    val reflectionsI: CachedReflections,
    firstIndex: Int,
    style: Style
) : TablePanel(reflectionsI.serializedProperties.size + (firstIndex < Int.MAX_VALUE).toInt(), values.size + 1, style) {

    constructor(values: ArrayList<Saveable>, firstIndex: Int, style: Style) :
            this(values, values.first().getReflections(), firstIndex, style)

    companion object {
        private val LOGGER = LogManager.getLogger(DataSetPanel::class)
    }

    val needsIDColumn = firstIndex < Int.MAX_VALUE
    val filtered = ArrayList(values)
    val filters = ArrayList<(Saveable) -> Boolean>()

    init {
        if (needsIDColumn) {
            fillInIDs(firstIndex)
        }
        fillInTable(true)
    }

    private fun fillInIDs(firstIndex: Int) {
        // define all ids :)
        fillInHeader(0, "ID")
        for (y in values.indices) {
            val idPanel = TextPanel((firstIndex + y).toString(), style)
            idPanel.alignmentX = AxisAlignment.MAX
            this[0, y + 1] = idPanel
        }
    }

    private fun fillInHeader(x: Int, title: String): Panel {
        val panel = TextPanel(title, style)
        panel.textAlignmentX = AxisAlignment.CENTER
        panel.textAlignmentY = AxisAlignment.CENTER
        this[x, 0] = panel
        return panel
    }

    private fun recalculateFiltered() {
        // todo optimize this, and maybe don't lose sorting...
        filtered.clear()
        filtered.addAll(values)
        for (filter in filters) {
            filtered.removeIf(filter)
        }
        // reset weights??? -> yes, good idea
        weightsY.fill(1f) // todo when changing size of wys, extend its values instead of just replacing them
        sizeY = filtered.size + 1
        fillInTable(false)
    }

    private fun fillInHeader(x: Int, name: String, prop: CachedProperty) {
        val title = fillInHeader(x, name.camelCaseToTitle())
        val allowedValues = HashSet<String>()
        filters.add { allowedValues.isNotEmpty() && prop[it].toString() !in allowedValues }
        val lastString = 65535.toChar().toString()
        val instanceToPropString = { it: Saveable ->
            // null and empty strings last? -> yes, seems sensible
            prop[it]?.toString()?.ifBlank { lastString } ?: lastString
        }
        title.addRightClickListener { p ->
            openMenu(
                p.windowStack, listOf(
                    MenuOption(NameDesc("Sort Ascending")) {
                        values.sortBy(instanceToPropString)
                        filtered.sortBy(instanceToPropString)
                        fillInTable(false)
                    },
                    MenuOption(NameDesc("Sort Descending")) {
                        values.sortByDescending(instanceToPropString)
                        filtered.sortByDescending(instanceToPropString)
                        fillInTable(false)
                    },
                    MenuOption(NameDesc("Filter By")) {
                        val panels = ArrayList<Panel>()
                        // option to cancel filter
                        panels.add(TextButton("Remove filter", style)
                            .addLeftClickListener {
                                allowedValues.clear()
                                recalculateFiltered()
                            })
                        val presentValues = values.map(instanceToPropString).toHashSet().sorted()
                        for (value in presentValues) {
                            // checkbox for each option
                            val box = BooleanInput(value, value in allowedValues, false, style)
                            box.setChangeListener { allowValue ->
                                if (allowValue) {
                                    allowedValues.add(value)
                                } else {
                                    allowedValues.remove(value)
                                }
                                recalculateFiltered()
                            }
                            panels.add(box)
                        }

                        val okButton = TextButton("Close", style)
                        okButton.addLeftClickListener(Menu::close)
                        panels.add(okButton)
                        // todo highlight headers with filters enabled, maybe with a star
                        openMenuByPanels(p.windowStack, NameDesc("Define Filters"), panels)
                        LOGGER.warn("Filtering hasn't been implemented yet")
                    },
                )
            )
        }
    }

    private fun fillInTable(headers: Boolean) {
        val x0 = needsIDColumn.toInt()
        if (headers) {
            filters.clear()
        }
        for (x in x0 until sizeX) {
            val name = reflectionsI.propertiesByClassList[x - x0]
            val property = reflectionsI.allProperties[name]!!
            if (headers) {
                fillInHeader(x, name, property)
            }
            fillInColumn(x, property)
        }
        invalidateLayout()
    }

    private fun fillInColumn(x: Int, prop: CachedProperty) {
        for (y in filtered.indices) {
            val v = filtered[y]
            val default = prop[v] // good? maybe...
            val property2 = object : IProperty<Any?> {
                override fun get() = prop[v]
                override val annotations: List<Annotation> get() = prop.annotations
                override fun getDefault() = default
                override fun reset(panel: Panel?) = getDefault()
                override fun init(panel: Panel?) {}
                override fun set(panel: Panel?, value: Any?, mask: Int) {
                    prop[v] = value
                }
            }
            val comp = ComponentUI.createUI2(null, "", property2, prop.range, style)
            if (comp != null) {
                this[x, y + 1] = comp
            }
        }
    }

    // todo graph / statistical visualization of properties, e.g. damage to quickly find distribution, outliers,...
    // todo real database system like sqlite as backend

}