package me.anno.engine.ui

import me.anno.ecs.prefab.PrefabSaveable
import me.anno.language.translation.NameDesc
import me.anno.studio.StudioBase.Companion.dragged
import me.anno.ui.base.menu.Menu.msg
import me.anno.ui.base.menu.Menu.openMenu
import me.anno.ui.base.menu.MenuOption
import me.anno.ui.base.text.TextPanel
import me.anno.ui.style.Style
import kotlin.reflect.KClass

// todo click, then open tree view to select it (?)
/**
 * input panel for drag-dropping references to instances in the same scene
 * */
class PrefabSaveableInput<Type : PrefabSaveable>(val title: String, val clazz: KClass<*>, value0: Type?, style: Style) :
    TextPanel("$title: ${value0?.name}", style) {

    var value = value0
        set(value) {
            field = value
            changeListener(value)
            update(value)
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

    fun update(value0: Type?) {
        text = "$title: ${value0?.name}"
        invalidateDrawing()
    }

    override fun onPaste(x: Float, y: Float, data: String, type: String) {
        when (type) {
            "PrefabSaveable" -> {
                val instance = dragged!!.getOriginal() as PrefabSaveable
                if (clazz.isInstance(instance)) {
                    @Suppress("UNCHECKED_CAST")
                    value = instance as Type
                } else {
                    // check all children, if there is any match
                    for (childType in instance.listChildTypes()) {
                        for (child in instance.getChildListByType(childType)) {
                            if (clazz.isInstance(child)) {
                                @Suppress("UNCHECKED_CAST")
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

}