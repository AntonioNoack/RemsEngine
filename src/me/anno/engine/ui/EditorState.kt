package me.anno.engine.ui

import me.anno.config.DefaultConfig
import me.anno.ecs.interfaces.ControlReceiver
import me.anno.ecs.interfaces.CustomEditMode
import me.anno.ecs.prefab.PrefabCache
import me.anno.engine.ui.render.PlayMode
import me.anno.engine.ui.render.SceneView
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import me.anno.language.translation.Dict
import me.anno.studio.Inspectable
import me.anno.ui.custom.Type
import me.anno.ui.custom.UITypeLibrary
import me.anno.ui.editor.PropertyInspector

object EditorState {

    lateinit var projectFile: FileReference

    var prefabSource: FileReference = InvalidRef
    val prefab get() = PrefabCache[prefabSource]

    // todo box selecting with shift

    var control: ControlReceiver? = null
        set(value) {
            field = value
            if (value != null) editMode = null
        }

    var editMode: CustomEditMode? = null
        set(value) {
            field = value
            if (value != null) control = null
        }

    // todo we should be able to edit multiple values of the same type at the same time
    var selection: List<Inspectable> = emptyList()

    val typeList = arrayListOf(
        // todo not all stuff here makes sense
        // todo some stuff is (maybe) missing, e.g. animation panels, particle system editors, ...
        Type(Dict["Scene View", "ui.customize.sceneView"]) { SceneView(PlayMode.EDITING, DefaultConfig.style) },
        Type(Dict["Tree View", "ui.customize.treeView"]) { ECSTreeView(this, DefaultConfig.style) },
        Type(Dict["Properties", "ui.customize.inspector"]) { PropertyInspector({ selection }, DefaultConfig.style) },
        // Dict["Cutting Panel", "ui.customize.cuttingPanel"] to { CuttingView(DefaultConfig.style) },
        // Dict["Timeline", "ui.customize.timeline"] to { TimelinePanel(DefaultConfig.style) },
        // Dict["Animations", "ui.customize.graphEditor"] to { GraphEditor(DefaultConfig.style) },
        Type(Dict["Files", "ui.customize.fileExplorer"]) { ECSFileExplorer(projectFile, DefaultConfig.style) }
    )

    val uiLibrary = UITypeLibrary(typeList)

    var lastSelection: Inspectable? = null

    fun select(major: Inspectable?, add: Boolean = false) {
        if (add) {
            if (major != null) selection += major
        } else {
            selection = if (major == null) emptyList() else listOf(major)
        }
        // why?
        lastSelection = major
    }

    fun select(major: List<Inspectable>, add: Boolean = false) {
        if (add) selection += major
        else selection = major
        lastSelection = major.firstOrNull()
    }

    fun unselect(element: Inspectable) {
        selection = selection.filter { it != element }
        if (lastSelection == element) lastSelection = null
    }
}