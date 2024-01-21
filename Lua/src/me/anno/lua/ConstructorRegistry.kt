package me.anno.lua

import me.anno.io.ISaveable
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
        "Vector2i" to Vector2i::class,
        "Vector2f" to Vector2f::class,
        "Vector2d" to Vector2d::class,
        "Vector3i" to Vector3i::class,
        "Vector3f" to Vector3f::class,
        "Vector3d" to Vector3d::class,
        "Vector4i" to Vector4i::class,
        "Vector4f" to Vector4f::class,
        "Vector4d" to Vector4d::class,
        "Quaternionf" to Quaternionf::class,
        "Quaterniond" to Quaterniond::class,
        "Planef" to Planef::class,
        "Planed" to Planed::class,
        "AABBf" to AABBf::class,
        "AABBd" to AABBd::class,
        "AxisAngle4f" to AxisAngle4f::class,
        "AxisAngle4d" to AxisAngle4d::class,
        "Matrix2f" to Matrix2f::class,
        "Matrix2d" to Matrix2d::class,
        "Matrix3f" to Matrix3f::class,
        "Matrix3d" to Matrix3d::class,
        "Matrix4f" to Matrix4f::class,
        "Matrix4d" to Matrix4d::class,
        "Matrix3x2f" to Matrix3x2f::class,
        "Matrix3x2d" to Matrix3x2d::class,
        "Matrix4x3f" to Matrix4x3f::class,
        "Matrix4x3d" to Matrix4x3d::class,
        // other types?
    )

    override fun get(key: LuaValue): LuaValue {
        return get((key.tostring() as LuaString).tojstring())
    }

    override fun get(key: String): LuaValue {
        val entry = ISaveable.objectTypeRegistry[key]
        if (entry != null) return LuaConstructor(entry.sampleInstance::class)
        val extra = extraRegistry[key]
        if (extra != null) return LuaConstructor(extra)
        if (key == "ListOf") return LuaListOf
        return LuaValue.NIL
    }

    override fun tostring(): LuaValue {
        return this::class.simpleName.toLua()
    }
}