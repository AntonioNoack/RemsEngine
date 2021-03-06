package me.anno.ecs.components.script

import me.anno.Build
import me.anno.Engine
import me.anno.cache.CacheData
import me.anno.cache.CacheSection
import me.anno.ecs.Component
import me.anno.ecs.Entity
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.io.NamedSaveable
import me.anno.io.files.FileReference
import me.anno.io.files.FileReference.Companion.getReference
import me.anno.io.files.InvalidRef
import me.anno.utils.hpc.ThreadLocal2
import me.anno.utils.types.Strings.isBlank2
import org.apache.logging.log4j.LogManager
import org.luaj.vm2.*
import org.luaj.vm2.lib.DebugLib
import org.luaj.vm2.lib.LibFunction
import org.luaj.vm2.lib.OneArgFunction
import org.luaj.vm2.lib.ZeroArgFunction
import org.luaj.vm2.lib.jse.CoerceJavaToLua
import org.luaj.vm2.lib.jse.JsePlatform

// https://github.com/luaj/luaj
open class ScriptComponent : Component() {

    // todo TestComponent:
    // todo record a piece of gameplay with exact input, and scene loading times
    // todo and then a test condition;
    // todo record it also as video
    // todo when the condition fails, the code can be fixed, or the test adjusted to the new environment

    // lua starts indexing at 1? I may need to think over whether to choose lua as basic scripting language 😂

    // todo JavaScript from Java/Kotlin?
    // todo or just our custom visual language? :)

    var instructionLimit: Int = 1000

    var source: FileReference = InvalidRef

    override fun onCreate() {
        super.onCreate()
        callFunction("onCreate", source, this)
    }

    override fun onUpdate(): Int {
        return callIntFunction("onUpdate", source, this, 1)
    }

    override fun onVisibleUpdate(): Boolean {
        return callIntFunction("onVisibleUpdate", source, this, 1) > 0
    }

    override fun onDestroy() {
        super.onDestroy()
        callFunction("onDestroy", source, this)
    }

    override fun clone(): ScriptComponent {
        val clone = ScriptComponent()
        copy(clone)
        return clone
    }

    override fun copy(clone: PrefabSaveable) {
        super.copy(clone)
        clone as ScriptComponent
        clone.source = source
    }

    override val className get() = "ScriptComponent"

