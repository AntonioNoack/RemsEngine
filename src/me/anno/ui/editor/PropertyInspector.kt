package me.anno.ui.editor

import me.anno.Engine
import me.anno.gpu.GFX
import me.anno.language.translation.Dict
import me.anno.maths.Maths.MILLIS_TO_NANOS
import me.anno.maths.Maths.max
import me.anno.maths.Maths.min
import me.anno.studio.Inspectable
import me.anno.studio.StudioBase
import me.anno.ui.Panel
import me.anno.ui.base.SpacerPanel
import me.anno.ui.base.components.Padding
import me.anno.ui.base.constraints.AxisAlignment
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.base.scrolling.ScrollPanelY
import me.anno.ui.base.text.TextPanel
import me.anno.ui.debug.FrameTimings
import me.anno.ui.editor.files.Search
import me.anno.ui.input.ColorInput
import me.anno.ui.input.InputPanel
import me.anno.ui.input.TextInput
import me.anno.ui.style.Style
import me.anno.ui.utils.WindowStack
import me.anno.utils.types.Strings.isBlank2
import org.apache.logging.log4j.LogManager

class PropertyInspector(val getInspectables: () -> List<Inspectable>, style: Style) :
    ScrollPanelY(Padding(3), AxisAlignment.MIN, style.getChild("propertyInspector")) {

    @Suppress("unused")
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
                    // join all text (below a certain limit), and search that
                    // could be done more efficient
                    val joined = StringBuilder()
                    child.forAllPanels { panel ->
                        if (panel is TextPanel) {
                            joined.append(panel.text)
                            joined.append(' ')
                        }
                    }
                    child.isVisible = search.matches(joined.toString())
                }
            }
        }
    }

    private var needsUpdate = false

    fun invalidate() {
        needsUpdate = true
    }

    override fun getLayoutState(): List<Inspectable> = getInspectables()

    override fun onUpdate() {
        super.onUpdate()
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

        if (oldValues.isAnyChildInFocus)
            return

        lastSelected = selected
        needsUpdate = false

        newValues.clear()
        if (selected.isNotEmpty()) {
            createInspector(selected, newValues, style)
        }

        // is matching required? not really
        val newPanels = newValues.listOfAll.toList()
        val oldPanels = oldValues.listOfAll.toList()
        val sps = searchPanel.listOfAll.count()
        val newSize = newPanels.size + sps
        val oldSize = oldPanels.size
        val newPanelIter = newPanels.iterator()
        val oldPanelIter = oldPanels.iterator()

        // skip self
        newPanelIter.next()
        oldPanelIter.next()

        // skip search panel
        for (i in 0 until sps) {
            if (oldPanelIter.hasNext()) oldPanelIter.next()
            else break
        }

        // works as long as the structure stays the same
        var mismatch = false
        var isInFocus = false
        while (newPanelIter.hasNext() and oldPanelIter.hasNext()) {

            val newPanel = newPanelIter.next()
            val oldPanel = oldPanelIter.next()

            if (!mismatch && newPanel::class != oldPanel::class) {
                LOGGER.warn("Mismatch: ${newPanel::class} vs ${oldPanel::class}")
                mismatch = true
            }

            if (!mismatch && newPanel is InputPanel<*>) {
                if (newPanel.isAnyChildInFocus || oldPanel.isAnyChildInFocus ||
                    (oldPanel is ColorInput && oldPanel.contentView.isAnyChildInFocus)
                ) {
                    isInFocus = true
                    break
                }
                // only the value needs to be updated
                // no one to be notified
                @Suppress("unused", "unchecked_cast")
                (oldPanel as? InputPanel<Any?>)?.apply {
                    oldPanel.setValue(newPanel.lastValue, false)
                }
            }
        }
        if (!isInFocus && (mismatch || newSize != oldSize)) {
            oldValues.clear()
            oldValues.add(searchPanel)
            oldValues.addAll(newValues.children)
            LOGGER.debug("Updating everything")
        }
    }

    operator fun plusAssign(panel: Panel) {
        oldValues += panel
    }

    override fun onPropertiesChanged() {
        invalidate()
    }

    override val className: String get() = "PropertyInspector"

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
            addSpacingForFrameTimings(list)
        }

        fun createInspector(ins: Inspectable, list: PanelListY, style: Style) {
            val groups = HashMap<String, SettingCategory>()
            ins.createInspector(list, style) { title, description, dictSubPath ->
                createGroup(title, description, dictSubPath, list, groups, style)
            }
            addSpacingForFrameTimings(list)
        }

        /**
         * add extra spacing, so this panel doesn't get covered by the fps panel
         * */
        fun addSpacingForFrameTimings(list: PanelListY) {
            val panel = object : SpacerPanel(1, FrameTimings.height, list.style) {
                override fun onUpdate() {
                    super.onUpdate()
                    val window = window
                    val parent = uiParent
                    val win = GFX.activeWindow
                    sizeY = if (win != null && window != null && win.windowStack.contains(window)) {
                        if (parent != null && x + w >= window.w - FrameTimings.width) {
                            if (StudioBase.instance?.showFPS == true) {
                                max(1, window.y + min(parent.y + parent.h, window.h) + FrameTimings.height - win.height)
                            } else 1
                        } else 1
                    } else 1
                }
            }
            panel.name = "Spacing For FrameTimings"
            panel.backgroundColor = list.backgroundColor and 0xffffff
            list.add(panel)
        }

        /** expensive operation if something major was changed */
        var lastInvalidated = 0L
        fun invalidateUI(major: Boolean) {
            val time = Engine.gameTime
            if (time != lastInvalidated) {
                lastInvalidated = time + (if (major) 0L else 500L * MILLIS_TO_NANOS)
                for (window in GFX.windows) {
                    invalidateUI(window.windowStack)
                }
            }
        }

        private fun invalidateUI(windowStack: WindowStack = GFX.someWindow!!.windowStack) {
            for (window in windowStack) {
                for (panel in window.panel.listOfVisible) {
                    panel.onPropertiesChanged()
                }
            }
        }
    }
}