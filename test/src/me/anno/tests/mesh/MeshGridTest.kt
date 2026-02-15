package me.anno.tests.mesh

import me.anno.config.DefaultConfig.style
import me.anno.ecs.Entity
import me.anno.ecs.components.mesh.grid.MeshGrid
import me.anno.ecs.prefab.PrefabInspector
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.ecs.prefab.change.Path
import me.anno.engine.DefaultAssets.flatCube
import me.anno.engine.ui.ECSFileExplorer
import me.anno.engine.ui.ECSTreeView
import me.anno.engine.ui.EditorState
import me.anno.engine.ui.render.PlayMode
import me.anno.engine.ui.render.SceneView
import me.anno.engine.ui.render.SceneView.Companion.tryStartVR
import me.anno.engine.ui.scenetabs.ECSSceneTab
import me.anno.engine.ui.scenetabs.ECSSceneTabs
import me.anno.gpu.GFX
import me.anno.io.files.FileReference
import me.anno.ui.Panel
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.custom.CustomList
import me.anno.ui.debug.TestEngine.Companion.testUI3
import me.anno.ui.editor.PropertyInspector
import me.anno.utils.OS.documents

fun main() {
    // todo test placing some things into the grid
    // using a parameterized @DebugAction function -> works :3
    val scene = Entity()
        .add(MeshGrid().apply {
            cellSize.set(2.5)
            fill(0, 0, 2, 3, flatCube)
        })
    // we need a file explorer for drag-n-dropping things
    testSceneWithUIAndFE("Mesh Grid", scene)
}

fun testSceneWithUIAndFE(title: String, scene: PrefabSaveable, init: ((SceneView) -> Unit)? = null) {
    testUI3(title) {
        GFX.someWindow.windowStack.firstOrNull()?.drawDirectly = false
        createSceneUIAndFE(scene, init)
    }
}

@Suppress("unused")
fun createSceneUIAndFE(scene: PrefabSaveable, init: ((SceneView) -> Unit)? = null): Panel {
    scene.prefabPath = Path.ROOT_PATH
    return createSceneUIAndFE(scene.ref, init)
}

fun createSceneUIAndFE(scene: FileReference, init: ((SceneView) -> Unit)? = null): Panel {
    val listY = PanelListY(style)
    listY.add(ECSSceneTabs)
    ECSSceneTabs.open(ECSSceneTab(scene, PlayMode.EDITING), true)
    val sceneView = SceneView(PlayMode.EDITING, style)
    PrefabInspector.currentInspector = PrefabInspector(scene)
    val listX = CustomList(false, style)
    val leftSide = CustomList(true, style)
    leftSide.add(ECSTreeView(style), 2f)
    leftSide.add(ECSFileExplorer(documents, style), 1f)
    listX.add(leftSide, 1f)
    listX.add(sceneView, 3f)
    listX.add(PropertyInspector({ EditorState.selection }, style), 1f)
    if (init != null) init(sceneView)
    tryStartVR(sceneView)
    listY.add(listX)
    listX.weight = 1f
    listY.weight = 1f
    return listY
}