package me.anno.objects.text

import me.anno.config.DefaultConfig
import me.anno.language.translation.Dict
import me.anno.objects.Transform
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.editor.SettingCategory
import me.anno.ui.input.TextInputML
import me.anno.ui.style.Style
import me.anno.maths.Maths.fract
import org.joml.Matrix4fArrayList
import org.joml.Vector4fc
import java.net.URL
import java.util.*
import kotlin.math.floor

class Timer(parent: Transform? = null): Text("", parent) {

    init { forceVariableBuffer = true } // saves buffer creation

    // todo extra start value in a date format?

    override fun getDocumentationURL() = URL("https://remsstudio.phychi.com/?s=learn/timer")

    var format = "hh:mm:ss.s2"

    /*override fun splitSegments(text: String): PartResult? {
        if(text.isEmpty()) return null
        var index = 0
        var startIndex = 0
        var partResult: PartResult? = null
        fun add(pr: PartResult){
            partResult =
                if(partResult == null) pr
                else partResult!! + pr
        }
        while(index < text.length){
            when(text[index]){
                in '0' .. '9' -> {
                    if(index > startIndex) add(super.splitSegments(text.substring(startIndex, index))!!)
                    add(super.splitSegments(text[index].toString())!!)
                    startIndex = index+1
                }
            }
            index++
        }
        if(index > startIndex) add(super.splitSegments(text.substring(startIndex, index))!!)
        return partResult!!
    }*/

    override fun onDraw(stack: Matrix4fArrayList, time: Double, color: Vector4fc) {

        val fract = fract(time)
        val s0 = floor(time).toLong()
        var s = s0
        var m = s / 60
        var h = m / 60
        var d = h / 24

        s %= 60
        if(s < 0){
            s += 60
            m--
        }
        m %= 60
        if(m < 0){
            m += 60
            h--
        }
        h %= 24
        if(h < 0){
            h += 24
            d--
        }

        text.set(
            format
                .replace("s0", "")
                .replace("s6", "%.6f".format(Locale.ENGLISH, fract).substring(2))
                .replace("s5", "%.5f".format(Locale.ENGLISH, fract).substring(2))
                .replace("s4", "%.4f".format(Locale.ENGLISH, fract).substring(2))
                .replace("s3", "%.3f".format(Locale.ENGLISH, fract).substring(2))
                .replace("s2", "%.2f".format(Locale.ENGLISH, fract).substring(2))
                .replace("s1", "%.1f".format(Locale.ENGLISH, fract).substring(2))
                .replace("ZB", s0.toString(2))
                .replace("ZO", s0.toString(8))
                .replace("ZD", s0.toString(10))
                .replace("ZH", s0.toString(16))
                .replace("Z", s0.toString())
                .replace("ss", s.f2())
                .replace("mm", m.f2())
                .replace("hh", h.f2())
                .replace("dd", d.f2())
        )

        super.onDraw(stack, time, color)

    }

    fun Long.f2() = if(this < 10) "0$this" else this.toString()

    override fun createInspector(list: PanelListY, style: Style, getGroup: (title: String, description: String, dictSubPath: String) -> SettingCategory) {
        super.createInspector(list, style, getGroup)
        list.children.removeIf { it is TextInputML && it.base.placeholder == "Text" }
        list += vi("Format", "ss=sec, mm=min, hh=hours, dd=days, s3=millis", null, format, style){ format = it }
    }

    override val className get() = "Timer"
    override val defaultDisplayName get() = Dict["Timer", "obj.timer"]
    override val symbol get() = DefaultConfig["ui.symbol.timer", "\uD83D\uDD51"]

}