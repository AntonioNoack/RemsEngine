package me.anno.engine.ui

import me.anno.ecs.Component
import me.anno.ecs.Entity
import me.anno.ecs.prefab.CAdd
import me.anno.ecs.prefab.CSet
import me.anno.ecs.prefab.Path
import me.anno.ecs.prefab.Prefab
import me.anno.ecs.prefab.PrefabCache.loadPrefab
import me.anno.ecs.prefab.PrefabInspector.Companion.currentInspector
import me.anno.engine.ui.ECSTypeLibrary.Companion.lastSelection
import me.anno.engine.ui.scenetabs.ECSSceneTabs
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import me.anno.io.text.TextReader
import me.anno.io.text.TextWriter
import me.anno.ui.editor.files.FileContentImporter
import me.anno.ui.editor.treeView.AbstractTreeView
import me.anno.ui.style.Style
import me.anno.utils.structures.lists.UpdatingList
import org.apache.logging.log4j.LogManager
import org.joml.Vector4f
import sun.reflect.generics.reflectiveObjects.NotImplementedException

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
        child as Prefab
        if (child.src != InvalidRef) {
            // simple: just add a reference
            when (child.clazzName) {
                "Entity" -> currentInspector!!.addEntityChild(element, child)
                "Component" -> currentInspector!!.addComponentChild(element, child)
                else -> LOGGER.warn("Unknown type of child: ${child.clazzName}")
            }
        } else {
            // not so simple:
            // add prefab to child: modify changes, make path for changes longer
            val type = when (child.getSampleInstance(HashSet())) {
                is Entity -> 'e'
                is Component -> 'c'
                else -> {
                    LOGGER.warn("Unknown type to add: ${child.clazzName}")
                    return
                }
            }
            val path = element.pathInRoot2(null, false)
            val prefab = inspector.prefab
            val add = CAdd(path, type, child.clazzName!!)
            prefab.add(add)
            val index = if (type == 'c') element.components.size else element.children.size
            val instancePath = add.getChildPath(index)
            for(change in child.adds ?: emptyList()){
                prefab.add(change.withPath(Path(instancePath, change.path)))
            }
            // todo either invalidate the inspector, or actually add the child
        }
    }

    override fun addAfter(self: Entity, sibling: Any) {
        sibling as Entity
        self.addAfter(sibling)
        // todo add the changes for this
    }

    override fun addBefore(self: Entity, sibling: Any) {
        sibling as Entity
        self.addBefore(sibling)
        // todo add the changes for this
    }

    override fun removeChild(element: Entity, child: Entity) {
        element.remove(child)
        // todo add the changes for this
    }

    override fun destroy(element: Entity) {
        element.onDestroy()
    }

    override fun getLocalColor(element: Entity, dst: Vector4f): Vector4f {
        val c = if (element.allInHierarchy { it.isEnabled }) 1f else 0.5f
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

    override fun getDragType(element: Entity): String {
        return "PrefabSaveable"
    }

    override fun stringifyForCopy(element: Entity): String {
        val tab = ECSSceneTabs.currentTab ?: return ""
        val root = tab.inspector.root
        if (element == root) {
            return tab.inspector.toString()
        } else {
            val prefab = Prefab(element.className)
            prefab.prefab = element.prefab2?.prefab?.nullIfUndefined() ?: element.prefab2?.src ?: InvalidRef
            prefab.createLists()
            val adders = prefab.adds as ArrayList<CAdd>
            val setters = prefab.sets as ArrayList<CSet>
            val path = element.pathInRoot2(null, false)
            // collect changes from this element going upwards
            var someParent = element
            val collDepth = element.depthInHierarchy
            setters.add(CSet(Path.ROOT_PATH, "name", element.name))
            for (depth in collDepth downTo 0) {
                // the last level doesn't need to be transferred
                someParent = someParent.parentEntity ?: break
                LOGGER.info("checking depth $depth/$collDepth, ${someParent.name}")
                var someRelatedParent = someParent.prefab2
                while (true) {// follow the chain of prefab-inheritance
                    val changes = someRelatedParent?.adds
                    LOGGER.info("changes from $depth/${someRelatedParent?.getPrefabOrSource()}: ${changes?.size}")
                    if (changes != null) {
                        // get all changes
                        // filter them & short them by their filter
                        for (change in changes.mapNotNull { path.getSubPathIfMatching(it, depth) }) {
                            when (change) {
                                is CAdd -> adders.add(change)
                                is CSet -> {
                                    // don't apply changes twice, especially, because the order is reversed
                                    // this would cause errors
                                    if (setters.none { it.path == change.path && it.name == change.name }) {
                                        setters.add(change)
                                    }
                                }
                                else -> throw NotImplementedException()
                            }
                        }
                    }
                    someRelatedParent = getPrefab(someRelatedParent?.prefab) ?: break
                }
            }
            LOGGER.info("found: ${prefab.prefab}, prefab: ${element.prefab2?.prefab}, own file: ${element.prefab2?.src}, has prefab: ${element.prefab2 != null}")
            return TextWriter.toText(prefab)
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
            is Prefab -> TODO("paste prefab")
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