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
import me.anno.ui.input.InputPanel
import me.anno.ui.input.TextInput
import me.anno.ui.style.Style
import me.anno.ui.utils.WindowStack
import me.anno.utils.structures.lists.Lists.size
import me.anno.utils.types.Strings.isBlank2
import org.apache.logging.log4j.LogManager

class PropertyInspector(val getInspectables: () -> List<Inspectable>, style: Style) :
    ScrollPanelY(Padding(3), AxisAlignment.MIN, style.getChild("propertyInspector")) {

    constructor(getInspectable: () -> Inspectable?, style: Style, @Suppress("unused_parameter") ignored: Unit) :
            this({ getInspectable().run { if (this == null) emptyList() else listOf(this) } }, style)

    val oldValues = child as PanelListY
    val newValues = PanelListY(style)
    var lastSelected: List<Inspectable> = emptyList()

    val searchPanel = TextInput("Search Properties", "", true, style)

    init {
        searchPanel.addChangeListener { searchTerms ->
            // todo if an element is hidden by VisibilityKey, and it contains the search term, toggle that VisibilityKey
            val search = Search(searchTerms)
            for ((index, child) in oldValues.children.withIndex()) {
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
            oldValues.clear()
            if (selected.isNotEmpty()) {
                oldValues.add(searchPanel)
                createInspector(selected, oldValues, style)
            }
        } else if (needsUpdate) {
            update(selected)
        }
    }

    fun update(selected: List<Inspectable>) {
        invalidateDrawing()
        lastSelected = selected
        needsUpdate = false
        newValues.clear()
        if (selected.isNotEmpty()) {
            createInspector(selected, newValues, style)
        }
        // is matching required? not really
        val newPanels = newValues.listOfAll.toList()
        val oldPanels = oldValues.listOfAll.toList()
        val newSize = newPanels.size + searchPanel.listOfAll.size
        val oldSize = oldPanels.size
        val newPanelIter = newPanels.iterator()
        val oldPanelIter = oldPanels.iterator()
        if (newPanelIter.hasNext()) newPanelIter.next() // skip search panel
        // works as long as the structure stays the same
        while (newPanelIter.hasNext() and oldPanelIter.hasNext()) {
            val newPanel = newPanelIter.next()
            val oldPanel = oldPanelIter.next()
            // don't change the value while the user is editing it
            // this would cause bad user experience:
            // e.g. 0.0001 would be replaced with 1e-4
            if (!oldPanel.isAnyChildInFocus &&
                !newPanel.isAnyChildInFocus &&
                newPanel is InputPanel<*> &&
                newPanel::class == oldPanel::class
            ) {
                // only the value needs to be updated
                // no one to be notified
                (oldPanel as? InputPanel<Any?>)?.apply {
                    oldPanel.setValue(newPanel.lastValue, false)
                }
            }
        }
        if (newSize != oldSize && selected.isNotEmpty()) {
            // we need to update the structure...
            // todo transfer focussed elements, so we don't loose focus
            LOGGER.info("Whole structure needed update, new: ${newPanels.size} vs old: ${oldPanels.size}")
            oldValues.clear()
            oldValues.add(searchPanel)
            oldValues.addAll(newValues.children)
        }
    }

    operator fun plusAssign(panel: Panel) {
        oldValues += panel
    }

    override fun onPropertiesChanged() {
        invalidate()
    }

    override val className: String = "PropertyInspector"

    companion object {

        private val LOGGER = LogManager.getLogger(PropertyInspector::class)

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

        /** expensive operation if something major was changed */
        fun invalidateUI() {
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