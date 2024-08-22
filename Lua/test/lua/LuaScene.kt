package lua

import me.anno.ecs.Entity
import me.anno.engine.OfficialExtensions
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.lua.QuickScriptComponent
import me.anno.utils.OS.res

/**
 * Shows how Lua scripting can be used.
 * */
fun main() {

    OfficialExtensions.initForTests()

    val qs = QuickScriptComponent()
    qs.createScript = res.getChild("lua/LuaScene.lua").readTextSync()

    testSceneWithUI("LuaScene", Entity(qs))
}