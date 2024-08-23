package me.anno.engine.ui.input

import me.anno.ecs.prefab.PrefabSaveable
import me.anno.engine.ui.scenetabs.ECSSceneTabs
import me.anno.ui.editor.files.FileContentImporter
import me.anno.ui.editor.treeView.TreeView
import me.anno.utils.Color.white
import me.anno.utils.structures.Collections.setContains
import me.anno.utils.structures.lists.Lists.firstOrNull2
import org.apache.logging.log4j.LogManager

/**
 * helper class for SameSceneRefInput
 * */
class SameSceneRefTreeView<V : PrefabSaveable?>(val sameSceneRefInput: SameSceneRefInput<V>) :
    TreeView<PrefabSaveable>(FileContentImporter(), true, sameSceneRefInput.style) {

    companion object {
        private val LOGGER = LogManager.getLogger(SameSceneRefTreeView::class)
    }

    private val notCollapsed = HashSet<PrefabSaveable>()
    override fun listRoots(): List<PrefabSaveable> {
        val instance = ECSSceneTabs.currentTab?.inspector?.prefab?.getSampleInstance()
        return if (instance != null) {
            notCollapsed.add(instance)
            listOf(instance)
        } else emptyList()
    }

    override fun getLocalColor(element: PrefabSaveable, isHovered: Boolean, isInFocus: Boolean): Int {
        val type = when {
            sameSceneRefInput.clazz.isInstance(element) -> 5
            sameSceneRefInput.containsType(element) -> 4
            else -> 0
        }
        return SameSceneRefInput.getColor(white, type)
    }

    override fun isValidElement(element: Any?): Boolean {
        return element is PrefabSaveable
    }

    override fun getDragType(element: PrefabSaveable): String {
        return "PrefabSaveable" // ??? todo should be disabled
    }

    override fun canBeInserted(parent: PrefabSaveable, element: PrefabSaveable, index: Int): Boolean = false
    override fun canBeRemoved(element: PrefabSaveable): Boolean = false
    override fun stringifyForCopy(element: PrefabSaveable): String = getName(element)

    // todo disable this somehow...
    override fun setName(element: PrefabSaveable, name: String) {
        LOGGER.warn("Setting name is not supported")
    }

    override fun getName(element: PrefabSaveable): String {
        return SameSceneRefInput.formatDisplay0(element, false)
    }

    override fun destroy(element: PrefabSaveable) {}

    override fun getParent(element: PrefabSaveable): PrefabSaveable? = element.parent

    override fun removeRoot(root: PrefabSaveable) {}
    override fun removeChild(parent: PrefabSaveable, child: PrefabSaveable) {}

    override fun addChild(element: PrefabSaveable, child: Any, type: Char, index: Int): Boolean {
        return false
    }

    override fun setCollapsed(element: PrefabSaveable, collapsed: Boolean) {
        notCollapsed.setContains(element, !collapsed)
    }

    override fun isCollapsed(element: PrefabSaveable): Boolean {
        return element !in notCollapsed
    }

    override fun getChildren(element: PrefabSaveable): List<PrefabSaveable> {
        return element.listChildTypes().flatMap { type ->
            element.getChildListByType(type)
        }
    }

    override fun openAddMenu(parent: PrefabSaveable) {
        LOGGER.warn("Cannot add things")
    }

    override fun focusOnElement(element: PrefabSaveable): Boolean {
        // can we do that?
        // if we would, the selection would change, and our original panel would become invalid
        selectElements(listOf(element))
        return true
    }

    override fun selectElements(elements: List<PrefabSaveable>) {
        val sample = elements.firstOrNull2 { sameSceneRefInput.clazz.isInstance(it) }
            ?: elements.mapNotNull { instance ->
                instance.listChildTypes().flatMap { type ->
                    instance.getChildListByType(type)
                }.firstOrNull { child ->
                    sameSceneRefInput.clazz.isInstance(child)
                }
            }.firstOrNull() ?: sameSceneRefInput.value
        if (sample != null) {
            @Suppress("unchecked_cast")
            sameSceneRefInput.setValue(sample as V, true)
        }
    }
}