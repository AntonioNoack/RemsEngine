package me.anno.engine.ui.input

import me.anno.language.translation.NameDesc
import me.anno.maths.Maths.fract
import me.anno.ui.Panel
import me.anno.ui.Style
import me.anno.ui.base.buttons.TextButton
import me.anno.ui.base.components.AxisAlignment
import me.anno.ui.base.groups.PanelListX
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.base.groups.PanelOverflowListX
import me.anno.ui.editor.color.spaces.HSV
import me.anno.ui.input.InputPanel
import me.anno.ui.input.TextInput
import me.anno.utils.Color
import me.anno.utils.Color.toARGB
import me.anno.utils.structures.Collections.filterIsInstance2
import me.anno.utils.structures.lists.Lists.any2

// tag-manager UI:
//  - shows tags of current prefab
//  - you can add tags
// todo assign colors to tags manually??
// super tags, which combine multiple tags??
// todo tag rules, e.g., pirate -> human
// todo file explorer mode that uses tags instead of folders
// todo collection-projects, which can be added to regular projects, and then are indexed, too?
class TagsPanel(value0: List<String>, style: Style) : PanelListY(style), InputPanel<List<String>> {
    constructor(style: Style) : this(emptyList(), style)

    val top = PanelListX(style)
    val tags = PanelOverflowListX(style)

    // todo auto-complete in search of known tags
    // todo each entry should have an X to exclude it
    // todo it gets added back, as soon as you use=enter it

    val search = TextInput(NameDesc("Add Tags With Enter"), "", "", style)

    init {
        search.setEnterListener {
            addTag(it.trim(), true)
            search.setValue("", -1, false)
        }
    }

    val rulesButton = TextButton(NameDesc("R"), true, style)
        .addLeftClickListener {
            // todo open rules
            //  tagA -> tagB
            //  tagC -> !tagD
            //  tagA & tagB -> tagC
        }

    init {
        // add search
        top.add(search.fill(1f))
        // top.add(rulesButton) // non-functional -> not added
        add(top)
        add(tags)
        tags.spacing = 4
        tags.lineAlignmentX = AxisAlignment.MIN
        for (tag in value0) {
            addTag(tag, false)
        }
    }

    fun addTag(tag: String, notify: Boolean) {
        if (tag.isEmpty() || hasTag(tag)) return
        val hue = fract(tags.children.size * 0.2f)
        val color = HSV.toRGB(hue, 0.3f, 0.7f, 1f).toARGB()
        val tagPanel = TagPanel(tag, style)
        tagPanel.padding.add(3)
        tagPanel.textColor = Color.white
        tagPanel.backgroundColor = color
        tagPanel.backgroundRadius = (tagPanel.textSize + tagPanel.padding.width) * 0.5f
        tags.add(tagPanel)
        if (notify) onChange(getTags())
    }

    fun removeTag(tag: String, notify: Boolean) {
        setValue(getTags() - tag, notify)
    }

    fun hasTag(tag: String): Boolean {
        return tags.children.any2 {
            it is TagPanel && it.text == tag
        }
    }

    fun getTags(): List<String> {
        return tags.children.filterIsInstance2(TagPanel::class).map { it.text }
    }

    override var isInputAllowed: Boolean
        get() = search.isInputAllowed
        set(value) {
            search.isInputAllowed = value
        }

    override val value: List<String> get() = getTags()

    private val changeListeners = ArrayList<(List<String>) -> Unit>()
    fun addChangeListener(changeListener: (List<String>) -> Unit) {
        changeListeners += changeListener
    }

    private fun onChange(tags: List<String>) {
        for (i in changeListeners.indices) {
            changeListeners[i](tags)
        }
    }

    override fun setValue(newValue: List<String>, mask: Int, notify: Boolean): Panel {
        tags.clear()
        for (i in newValue.indices) {
            addTag(newValue[i], false)
        }
        if (notify) onChange(newValue)
        return this
    }
}