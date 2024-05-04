package lua

import me.anno.ecs.Entity
import me.anno.engine.OfficialExtensions
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.io.files.Reference.getReference
import me.anno.lua.QuickScriptComponent

/**
 * Shows how Lua scripting can be used.
 * */
fun main() {

    OfficialExtensions.initForTests()

    val qs = QuickScriptComponent()
    val prefix = "res://"
    qs.createScript = getReference(prefix + "lua/LuaScene.lua").readTextSync()

    testSceneWithUI("LuaScene", Entity(qs))
}