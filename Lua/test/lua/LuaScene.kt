package lua

import me.anno.ecs.Entity
import me.anno.engine.ECSRegistry
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.io.files.FileReference.Companion.getReference
import me.anno.lua.QuickScriptComponent

/**
 * Shows how Lua scripting can be used.
 * */
fun main() {

    ECSRegistry.init()

    val qs = QuickScriptComponent()
    val prefix = "res://"
    qs.createScript = getReference(prefix + "lua/LuaScene.lua").readTextSync()

    testSceneWithUI("LuaScene", Entity(qs))
}