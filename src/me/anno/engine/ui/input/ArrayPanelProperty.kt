package me.anno.engine.ui.input

import me.anno.engine.inspector.IProperty
import me.anno.utils.structures.Pointer
import me.anno.ui.Panel

/**
 * IProperty for ArrayPanels
 * */
class ArrayPanelProperty(
    val anyArrayPanel: AnyArrayPanel,
    val value: Any?,
    val arrayType: String,
    val panel: Pointer<Panel?>
) :
    IProperty<Any?> {
    override fun init(panel: Panel?) {}
    override val annotations: List<Annotation> get() = emptyList()
    override fun set(panel: Panel?, value: Any?, mask: Int) = anyArrayPanel.set(this.panel.value!!, value)
    override fun get(): Any? = value
    override fun getDefault(): Any? = ComponentUI.getDefault(arrayType)
    override fun reset(panel: Panel?): Any? = getDefault().apply { set(panel, this) }
}