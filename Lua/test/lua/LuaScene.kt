package lua

import me.anno.ecs.Entity
import me.anno.engine.OfficialExtensions
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.lua.QuickScriptComponent
import me.anno.utils.OS.res

/**
 * Shows how Lua scripting can be used to populate a simple scene
 * */
fun main() {
    OfficialExtensions.initForTests()
    res.getChild("lua/LuaScene.lua").readText { txt, err ->
        err?.printStackTrace()

        val qs = QuickScriptComponent()
        qs.createScript = txt ?: ""

        testSceneWithUI("LuaScene", Entity().add(qs))
    }
}