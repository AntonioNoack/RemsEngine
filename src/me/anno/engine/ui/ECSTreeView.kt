package me.anno.engine.ui

import me.anno.ecs.Component
import me.anno.ecs.Entity
import me.anno.ecs.components.light.LightComponent
import me.anno.ecs.prefab.Hierarchy
import me.anno.ecs.prefab.Prefab
import me.anno.ecs.prefab.PrefabCache.loadPrefab
import me.anno.ecs.prefab.PrefabInspector.Companion.currentInspector
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.ecs.prefab.change.CSet
import me.anno.ecs.prefab.change.Path
import me.anno.engine.ui.scenetabs.ECSSceneTabs
import me.anno.io.files.FileReference
import me.anno.io.text.TextReader
import me.anno.language.translation.NameDesc
import me.anno.ui.base.menu.Menu.askName
import me.anno.ui.editor.files.FileContentImporter
import me.anno.ui.editor.treeView.AbstractTreeView
import me.anno.ui.style.Style
import me.anno.utils.Color.normARGB
import me.anno.utils.maths.Maths.mixARGB
import me.anno.utils.structures.lists.UpdatingList
import org.apache.logging.log4j.LogManager
import javax.xml.bind.Element

// todo runtime and pre-runtime view
// todo unity oriented
// todo easily add stuff
// todo add prefabs
// todo generally: prefabs

// todo easy scripting
// todo support many languages at runtime via scripting
// todo compile all scripting languages for export? <3

// todo add / remove components
// todo reorder them by dragging
// todo just generalize the TreeViewPanel? sounds like a good idea :)


// todo switch between programming language styles easily, throughout the code?... idk whether that's possible...
// maybe on a per-function-basis

