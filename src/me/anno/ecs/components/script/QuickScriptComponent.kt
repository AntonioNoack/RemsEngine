package me.anno.ecs.components.script

import me.anno.Engine
import me.anno.ecs.Component
import me.anno.ecs.Entity
import me.anno.ecs.Transform
import me.anno.ecs.annotations.DebugAction
import me.anno.ecs.annotations.Type
import me.anno.ecs.components.mesh.sdf.SDFComponent
import me.anno.ecs.components.script.ScriptComponent.Companion.getFunction
import me.anno.ecs.components.script.ScriptComponent.Companion.toLua
import me.anno.ecs.prefab.PrefabSaveable
import org.luaj.vm2.LuaValue
import org.luaj.vm2.lib.OneArgFunction

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

    override fun clone(): QuickScriptComponent {
        val clone = QuickScriptComponent()
        copy(clone)
        return clone
    }

    override fun copy(clone: PrefabSaveable) {
        super.copy(clone)
        clone as QuickScriptComponent
        clone.createScript = createScript
        clone.updateScript = updateScript
        clone.visualUpdateScript = visualUpdateScript
        clone.destroyScript = destroyScript
    }

    override val className: String = "QuickScriptComponent"

    fun defineTransform1(
        transform: (Transform, Double) -> Unit,
        sdfComponent: (SDFComponent, Float) -> Unit
    ): OneArgFunction {
        return object : OneArgFunction() {
            override fun call(arg: LuaValue): LuaValue {
                val parent = parent
                val angle = arg.todouble()
                when (parent) {
                    is Entity -> transform(parent.transform, angle)
                    is SDFComponent -> sdfComponent(parent, angle.toFloat())
                }
                return arg
            }
        }
    }

    inline fun getFunction1(code: String, init: (scope: LuaValue) -> Unit): LuaValue {
        return getFunction(code) { globals ->
            globals.set("entity", entity.toLua())
            globals.set("transform", entity?.transform.toLua())
            globals.set("parent", parent.toLua())
            // todo define translation, rotation, scale functions and such :)
            globals.set("dt", LuaValue.valueOf(Engine.deltaTime.toDouble()))
            globals.set("t", LuaValue.valueOf(Engine.gameTimeD))
            globals.set("rotateX", defineTransform1({ t, a ->
                t.localRotation.rotateX(a)
                t.invalidateGlobal()
            }, { t, a ->
                t.rotation = t.rotation.rotateX(a)
            }))
            globals.set("rotateY", defineTransform1({ t, a ->
                t.localRotation.rotateY(a)
                t.invalidateGlobal()
            }, { t, a ->
                t.rotation = t.rotation.rotateY(a)
            }))
            globals.set("rotateZ", defineTransform1({ t, a ->
                t.localRotation.rotateZ(a)
                t.invalidateGlobal()
            }, { t, a ->
                t.rotation = t.rotation.rotateZ(a)
            }))
            init(globals)
        }
    }

    fun callFunction(code: String): LuaValue {
        val func = getFunction1(code) {}
        return if (func.isfunction()) func.call()
        else func
    }

    companion object {

        @JvmStatic
        fun main(args: Array<String>) {
            val c = QuickScriptComponent()
            val e = Entity()
            e.add(c)
            c.createScript = "print(entity)"
            c.onCreate()
        }

    }

}