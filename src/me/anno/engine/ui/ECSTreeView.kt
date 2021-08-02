package me.anno.engine.ui

import me.anno.ecs.Entity
import me.anno.engine.ui.ECSTypeLibrary.Companion.lastSelection
import me.anno.ui.editor.files.FileContentImporter
import me.anno.ui.editor.treeView.AbstractTreeView
import me.anno.ui.style.Style
import me.anno.utils.structures.lists.UpdatingList
import org.joml.Vector4f

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

    override fun destroy(element: Entity) {
        element.onDestroy()
    }

    override fun getLocalColor(element: Entity, dst: Vector4f): Vector4f {
        val c = if (element.listOfHierarchy.all { it.isEnabled }) 1f else 0.5f
        dst.set(1f, 1f, 1f, c)
        return dst
    }

    override fun getChildren(element: Entity): List<Entity> {
        return element.children
    }

    override fun isCollapsed(element: Entity): Boolean {
        return element.isCollapsed
    }

    override fun setCollapsed(element: Entity, collapsed: Boolean) {
        element.isCollapsed = collapsed
        needsTreeUpdate = true
        invalidateLayout()
    }

    override fun addAfter(self: Entity, sibling: Entity) {
        self.addAfter(sibling)
        // todo notify the inspector
    }

    override fun addBefore(self: Entity, sibling: Entity) {
        self.addBefore(sibling)
        // todo notify the inspector
    }

    override fun addChild(element: Entity, child: Entity) {
        element.add(child)
        // todo notify the inspector
    }

    override fun removeChild(element: Entity, child: Entity) {
        element.remove(child)
        // todo notify the inspector
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
        parent.add(Entity())
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

    override val selectedElement: Entity? = library.selection as? Entity

    override fun selectElement(element: Entity?) {
        library.select(element)
        lastSelection = element
    }

    override fun focusOnElement(element: Entity) {
        selectElement(element)
        // todo focus on the element by inverting the camera transform and such...
    }

    override val className get() = "ECSTreeView"

    companion object {
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