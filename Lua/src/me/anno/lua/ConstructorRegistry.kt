package me.anno.lua

import me.anno.io.saveable.Saveable
import me.anno.lua.ScriptComponent.Companion.toLua
import org.joml.AABBd
import org.joml.AABBf
import org.joml.AxisAngle4d
import org.joml.AxisAngle4f
import org.joml.Matrix2d
import org.joml.Matrix2f
import org.joml.Matrix3d
import org.joml.Matrix3f
import org.joml.Matrix3x2d
import org.joml.Matrix3x2f
import org.joml.Matrix4d
import org.joml.Matrix4f
import org.joml.Matrix4x3d
import org.joml.Matrix4x3f
import org.joml.Planed
import org.joml.Planef
import org.joml.Quaterniond
import org.joml.Quaternionf
import org.joml.Vector2d
import org.joml.Vector2f
import org.joml.Vector2i
import org.joml.Vector3d
import org.joml.Vector3f
import org.joml.Vector3i
import org.joml.Vector4d
import org.joml.Vector4f
import org.joml.Vector4i
import org.luaj.vm2.LuaString
import org.luaj.vm2.LuaUserdata
import org.luaj.vm2.LuaValue

object ConstructorRegistry : LuaUserdata(Any()) {

    val extraRegistry = hashMapOf(
        "Vector2i" to Vector2i::class.java,
        "Vector2f" to Vector2f::class.java,
        "Vector2d" to Vector2d::class.java,
        "Vector3i" to Vector3i::class.java,
        "Vector3f" to Vector3f::class.java,
        "Vector3d" to Vector3d::class.java,
        "Vector4i" to Vector4i::class.java,
        "Vector4f" to Vector4f::class.java,
        "Vector4d" to Vector4d::class.java,
        "Quaternionf" to Quaternionf::class.java,
        "Quaterniond" to Quaterniond::class.java,
        "Planef" to Planef::class.java,
        "Planed" to Planed::class.java,
        "AABBf" to AABBf::class.java,
        "AABBd" to AABBd::class.java,
        "AxisAngle4f" to AxisAngle4f::class.java,
        "AxisAngle4d" to AxisAngle4d::class.java,
        "Matrix2f" to Matrix2f::class.java,
        "Matrix2d" to Matrix2d::class.java,
        "Matrix3f" to Matrix3f::class.java,
        "Matrix3d" to Matrix3d::class.java,
        "Matrix4f" to Matrix4f::class.java,
        "Matrix4d" to Matrix4d::class.java,
        "Matrix3x2f" to Matrix3x2f::class.java,
        "Matrix3x2d" to Matrix3x2d::class.java,
        "Matrix4x3f" to Matrix4x3f::class.java,
        "Matrix4x3d" to Matrix4x3d::class.java,
        // other types?
    )

    override fun get(key: LuaValue): LuaValue {
        return get((key.tostring() as LuaString).tojstring())
    }

    override fun get(key: String): LuaValue {
        val entry = Saveable.objectTypeRegistry[key]
        if (entry != null) return LuaConstructor(entry.sampleInstance::class.java)
        val extra = extraRegistry[key]
        if (extra != null) return LuaConstructor(extra)
        if (key == "ListOf") return LuaListOf
        return LuaValue.NIL
    }

    override fun tostring(): LuaValue {
        return this::class.simpleName.toLua()
    }
}