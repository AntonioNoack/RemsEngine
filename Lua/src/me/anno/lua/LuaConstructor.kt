package me.anno.lua

import me.anno.lua.ScriptComponent.Companion.toLua
import me.anno.utils.types.Booleans.toInt
import org.apache.logging.log4j.LogManager
import org.luaj.vm2.LuaDouble
import org.luaj.vm2.LuaInteger
import org.luaj.vm2.LuaValue
import org.luaj.vm2.Varargs
import org.luaj.vm2.lib.VarArgFunction
import java.lang.reflect.Constructor

class LuaConstructor(clazz: Class<*>) : VarArgFunction() {

    companion object {
        private val LOGGER = LogManager.getLogger(LuaConstructor::class)
    }

    val constructors = clazz.constructors
    val primary = constructors.firstOrNull { it.parameters.isEmpty() }

    override fun onInvoke(args: Varargs): Varargs {
        val size = args.narg() - 1
        if (size == 0) return primary?.newInstance().toLua()
        // types: array, number, string, hashmap, userdata
        return create((0 until size).map { args.arg(it + 2) })
    }

    fun create(args: List<LuaValue>): LuaValue {
        // types: array, number, string, hashmap, userdata
        val size = args.size
        var bestScore = 0
        var bestMatch: Constructor<*>? = null
        for (candidate in constructors) {
            val parameters = candidate.parameterTypes
            if (parameters.size == size) {
                // check if all types are compatible
                val scores = args.mapIndexed { i, arg ->
                    getScore(parameters[i], arg)
                }
                if (scores.min() > 0) {
                    val ourScore = scores.sum()
                    if (ourScore > bestScore) {
                        bestScore = ourScore
                        bestMatch = candidate
                    }
                }
            }
        }
        if (bestMatch == null) return LuaValue.NIL
        val parameters = bestMatch.parameterTypes
        val values = Array(size) { // must be an array
            convert(parameters[it], args[it])
        }
        return try {
            bestMatch.newInstance(*values).toLua()
        } catch (e: Exception) {
            LOGGER.error(
                "Mismatch? $parameters -> " +
                        "${values.map { if (it == null) null else it::class.simpleName }}",
            )
            LuaValue.NIL
        }
    }

    fun getScore(type: Class<*>, value: LuaValue): Int {
        return when {
            value.isnumber() -> when (type) {
                Byte::class.java -> 1
                Short::class.java -> 2
                Int::class.java -> 3
                Long::class.java -> 5
                Float::class.java -> 4
                Double::class.java -> 6
                else -> 0
            }
            value.isstring() -> (type == String::class.java || type == CharSequence::class.java).toInt()
            value.isboolean() -> (type == Boolean::class.java || type == Boolean::class.javaPrimitiveType).toInt()
            value.isnil() -> 1 // type.isMarkedNullable.toInt()
            value.isuserdata() -> type.isInstance(value.checkuserdata()).toInt()
            else -> 0
        }
    }

    fun convert(type: Class<*>, value: LuaValue): Any? {
        return when {
            value.isnumber() -> when (type) {
                Byte::class.java, Byte::class.javaPrimitiveType -> when (value) {
                    is LuaInteger -> value.v.toByte()
                    is LuaDouble -> value.tobyte()
                    else -> throw NotImplementedError()
                }
                Short::class.java, Short::class.javaPrimitiveType -> when (value) {
                    is LuaInteger -> value.v.toShort()
                    is LuaDouble -> value.toshort()
                    else -> throw NotImplementedError()
                }
                Int::class.java, Int::class.javaPrimitiveType -> when (value) {
                    is LuaInteger -> value.v
                    is LuaDouble -> value.toint()
                    else -> throw NotImplementedError()
                }
                Long::class.java, Long::class.javaPrimitiveType -> when (value) {
                    is LuaInteger -> value.v.toLong()
                    is LuaDouble -> value.tolong()
                    else -> throw NotImplementedError()
                }
                Float::class.java, Float::class.javaPrimitiveType -> when (value) {
                    is LuaInteger -> value.v.toFloat()
                    is LuaDouble -> value.tofloat()
                    else -> throw NotImplementedError()
                }
                Double::class.java, Double::class.javaPrimitiveType -> when (value) {
                    is LuaInteger -> value.v.toDouble()
                    is LuaDouble -> value.todouble()
                    else -> throw NotImplementedError()
                }
                else -> null
            }
            value.isstring() -> value.tojstring()
            value.isboolean() -> value.toboolean()
            value.isuserdata() -> value.checkuserdata()
            value.isnil() -> null
            else -> null
        }
    }
}