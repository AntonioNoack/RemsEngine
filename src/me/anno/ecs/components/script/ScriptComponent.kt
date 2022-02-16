package me.anno.ecs.components.script

import me.anno.cache.CacheData
import me.anno.cache.CacheSection
import me.anno.ecs.Component
import me.anno.ecs.Entity
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.gpu.GFX
import me.anno.io.NamedSaveable
import me.anno.io.files.FileReference
import me.anno.io.files.FileReference.Companion.getReference
import me.anno.io.files.InvalidRef
import me.anno.utils.OS
import me.anno.utils.hpc.ThreadLocal2
import me.anno.utils.types.Strings.isBlank2
import org.luaj.vm2.Globals
import org.luaj.vm2.LuaValue
import org.luaj.vm2.lib.OneArgFunction
import org.luaj.vm2.lib.ZeroArgFunction
import org.luaj.vm2.lib.jse.CoerceJavaToLua
import org.luaj.vm2.lib.jse.JsePlatform

// https://github.com/luaj/luaj
open class ScriptComponent : Component() {

    // todo test-component:
    // todo record a piece of gameplay with exact input, and scene loading times
    // todo and then a test condition;
    // todo record it also as video
    // todo when the condition fails, the code can be fixed, or the test adjusted to the new environment

    // lua starts indexing at 1? I may need to think over whether to choose lua as basic scripting language 😂

    // todo src or content? both?
    // todo languages supported?
    // todo lua from Java/Kotlin?
    // todo JavaScript from Java/Kotlin?
    // todo or just our custom visual language? :)

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

        val global = ThreadLocal2 { defineVM() }

        val luaCache = CacheSection("Lua")
        val timeout = 20_000L

        fun getFunction(name: String, source: FileReference): LuaValue? {
            val value = luaCache.getFileEntry(source, false, timeout, false) { file, date ->
                val vm = defineVM()
                val text = file.readText()
                val code = vm.load(text).call()
                println(text)
                // val code = file.inputStream().use { LuaC.instance.compile(it, "${source.absolutePath}-$date") }
                // val func = LuaClosure(code, global.get())
                CacheData(code)
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
                    return LuaValue.valueOf(GFX.gameTime.toDouble() / 1e9)
                }
            })
            g.set("getName", object : OneArgFunction() {
                override fun call(p0: LuaValue): LuaValue {
                    return if (p0.isuserdata()) {
                        when (val data = p0.touserdata()) {
                            is NamedSaveable -> LuaValue.valueOf(data.name)
                            is PrefabSaveable -> LuaValue.valueOf(data.name)
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
        inline fun getFunction(code: String, init: (scope: LuaValue) -> Unit): LuaValue {
            if (code.isBlank2()) return LuaValue.NIL
            val funcObj = luaCache.getEntry(code, timeout, false) { code1 ->
                CacheData(
                    try {
                        val vm = global.get()
                        Pair(vm, vm.load(code1))
                    } catch (e: Exception) {
                        e.printStackTrace()
                        null
                    }
                )
            }
            if (funcObj !is CacheData<*> || funcObj.value == null)
                return LuaValue.NIL
            val (globals, func) = funcObj.value as Pair<Globals, LuaValue>
            if (func.isfunction()) {
                init(globals)
                return func
            }
            return LuaValue.NIL
        }

        @JvmStatic
        fun main(args: Array<String>) {
            callLua(Entity(), OS.desktop)
        }

    }

}