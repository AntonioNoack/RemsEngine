package me.anno.ecs.components.script

import me.anno.ecs.Component
import me.anno.ecs.Entity
import me.anno.ecs.annotations.Type
import me.anno.ecs.components.script.ScriptComponent.Companion.getFunction
import me.anno.ecs.components.script.ScriptComponent.Companion.toLua
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

    override fun onCreate() {
        super.onCreate()
        callFunction(createScript)
    }

    override fun onUpdate(): Int {
        callFunction(updateScript)
        return 1
    }

    override fun onVisibleUpdate(): Boolean {
        callFunction(visualUpdateScript)
        return super.onVisibleUpdate()
    }

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

    inline fun getFunction1(code: String, init: (scope: LuaValue) -> Unit): LuaValue {
        return getFunction(code) { globals ->
            globals.set("entity", entity.toLua())
            globals.set("transform", entity?.transform.toLua())
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