class ECSTreeView(val library: ECSTypeLibrary, isGaming: Boolean, style: Style) :
    AbstractTreeView<Entity>(
        UpdatingList { listOf(library.world) },
        ECSFileImporter as FileContentImporter<Entity>,
        false,
        style
    ) {

    val inspector get() = currentInspector!!

    override fun addChild(element: Entity, child: Any) {
        val entityIndex = child is Entity || (child is Prefab && child.clazzName == "Entity")
        val index = if(entityIndex) element.children.size else element.components.size
        addChild(element, child, index)
    }

    fun addChild(element: Entity, child: Any, index: Int) {
        when (child) {
            is Prefab -> {
                val elementRoot = element.root
                val dstPath = element.pathInRoot2(element, true)
                Hierarchy.add(
                    child,
                    Path.ROOT_PATH,
                    elementRoot.prefab2!!,
                    dstPath,
                    element
                )
            }
            is PrefabSaveable -> {
                val childRoot = child.root
                val elementRoot = element.root
                val dstPath = element.pathInRoot2(element, true)
                dstPath.setLast(child.name, index, element.getTypeOf(child))
                Hierarchy.add(
                    childRoot.prefab2!!,
                    child.pathInRoot2(childRoot, false),
                    elementRoot.prefab2!!,
                    dstPath,
                    element
                )
            }
            else -> {
                LOGGER.warn("Unknown type $child")
            }
        }
    }

    override fun addAfter(self: Entity, sibling: Any) {
        // self.addAfter(sibling as Entity)
        addChild(self.parentEntity!!, sibling, self.indexInParent!! + 1)
    }

    override fun addBefore(self: Entity, sibling: Any) {
        // self.addBefore(sibling as Entity)
        addChild(self.parentEntity!!, sibling, self.indexInParent!!)
    }

    override fun removeChild(element: Entity, child: Entity) {
        // element.remove(child)
        val dstPath = child.pathInRoot2()
        Hierarchy.remove(element.root.prefab2!!, dstPath)
    }

    override fun destroy(element: Entity) {
        // element.onDestroy()
    }

    override fun getLocalColor(element: Entity, isHovered: Boolean, isInFocus: Boolean): Int {
        val isEnabled = element.allInHierarchy { it.isEnabled }
        var color = if (isEnabled)
            if (isInFocus) 0xffcc15 else 0xcccccc
        else
            if (isInFocus) 0xcc15ff else 0x333333
        // if is light component, we can mix in its color
        val light = element.getComponent(LightComponent::class)
        if (light != null) color = mixARGB(color, normARGB(light.color), 0.5f)
        if (isHovered) color = mixARGB(color, -1, 0.5f)
        return color or (255 shl 24)
    }

    override fun getTooltipText(element: Entity): String {
        var desc = element.description
        val maxLength = 100
        desc = when (desc.length) {
            0 -> ""
            in 1..maxLength -> desc
            else -> desc.substring(0, maxLength - 3) + "..."
        }
        val descLn = if (desc.isEmpty()) desc else desc + "\n"
        return when {
            element.children.isEmpty() -> when (element.components.size) {
                0 -> desc
                1, 2, 3 -> descLn + element.components.joinToString { it.className }
                else -> descLn + "${element.components.size} C"
            }
            element.components.isEmpty() -> descLn + "${element.children.size} E, ${element.sizeOfAllChildren} total"
            else -> descLn + "${element.children.size} E + ${element.components.size} C, ${element.sizeOfAllChildren} total"
        }
    }

    override fun getChildren(element: Entity): List<Entity> {
        return element.children
    }

    override fun isCollapsed(element: Entity): Boolean {
        return element.isCollapsed
    }

    override fun setCollapsed(element: Entity, collapsed: Boolean) {
        element.isCollapsed = collapsed
        element.root.prefab2!!.add(CSet(element.pathInRoot2(), "isCollapsed", collapsed))
        needsTreeUpdate = true
        invalidateLayout()
    }

    override fun getDragType(element: Entity): String {
        return "PrefabSaveable"
    }

    override fun stringifyForCopy(element: Entity): String {
        val tab = ECSSceneTabs.currentTab ?: return ""
        val root = tab.inspector.root
        return if (element == root) {
            tab.inspector.toString()
        } else {
            Hierarchy.stringify(element)
        }
    }

    private fun getPrefab(ref: FileReference?): Prefab? {
        return loadPrefab(ref)
    }

    override fun getSymbol(element: Entity): String {
        return ""
    }

    override fun getParent(element: Entity): Entity? {
        return element.parent as? Entity
    }

    override fun getName(element: Entity): String {
        return element.name.ifBlank { "Entity" }
    }

    override fun setName(element: Entity, name: String) {
        element.name = name
    }

    override fun openAddMenu(parent: Entity) {
        // todo open add menu for often created entities: camera, light, nodes, ...
        // we could use which prefabs were most often created :)
        // temporary solution:
        askName(NameDesc("Name"), "Entity", NameDesc(), { -1 }) {
            val child = Entity()
            val prefab = parent.root.prefab2!!
            val dstPath = parent.pathInRoot2()
            Hierarchy.add(prefab, dstPath, parent, child)
        }
    }

    override fun canBeInserted(parent: Entity, element: Entity, index: Int): Boolean {
        return parent.prefab.run { this == null || index >= children.size }
    }

    override fun canBeRemoved(element: Entity): Boolean {
        val indexInParent = element.indexInParent!!
        val parent = element.parent!!
        val parentPrefab = parent.prefab
        return parentPrefab == null || indexInParent >= parentPrefab.children.size
    }

    override val selectedElement: Entity? = library.selected as? Entity

    override fun selectElement(element: Entity?) {
        library.select(element)
    }

    override fun focusOnElement(element: Entity) {
        selectElement(element)
        // todo focus on the element by inverting the camera transform and such...
    }

    override fun onPaste(x: Float, y: Float, data: String, type: String) {
        if (!tryPaste(data)) {
            super.onPaste(x, y, data, type)
        }
    }

    /**
     * returns true on success
     * */
    private fun tryPaste(data: String): Boolean {
        return when (val element = TextReader.read(data).firstOrNull()) {
            is Prefab -> {
                val root = library.getWorld().root
                Hierarchy.add(root.prefab2!!, Path.ROOT_PATH, element, Path.ROOT_PATH, root)
                true
            }
            is Entity -> TODO("paste entity somehow")
            is Component -> TODO("paste component somehow")
            else -> {
                LOGGER.warn("Unknown type ${element?.className}")
                false
            }
        }
    }

    override val className get() = "ECSTreeView"

    companion object {
        private val LOGGER = LogManager.getLogger(ECSTreeView::class)
        /*fun listOfVisible(root: ECSWorld, isGaming: Boolean): List<Entity> {
            return if (isGaming) listOf(
                root.globallyShared,
                root.playerPrefab,
                root.locallyShared,
                root.localPlayers,
                root.remotePlayers
            )
            else listOf(root.globallyShared, root.playerPrefab, root.locallyShared)
        }*/
    }

}