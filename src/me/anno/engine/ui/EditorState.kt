package me.anno.engine.ui

import me.anno.config.DefaultConfig
import me.anno.ecs.interfaces.CustomEditMode
import me.anno.ecs.interfaces.InputListener
import me.anno.ecs.prefab.Prefab
import me.anno.ecs.prefab.PrefabCache
import me.anno.ecs.prefab.PrefabInspector
import me.anno.engine.inspector.Inspectable
import me.anno.engine.ui.render.PlayMode
import me.anno.engine.ui.render.SceneView
import me.anno.engine.ui.scenetabs.ECSSceneTabs
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import me.anno.language.translation.NameDesc
import me.anno.ui.custom.CustomPanelType
import me.anno.ui.custom.UITypeLibrary
import me.anno.ui.editor.PropertyInspector

object EditorState {

    lateinit var projectFile: FileReference

    var prefabSource: FileReference = InvalidRef
    val prefab get() = PrefabCache[prefabSource]

    // todo box selecting with shift

    var control: InputListener? = null
        set(value) {
            field = value
            if (value != null) editMode = null
        }

    var editMode: CustomEditMode? = null
        set(value) {
            field = value
            if (value != null) control = null
        }

    var selection: List<Inspectable> = emptyList()

    val typeList = arrayListOf(
        CustomPanelType(NameDesc("Scene View", "", "ui.customize.sceneView")) {
            SceneView(PlayMode.EDITING, DefaultConfig.style)
        },
        CustomPanelType(NameDesc("Tree View", "", "ui.customize.treeView")) {
            ECSTreeView(DefaultConfig.style)
        },
        CustomPanelType(NameDesc("Properties", "", "ui.customize.inspector")) {
            PropertyInspector({ selection }, DefaultConfig.style)
        },
        CustomPanelType(NameDesc("Files", "", "ui.customize.fileExplorer")) {
            ECSFileExplorer(projectFile, DefaultConfig.style)
        }
    )

    val uiLibrary = UITypeLibrary(typeList)

    var lastSelection: Inspectable? = null

    fun select(major: Inspectable?, add: Boolean = false) {
        select(if (major != null) listOf(major) else emptyList(), add)
    }

    fun select(major: List<Inspectable>, add: Boolean = false) {
        doSelect(major, add, true)
    }

    fun selectForeignPrefab(prefab: Prefab) {
        val major = listOf(prefab.getSampleInstance())
        PrefabInspector.currentInspector = PrefabInspector(prefab.source)
        doSelect(major, add = false, refocus = false)
    }

    private fun doSelect(major: List<Inspectable>, add: Boolean, refocus: Boolean) {
        if (refocus) ECSSceneTabs.refocus()
        if (add) selection += major
        else selection = major
        // why?
        lastSelection = major.firstOrNull()
    }

    fun unselect(element: Inspectable) {
        selection = selection.filter { it != element }
        if (lastSelection == element) lastSelection = null
    }
}