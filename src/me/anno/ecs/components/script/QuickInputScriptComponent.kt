package me.anno.ecs.components.script

import me.anno.ecs.Entity
import me.anno.ecs.annotations.Type
import me.anno.ecs.components.script.ScriptComponent.Companion.toLua
import me.anno.ecs.interfaces.ControlReceiver
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.input.MouseButton
import org.luaj.vm2.LuaValue

@Suppress("MemberVisibilityCanBePrivate")
open class QuickInputScriptComponent : QuickScriptComponent(), ControlReceiver {

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

    override fun onKeyDown(key: Int): Boolean {
        return callBoolFunction(keyDownScript) {
            it.set("key", LuaValue.valueOf(key))
        }
    }

    override fun onKeyUp(key: Int): Boolean {
        return callBoolFunction(keyUpScript) {
            it.set("key", LuaValue.valueOf(key))
        }
    }

    override fun onKeyTyped(key: Int): Boolean {
        return callBoolFunction(keyTypedScript) {
            it.set("key", LuaValue.valueOf(key))
        }
    }

    override fun onCharTyped(char: Int): Boolean {
        return callBoolFunction(charTypedScript) {
            it.set("char", LuaValue.valueOf(char))
        }
    }

    override fun onMouseDown(button: MouseButton): Boolean {
        return callBoolFunction(mouseDownScript) {
            it.set("button", button.toLua())
        }
    }

    override fun onMouseUp(button: MouseButton): Boolean {
        return callBoolFunction(mouseUpScript) {
            it.set("button", button.toLua())
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

    override fun onMouseClicked(button: MouseButton, long: Boolean): Boolean {
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

    override fun clone(): QuickInputScriptComponent {
        val clone = QuickInputScriptComponent()
        copy(clone)
        return clone
    }

    override fun copy(clone: PrefabSaveable) {
        super.copy(clone)
        clone as QuickInputScriptComponent
        clone.actionScript = actionScript
        clone.charTypedScript = charTypedScript
        clone.keyDownScript = keyDownScript
        clone.keyTypedScript = keyTypedScript
        clone.keyUpScript = keyUpScript
        clone.mouseClickScript = mouseClickScript
        clone.mouseDownScript = mouseDownScript
        clone.mouseMoveScript = mouseMoveScript
        clone.mouseUpScript = mouseUpScript
        clone.mouseWheelScript = mouseWheelScript
    }

    override val className = "QuickInputScriptComponent"

    inline fun callBoolFunction(code: String, defines: (scope: LuaValue) -> Unit): Boolean {
        val func = getFunction1(code, defines)
        if (func.isnil()) return false
        val ret = func.call()
        return when {
            ret.isboolean() -> ret.toboolean()
            ret.isint() -> ret.toint() != 0
            ret.isnil() -> false
            else -> true
        }
    }

}