package lua

import me.anno.ecs.Entity
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.engine.DefaultAssets.flatCube
import me.anno.engine.OfficialExtensions
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.lua.QuickScriptComponent
import me.anno.utils.OS.res

/**
 * Shows how Lua scripting can be used to spin a cube
 * */
fun main() {
    OfficialExtensions.initForTests()
    res.getChild("lua/SpinningCube.lua").readText { txt, err ->
        err?.printStackTrace()

        val quickScript = QuickScriptComponent()
        quickScript.updateScript = txt ?: ""

        val scene = Entity("Cube")
            .add(MeshComponent(flatCube))
            .add(quickScript)
        testSceneWithUI("SpinningCube", scene)
    }
}