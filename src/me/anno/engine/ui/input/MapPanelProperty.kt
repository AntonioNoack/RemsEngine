package me.anno.engine.ui.input

import me.anno.engine.IProperty
import me.anno.engine.Ptr
import me.anno.ui.Panel

class MapPanelProperty(
    val value: Any?,
    val setFunc: (Any?) -> Unit,
    val type: String,
    val panel: Ptr<Panel?>
) : IProperty<Any?> {
    override val annotations: List<Annotation> get() = emptyList()
    override fun init(panel: Panel?) {}
    override fun get(): Any? = value
    override fun getDefault(): Any? = ComponentUI.getDefault(type)
    override fun reset(panel: Panel?): Any? = getDefault().apply { set(panel, this) }
    override fun set(panel: Panel?, value: Any?) {
        this.setFunc(value)
    }
}