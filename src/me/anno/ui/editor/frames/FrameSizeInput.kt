package me.anno.ui.editor.frames

import me.anno.config.DefaultConfig
import me.anno.language.translation.NameDesc
import me.anno.parser.SimpleExpressionParser.parseDouble
import me.anno.ui.Style
import me.anno.ui.base.groups.PanelListX
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.input.EnumInput
import me.anno.ui.input.IntInput

/** input for width and height; offers presets and a custom input field; used in Rem's Studio */
@Suppress("unused")
class FrameSizeInput(nameDesc: NameDesc, value0: String, style: Style) : PanelListY(style) {

    private val val0 = value0.parseResolution() ?: defaultResolution

    private val typeInput = EnumInput(nameDesc, true,
        NameDesc(val0.toString()), defaultResolutions.map { NameDesc(it.toString()) } +
                NameDesc("Custom", "", "ui.frameSizeInput.custom"),
        style)

    private val deepStyle = style.getChild("deep")
    private val customInput = PanelListX(deepStyle)
    private val customX = IntInput(NameDesc("Width"), "", 0, deepStyle)
    private val customY = IntInput(NameDesc("Height"), "", 0, deepStyle)

    init {
        this += typeInput
        typeInput
            .setChangeListener { it, _, _ ->
                when (it.englishName) {
                    "Custom" -> {
                        customInput.isVisible = true
                    }
                    else -> {
                        val wh = it.englishName.split('x')
                        val ws = wh[0].trim().toInt()
                        val hs = wh[1].trim().toInt()
                        update(ws, hs)
                        customInput.isVisible = false
                    }
                }
            }
        customX.setValue(val0.width, false)
        customY.setValue(val0.height, false)
        customX.weight = 1f
        customY.weight = 1f
        customX.setChangeListener { update(it, customY.value) }
        customY.setChangeListener { update(customX.value, it) }
        customInput += customX
        customInput += customY
        customInput.isVisible = false
        this += customInput
    }

    fun update(ws: Long, hs: Long) {
        update(ws.toInt(), hs.toInt())
    }

    fun update(w: Int, h: Int) {
        changeListener(w, h)
        defaultResolution = Resolution(w, h)
    }

    class Resolution(val width: Int, val height: Int) : Comparable<Resolution> {
        override fun toString() = "${width}x$height"
        private val sortValue = width * (height + 1)
        override fun compareTo(other: Resolution): Int = sortValue.compareTo(other.sortValue)
    }

    var isSelectedListener: () -> Unit = {}
    var changeListener: (w: Int, h: Int) -> Unit = { _, _ -> }
    fun setChangeListener(listener: (w: Int, h: Int) -> Unit): FrameSizeInput {
        changeListener = listener
        return this
    }

    fun setIsSelectedListener(listener: () -> Unit): FrameSizeInput {
        isSelectedListener = listener
        return this
    }

    override fun draw(x0: Int, y0: Int, x1: Int, y1: Int) {
        super.draw(x0, y0, x1, y1)
        if (isInFocus) isSelectedListener()
    }

    companion object {

        @JvmStatic
        fun String.parseResolution(): Resolution? {
            val wh = lowercase()
                .replace('×', 'x') // utf8 x -> ascii x
                .split('x')
            val w = parseDouble(wh[0])?.toInt() ?: return null
            val h = wh.getOrNull(1)?.run { parseDouble(this) }?.toInt() ?: return null
            return Resolution(w, h)
        }

        const val configNamespace = "rendering.resolutions"

        @JvmField
        var defaultResolution =
            DefaultConfig["$configNamespace.default", ""].parseResolution() ?: Resolution(1920, 1080)

        @JvmField
        val defaultResolutions =
            DefaultConfig["$configNamespace.defaultValues", ""]
                .split(',').mapNotNull { it.parseResolution() }.toMutableList()

        init {
            val sortResolutions = DefaultConfig["$configNamespace.sort", 1]
            if (sortResolutions > 0) {
                defaultResolutions.sort()
            } else if (sortResolutions < 0) {
                defaultResolutions.sortDescending()
            }
        }

    }

}