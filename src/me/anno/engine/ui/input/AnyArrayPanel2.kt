package me.anno.engine.ui.input

import me.anno.engine.IProperty
import me.anno.engine.Ptr
import me.anno.ui.Panel
import me.anno.ui.Style
import me.anno.ui.editor.stacked.ArrayPanel2
import me.anno.ui.input.InputPanel
import org.apache.logging.log4j.LogManager

/**
 * UI for editing arrays
 * */
open class AnyArrayPanel2(title: String, visibilityKey: String, val childType: String, style: Style) :
    ArrayPanel2<Any?, Panel>(title, visibilityKey, { ComponentUI.getDefault(childType) }, style) {

    companion object {
        private val LOGGER = LogManager.getLogger(AnyArrayPanel2::class)
    }

    override fun createPanel(value: Any?): Panel {
        val panel = Ptr<Panel?>(null)
        val property = ArrayPanelProperty(this, value, childType, panel)
        panel.value = ComponentUI.createUIByTypeName(null, visibilityKey, property, childType, null, style)
        return panel.value!!
    }

    override fun updatePanel(value: Any?, panel: Panel) {
        when (panel) {
            is InputPanel<*> -> {
                if (panel.value != value) {
                    @Suppress("UNCHECKED_CAST")
                    (panel as InputPanel<Any?>).setValue(value, false)
                }
            }
            else -> LOGGER.warn("Unknown type ${panel.className} is not an InputPanel")
        }
    }

    override val className: String
        get() = "AnyArrayPanel2"

    class ArrayPanelProperty(
        val anyArrayPanel: AnyArrayPanel2,
        val value: Any?,
        val arrayType: String,
        val panel: Ptr<Panel?>
    ) : IProperty<Any?> {
        override fun init(panel: Panel?) {}
        override val annotations: List<Annotation> get() = emptyList()
        override fun set(panel: Panel?, value: Any?) = anyArrayPanel.set(this.panel.value!!, value)
        override fun get(): Any? = value
        override fun getDefault(): Any? = ComponentUI.getDefault(arrayType)
        override fun reset(panel: Panel?): Any? = getDefault().apply { set(panel, this) }
    }
}