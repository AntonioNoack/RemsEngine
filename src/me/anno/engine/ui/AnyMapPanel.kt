package me.anno.engine.ui

import me.anno.engine.IProperty
import me.anno.engine.Ptr
import me.anno.ui.base.Panel
import me.anno.ui.base.groups.PanelListX
import me.anno.ui.editor.stacked.ArrayPanel
import me.anno.ui.style.Style
import me.anno.utils.structures.tuples.MutablePair

// todo sort list/map by key or property of the users choice
open class AnyMapPanel(
    title: String,
    visibilityKey: String,
    val keyType: String,
    val valueType: String,
    style: Style
) : ArrayPanel<MutablePair<Any?, Any?>, Panel>(title, visibilityKey, {
    MutablePair(ComponentUI.getDefault(keyType), ComponentUI.getDefault(valueType))
}, style) {

    override fun createPanel(value: MutablePair<Any?, Any?>): Panel {

        val keyPanel = Ptr<Panel?>(null)
        val keyProperty = AAPProperty(value.first, { value.first = it; onChange() }, keyType, keyPanel)
        keyPanel.value = ComponentUI.createUI2("", visibilityKey, keyProperty, null, style)!!
        keyPanel.value!!.weight = 1f

        val valuePanel = Ptr<Panel?>(null)
        val valueProperty = AAPProperty(value.second, { value.second = it; onChange() }, valueType, valuePanel)
        valuePanel.value = ComponentUI.createUI2("", visibilityKey, valueProperty, null, style)!!
        valuePanel.value!!.weight = 1f

        val list = PanelListX(style)
        list.add(keyPanel.value!!)
        list.add(valuePanel.value!!)
        return list

    }

    class AAPProperty(
        val value: Any?,
        val setFunc: (Any?) -> Unit,
        val type: String,
        val panel: Ptr<Panel?>
    ) :
        IProperty<Any?> {
        override val annotations: List<Annotation> get() = emptyList()
        override fun init(panel: Panel?) {}
        override fun get(): Any? = value
        override fun getDefault(): Any? = ComponentUI.getDefault(type)
        override fun reset(panel: Panel?): Any? = getDefault().apply { set(panel, this) }
        override fun set(panel: Panel?, value: Any?) {
            this.setFunc(value)
        }
    }

}