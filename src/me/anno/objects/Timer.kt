package me.anno.objects

import me.anno.fonts.PartResult
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.input.TextInput
import me.anno.ui.input.TextInputML
import me.anno.ui.style.Style
import me.anno.utils.fract
import org.joml.Matrix4fArrayList
import org.joml.Vector4f
import java.util.*

class Timer(parent: Transform? = null): Text("", parent) {

    // todo start value
    // todo date input?

    var format = "hh:mm:ss.s2"

    override fun splitSegments(text: String): PartResult? {
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
    }

    override fun onDraw(stack: Matrix4fArrayList, time: Double, color: Vector4f) {

        val fract = fract(time)
        var s = time.toLong()
        var m = s / 60
        var h = m / 60
        val d = h / 24

        s %= 60
        m %= 60
        h %= 24

        text = format
            .replace("s0", "")
            .replace("s6", "%.6f".format(Locale.ENGLISH, fract).substring(2))
            .replace("s5", "%.5f".format(Locale.ENGLISH, fract).substring(2))
            .replace("s4", "%.4f".format(Locale.ENGLISH, fract).substring(2))
            .replace("s3", "%.3f".format(Locale.ENGLISH, fract).substring(2))
            .replace("s2", "%.2f".format(Locale.ENGLISH, fract).substring(2))
            .replace("s1", "%.1f".format(Locale.ENGLISH, fract).substring(2))
            .replace("ss", s.f2())
            .replace("mm", m.f2())
            .replace("hh", h.f2())
            .replace("dd", d.f2())

        super.onDraw(stack, time, color)
    }

    fun Long.f2() = if(this < 10) "0$this" else this.toString()

    override fun createInspector(list: PanelListY, style: Style) {
        super.createInspector(list, style)
        list.children.removeIf { it is TextInputML && it.base.placeholder == "Text" }
        list += TextInput("Format", style)
            .setChangeListener { format = it }
            .setIsSelectedListener { show(null) }
    }

    override fun getClassName() = "Timer"

}