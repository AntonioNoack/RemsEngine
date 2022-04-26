package me.anno.ui.editor

import me.anno.gpu.GFX
import me.anno.language.translation.Dict
import me.anno.studio.Inspectable
import me.anno.ui.Panel
import me.anno.ui.base.Visibility
import me.anno.ui.base.components.Padding
import me.anno.ui.base.constraints.AxisAlignment
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.base.scrolling.ScrollPanelY
import me.anno.ui.editor.files.Search
import me.anno.ui.input.*
import me.anno.ui.input.components.Checkbox
import me.anno.ui.style.Style
import me.anno.ui.utils.WindowStack
import me.anno.utils.types.Strings.isBlank2

class PropertyInspector(val getInspectables: () -> List<Inspectable>, style: Style) :
    ScrollPanelY(Padding(3), AxisAlignment.MIN, style.getChild("propertyInspector")) {

    constructor(getInspectable: () -> Inspectable?, style: Style, @Suppress("unused_parameter") ignored: Unit) :
            this({ getInspectable().run { if (this == null) emptyList() else listOf(this) } }, style)

    val list0 = child as PanelListY
    val list1 = PanelListY(style)
    var lastSelected: List<Inspectable> = emptyList()

    val searchPanel = TextInput("Search Properties", "", true, style)

    init {
        searchPanel.addChangeListener { searchTerms ->
            // todo if an element is hidden by VisibilityKey, and it contains the search term, toggle that VisibilityKey
            val search = Search(searchTerms)
            for ((index, child) in list0.children.withIndex()) {
                if (index > 0) {
                    child.visibility = Visibility[child.fulfillsSearch(search)]
                }
            }
        }
    }

    private var needsUpdate = false

    fun invalidate() {
        needsUpdate = true
    }

    override fun getLayoutState(): List<Inspectable> = getInspectables()

    override fun tickUpdate() {
        super.tickUpdate()
        val selected = getInspectables()
        if (selected != lastSelected) {
            lastSelected = selected
            needsUpdate = false
            list0.clear()
            if (selected.isNotEmpty()) {
                list0.add(searchPanel)
                createInspector(selected, list0, style)
            }
        } else if (needsUpdate) {
            update(selected)
        }
    }

    fun update(selected: List<Inspectable>) {
        invalidateDrawing()
        lastSelected = selected
        needsUpdate = false
        list1.clear()
        if (selected.isNotEmpty()) {
            createInspector(selected, list1, style)
        }
        // is matching required? not really
        val src = list1.listOfAll.iterator()
        val dst = list0.listOfAll.iterator()
        if (src.hasNext()) src.next() // skip search panel
        // works as long as the structure stays the same
        while (src.hasNext() && dst.hasNext()) {
            val s = src.next()
            val d = dst.next()
            // don't change the value while the user is editing it
            // this would cause bad user experience:
            // e.g. 0.0001 would be replaced with 1e-4
            if (d.listOfAll.any { it.isInFocus }) continue
            when (s) {
                is FloatInput -> {
                    (d as? FloatInput)?.apply {
                        d.setValue(s.lastValue, false)
                    }
                }
                is FloatVectorInput -> {
                    (d as? FloatVectorInput)?.apply {
                        d.setValue(s, false)
                    }
                }
                is ColorInput -> {
                    (d as? ColorInput)?.apply {
                        d.setValue(s.getValue(), false)
                    }
                }
                is Checkbox -> {
                    (d as? Checkbox)?.apply {
                        d.isChecked = s.isChecked
                    }
                }
                is TextInputML -> {
                    (d as? TextInputML)?.apply {
                        d.setValue(s.text, false)
                    }
                }
            }
        }
        if (src.hasNext() != dst.hasNext() && selected.isNotEmpty()) {
            // we need to update the structure...
            lastSelected = emptyList()
            tickUpdate()
        }
    }

    operator fun plusAssign(panel: Panel) {
        list0 += panel
    }

    override fun onPropertiesChanged() {
        invalidate()
    }

    override val className: String = "PropertyInspector"

    companion object {

        private fun createGroup(
            title: String, description: String, dictSubPath: String,
            list: PanelListY, groups: HashMap<String, SettingCategory>, style: Style
        ): SettingCategory {
            val cat = groups.getOrPut(dictSubPath) {
                val group = SettingCategory(Dict[title, "obj.$dictSubPath"], style)
                list += group
                group
            }
            if (cat.tooltip?.isBlank2() != false) {
                cat.tooltip = Dict[description, "obj.$dictSubPath.desc"]
            }
            return cat
        }

        fun createInspector(ins: List<Inspectable>, list: PanelListY, style: Style) {
            val groups = HashMap<String, SettingCategory>()
            ins[0].createInspector(ins, list, style) { title, description, dictSubPath ->
                createGroup(title, description, dictSubPath, list, groups, style)
            }
        }

        fun createInspector(ins: Inspectable, list: PanelListY, style: Style) {
            val groups = HashMap<String, SettingCategory>()
            ins.createInspector(list, style) { title, description, dictSubPath ->
                createGroup(title, description, dictSubPath, list, groups, style)
            }
        }

        fun invalidateUI() {
            // expensive operation
            for (window in GFX.windows) {
                invalidateUI(window.windowStack)
            }
        }

        private fun invalidateUI(windowStack: WindowStack = GFX.someWindow.windowStack) {
            for (window in windowStack) {
                for (panel in window.panel.listOfVisible) {
                    panel.onPropertiesChanged()
                }
            }
        }

    }

}