package me.anno.engine.ui.input

import me.anno.config.DefaultStyle
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.engine.EngineBase
import me.anno.language.translation.DefaultNames
import me.anno.language.translation.NameDesc
import me.anno.ui.Panel
import me.anno.ui.Style
import me.anno.ui.base.buttons.TextButton
import me.anno.ui.base.groups.PanelListX
import me.anno.ui.base.groups.TitledListY
import me.anno.ui.base.menu.Menu
import me.anno.ui.base.menu.MenuOption
import me.anno.ui.base.text.TextPanel
import me.anno.ui.base.text.TextStyleable
import me.anno.ui.editor.treeView.TreeViewEntryPanel
import me.anno.ui.input.InputPanel
import me.anno.ui.input.InputVisibility
import me.anno.utils.Color.hex32
import me.anno.utils.Color.mixARGB
import me.anno.utils.structures.lists.Lists.count2
import me.anno.utils.types.Strings.ifBlank2
import org.apache.logging.log4j.LogManager
import kotlin.reflect.KClass

/**
 * input panel for drag-dropping references to instances in the same scene
 * */
class SameSceneRefInput<Type : PrefabSaveable?>(
    nameDesc: NameDesc,
    visibilityKey: String,
    val clazz: KClass<*>,
    value0: Type,
    style: Style
) : TitledListY(nameDesc, visibilityKey, style), InputPanel<Type>, TextStyleable {

    val list = object : PanelListX(style) {
        override var isEnabled: Boolean
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
            val dragged = EngineBase.dragged?.getOriginal()
            return when {
                clazz.isInstance(dragged) -> 5
                containsType(dragged) -> 4
                dragged != null -> -2
                // check if hovered element is potentially it xD
                rootPanel.anyInChildren({ it.isHovered }) {
                    it is TreeViewEntryPanel<*> && clazz.isInstance(it.getElement())
                } -> 3
                rootPanel.anyInChildren({ it.isHovered }) {
                    it.isHovered && it is TreeViewEntryPanel<*> && containsType(it.getElement())
                } -> 2
                rootPanel.anyInChildren({ it.isHovered }) {
                    it.isHovered && it is TreeViewEntryPanel<*> && it.getElement() is PrefabSaveable
                } -> -1
                else -> 0
            }
        }

        override val effectiveTextColor: Int
            get() = getColor(super.effectiveTextColor, isDraggingGoodType())
    }.apply {
        padding.set(valueButton.padding)
        setTooltip("Drag entities or components of matching type (${clazz.simpleName}) here.")
    }

    init {
        valueButton.addLeftClickListener {
            // open tree view to select an element without drag-and-drop
            val oldValue: Type = value
            val panel = SameSceneRefTreeView(this)
            val acceptButton = TextButton(DefaultNames.accept, style)
                .addLeftClickListener {
                    it.window?.close()
                }
            val cancelButton = TextButton(DefaultNames.cancel, style)
                .addLeftClickListener {
                    setValue(oldValue, true)
                    it.window?.close()
                }
            val buttonList = PanelListX(style)
            buttonList.add(acceptButton)
            acceptButton.weight = 1f
            buttonList.add(cancelButton)
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
        valueButton.text = formatDisplay0(newValue, true)
        return this
    }

    private var changeListener: (v: Type) -> Unit = {}
    fun setChangeListener(changeListener: (v: Type) -> Unit): SameSceneRefInput<Type> {
        this.changeListener = changeListener
        return this
    }

    private var resetListener: () -> Type = { value0 }
    fun setResetListener(resetListener: () -> Type): SameSceneRefInput<Type> {
        this.resetListener = resetListener
        return this
    }

    override fun onPaste(x: Float, y: Float, data: String, type: String) {
        when (type) {
            "PrefabSaveable" -> {
                val instance = EngineBase.dragged?.getOriginal() as? PrefabSaveable
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

        fun formatDisplay0(value: PrefabSaveable?, forPrimary: Boolean): String {
            val prefix = if (forPrimary) "\uD83D\uDCC1 " else ""
            value ?: return "${prefix}null"
            return "$prefix${value.name.ifBlank2(value.className)} #${hex32(hashCode(value))}"
        }

        fun formatDisplay(value: PrefabSaveable?, forPrimary: Boolean): NameDesc {
            return NameDesc(formatDisplay0(value, forPrimary))
        }

        fun hashCode(value: PrefabSaveable): Int {
            return value.prefabPath.hashCode()
        }

        fun getColor(baseColor: Int, type: Int): Int {
            return mixARGB(
                baseColor,
                if (type < 0) DefaultStyle.errorRed // bad
                else DefaultStyle.greatGreen, // good
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