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
import me.anno.maths.Maths.length
import me.anno.maths.Maths.mixARGB
import me.anno.studio.StudioBase
import me.anno.ui.Panel
import me.anno.ui.base.menu.Menu.openMenu
import me.anno.ui.base.menu.MenuOption
import me.anno.ui.editor.PropertyInspector
import me.anno.ui.editor.files.FileContentImporter
import me.anno.ui.editor.treeView.TreeView
import me.anno.ui.editor.treeView.TreeViewPanel
import me.anno.ui.style.Style
import me.anno.utils.Color.normARGB
import me.anno.utils.strings.StringHelper.camelCaseToTitle
import me.anno.utils.strings.StringHelper.shorten
import me.anno.utils.structures.lists.Lists.flatten
import me.anno.utils.structures.lists.UpdatingList
import me.anno.utils.types.Strings.isBlank2
import org.apache.logging.log4j.LogManager

class ECSTreeView(val library: EditorState, style: Style) :
    TreeView<ISaveable>(
        UpdatingList {
            val world = library.prefab?.getSampleInstance()// ?: library.world
            if (world != null) listOf(world) else emptyList()
        },
        ECSFileImporter as FileContentImporter<ISaveable>,
        true,
        style
    ) {

    val inspector get() = currentInspector!!

    override fun isValidElement(element: Any?): Boolean {
        return element is PrefabSaveable
    }

    override fun toggleCollapsed(element: ISaveable) {
        val isCollapsed = isCollapsed(element)
        val target = !isCollapsed
        // remove children from the selection???...
        val targets = content.children.filterIsInstance<TreeViewPanel<*>>().filter { it.isInFocus }
        if (targets.isEmpty()) {
            setCollapsed(element, target)
        } else {
            for (it in targets) {
                val element2 = it.getElement()
                if (element2 is PrefabSaveable) setCollapsed(element2, target)
            }
        }
    }

    override fun addChild(element: ISaveable, child: Any, index: Int) {
        element as PrefabSaveable
        val prefab: Prefab
        val prefabPath: Path
        when (child) {
            is Prefab -> {
                prefab = child
                prefabPath = Path.ROOT_PATH
            }
            is PrefabSaveable -> {
                prefab = child.root.prefab!!
                prefabPath = child.prefabPath!!
            }
            else -> {
                LOGGER.warn("Unknown type $child")
                return
            }
        }
        Hierarchy.add(
            prefab,
            prefabPath,
            element,
            index
        )
    }

    override fun getIndexInParent(parent: ISaveable, child: ISaveable): Int {
        // todo we need the add index (inside the current prefab) for this to work correctly
        // todo or we should support complete reordering
        LOGGER.warn("getIndexInParent isn't implemented fully")
        return if (child is PrefabSaveable) {
            child.indexInParent
        } else 0
    }

    override fun removeChild(parent: ISaveable, child: ISaveable) {
        // todo somehow the window element cannot be removed
        if (parent is PrefabSaveable && child is PrefabSaveable) {
            LOGGER.info("Trying to remove element ${child.className} from ${parent.className}")
            EditorState.selection = EditorState.selection.filter { it !in child.listOfHierarchy }
            Hierarchy.removePathFromPrefab(parent.root.prefab!!, child)
        } else throw NotImplementedError()
    }

    override fun destroy(element: ISaveable) {
        if (element is PrefabSaveable) element.onDestroy()
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

    override fun getLocalColor(element: ISaveable, isHovered: Boolean, isInFocus: Boolean): Int {

        val isInFocus2 = isInFocus || (element is PrefabSaveable && element in library.selection)
        // show a special color, if the current element contains something selected

        val isIndirectlyInFocus = !isInFocus2
                // && library.selection.isNotEmpty()
                && library.selection.any { it is PrefabSaveable && it.anyInHierarchy { p -> p === element } }
        // element.findFirstInAll { it in library.selection } != null
        val isEnabled = if (element is PrefabSaveable) element.allInHierarchy { it.isEnabled } else true
        var color = if (isEnabled)
            if (isInFocus2) 0xffcc15 else if (isIndirectlyInFocus) 0xddccaa else 0xcccccc
        else
            if (isInFocus2) 0xcc15ff else if (isIndirectlyInFocus) 0x442255 else 0x333333
        // if is light component, we can mix in its color
        val light = if (element is Entity) element.getComponent(LightComponent::class) else element as? LightComponent
        if (light != null) color = mixARGB(color, normARGB(light.color), 0.5f)
        if (isHovered) color = mixARGB(color, -1, 0.5f)
        if (element is PrefabSaveable && hasWarning(element)) color = mixARGB(color, 0xffff00, 0.8f)
        return color or (255 shl 24)
    }

    override fun getTooltipText(element: ISaveable): String {
        val maxLength = 100
        val warn = if (element is PrefabSaveable) getWarning(element) else null
        if (warn != null) return warn
        val desc = if (element is PrefabSaveable) element.description.shorten(maxLength).toString() else ""
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

    override fun getChildren(element: ISaveable): List<ISaveable> {
        return if (element is PrefabSaveable) {
            val types = element.listChildTypes()
            when (types.length) {
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
        } else emptyList()
    }

    override fun isCollapsed(element: ISaveable): Boolean {
        return if (element is PrefabSaveable) element.isCollapsed else false
    }

    override fun setCollapsed(element: ISaveable, collapsed: Boolean) {
        if (element !is PrefabSaveable) return
        element.isCollapsed = collapsed
        try {
            element.root.prefab!![element.prefabPath!!, "isCollapsed"] = collapsed
        } catch (e: Exception) {
        } // idc too much about saving that property; main thing is that we can collapse and expand stuff in the editor
        needsTreeUpdate = true
        invalidateLayout()
    }

    override fun getDragType(element: ISaveable): String {
        return when (element) {
            is PrefabSaveable -> "PrefabSaveable"
            is Prefab -> "Prefab"
            else -> element.className
        }
    }

    override fun stringifyForCopy(element: ISaveable): String {
        if (element !is PrefabSaveable) return element.toString()
        val tab = ECSSceneTabs.currentTab ?: return ""
        val root = tab.inspector.root
        return if (element == root) {
            tab.inspector.toString()
        } else {
            Hierarchy.stringify(element)
        }
    }

    override fun getSymbol(element: ISaveable): String {
        return if (element is PrefabSaveable && element.root.prefab?.isWritable == false) "\uD83D\uDD12" else "???"
    }

    override fun getParent(element: ISaveable): ISaveable? {
        return (element as? PrefabSaveable)?.parent
    }

    override fun getName(element: ISaveable): String {
        element as PrefabSaveable
        val name = element.name
        return if (name.isBlank2()) element.className.camelCaseToTitle() else name
    }

    override fun setName(element: ISaveable, name: String) {
        ECSFileImporter.setName(element, name)
    }

    override fun openAddMenu(parent: ISaveable) {
        parent as PrefabSaveable
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
                        PropertyInspector.invalidateUI(true)
                    }
                }
            )
        } else LOGGER.warn("Prefab is not writable!")
    }

    override fun canBeInserted(parent: ISaveable, element: ISaveable, index: Int): Boolean {
        if (parent !is PrefabSaveable) return false
        return parent.getOriginal().run { this == null || index >= children.size }
    }

    override fun canBeRemoved(element: ISaveable): Boolean {
        if (element !is PrefabSaveable) return false
        val indexInParent = element.indexInParent
        val parent = element.parent!!
        val parentPrefab = parent.getOriginal()
        return parentPrefab == null || indexInParent >= parentPrefab.children.size
    }

    override fun selectElement(element: ISaveable?) {
        if (element is PrefabSaveable)
            library.select(element)
    }

    override fun focusOnElement(element: ISaveable) {
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
        return when (val element = TextReader.read(data, StudioBase.workspace, true).firstOrNull()) {
            is Prefab -> {
                val prefab = library.prefab
                val root = prefab?.getSampleInstance() ?: return false
                Hierarchy.add(prefab, Path.ROOT_PATH, element, Path.ROOT_PATH, root)
                true
            }
            is PrefabSaveable -> return false
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