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
    override val annotations: List<Annotation> get() = emptyList()
    override fun set(value: Any?) = anyArrayPanel.set(panel.value!!, value)
    override fun get(): Any? = value
    override fun getDefault(): Any? = ComponentUI.getDefault(arrayType)
    override fun reset(): Any? = getDefault().apply { set(this) }
}