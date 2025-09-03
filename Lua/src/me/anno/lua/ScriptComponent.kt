package me.anno.lua

import me.anno.Build
import me.anno.Engine
import me.anno.Time
import me.anno.cache.CacheSection
import me.anno.cache.DualCacheSection
import me.anno.cache.FileCacheSection.getFileEntry
import me.anno.ecs.Component
import me.anno.ecs.EntityQuery
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.ecs.systems.OnEnable
import me.anno.ecs.systems.OnUpdate
import me.anno.io.files.FileKey
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import me.anno.lua.utils.SafeFunction
import me.anno.lua.utils.WhileTrueYield
import me.anno.utils.OS
import me.anno.utils.hpc.threadLocal
import me.anno.utils.types.Strings.isBlank2
import org.apache.logging.log4j.LogManager
import org.luaj.vm2.Globals
import org.luaj.vm2.LuaError
import org.luaj.vm2.LuaString
import org.luaj.vm2.LuaThread
import org.luaj.vm2.LuaThread.STATUS_DEAD
import org.luaj.vm2.LuaThread.thread_orphan_check_interval
import org.luaj.vm2.LuaValue
import org.luaj.vm2.lib.DebugLib
import org.luaj.vm2.lib.jse.CoerceJavaToLua
import org.luaj.vm2.lib.jse.CoerceLuaToJava
import org.luaj.vm2.lib.jse.JsePlatform
import java.util.WeakHashMap

/**
 * Uses https://github.com/luaj/luaj.
 *
 * todo make using Lua as similar as possible to Kotlin, and make it was easy as possible, too
 *  -> to make conversions to Kotlin easy
 * */
open class ScriptComponent : Component(), OnUpdate, OnEnable {

    var source: FileReference = InvalidRef

    override fun onEnable() {
        callFunction("onEnable")
    }

    override fun onDisable() {
        callFunction("onDisable")
    }

    override fun onUpdate() {
        callFunction("onUpdate")
    }

    override fun destroy() {
        super.destroy()
        callFunction("destroy")
    }

    private fun callFunction(name: String) {
        callFunction(name, source, this)
    }

    override fun copyInto(dst: PrefabSaveable) {
        super.copyInto(dst)
        if (dst !is ScriptComponent) return
        dst.source = source
    }

    companion object {

        @JvmStatic
        private val LOGGER = LogManager.getLogger(ScriptComponent::class)

        @JvmField
        val global = threadLocal { defineVM() }

        @JvmField
        val luaCache = CacheSection<FileKey, LuaValue>("Lua")

        @JvmField
        val functionCache = DualCacheSection<String, Any?, Pair<Globals, LuaValue>>("LuaFunctions")

        @JvmField
        val timeoutMillis = 20_000L

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
            return luaCache.getFileEntry(source, false, timeoutMillis) { key, result ->
                val vm = defineVM()
                key.file.readText { text, err ->
                    err?.printStackTrace()
                    LOGGER.debug(text)
                    val code0 = vm.load(text)
                    val code1 = if (Build.isDebug) wrapIntoLimited(code0, vm, instructionLimit) else code0
                    result.value = code1.call()
                }
                // val code = file.inputStream().use { LuaC.instance.compile(it, "${source.absolutePath}-$date") }
                // val func = LuaClosure(code, global.get())
            }.waitFor()?.get(name)
        }

        @JvmStatic
        fun Any?.toLua(): LuaValue {
            return CoerceJavaToLua.coerce(this)
        }

        @JvmStatic
        fun LuaValue.toJava(): Any? {
            return CoerceLuaToJava.coerce(this, Any::class.java)
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
            for (cache in CacheSection.caches) {
                g.set(cache.name, cache.toLua())
            }
            g.set("EntityQuery", EntityQuery.toLua())
            g.set("FindClass", LuaFindClass)
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
            return functionCache.getDualEntry(code, null, timeoutMillis) { code1, _, result ->
                val vm = global.get()
                try {
                    val fn = vm.load(code1)
                    result.value = vm to fn
                } catch (error: LuaError) {
                    LOGGER.warn(error)
                    result.value = null
                }
            }.waitFor()
        }

        @JvmStatic
        fun getFunction(code: String, init: (scope: LuaValue) -> Unit): LuaValue {
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
            return functionCache.getDualEntry(code, key, timeoutMillis) { code1, _, result ->
                val vm = global.get()
                try {
                    val func = vm.load(code1)
                    init(vm)
                    wrapIntoLimited(func, vm, 10_000)
                    result.value = Pair(vm, func)
                } catch (error: LuaError) {
                    LOGGER.warn(error)
                    result.value = null
                }
            }.waitFor()
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