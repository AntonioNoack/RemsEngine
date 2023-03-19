package me.anno.ecs.components.script

import me.anno.Engine
import me.anno.ecs.Component
import me.anno.ecs.Entity
import me.anno.ecs.annotations.DebugAction
import me.anno.ecs.annotations.Type
import me.anno.ecs.components.player.LocalPlayer
import me.anno.ecs.components.script.ScriptComponent.Companion.getFunction
import me.anno.ecs.components.script.ScriptComponent.Companion.toLua
import me.anno.ecs.components.script.functions.*
import me.anno.ecs.prefab.PrefabSaveable
import org.luaj.vm2.LuaValue

@Suppress("MemberVisibilityCanBePrivate")
open class QuickScriptComponent : Component() {

    @Type("Lua/Code")
    var createScript = ""

    @Type("Lua/Code")
    var updateScript = ""

    @Type("Lua/Code")
    var visualUpdateScript = ""

    @Type("Lua/Code")
    var destroyScript = ""

    @DebugAction
    override fun onCreate() {
        super.onCreate()
        callFunction(createScript)
    }

    @DebugAction
    override fun onUpdate(): Int {
        callFunction(updateScript)
        return 1
    }

    @DebugAction
    override fun onVisibleUpdate(): Boolean {
        callFunction(visualUpdateScript)
        return super.onVisibleUpdate()
    }

    @DebugAction
    override fun onDestroy() {
        super.onDestroy()
        callFunction(destroyScript)
    }

    override fun copy(clone: PrefabSaveable) {
        super.copy(clone)
        clone as QuickScriptComponent
        clone.createScript = createScript
        clone.updateScript = updateScript
        clone.visualUpdateScript = visualUpdateScript
        clone.destroyScript = destroyScript
    }

    override val className get() = "QuickScriptComponent"

    inline fun getFunction1(code: String, init: (scope: LuaValue) -> Unit): LuaValue {
        return getFunction(code) { globals ->
            globals.set("entity", entity.toLua())
            globals.set("transform", entity?.transform.toLua())
            globals.set("parent", parent.toLua())
            globals.set("dt", LuaValue.valueOf(Engine.deltaTime.toDouble()))
            globals.set("t", LuaValue.valueOf(Engine.gameTimeD))
            globals.set("localPlayer", LocalPlayer.currentLocalPlayer.toLua())
            globals.set("get", GetProperty)
            globals.set("set", SetProperty)
            // todo "call", which calls a function via reflections
            // define translation, rotation, scale functions and such :)
            // todo more transform functions
            globals.set("getPosition", GetPosition)
            globals.set("getRotation", GetRotation)
            globals.set("getScale", GetScale)
            globals.set("rotateX", RotateX)
            globals.set("rotateY", RotateY)
            globals.set("rotateZ", RotateZ)
            globals.set("setPosition", setPosition)
            init(globals)
        }
    }

    fun callFunction(code: String): LuaValue {
        val func = getFunction1(code) {}
        return if (func.isfunction()) func.call()
        else func
    }

}