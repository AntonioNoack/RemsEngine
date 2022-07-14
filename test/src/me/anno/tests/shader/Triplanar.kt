package me.anno.tests.shader

import me.anno.config.DefaultConfig
import me.anno.ecs.components.shaders.TriplanarMaterial
import me.anno.ecs.prefab.PrefabInspector
import me.anno.ecs.prefab.change.Path
import me.anno.engine.ui.EditorState
import me.anno.engine.ui.render.PlayMode
import me.anno.engine.ui.render.SceneView
import me.anno.ui.custom.CustomList
import me.anno.ui.debug.TestStudio
import me.anno.ui.editor.PropertyInspector
import me.anno.utils.OS

fun main() {
    // test this shader, maybe with the brick texture :)
    TestStudio.testUI {
        val mat = TriplanarMaterial()
        mat.diffuseMap = OS.pictures.getChild("uv-checker.jpg")
        mat.normalMap = OS.pictures.getChild("BricksNormal.png")
        mat.prefabPath = Path.ROOT_PATH
        EditorState.prefabSource = mat.ref
        PrefabInspector.currentInspector = PrefabInspector(mat.ref)
        val list = CustomList(false, DefaultConfig.style)
        list.add(SceneView(EditorState, PlayMode.EDITING, DefaultConfig.style), 3f)
        list.add(PropertyInspector({ mat }, DefaultConfig.style, Unit), 1f)
        list
    }
}