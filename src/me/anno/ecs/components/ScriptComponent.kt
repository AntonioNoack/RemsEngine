package me.anno.ecs.components

import me.anno.cache.CacheSection
import me.anno.ecs.Component
import me.anno.ecs.Entity
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import me.anno.utils.OS
import org.luaj.vm2.lib.jse.JsePlatform

// https://github.com/luaj/luaj
class ScriptComponent : Component() {

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

        val luaCache = CacheSection("Lua")

        // todo lua-script directory, from which files for "dofile" are loaded
        // nil, boolean, number, string, userdata, function, thread, and table

        fun callLua(entity: Entity, source: FileReference) {
            if (source == InvalidRef) return

            val globals = JsePlatform.standardGlobals();
            val chunk = globals.load("print 'hello world'")
            chunk.call()
            chunk.call()

        }

        @JvmStatic
        fun main(args: Array<String>) {
            callLua(Entity(), OS.desktop)
        }

    }

}