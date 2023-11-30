package me.anno.engine.ui.input

import me.anno.ecs.prefab.PrefabSaveable
import me.anno.language.translation.NameDesc
import me.anno.utils.Color.mixARGB
import me.anno.studio.StudioBase
import me.anno.ui.Panel
import me.anno.ui.Style
import me.anno.ui.base.buttons.TextButton
import me.anno.ui.base.components.AxisAlignment
import me.anno.ui.base.groups.PanelListX
import me.anno.ui.base.groups.TitledListY
import me.anno.ui.base.menu.Menu
import me.anno.ui.base.menu.MenuOption
import me.anno.ui.base.text.TextPanel
import me.anno.ui.base.text.TextStyleable
import me.anno.ui.editor.treeView.TreeViewEntryPanel
import me.anno.ui.input.InputPanel
import me.anno.ui.input.InputVisibility
import me.anno.utils.Color.black
import me.anno.utils.Color.hex32
import me.anno.utils.structures.lists.Lists.count2
import me.anno.utils.types.Strings.ifBlank2
import org.apache.logging.log4j.LogManager

/**
 * input panel for drag-dropping references to instances in the same scene
 * */
class SameSceneRefInput<Type : PrefabSaveable?>(
    title: String,
    visibilityKey: String,
    val clazz: Class<*>,
    value0: Type,
    style: Style
) : TitledListY(title, visibilityKey, style), InputPanel<Type>, TextStyleable {

    val list = object : PanelListX(style) {
        override var isVisible: Boolean
            get() = InputVisibility[visibilityKey]
            set(_) {}
    }

    val valueButton = TextButton(formatDisplay(value0, true), style).apply {
        setTooltip("Current Value; click to open scene tree to select value.")
    }

    fun containsType(element: Any?): Boolean {
        return element is PrefabSaveable && element.listChildTypes().sumOf { type ->
            element.getChildListByType(type).count2 {
                clazz.isInstance(it)
            }
        } == 1
    }

    /**
     * shows the status
     * */
    val linkIcon = object : TextPanel("\uD83D\uDD17", style) {

        fun isDraggingGoodType(): Int {
            val dragged = StudioBase.dragged?.getOriginal()
            return when {
                clazz.isInstance(dragged) -> 5
                containsType(dragged) -> 4
                dragged != null -> -2
                // check if hovered element is potentially it xD
                rootPanel.anyHierarchical({ it.isHovered }) {
                    it is TreeViewEntryPanel<*> && clazz.isInstance(it.getElement())
                } -> 3
                rootPanel.anyHierarchical({ it.isHovered }) {
                    it.isHovered && it is TreeViewEntryPanel<*> && containsType(it.getElement())
                } -> 2
                rootPanel.anyHierarchical({ it.isHovered }) {
                    it.isHovered && it is TreeViewEntryPanel<*> && it.getElement() is PrefabSaveable
                } -> -1
                else -> 0
            }
        }

        private var lastType = 0
        override fun onUpdate() {
            super.onUpdate()
            val type = isDraggingGoodType()
            if (lastType != type) {
                invalidateDrawing()
                lastType = type
            }
        }

        override val effectiveTextColor: Int
            get() = getColor(super.effectiveTextColor, lastType)
    }.apply {
        padding.set(valueButton.padding)
        setTooltip("Drag entities or components of matching type (${clazz.javaClass.simpleName}) here.")
    }

    init {
        valueButton.addLeftClickListener {
            // open tree view to select an element without drag'n'drop
            val oldValue: Type = value
            val panel = SameSceneRefTreeView(this)
            val acceptButton = TextButton("Accept", style)
                .addLeftClickListener {
                    it.window?.close()
                }
            val cancelButton = TextButton("Cancel", style)
                .addLeftClickListener {
                    setValue(oldValue, true)
                    it.window?.close()
                }
            val buttonList = PanelListX(style)
            buttonList.alignmentX = AxisAlignment.FILL
            buttonList.add(acceptButton)
            acceptButton.alignmentX = AxisAlignment.FILL
            acceptButton.weight = 1f
            buttonList.add(cancelButton)
            cancelButton.alignmentX = AxisAlignment.FILL
            cancelButton.weight = 1f
            val title1 = NameDesc("Select Value", "This is the current scene, so all available instances are here", "")
            val window = Menu.openMenuByPanels(windowStack, title1, listOf(panel, buttonList))
            window?.acceptsClickAway = {
                setValue(oldValue, true)
                true
            }
        }

        // right click to reset it
        addRightClickListener {
            Menu.openMenu(windowStack, listOf(MenuOption(NameDesc("Reset")) {
                value = resetListener()
            }))
        }

        titleView?.setTooltip("Link to '${clazz.simpleName}' instance in scene")

        list.add(linkIcon)
        list.add(valueButton)

        add(list)
    }

    override var isInputAllowed = true

    override var value: Type = value0

    override fun setValue(newValue: Type, mask: Int, notify: Boolean): Panel {
        value = newValue
        if (notify) {
            changeListener(newValue)
        }
        valueButton.text = formatDisplay(newValue, true)
        return this
    }

    private var changeListener: (v: Type) -> Unit = {}
    fun setChangeListener(changeListener: (v: Type) -> Unit) {
        this.changeListener = changeListener
    }

    private var resetListener: () -> Type = { value0 }
    fun setResetListener(resetListener: () -> Type) {
        this.resetListener = resetListener
    }

    override fun onPaste(x: Float, y: Float, data: String, type: String) {
        when (type) {
            "PrefabSaveable" -> {
                val instance = StudioBase.dragged?.getOriginal() as? PrefabSaveable
                if (instance == null) {
                    LOGGER.warn("Dragged instance was not PrefabSaveable")
                } else if (clazz.isInstance(instance)) {
                    @Suppress("unchecked_cast")
                    setValue(instance as Type, true)
                } else {
                    // check all children, if there is any match
                    for (childType in instance.listChildTypes()) {
                        for (child in instance.getChildListByType(childType)) {
                            if (clazz.isInstance(child)) {
                                @Suppress("unchecked_cast")
                                setValue(child as Type, true)
                                return
                            }
                        }
                    }
                    LOGGER.warn(
                        "Incorrect type: {} is not instance of {}, and none of its direct children is either",
                        formatDisplay(instance, false), clazz
                    )
                }
            }
            else -> super.onPaste(x, y, data, type)
        }
    }

    companion object {
        private val LOGGER = LogManager.getLogger(SameSceneRefInput::class)

        fun formatDisplay(value: PrefabSaveable?, forPrimary: Boolean): String {
            val prefix = if (forPrimary) "\uD83D\uDCC1 " else ""
            value ?: return "${prefix}null"
            return "$prefix${value.name.ifBlank2(value.className)} #${hex32(hashCode(value))}"
        }

        fun hashCode(value: PrefabSaveable): Int {
            return value.prefabPath.hashCode()
        }

        fun getColor(baseColor: Int, type: Int): Int {
            return mixARGB(
                baseColor,
                if (type < 0) 0xff0000 or black // bad -> red
                else 0x00ff00 or black, // good -> green
                when (type) {
                    5 -> 1.0f
                    4 -> 0.7f
                    3 -> 0.8f
                    2 -> 0.4f
                    -1 -> 0.5f
                    -2 -> 1.0f
                    else -> 0f
                }
            )
        }
    }
}