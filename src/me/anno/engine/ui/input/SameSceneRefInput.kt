package me.anno.engine.ui.input

import me.anno.ecs.prefab.PrefabSaveable
import me.anno.language.translation.NameDesc
import me.anno.maths.Maths.mixARGB
import me.anno.studio.StudioBase
import me.anno.ui.Panel
import me.anno.ui.Style
import me.anno.ui.base.buttons.TextButton
import me.anno.ui.base.constraints.AxisAlignment
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
import me.anno.utils.structures.lists.Lists.any2
import me.anno.utils.types.Strings.ifBlank2
import org.apache.logging.log4j.LogManager

// todo click, then open tree view to select it (?)

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

    val openSceneSelection = TextButton("\uD83D\uDCC1", true, style).apply {
        setTooltip("Open scene tree to select value.")
    }

    fun containsType(element: Any?): Boolean {
        return element is PrefabSaveable && element.listChildTypes().any { type ->
            element.getChildListByType(type).any2 {
                clazz.isInstance(it)
            }
        }
    }

    val linkIcon = object : TextPanel("\uD83D\uDD17", style) {

        fun isDraggingGoodType(): Int {
            val dragged = StudioBase.dragged?.getOriginal()
            return when {
                clazz.isInstance(dragged) -> 5
                containsType(dragged) -> 4
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
            get() = mixARGB(
                super.effectiveTextColor,
                if (lastType < 0) 0xff0000 or black
                else 0x00ff00 or black,
                when (lastType) {
                    5 -> 1.0f
                    4 -> 0.7f
                    3 -> 0.8f
                    2 -> 0.4f
                    -1 -> 0.5f
                    else -> 0f
                }
            )
    }.apply {
        padding.set(openSceneSelection.padding)
        setTooltip("Drag entities or components of matching type (${clazz.javaClass.simpleName}) here.")
    }

    val nameDisplayPanel = TextPanel(formatDisplay(value0), style).apply {
        padding.set(openSceneSelection.padding)
    }

    init {
        nameDisplayPanel.setTooltip("Current Value")
        openSceneSelection.addLeftClickListener {
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

        list.add(linkIcon)
        list.add(openSceneSelection)
        list.add(nameDisplayPanel)

        add(list)
    }

    override var isInputAllowed = true

    override var value: Type = value0

    fun formatDisplay(value: PrefabSaveable?): String {
        value ?: return "null"
        return "${value.name.ifBlank2(value.className)} #${hex32(hashCode(value))}"
    }

    fun hashCode(value: PrefabSaveable): Int {
        return value.prefabPath.hashCode()
    }

    override fun setValue(newValue: Type, notify: Boolean): Panel {
        value = newValue
        if (notify) {
            changeListener(newValue)
        }
        nameDisplayPanel.text = formatDisplay(newValue)
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

    init {
        // right click to reset it
        addRightClickListener {
            Menu.openMenu(windowStack, listOf(MenuOption(NameDesc("Reset")) {
                value = resetListener()
            }))
        }
    }

    override fun onPaste(x: Float, y: Float, data: String, type: String) {
        when (type) {
            "PrefabSaveable" -> {
                val instance = StudioBase.dragged?.getOriginal() as? PrefabSaveable
                LOGGER.info("Dropping $instance :)")
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
                    LOGGER.warn("Incorrect type: ${instance.name} is not instance of $clazz, and none of its direct children is either")
                }
            }
            else -> super.onPaste(x, y, data, type)
        }
    }

    companion object {
        private val LOGGER = LogManager.getLogger(SameSceneRefInput::class)
    }
}