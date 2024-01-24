package me.anno.ecs.database

import me.anno.engine.inspector.IProperty
import me.anno.engine.ui.input.ComponentUI
import me.anno.io.Saveable
import me.anno.engine.inspector.CachedReflections
import me.anno.ui.Panel
import me.anno.ui.base.components.AxisAlignment
import me.anno.ui.base.groups.TablePanel
import me.anno.ui.base.text.TextPanel
import me.anno.ui.Style
import me.anno.utils.strings.StringHelper.camelCaseToTitle
import me.anno.utils.types.Booleans.toInt

// todo move this to UI package, idk why it's here
class DataSetPanel(values: List<Saveable>, reflections: CachedReflections, firstIndex: Int, style: Style) :
    TablePanel(reflections.serializedProperties.size + (firstIndex < Int.MAX_VALUE).toInt(), values.size + 1, style) {

    constructor(values: List<Saveable>, firstIndex: Int, style: Style) :
            this(values, values.first().getReflections(), firstIndex, style)

    init {
        var x0 = 0
        if (firstIndex < Int.MAX_VALUE) {
            // define all ids :)
            val title = TextPanel("ID", style)
            title.alignmentX = AxisAlignment.CENTER
            this[x0, 0] = title
            for (y in values.indices) {
                val idPanel = TextPanel((firstIndex + y).toString(), style)
                idPanel.alignmentX = AxisAlignment.MAX
                this[x0, y + 1] = idPanel
            }
            x0++
        }
        // define header
        for (x in x0 until sizeX) {
            val name = reflections.propertiesByClassList[x - x0]
            val title = TextPanel(name.camelCaseToTitle(), style)
            title.alignmentX = AxisAlignment.CENTER
            this[x, 0] = title
            // define all fields :)
            val prop = reflections.allProperties[name]!!
            for (y in values.indices) {
                // todo if value is int or long, align them right
                // if it is float, how should we align it?
                val v = values[y]
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
                    // comp.alignmentX = AxisAlignment.FILL
                    this[x, y + 1] = comp
                }
            }
        }
    }

    // todo graph / statistical visualization of properties, e.g. damage to quickly find distribution, outliers,...
    // todo real database system like sqlite as backend

}