package me.anno.lua

import me.anno.Build
import me.anno.Engine
import me.anno.Time
import me.anno.cache.CacheData
import me.anno.cache.CacheSection
import me.anno.ecs.Component
import me.anno.ecs.components.mesh.MeshCache
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import me.anno.lua.utils.SafeFunction
import me.anno.lua.utils.WhileTrueYield
import me.anno.utils.OS
import me.anno.utils.hpc.ThreadLocal2
import me.anno.utils.types.Strings.isBlank2
import org.apache.logging.log4j.LogManager
import org.luaj.vm2.*
import org.luaj.vm2.LuaThread.STATUS_DEAD
import org.luaj.vm2.LuaThread.thread_orphan_check_interval
import org.luaj.vm2.lib.DebugLib
import org.luaj.vm2.lib.jse.CoerceJavaToLua
import org.luaj.vm2.lib.jse.JsePlatform
import java.util.*

/**
 * Uses https://github.com/luaj/luaj.
 *
 * todo make using Lua as similar as possible to Kotlin, and make it was easy as possible, too
 *  -> to make conversions to Kotlin easy
 * */
open class ScriptComponent : Component() {

    // lua starts indexing at 1? I may need to reevaluate choosing lua as basic scripting language 😂

    var instructionLimit: Int = 1000

    var source: FileReference = InvalidRef

    override fun onCreate() {
        super.onCreate()
        callFunction("onCreate", source, this)
    }

    override fun onUpdate(): Int {
        return callIntFunction("onUpdate", source, this, 1)
    }

    override fun onDestroy() {
        super.onDestroy()
        callFunction("onDestroy", source, this)
    }

    override fun copyInto(dst: PrefabSaveable) {
        super.copyInto(dst)
        dst as ScriptComponent
        dst.source = source
    }

    override val className: String get() = "ScriptComponent"

    companion object {

        @JvmStatic
        private val LOGGER = LogManager.getLogger(ScriptComponent::class)

        @JvmField
        val global = ThreadLocal2 { defineVM() }

        @JvmField
        val luaCache = CacheSection("Lua")

        @JvmField
        val timeout = 20_000L

        val luaThreads = WeakHashMap<LuaThread, Unit>()

        init {
            // look for dead threads every 50ms
            thread_orphan_check_interval = 50
            // on shutdown, set all lua threads to be dead
            Engine.registerForShutdown {
                synchronized(luaThreads) {
                    for (thread in luaThreads.keys) {
                        thread.state.status = STATUS_DEAD
                    }
                }
            }
        }

        @JvmStatic
        fun getFunction(name: String, source: FileReference, instructionLimit: Int = 10_000): LuaValue? {
            val value = luaCache.getFileEntry(source, false, timeout, false) { file, _ ->
                val vm = defineVM()
                val text = file.readTextSync()
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

        @JvmStatic
        fun Any?.toLua(): LuaValue {
            return CoerceJavaToLua.coerce(this)
        }

        @JvmStatic
        fun callIntFunction(name: String, source: FileReference, instance: Component, default: Int): Int {
            val ret = callFunction(name, source, instance)
            return if (ret.isint()) ret.toint() else default
        }

        @JvmStatic
        fun callFunction(name: String, source: FileReference, instance: Component): LuaValue {
            val func = getFunction(name, source) ?: return LuaValue.NIL
            return if (func.isfunction()) func.call(instance.entity!!.toLua(), instance.toLua())
            else LuaValue.NIL
        }

        @JvmStatic
        fun defineVM(): Globals {
            val g = JsePlatform.standardGlobals()
            g.set("Engine", Time.toLua())
            g.set("R", ConstructorRegistry) // R for Rem or Registry
            g.set("OS", OS.toLua())
            // todo register all important caches
            g.set("MeshCache", MeshCache.toLua())
            return g
        }

        // todo lua-script directory, from which files for "dofile" are loaded
        // data types:
        // nil, boolean, number, string, userdata, function, thread, and table

        @JvmStatic
        @Suppress("unused")
        fun getRawFunction(code: String): Any {
            return getRawScopeAndFunction(code)?.second ?: LuaValue.NIL
        }

        @JvmStatic
        @Suppress("unchecked_cast")
        fun getRawScopeAndFunction(code: String): Pair<Globals, Any>? {
            val funcObj = luaCache.getEntry(code, timeout, false) { code1 ->
                val vm = global.get()
                val fn = try {
                    vm.load(code1)
                } catch (error: LuaError) {
                    LOGGER.warn(error)
                    error
                }
                CacheData(Pair(vm, fn))
            }
            if (funcObj !is CacheData<*> || funcObj.value == null)
                return null
            return funcObj.value as Pair<Globals, LuaValue>
        }

        @JvmStatic
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

        @JvmStatic
        @Suppress("unchecked_cast")
        fun getFunction(code: String, key: Any?, init: (scope: LuaValue) -> Unit): Pair<Globals, LuaValue>? {
            if (code.isBlank2()) return null
            val funcObj = luaCache.getDualEntry(code, key, timeout, false) { code1, _ ->
                val vm = global.get()
                val func = try {
                    val func = vm.load(code1)
                    init(vm)
                    wrapIntoLimited(func, vm, 10_000)
                } catch (error: LuaError) {
                    LOGGER.warn(error)
                    error
                }
                CacheData(Pair(vm, func))
            }
            return (funcObj as? CacheData<*>)?.value as? Pair<Globals, LuaValue>
        }

        @JvmStatic
        private val lDebug = LuaString.valueOf("debug")

        @JvmStatic
        private val lSetHook = LuaString.valueOf("set" + "hook")

        @JvmStatic
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
            synchronized(luaThreads) {
                luaThreads[thread] = Unit
            }
            return SafeFunction(thread)
        }
    }
}