package me.anno.ui.anim

import me.anno.utils.Color.black
import me.anno.ecs.annotations.Docs
import me.anno.ecs.components.script.ScriptComponent
import me.anno.ecs.components.script.ScriptComponent.Companion.toLua
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.io.base.BaseWriter
import me.anno.ui.style.Style
import org.apache.logging.log4j.LogManager
import org.luaj.vm2.LuaString
import org.luaj.vm2.LuaValue
import org.luaj.vm2.lib.ThreeArgFunction
import org.luaj.vm2.lib.TwoArgFunction

@Docs("Char-wise animated text panel with Lua script for quick tests")
class LuaAnimTextPanel(text: String, var animation: String, style: Style) : AnimTextPanel(text, style) {

    constructor(style: Style) : this("", "", style)

    companion object {

        @JvmStatic
        private val LOGGER = LogManager.getLogger(LuaAnimTextPanel::class)

        @JvmStatic
        var cx = 0f
        @JvmStatic
        var cy = 0f

        object Translate : TwoArgFunction() {
            override fun call(arg1: LuaValue, arg2: LuaValue): LuaValue {
                translate(arg1.tofloat(), arg2.tofloat())
                return NIL
            }
        }

        object Scale : TwoArgFunction() {
            override fun call(arg1: LuaValue, arg2: LuaValue): LuaValue {
                scale(arg1.tofloat(), arg2.tofloat())
                return NIL
            }
        }

        object Rotate : ThreeArgFunction() {
            override fun call(arg1: LuaValue, arg2: LuaValue, arg3: LuaValue): LuaValue {
                if (arg2 == NIL) rotate(arg1.tofloat(), cx, cy)
                else rotate(arg1.tofloat(), arg2.tofloat(), arg3.tofloat())
                return NIL
            }
        }

        object Hsluv : ThreeArgFunction() {
            override fun call(arg1: LuaValue, arg2: LuaValue, arg3: LuaValue): LuaValue {
                return when {
                    arg2.isnil() -> hsluv(arg1.tofloat())
                    arg3.isnil() -> hsluv(arg1.tofloat(), arg2.tofloat())
                    else -> hsluv(arg1.tofloat(), arg2.tofloat(), arg3.tofloat())
                }.toLua()
            }
        }

        // cached, because LuaJ copies all string content every time otherwise
        @JvmStatic
        private val lTime = LuaString.valueOf("time")
        @JvmStatic
        private val lIndex = LuaString.valueOf("index")
        @JvmStatic
        private val lcx = LuaString.valueOf("cx")
        @JvmStatic
        private val lcy = LuaString.valueOf("cy")

    }

    override fun animate(time: Float, index: Int, cx: Float, cy: Float): Int {
        val (globals, func) = ScriptComponent.getFunction(animation, LuaAnimTextPanel::class) { globals ->
            // define all relevant properties; we could add a few helper functions and more properties later
            // if you need any specific property in your project, consider writing it in Kotlin, or ask me
            globals.set("translate", Translate)
            globals.set("scale", Scale)
            globals.set("rotate", Rotate)
            globals.set("hsluv", Hsluv)
        } ?: return textColor
        if (!func.isfunction() && !func.isint()) LOGGER.warn("Function: $func")
        if (func.isint()) return func.toint() or (textColor and black)
        if (func.isnil()) return textColor
        globals.set(lTime, time.toLua())
        globals.set(lIndex, index.toLua())
        globals.set(lcx, cx.toLua())
        globals.set(lcy, cy.toLua())
        Companion.cx = cx
        Companion.cy = cy
        val ret = func.call()
        if (!ret.isint()) LOGGER.warn("Return type: $ret")
        return when {
            ret.isint() -> ret.toint()
            else -> textColor
        }
    }

    override fun save(writer: BaseWriter) {
        super.save(writer)
        writer.writeString("animation", animation)
    }

    override fun clone(): LuaAnimTextPanel {
        val clone = LuaAnimTextPanel(text, animation, style)
        copy(clone)
        return clone
    }

    override fun copy(clone: PrefabSaveable) {
        super.copy(clone)
        clone as LuaAnimTextPanel
        clone.animation = animation
    }

    override val className get() = "LuaAnimTextPanel"

}