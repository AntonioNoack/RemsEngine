package me.anno.lua

import me.anno.lua.ScriptComponent.Companion.toLua
import me.anno.utils.structures.lists.Lists.createArrayList
import me.anno.utils.types.Booleans.toInt
import org.apache.logging.log4j.LogManager
import org.luaj.vm2.LuaDouble
import org.luaj.vm2.LuaInteger
import org.luaj.vm2.LuaValue
import org.luaj.vm2.Varargs
import org.luaj.vm2.lib.VarArgFunction
import java.lang.Exception
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KType

class LuaConstructor(clazz: KClass<*>) : VarArgFunction() {

    companion object {
        private val LOGGER = LogManager.getLogger(LuaConstructor::class)
    }

    val constructors = clazz.constructors
    val primary = constructors.firstOrNull { it.parameters.isEmpty() }

    override fun onInvoke(args: Varargs): Varargs {
        val size = args.narg() - 1
        if (size == 0) return primary?.call().toLua()
        // types: array, number, string, hashmap, userdata
        return create((0 until size).map { args.arg(it + 2) })
    }

    fun create(args: List<LuaValue>): LuaValue {
        // types: array, number, string, hashmap, userdata
        val size = args.size
        var bestScore = 0
        var bestMatch: KFunction<Any>? = null
        for (candidate in constructors) {
            val parameters = candidate.parameters
            if (parameters.size == size) {
                // check if all types are compatible
                val scores = args.mapIndexed { i, arg ->
                    getScore(parameters[i].type, arg)
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
        val parameters = bestMatch.parameters
        val values = Array(size) { // must be an array
            convert(parameters[it].type, args[it])
        }
        return try {
            bestMatch.call(*values).toLua()
        } catch (e: Exception) {
            LOGGER.error(
                "Mismatch? ${parameters.map { it.type }} -> " +
                        "${values.map { if (it == null) null else it::class.simpleName }}",
            )
            LuaValue.NIL
        }
    }

    fun getScore(type: KType, value: LuaValue): Int {
        return when {
            value.isnumber() -> when (type.classifier) {
                Byte::class -> 1
                Short::class -> 2
                Int::class -> 3
                Long::class -> 5
                Float::class -> 4
                Double::class -> 6
                else -> 0
            }
            value.isstring() -> (type.classifier == String::class || type.classifier == CharSequence::class).toInt()
            value.isboolean() -> (type.classifier == Boolean::class).toInt()
            value.isnil() -> type.isMarkedNullable.toInt()
            value.isuserdata() -> ((type.classifier as? KClass<*>)?.isInstance(value.checkuserdata()) == true).toInt()
            else -> 0
        }
    }

    fun convert(type: KType, value: LuaValue): Any? {
        return when {
            value.isnumber() -> when (type.classifier) {
                Byte::class -> when (value) {
                    is LuaInteger -> value.v.toByte()
                    is LuaDouble -> value.tobyte()
                    else -> throw NotImplementedError()
                }
                Short::class -> when (value) {
                    is LuaInteger -> value.v.toShort()
                    is LuaDouble -> value.toshort()
                    else -> throw NotImplementedError()
                }
                Int::class -> when (value) {
                    is LuaInteger -> value.v
                    is LuaDouble -> value.toint()
                    else -> throw NotImplementedError()
                }
                Long::class -> when (value) {
                    is LuaInteger -> value.v.toLong()
                    is LuaDouble -> value.tolong()
                    else -> throw NotImplementedError()
                }
                Float::class -> when (value) {
                    is LuaInteger -> value.v.toFloat()
                    is LuaDouble -> value.tofloat()
                    else -> throw NotImplementedError()
                }
                Double::class -> when (value) {
                    is LuaInteger -> value.v.toDouble()
                    is LuaDouble -> value.todouble()
                    else -> throw NotImplementedError()
                }
                else -> throw IllegalArgumentException()
            }
            value.isstring() -> value.tojstring()
            value.isboolean() -> value.toboolean()
            value.isuserdata() -> value.checkuserdata()
            value.isnil() -> null
            else -> throw IllegalArgumentException()
        }
    }
}