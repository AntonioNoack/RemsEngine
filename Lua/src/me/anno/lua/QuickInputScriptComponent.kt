package me.anno.lua

import me.anno.ecs.annotations.Type
import me.anno.ecs.interfaces.InputListener
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.input.Key
import me.anno.lua.ScriptComponent.Companion.toLua
import org.apache.logging.log4j.LogManager
import org.luaj.vm2.LuaValue

@Suppress("MemberVisibilityCanBePrivate")
open class QuickInputScriptComponent : QuickScriptComponent(), InputListener {

    @Type("Lua/Code")
    var keyDownScript = ""

    @Type("Lua/Code")
    var keyUpScript = ""

    @Type("Lua/Code")
    var keyTypedScript = ""


    @Type("Lua/Code")
    var charTypedScript = ""


    @Type("Lua/Code")
    var mouseDownScript = ""

    @Type("Lua/Code")
    var mouseUpScript = ""

    @Type("Lua/Code")
    var mouseMoveScript = ""

    @Type("Lua/Code")
    var mouseWheelScript = ""

    @Type("Lua/Code")
    var mouseClickScript = ""


    @Type("Lua/Code")
    var actionScript = ""

    override fun onKeyDown(key: Key): Boolean {
        return callBoolFunction(keyDownScript) {
            it.set("key", key.toLua())
        }
    }

    override fun onKeyUp(key: Key): Boolean {
        return callBoolFunction(keyUpScript) {
            it.set("key", key.toLua())
        }
    }

    override fun onKeyTyped(key: Key): Boolean {
        return callBoolFunction(keyTypedScript) {
            it.set("key", key.toLua())
        }
    }

    override fun onCharTyped(codepoint: Int): Boolean {
        return callBoolFunction(charTypedScript) {
            it.set("char", LuaValue.valueOf(codepoint))
        }
    }

    override fun onMouseMoved(x: Float, y: Float, dx: Float, dy: Float): Boolean {
        return callBoolFunction(mouseMoveScript) {
            it.set("x", LuaValue.valueOf(x.toDouble()))
            it.set("y", LuaValue.valueOf(y.toDouble()))
            it.set("dx", LuaValue.valueOf(dx.toDouble()))
            it.set("dy", LuaValue.valueOf(dy.toDouble()))
        }
    }

    override fun onMouseWheel(x: Float, y: Float, dx: Float, dy: Float, byMouse: Boolean): Boolean {
        return callBoolFunction(mouseWheelScript) {
            it.set("x", LuaValue.valueOf(x.toDouble()))
            it.set("y", LuaValue.valueOf(y.toDouble()))
            it.set("dx", LuaValue.valueOf(dx.toDouble()))
            it.set("dy", LuaValue.valueOf(dy.toDouble()))
            it.set("byMouse", LuaValue.valueOf(byMouse))
        }
    }

    override fun onMouseClicked(button: Key, long: Boolean): Boolean {
        return callBoolFunction(mouseClickScript) {
            it.set("button", button.toLua())
            it.set("long", LuaValue.valueOf(long))
        }
    }

    override fun onGotAction(x: Float, y: Float, dx: Float, dy: Float, action: String): Boolean {
        return callBoolFunction(actionScript) {
            it.set("x", LuaValue.valueOf(x.toDouble()))
            it.set("y", LuaValue.valueOf(y.toDouble()))
            it.set("dx", LuaValue.valueOf(dx.toDouble()))
            it.set("dy", LuaValue.valueOf(dy.toDouble()))
            it.set("action", LuaValue.valueOf(action))
        }
    }

    override fun copyInto(dst: PrefabSaveable) {
        super.copyInto(dst)
        if (dst !is QuickInputScriptComponent) return
        dst.actionScript = actionScript
        dst.charTypedScript = charTypedScript
        dst.keyDownScript = keyDownScript
        dst.keyTypedScript = keyTypedScript
        dst.keyUpScript = keyUpScript
        dst.mouseClickScript = mouseClickScript
        dst.mouseDownScript = mouseDownScript
        dst.mouseMoveScript = mouseMoveScript
        dst.mouseUpScript = mouseUpScript
        dst.mouseWheelScript = mouseWheelScript
    }

    fun callBoolFunction(code: String, defines: (scope: LuaValue) -> Unit): Boolean {
        val func = getFunction1(code, defines)
        if (func.isnil()) return false
        val ret = func.call()
        return when {
            ret.isboolean() -> ret.toboolean()
            ret.isint() -> ret.toint() != 0
            ret.isnil() -> false
            ret.isstring() -> {
                LOGGER.warn(ret.tojstring())
                false
            }
            else -> true
        }
    }

    companion object {
        private val LOGGER = LogManager.getLogger(QuickInputScriptComponent::class)
    }
}