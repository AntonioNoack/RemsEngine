package me.anno.lua.functions

import me.anno.ecs.Component
import me.anno.ecs.Entity
import me.anno.ecs.Transform
import me.anno.lua.ScriptComponent.Companion.toLua
import me.anno.sdf.SDFComponent
import org.luaj.vm2.LuaValue
import org.luaj.vm2.lib.OneArgFunction

object GetPosition : OneArgFunction() {
    override fun call(arg: LuaValue): LuaValue {
        val value = arg.touserdata()
        if (value is Entity) return value.transform.localPosition.toLua()
        if (value is Transform) return value.localPosition.toLua()
        if (value is SDFComponent) return value.position.toLua()
        if (value is Component) return value.entity?.transform?.localPosition.toLua()
        return NIL
    }
}