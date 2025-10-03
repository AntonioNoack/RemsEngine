package me.anno.games.visualnovel

import me.anno.Time
import me.anno.config.DefaultConfig
import me.anno.games.visualnovel.VisualNovelState.numOptions
import me.anno.games.visualnovel.VisualNovelState.primary
import me.anno.games.visualnovel.VisualNovelState.questionNode
import me.anno.games.visualnovel.VisualNovelState.secondary
import me.anno.games.visualnovel.VisualNovelState.shownText
import me.anno.games.visualnovel.VisualNovelState.textTime
import me.anno.gpu.drawing.DrawTextures
import me.anno.gpu.texture.Clamping
import me.anno.gpu.texture.Filtering
import me.anno.gpu.texture.ITexture2D
import me.anno.gpu.texture.TextureCache
import me.anno.graph.visual.states.StateMachine
import me.anno.image.ImageScale
import me.anno.input.Key
import me.anno.maths.Maths
import me.anno.maths.MinMax.max
import me.anno.ui.base.groups.PanelList
import me.anno.ui.base.text.TextPanel
import me.anno.utils.Color.mulARGB
import me.anno.utils.types.Strings.isNotBlank2
import kotlin.math.min

class NovelGamePanel(val stateMachine: StateMachine) : PanelList(DefaultConfig.style) {
    override val canDrawOverBorders: Boolean get() = true

    val shownTextPanel = TextPanel(style)

    init {
        addChild(shownTextPanel)
        shownTextPanel.background.radius = 15f
        shownTextPanel.padding.set(10)
        shownTextPanel.instantTextLoading = true // todo this isn't working :(
        shownTextPanel.background.color = background.color
        shownTextPanel.focusBackgroundColor = background.color
        shownTextPanel.focusTextColor = shownTextPanel.textColor
        shownTextPanel.breaksIntoMultiline = true
        background.color = background.color.mulARGB(0xffcccccc.toInt())
    }

    override fun onMouseClicked(x: Float, y: Float, button: Key, long: Boolean) {
        if (button == Key.BUTTON_LEFT && !long && numOptions < 1) {
            stateMachine.update()
        } else super.onMouseClicked(x, y, button, long)
    }

    override fun onCharTyped(x: Float, y: Float, codepoint: Int) {
        val idx = codepoint - '0'.code
        if (idx in 1..numOptions) {
            questionNode?.setOutput(1, idx)
            stateMachine.update()
        } else super.onCharTyped(x, y, codepoint)
    }

    override fun calculateSize(w: Int, h: Int) {
        super.calculateSize(w, h)
        val maxW = w * 17 / 20
        shownTextPanel.calculateSize(maxW, h)
    }

    override fun placeChildrenWithoutPadding(x: Int, y: Int, width: Int, height: Int) {
        val w = width * 9 / 10
        val h = height * 5 / 20
        shownTextPanel.setPosSize(x + (width - w) / 2, y + height - h, w, h)
    }

    fun drawChar(image: ITexture2D, pos: Int, hasText: Boolean, w: Int) {
        val h = image.height * w / image.width
        image.bind(0, Filtering.LINEAR, Clamping.CLAMP)
        DrawTextures.drawTexture(
            x + width * pos / 100 - w / 2,
            y + height * (if (hasText) 7 else 9) / 10 - h / 2, w, h, image
        )
    }

    override fun draw(x0: Int, y0: Int, x1: Int, y1: Int) {

        if (textTime == 0L) textTime = Time.nanoTime

        val hasText = shownText.isNotBlank2()

        // draw background
        val bgImage = TextureCache[VisualNovelState.background].value
        if (bgImage != null) {
            // to do can/should we blur the background a little?
            // to do maybe foreground, too, based on cursor?
            val (w, h) = ImageScale.scaleMin(bgImage.width, bgImage.height, width, height)
            val yi = if (hasText) y + (height * 8 / 10 - h) / 2 else y + (height - h) / 2
            DrawTextures.drawTexture(x + (width - w) / 2, yi, w, h, bgImage)
        } else drawBackground(x0, y0, x1, y1)

        val charWidth = (0.38f * max(width, height)).toInt()

        val right = TextureCache[secondary].value
        val left = TextureCache[primary].value
        when {
            left != null && right != null -> {
                drawChar(right, 75, hasText, charWidth)
                drawChar(left, 25, hasText, charWidth)
            }
            left != null -> {
                drawChar(left, 42, hasText, charWidth)
            }
            right != null -> {
                drawChar(right, 58, hasText, charWidth)
            }
        }

        if (hasText) {
            drawBackground(x, y + height * 8 / 10, x + width, y + height)

            val progress = 10 * Maths.sq(1e-9 * (Time.nanoTime - textTime))
            shownTextPanel.text = shownText.substring(0, min(progress.toInt(), shownText.length))

            drawChildren(x0, y0, x1, y1)
        }
    }
}