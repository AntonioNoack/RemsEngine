package me.anno.ecs.components.script.functions

import me.anno.ecs.Component
import me.anno.ecs.Entity
import me.anno.ecs.Transform
import me.anno.ecs.components.mesh.sdf.SDFComponent
import me.anno.ecs.components.script.ScriptComponent.Companion.toLua
import org.luaj.vm2.LuaValue
import org.luaj.vm2.lib.OneArgFunction

object GetPosition : OneArgFunction() {
    override fun call(arg: LuaValue): LuaValue {
        return when (val value = arg.touserdata()) {
            is Entity -> value.transform.localPosition.toLua()
            is Transform -> value.localPosition.toLua()
            is SDFComponent -> value.position.toLua()
            is Component -> value.entity?.transform?.localPosition.toLua()
            else -> NIL
        }
    }
}