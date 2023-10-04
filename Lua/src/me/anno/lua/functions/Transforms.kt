package me.anno.lua.functions

import me.anno.ecs.Entity
import me.anno.ecs.Transform
import me.anno.sdf.SDFComponent
import org.joml.Vector3d
import org.joml.Vector3f
import org.luaj.vm2.LuaValue
import org.luaj.vm2.lib.TwoArgFunction

abstract class Transform1 : TwoArgFunction() {
    abstract fun transform(t: Transform, a: Double)
    abstract fun transform(t: SDFComponent, a: Float)
    override fun call(arg: LuaValue, v: LuaValue): LuaValue {
        val parent = arg.touserdata()
        val angle = v.todouble()
        when (parent) {
            is Entity -> transform(parent.transform, angle)
            is SDFComponent -> transform(parent, angle.toFloat())
            else -> return NIL
        }
        return arg
    }
}

object RotateX : Transform1() {
    override fun transform(t: Transform, a: Double) {
        t.localRotation.rotateX(a)
        t.invalidateGlobal()
        t.smoothUpdate()
    }

    override fun transform(t: SDFComponent, a: Float) {
        t.rotation = t.rotation.rotateX(a)
    }
}

object RotateY : Transform1() {
    override fun transform(t: SDFComponent, a: Float) {
        t.rotation = t.rotation.rotateY(a)
    }

    override fun transform(t: Transform, a: Double) {
        t.localRotation.rotateY(a)
        t.invalidateGlobal()
        t.smoothUpdate()
    }
}

object RotateZ : Transform1() {
    override fun transform(t: Transform, a: Double) {
        t.localRotation.rotateZ(a)
        t.invalidateGlobal()
        t.smoothUpdate()
    }

    override fun transform(t: SDFComponent, a: Float) {
        t.rotation = t.rotation.rotateZ(a)
    }
}

fun defineTransformVec3(
    transform: (Transform, Vector3d) -> Unit,
    sdfComponent: (SDFComponent, Vector3f) -> Unit
) = object : TwoArgFunction() {
    override fun call(arg: LuaValue, v: LuaValue): LuaValue {
        val parent = arg.touserdata()
        val data = v.touserdata()
        val angle = if (data is Vector3f) Vector3d(data) else data as Vector3d
        when (parent) {
            is Entity -> transform(parent.transform, angle)
            is SDFComponent -> sdfComponent(
                parent, Vector3f()
                    .set(angle)
            )
            else -> return NIL
        }
        return arg
    }
}

val setPosition = defineTransformVec3({ t, a ->
    t.localPosition = a
    t.smoothUpdate()
}, { t, a ->
    t.position = a
})