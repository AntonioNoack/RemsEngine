package me.anno.ui.editor.frames

import me.anno.config.DefaultConfig
import me.anno.ui.base.Visibility
import me.anno.ui.base.groups.PanelListX
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.input.EnumInput
import me.anno.ui.input.components.PureTextInput
import me.anno.ui.style.Style

class FrameSizeInput(title: String, value0: String, style: Style): PanelListY(style){

    val val0 = value0.parseResolution() ?: defaultResolution

    val typeInput = EnumInput(title, true,
        val0.toString(), defaultResolutions.map { it.toString() } + "Custom",
        style)

    val deepStyle = style.getChild("deep")
    val customInput = PanelListX(deepStyle)
    val customX = PureTextInput(deepStyle)
    val customY = PureTextInput(deepStyle)

    init {
        this += typeInput
        typeInput
            .setChangeListener { it, _, _ ->
                when(it){
                    "Custom" -> {
                        customInput.visibility = Visibility.VISIBLE
                    }
                    else -> {
                        val wh = it.split('x')
                        val ws = wh[0].trim()
                        val hs = wh[1].trim()
                        update(ws, hs)
                        customInput.visibility = Visibility.GONE
                    }
                }
            }
        customX.text = val0.w.toString()
        customY.text = val0.h.toString()
        customX.updateChars(false)
        customY.updateChars(false)
        customInput += customX.setChangeListener { update(it, customY.text) }.setWeight(1f)
        customInput += customY.setChangeListener { update(customX.text, it) }.setWeight(1f)
        customInput.visibility = Visibility.GONE
        this += customInput
    }

    fun update(ws: String, hs: String){
        val w = ws.toIntOrNull() ?: return
        val h = hs.toIntOrNull() ?: return
        if(ws != customX.text){
            customX.text = ws
            customX.updateChars(false)
        }
        if(hs != customY.text){
            customY.text = hs
            customY.updateChars(false)
        }
        changeListener(w, h)
        defaultResolution = Resolution(w, h)
    }

    class Resolution(val w: Int, val h: Int): Comparable<Resolution> {
        override fun toString() = "${w}x$h"
        private val sortValue = w*(h+1)
        override fun compareTo(other: Resolution): Int = sortValue.compareTo(other.sortValue)
    }

    var changeListener: (w: Int, h: Int) -> Unit = { _,_ -> }
    fun setChangeListener(listener: (w: Int, h: Int) -> Unit): FrameSizeInput {
        changeListener = listener
        return this
    }

    companion object {

        fun String.parseResolution(): Resolution? {
            val wh = toLowerCase()
                .replace('×', 'x') // utf8 x -> ascii x
                .split('x')
            val w = wh[0].trim().toIntOrNull() ?: return null
            val h = wh.getOrNull(1)?.trim()?.toIntOrNull() ?: return null
            return Resolution(w, h)
        }

        const val configNamespace = "rendering.resolutions"
        var defaultResolution = DefaultConfig["$configNamespace.default", ""].parseResolution() ?: Resolution(1920, 1080)
        val defaultResolutions =
            DefaultConfig["$configNamespace.defaultValues", ""]
                .split(',').mapNotNull { it.parseResolution() }.toMutableList()

        init {
            val sortResolutions = DefaultConfig["$configNamespace.sort", 1]
            if(sortResolutions > 0){
                defaultResolutions.sort()
            } else if(sortResolutions < 0){
                defaultResolutions.sortDescending()
            }
        }

    }

}