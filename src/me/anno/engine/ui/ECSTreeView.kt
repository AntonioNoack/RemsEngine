package me.anno.engine.ui

import me.anno.ecs.Entity
import me.anno.ecs.components.light.LightComponent
import me.anno.ecs.prefab.Hierarchy
import me.anno.ecs.prefab.Prefab
import me.anno.ecs.prefab.PrefabInspector.Companion.currentInspector
import me.anno.ecs.prefab.PrefabInspector.Companion.formatWarning
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.ecs.prefab.change.Path
import me.anno.engine.ui.render.RenderView
import me.anno.engine.ui.scenetabs.ECSSceneTabs
import me.anno.io.ISaveable
import me.anno.io.text.TextReader
import me.anno.language.translation.NameDesc
import me.anno.ui.base.Panel
import me.anno.ui.base.menu.Menu.openMenu
import me.anno.ui.base.menu.MenuOption
import me.anno.ui.editor.PropertyInspector
import me.anno.ui.editor.files.FileContentImporter
import me.anno.ui.editor.treeView.TreeView
import me.anno.ui.style.Style
import me.anno.utils.Color.normARGB
import me.anno.maths.Maths.length
import me.anno.maths.Maths.mixARGB
import me.anno.utils.strings.StringHelper.camelCaseToTitle
import me.anno.utils.strings.StringHelper.shorten
import me.anno.utils.structures.lists.UpdatingList
import me.anno.utils.types.AABBs.deltaX
import me.anno.utils.types.AABBs.deltaY
import me.anno.utils.types.AABBs.deltaZ
import me.anno.utils.types.Lists.flatten
import me.anno.utils.types.Strings.isBlank2
import org.apache.logging.log4j.LogManager

// todo runtime and pre-runtime view

// todo easy scripting
// todo support many languages at runtime via scripting
// todo compile all scripting languages for export? <3

// todo add / remove components
// todo reorder them by dragging


// todo switch between programming language styles easily, throughout the code?... idk whether that's possible...
// maybe on a per-function-basis

