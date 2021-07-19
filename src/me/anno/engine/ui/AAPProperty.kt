package me.anno.engine.ui

import me.anno.engine.IProperty
import me.anno.engine.Ptr
import me.anno.ui.base.Panel

class AAPProperty(
    val anyArrayPanel: AnyArrayPanel,
    val value: Any?,
    val arrayType: String,
    val panel: Ptr<Panel?>
) :
    IProperty<Any?> {
    override fun init(panel: Panel?) {}
    override val annotations: List<Annotation> get() = emptyList()
    override fun set(panel: Panel?, value: Any?) = anyArrayPanel.set(this.panel.value!!, value)
    override fun get(): Any? = value
    override fun getDefault(): Any? = ComponentUI.getDefault(arrayType)
    override fun reset(panel: Panel?): Any? = getDefault().apply { set(panel, this) }
}