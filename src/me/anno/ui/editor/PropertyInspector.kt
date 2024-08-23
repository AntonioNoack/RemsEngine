package me.anno.ui.editor

import me.anno.Time
import me.anno.engine.EngineBase
import me.anno.engine.inspector.Inspectable
import me.anno.gpu.GFX
import me.anno.language.translation.NameDesc
import me.anno.maths.Maths.MILLIS_TO_NANOS
import me.anno.maths.Maths.max
import me.anno.maths.Maths.min
import me.anno.ui.Panel
import me.anno.ui.Style
import me.anno.ui.WindowStack
import me.anno.ui.base.SpacerPanel
import me.anno.ui.base.components.Padding
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.base.scrolling.ScrollPanelXY
import me.anno.ui.base.text.TextPanel
import me.anno.ui.debug.FrameTimings
import me.anno.ui.editor.files.Search
import me.anno.ui.input.ColorInput
import me.anno.ui.input.InputPanel
import me.anno.ui.input.TextInput
import me.anno.utils.structures.Collections.filterIsInstance2
import me.anno.utils.structures.lists.Lists.wrap
import org.apache.logging.log4j.LogManager

open class PropertyInspector(val getInspectables: () -> List<Inspectable>, style: Style) :
    ScrollPanelXY(Padding(3), style.getChild("propertyInspector")) {

    @Suppress("unused")
    constructor(getInspectable: () -> Inspectable?, style: Style, @Suppress("unused_parameter") ignored: Unit) :
            this({ getInspectable().wrap() }, style)

    val oldValues = child as PanelListY
    val newValues = PanelListY(style)
    var lastSelected: List<Inspectable> = emptyList()

    val searchPanel = TextInput(NameDesc("Search Properties"), "", true, style)

    init {
        alwaysShowShadowY = true
        oldValues.makeBackgroundTransparent()
        searchPanel.addChangeListener { searchTerms ->

            val search = Search(searchTerms)

            LOGGER.info("Applying search '$searchTerms', $search, all? ${search.matchesEverything()}")

            val joined = StringBuilder()
            fun join(child: Panel): CharSequence {
                joined.clear()
                child.forAllPanels { panel ->
                    if (panel is TextPanel) {
                        joined.append(panel.text)
                        joined.append(' ')
                    }
                }
                return joined
            }

            fun handleVisibility(child: Panel): Boolean {
                // join all text (below a certain limit), and search that
                // could be done more efficient
                val matches = search.matches(join(child))
                child.isVisible = matches
                return matches
            }

            val list0 = oldValues.children
            for (i in 1 until list0.size) {
                val child = list0[i]
                if (child is SettingCategory) {
                    if (search.matchesEverything()) {
                        val list1 = child.content.children
                        for (j in list1.indices) {
                            list1[j].isVisible = true
                        }
                        child.isVisible = true
                        child.visibilityByKey = defaultState.getOrPut(child.visibilityKey) { child.visibilityByKey }
                    } else {
                        val list1 = child.content.children
                        var anyChildMatches = false
                        for (j in list1.indices) { // all children need to be handled!
                            anyChildMatches = handleVisibility(list1[j]) || anyChildMatches
                        }
                        child.isVisible = anyChildMatches
                        defaultState.getOrPut(child.visibilityKey) { child.visibilityByKey }
                        child.visibilityByKey = anyChildMatches
                    }
                } else {
                    handleVisibility(child)
                }
            }
        }
    }

    private var needsUpdate = false

    fun invalidate() {
        needsUpdate = true
        invalidateLayout()
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
                createInspector1(selected, oldValues, style)
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
            createInspector1(selected, newValues, style)
        }

        // is matching required? not really
        val newPanels = newValues.listOfAll
            .filterIsInstance2(InputPanel::class)
        val oldPanels = oldValues.listOfAll
            .filter { !it.anyInHierarchy { p -> p == searchPanel } }
            .filterIsInstance2(InputPanel::class)

        val newSize = newPanels.size
        val oldSize = oldPanels.size
        val newPanelIter = newPanels.iterator()
        val oldPanelIter = oldPanels.iterator()

        // works as long as the structure stays the same
        var mismatch = false
        var isInFocus = false
        while (newPanelIter.hasNext() and oldPanelIter.hasNext()) {

            val newPanel = newPanelIter.next()
            val oldPanel = oldPanelIter.next()

            newPanel as Panel
            oldPanel as Panel

            if (!mismatch && newPanel::class != oldPanel::class) {
                LOGGER.info("Mismatch: ${newPanel::class} vs ${oldPanel::class}")
                mismatch = true
            }

            if (!mismatch) {
                if (newPanel.isAnyChildInFocus || oldPanel.isAnyChildInFocus ||
                    (oldPanel is ColorInput && oldPanel.contentView.isAnyChildInFocus)
                ) {
                    isInFocus = true
                    break
                }
                // only the value needs to be updated
                // no one to be notified
                @Suppress("unchecked_cast")
                (oldPanel as? InputPanel<Any?>)?.apply {
                    oldPanel.setValue(newPanel.value, false)
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

    companion object {

        private val LOGGER = LogManager.getLogger(PropertyInspector::class)
        private val defaultState = HashMap<String, Boolean>()

        private fun createGroup(
            nameDesc: NameDesc, list: PanelListY,
            groups: HashMap<String, SettingCategory>, style: Style
        ): SettingCategory {
            return groups.getOrPut(nameDesc.key) {
                val group = SettingCategory(nameDesc, style)
                list += group
                group.showContent()
            }
        }

        private fun createInspector1(ins: List<Inspectable>, list: PanelListY, style: Style) {
            val groups = HashMap<String, SettingCategory>()
            ins.first().createInspector(ins, list, style) {
                createGroup(it, list, groups, style)
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
                    val paddingForScrolling = 10
                    sizeY =
                        paddingForScrolling + if (win != null && window != null && win.windowStack.contains(window)) {
                            if (parent != null && x + width >= window.width - FrameTimings.width) {
                                if (EngineBase.showFPS) {
                                    max(
                                        1,
                                        window.y + min(
                                            parent.y + parent.height,
                                            window.height
                                        ) + FrameTimings.height - win.height
                                    )
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
            val time = Time.nanoTime
            if (time != lastInvalidated) {
                lastInvalidated = time + (if (major) 0L else 500L * MILLIS_TO_NANOS)
                for (window in GFX.windows) {
                    invalidateUI(window.windowStack)
                }
                for (window in GFX.windows) {
                    for (window1 in window.windowStack) {
                        window1.panel.invalidateDrawing()
                    }
                }
            }
        }

        private fun invalidateUI(windowStack: WindowStack = GFX.someWindow.windowStack) {
            for (window in windowStack) {
                window.panel.forAllVisiblePanels(Panel::onPropertiesChanged)
            }
        }
    }
}