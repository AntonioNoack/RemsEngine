package me.anno.engine.ui

import me.anno.ecs.prefab.PrefabSaveable
import me.anno.language.translation.NameDesc
import me.anno.studio.StudioBase.Companion.dragged
import me.anno.ui.base.menu.Menu.msg
import me.anno.ui.base.menu.Menu.openMenu
import me.anno.ui.base.menu.MenuOption
import me.anno.ui.base.text.TextPanel
import me.anno.ui.Style
import me.anno.utils.strings.StringHelper.camelCaseToTitle
import org.apache.logging.log4j.LogManager
import kotlin.reflect.KClass

// todo click, then open tree view to select it (?)
/**
 * input panel for drag-dropping references to instances in the same scene
 * */
class PrefabSaveableInput<Type : PrefabSaveable>(val title: String, val clazz: Class<*>, value0: Type?, style: Style) :
    TextPanel("$title: ${getName(value0)}", style) {

    var value = value0
        set(value) {
            field = value
            text = "$title: ${getName(value)}"
            changeListener(value)
        }

    private var changeListener: (v: Type?) -> Unit = {}
    fun setChangeListener(changeListener: (v: Type?) -> Unit) {
        this.changeListener = changeListener
    }

    private var resetListener: () -> Type? = { value0 }
    fun setResetListener(resetListener: () -> Type?) {
        this.resetListener = resetListener
    }

    init {
        // right click to reset it
        addRightClickListener {
            openMenu(windowStack, listOf(MenuOption(NameDesc("Reset")) {
                value = resetListener()
            }))
        }
    }

    override fun onPaste(x: Float, y: Float, data: String, type: String) {
        when (type) {
            "PrefabSaveable" -> {
                val instance = dragged!!.getOriginal() as? PrefabSaveable
                if (instance == null) {
                    LOGGER.warn("Dragged instance was not PrefabSaveable")
                } else if (clazz.isInstance(instance)) {
                    @Suppress("unchecked_cast")
                    value = instance as Type
                } else {
                    // check all children, if there is any match
                    for (childType in instance.listChildTypes()) {
                        for (child in instance.getChildListByType(childType)) {
                            if (clazz.isInstance(child)) {
                                @Suppress("unchecked_cast")
                                value = child as Type
                                return
                            }
                        }
                    }
                    msg(
                        windowStack, NameDesc(
                            "Incorrect type",
                            "${instance.name} is not instance of $clazz, and none of its direct children is either",
                            ""
                        )
                    )
                }
            }
            else -> super.onPaste(x, y, data, type)
        }
    }

    companion object {

        fun getName(value: PrefabSaveable?): String {
            value ?: return "null"
            return value.name.ifEmpty { value.className.camelCaseToTitle() }
        }

        private val LOGGER = LogManager.getLogger(PrefabSaveableInput::class)
    }

}