class ECSTreeView(val library: EditorState, isGaming: Boolean, style: Style) :
    TreeView<PrefabSaveable>(
        UpdatingList { listOf(library.world) },
        ECSFileImporter as FileContentImporter<PrefabSaveable>,
        true,
        style
    ) {

    val inspector get() = currentInspector!!

    override fun addChild(element: PrefabSaveable, child: Any) {
        if (element !is Entity) return
        val entityIndex = child is Entity || (child is Prefab && child.clazzName == "Entity")
        val index = if (entityIndex) element.children.size else element.components.size
        addChild(element, child, index)
    }

    fun addChild(element: PrefabSaveable, child: Any, index: Int) {
        when (child) {
            is Prefab -> {
                val elementRoot = element.root
                val dstPath = element.prefabPath!!
                Hierarchy.add(
                    child,
                    Path.ROOT_PATH,
                    elementRoot.prefab!!,
                    dstPath,
                    element
                )
            }
            is PrefabSaveable -> {
                val childRoot = child.root
                val elementRoot = element.root
                val dstPath = element.prefabPath!!
                // dstPath.setLast(child.name, index, element.getTypeOf(child))
                // name must never change,
                // type must never change
                // index can change
                dstPath.index = index
                Hierarchy.add(
                    childRoot.prefab!!,
                    child.prefabPath!!,
                    elementRoot.prefab!!,
                    dstPath,
                    element
                )
            }
            else -> {
                LOGGER.warn("Unknown type $child")
            }
        }
    }

    override fun addAfter(self: PrefabSaveable, sibling: Any) {
        // self.addAfter(sibling as Entity)
        addChild(self.parent as PrefabSaveable, sibling, self.indexInParent + 1)
    }

    override fun addBefore(self: PrefabSaveable, sibling: Any) {
        // self.addBefore(sibling as Entity)
        addChild(self.parent as PrefabSaveable, sibling, self.indexInParent)
    }

    override fun removeChild(element: PrefabSaveable, child: PrefabSaveable) {
        // todo somehow the window element cannot be removed
        LOGGER.info("Trying to remove element")
        Hierarchy.removePathFromPrefab(element.root.prefab!!, child)
    }

    override fun destroy(element: PrefabSaveable) {
        // element.onDestroy()
    }

    private fun getWarning(element: PrefabSaveable): String? {
        for (warn in element.getReflections().debugWarnings) {
            val value = warn.getter.call(element)
            if (value != null) return formatWarning(warn.name, value)
        }
        if (element.isCollapsed) {// could become expensive... so only go to a certain depth
            for (childType in element.listChildTypes()) {
                for (child in element.getChildListByType(childType)) {
                    for (warn in child.getReflections().debugWarnings) {
                        val value = warn.getter.call(child)
                        if (value != null) return formatWarning(warn.name, value)
                    }
                }
            }
        }
        return null
    }

    private fun hasWarning(element: PrefabSaveable): Boolean {
        for (warn in element.getReflections().debugWarnings) {
            val value = warn.getter.call(element)
            if (value != null) return true
        }
        if (element.isCollapsed) {// could become expensive... so only go to a certain depth
            for (childType in element.listChildTypes()) {
                for (child in element.getChildListByType(childType)) {
                    for (warn in child.getReflections().debugWarnings) {
                        val value = warn.getter.call(child)
                        if (value != null) return true
                    }
                }
            }
        }
        return false
    }

    override fun getLocalColor(element: PrefabSaveable, isHovered: Boolean, isInFocus: Boolean): Int {

        val isInFocus2 = isInFocus || element in library.selection
        // show a special color, if the current element contains something selected

        val isIndirectlyInFocus = !isInFocus2
                // && library.selection.isNotEmpty()
                && library.selection.any { it is PrefabSaveable && it.anyInHierarchy { p -> p === element } }
        // element.findFirstInAll { it in library.selection } != null
        val isEnabled = element.allInHierarchy { it.isEnabled }
        var color = if (isEnabled)
            if (isInFocus2) 0xffcc15 else if (isIndirectlyInFocus) 0xddccaa else 0xcccccc
        else
            if (isInFocus2) 0xcc15ff else if (isIndirectlyInFocus) 0x442255 else 0x333333
        // if is light component, we can mix in its color
        val light = if (element is Entity) element.getComponent(LightComponent::class) else element as? LightComponent
        if (light != null) color = mixARGB(color, normARGB(light.color), 0.5f)
        if (isHovered) color = mixARGB(color, -1, 0.5f)
        if (hasWarning(element)) color = mixARGB(color, 0xffff00, 0.8f)
        return color or (255 shl 24)
    }

    override fun getTooltipText(element: PrefabSaveable): String {
        val maxLength = 100
        val warn = getWarning(element)
        if (warn != null) return warn
        val desc = element.description.shorten(maxLength).toString()
        val descLn = if (desc.isEmpty()) desc else desc + "\n"
        return when {
            element is Panel -> element.tooltip ?: desc
            element !is Entity -> desc
            element.children.isEmpty() -> when (element.components.size) {
                0 -> desc
                1, 2, 3 -> descLn + element.components.joinToString { it.className }
                else -> descLn + "${element.components.size} C"
            }
            element.components.isEmpty() -> descLn + "${element.children.size} E, ${element.sizeOfAllChildren} total"
            else -> descLn + "${element.children.size} E + ${element.components.size} C, ${element.sizeOfAllChildren} total"
        }
    }

    override fun getChildren(element: PrefabSaveable): List<PrefabSaveable> {
        val types = element.listChildTypes()
        return when (types.length) {
            0 -> emptyList()
            1 -> element.getChildListByType(types[0])
            else -> {
                val childCount = types.sumOf { element.getChildListByType(it).size }
                val joined = ArrayList<PrefabSaveable>(childCount)
                for (type in types) {
                    joined += element.getChildListByType(type)
                }
                joined
            }
        }
    }

    override fun isCollapsed(element: PrefabSaveable): Boolean {
        return element.isCollapsed
    }

    override fun setCollapsed(element: PrefabSaveable, collapsed: Boolean) {
        element.isCollapsed = collapsed
        try {
            element.root.prefab!!.set(element.prefabPath!!, "isCollapsed", collapsed)
        } catch (e: Exception) {
        } // idc too much about saving that property; main thing is that we can collapse and expand stuff in the editor
        needsTreeUpdate = true
        invalidateLayout()
    }

    override fun getDragType(element: PrefabSaveable): String {
        return "PrefabSaveable"
    }

    override fun stringifyForCopy(element: PrefabSaveable): String {
        val tab = ECSSceneTabs.currentTab ?: return ""
        val root = tab.inspector.root
        return if (element == root) {
            tab.inspector.toString()
        } else {
            Hierarchy.stringify(element)
        }
    }

    /*private fun getPrefab(ref: FileReference?): Prefab? {
        return loadPrefab(ref)
    }*/

    override fun getSymbol(element: PrefabSaveable): String {
        return if (element.root.prefab?.isWritable == false) "\uD83D\uDD12" else "⚪"
    }

    override fun getParent(element: PrefabSaveable): Entity? {
        return element.parent as? Entity
    }

    override fun getName(element: PrefabSaveable): String {
        val name = element.name
        return if (name.isBlank2()) element.className.camelCaseToTitle() else name
    }

    override fun setName(element: PrefabSaveable, name: String) {
        element.name = name
    }

    override fun openAddMenu(parent: PrefabSaveable) {
        // temporary solution:
        val prefab = parent.prefab!!
        if (prefab.isWritable) {
            // open add menu for often created entities: camera, light, nodes, ...
            // we could use which prefabs were most often created :)
            val classes = parent.listChildTypes().map { parent.getOptionsByType(it) }
                .flatten()
                .apply { sortBy { it.title } }
            openMenu(
                windowStack,
                classes.map { option ->
                    val sampleInstance = option.generator() as ISaveable
                    val className = sampleInstance.className
                    val title = option.title
                    MenuOption(NameDesc(title)) {
                        val nameId = Path.generateRandomId()
                        val path = prefab.add(
                            parent.prefabPath!!,
                            if (sampleInstance is Entity) 'e' else 'c',
                            className,
                            nameId
                        )
                        val child = option.generator() as PrefabSaveable
                        child.prefabPath = path
                        child.prefab = prefab
                        parent.addChild(child)
                        PropertyInspector.invalidateUI()
                    }
                }
            )
        } else LOGGER.warn("Prefab is not writable!")
    }

    override fun canBeInserted(parent: PrefabSaveable, element: PrefabSaveable, index: Int): Boolean {
        return parent.getOriginal().run { this == null || index >= children.size }
    }

    override fun canBeRemoved(element: PrefabSaveable): Boolean {
        val indexInParent = element.indexInParent
        val parent = element.parent!!
        val parentPrefab = parent.getOriginal()
        return parentPrefab == null || indexInParent >= parentPrefab.children.size
    }

    override fun selectElement(element: PrefabSaveable?) {
        library.select(element)
    }

    override fun focusOnElement(element: PrefabSaveable) {
        selectElement(element)
        // focus on the element by inverting the camera transform and such...
        val windowStack = window!!.windowStack
        if (element is Entity) for (window in windowStack) {
            window.panel.forAll {
                if (it is RenderView) {
                    // not perfect, but good enough probably
                    // todo smooth lerp over .2s for orientation?
                    val aabb = element.aabb
                    val newRadius = length(aabb.deltaX(), aabb.deltaY(), aabb.deltaZ())
                    if (newRadius.isFinite() && newRadius > 0.0) it.radius = newRadius
                    it.position.set(element.transform.globalPosition)
                    it.updateEditorCameraTransform()
                }
            }
        }
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
        return when (val element = TextReader.read(data, true).firstOrNull()) {
            is Prefab -> {
                val root = library.world
                Hierarchy.add(root.prefab!!, Path.ROOT_PATH, element, Path.ROOT_PATH, root)
                true
            }
            is PrefabSaveable -> TODO("paste prefab saveable somehow")
            else -> {
                LOGGER.warn("Unknown type ${element?.className}")
                false
            }
        }
    }

    override val className get() = "ECSTreeView"

    companion object {
        private val LOGGER = LogManager.getLogger(ECSTreeView::class)
    }

}