    companion object {

        private val LOGGER = LogManager.getLogger(ScriptComponent::class)

        val global = ThreadLocal2 { defineVM() }

        val luaCache = CacheSection("Lua")
        val timeout = 20_000L

        fun getFunction(name: String, source: FileReference, instructionLimit: Int = 10_000): LuaValue? {
            val value = luaCache.getFileEntry(source, false, timeout, false) { file, _ ->
                val vm = defineVM()
                val text = file.readText()
                val code0 = vm.load(text)
                val code1 = if (Build.isDebug) wrapIntoLimited(code0, vm, instructionLimit) else code0
                val code2 = code1.call()
                LOGGER.debug(text)
                // val code = file.inputStream().use { LuaC.instance.compile(it, "${source.absolutePath}-$date") }
                // val func = LuaClosure(code, global.get())
                CacheData(code2)
            } as? CacheData<*> ?: return null
            val func = value.value as LuaValue
            return func.get(name)
        }

        fun Any?.toLua(): LuaValue = CoerceJavaToLua.coerce(this)

        fun callIntFunction(name: String, source: FileReference, instance: Component, default: Int): Int {
            val ret = callFunction(name, source, instance)
            return if (ret.isint()) ret.toint() else default
        }

        fun callFunction(name: String, source: FileReference, instance: Component): LuaValue {
            val func = getFunction(name, source) ?: return LuaValue.NIL
            return if (func.isfunction()) func.call(instance.entity!!.toLua(), instance.toLua())
            else LuaValue.NIL
        }

        fun defineVM(): Globals {
            val g = JsePlatform.standardGlobals()
            g.set("getTime", object : ZeroArgFunction() {
                override fun call(): LuaValue {
                    return LuaValue.valueOf(Engine.gameTime / 1e9)
                }
            })
            g.set("getName", object : OneArgFunction() {
                override fun call(p0: LuaValue): LuaValue {
                    return if (p0.isuserdata()) {
                        when (val data = p0.touserdata()) {
                            is NamedSaveable -> LuaValue.valueOf(data.name)
                            else -> LuaValue.NIL
                        }
                    } else LuaValue.NIL
                }
            })
            return g
        }

        // todo lua-script directory, from which files for "dofile" are loaded
        // data types:
        // nil, boolean, number, string, userdata, function, thread, and table

        fun callLua(entity: Entity, source: FileReference) {
            if (source == InvalidRef) return

            entity.name = "Gustav"

            val globals = global.get()

            globals.set("entity", CoerceJavaToLua.coerce(entity))
            val chunk = globals.load(
                "" +
                        "entity:setName('leon')\n" +
                        "print(getName(entity) .. getTime())"
            )
            chunk.call()
            chunk.call()

            callFunction("onUpdate", getReference("res://scripts/luaTest.lua"), ScriptComponent())

        }

        @Suppress("unchecked_cast")
        fun getRawFunction(code: String): Any {
            return getRawScopeAndFunction(code)?.second ?: LuaValue.NIL
        }

        @Suppress("unchecked_cast")
        fun getRawScopeAndFunction(code: String): Pair<Globals, Any>? {
            val funcObj = luaCache.getEntry(code, timeout, false) { code1 ->
                val vm = global.get()
                val fn = try {
                    vm.load(code1)
                } catch (error: LuaError) {
                    error
                }
                CacheData(Pair(vm, fn))
            }
            if (funcObj !is CacheData<*> || funcObj.value == null)
                return null
            return funcObj.value as Pair<Globals, LuaValue>
        }

        @Suppress("unchecked_cast")
        inline fun getFunction(code: String, init: (scope: LuaValue) -> Unit): LuaValue {
            if (code.isBlank2()) return LuaValue.NIL
            val (globals, func) = getRawScopeAndFunction(code) ?: return LuaValue.NIL
            if (func is LuaValue && func.isfunction()) {
                init(globals)
                return if (Build.isDebug) {
                    // this is expensive memory-wise
                    // if a function was guaranteed to run only in a single scope,
                    // we could wrap it
                    wrapIntoLimited(func, globals, 10_000)
                } else func
            }
            return LuaValue.NIL
        }

        @Suppress("unchecked_cast")
        fun getFunction(code: String, key: Any?, init: (scope: LuaValue) -> Unit): Pair<Globals, LuaValue>? {
            if (code.isBlank2()) return null
            val funcObj = luaCache.getEntry(code, key, timeout, false) { code1, _ ->
                val vm = global.get()
                val func = try {
                    val func = vm.load(code1)
                    init(vm)
                    wrapIntoLimited(func, vm, 10_000)
                } catch (error: LuaError) {
                    error
                }
                CacheData(Pair(vm, func))
            }
            return (funcObj as? CacheData<*>)?.value as? Pair<Globals, LuaValue>
        }

        object ErrorFunction : ZeroArgFunction() {
            override fun call(): LuaValue {
                throw Error("Script run out of limits.")
            }
        }

        class SafeFunction(val thread: LuaThread) : ZeroArgFunction() {
            override fun call(): LuaValue {
                return thread.resume(LuaValue.NIL).arg(2)
            }
        }

        private val lDebug = LuaString.valueOf("debug")
        private val lSetHook = LuaString.valueOf("set" + "hook")

        class WhileTrueYield(
            val globals: Globals,
            val func: LuaValue,
            val setHook: LuaValue,
            val instructionLimit: Int
        ) : LibFunction() {
            lateinit var thread: LuaThread
            override fun invoke(varargs: Varargs?): Varargs {
                var run = 0L
                while (true) {
                    // reset limit
                    // todo this will crash after 2B instructions...
                    // todo reset debug counter
                    val limit = instructionLimit * run++
                    if (limit < Int.MAX_VALUE - 65000) {
                        setHook.invoke(
                            varargs(
                                thread,
                                ErrorFunction,
                                LuaValue.EMPTYSTRING,
                                LuaValue.valueOf(limit.toInt())
                            )
                        )
                    }// else will crash...
                    val ret = func.call()
                    globals.yield(ret)
                }
            }
        }

        fun wrapIntoLimited(
            function: LuaValue,
            globals: Globals,
            instructionLimit: Int = 20
        ): LuaValue {

            globals.load(DebugLib())
            val setHook = globals.get(lDebug).get(lSetHook)
            globals.set(lDebug, LuaValue.NIL)

            val func = WhileTrueYield(globals, function, setHook, instructionLimit)
            val thread = LuaThread(globals, func)
            func.thread = thread
            return SafeFunction(thread)

        }

        fun varargs(vararg v: LuaValue): Varargs {
            return LuaValue.varargsOf(v)
        }

    }

}