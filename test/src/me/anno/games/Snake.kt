package me.anno.games

import me.anno.Time
import me.anno.config.DefaultConfig.style
import me.anno.fonts.Font
import me.anno.gpu.RenderDoc.disableRenderDoc
import me.anno.gpu.drawing.DrawRectangles.drawRect
import me.anno.gpu.drawing.DrawTexts.drawText
import me.anno.input.Key
import me.anno.maths.Maths.MILLIS_TO_NANOS
import me.anno.maths.Maths.max
import me.anno.maths.Maths.min
import me.anno.ui.Panel
import me.anno.ui.base.components.AxisAlignment
import me.anno.ui.base.components.Padding
import me.anno.ui.base.groups.PanelContainer
import me.anno.ui.debug.TestEngine.Companion.testUI3
import me.anno.utils.Color.black
import java.util.Random

class Snake : Panel(style) {

    // create a simple snake game
    // personal high score: 22, then it just is way too fast currently xD

    // this was coded in ~ 45 minutes currently ^^

    val sx = 9
    val sy = 9

    var gameOver = false
    var lastStep = Time.gameTimeN
    var stepDelayNanos = 0L
    var isPaused = false

    var snakeStep = 0
    var headX = 0
    var headY = 0
    var snakeLength = 0
    val field = IntArray(sx * sy)
    var dx = 0
    var dy = 0

    val food = -1

    val random = Random()

    init {
        alignmentX = AxisAlignment.CENTER
        alignmentY = AxisAlignment.CENTER
    }

    fun isSnake(v: Int): Boolean {
        return v != food && v != 0 && v - snakeStep + snakeLength > 0
    }

    fun isSnake(x: Int, y: Int) =
        isSnake(field[x + y * sx])

    fun generateFood() {
        // if all blocks are snake,
        // you won, and the game will lock up ^^
        while (true) {
            val x = random.nextInt(sx)
            val y = random.nextInt(sy)
            if (isSnake(x, y)) continue
            field[x + y * sx] = food
            break
        }
    }

    fun startGame() {

        headX = 0
        headY = 0
        snakeLength = 1
        snakeStep = 1

        field.fill(0)

        dx = 0
        dy = 0

        field[headX + headY * sx] = snakeStep

        generateFood()

        gameOver = false
        stepDelayNanos = 500 * MILLIS_TO_NANOS
        isPaused = false
    }

    fun step() {
        if (isPaused || gameOver) return
        if (dx != 0 || dy != 0) {
            headX = (headX + dx + sx) % sx
            headY = (headY + dy + sy) % sy
            val dstIndex = headX + headY * sx
            val oldValue = field[dstIndex]
            snakeStep++ // must be called here, so we can step behind the snake without death
            if (isSnake(oldValue)) {
                gameOver = true
                return
            } else if (oldValue == food) {
                snakeLength++
                // todo better falloff
                stepDelayNanos = max(
                    stepDelayNanos - 30 * MILLIS_TO_NANOS,
                    50 * MILLIS_TO_NANOS
                )
            }
            field[dstIndex] = snakeStep
            if (oldValue == food) {
                generateFood()
            }
        }
        invalidateDrawing()
        lastStep = Time.gameTimeN
        if (windowStack.inFocus0 == null)
            requestFocus()
    }

    val font = Font("Verdana", 20)

    init {
        startGame()
        alignmentX = AxisAlignment.CENTER // not working :/
        alignmentY = AxisAlignment.CENTER
    }

    override fun calculateSize(w: Int, h: Int) {
        val wi = min(w, h * sx / sy)
        minW = wi
        minH = wi * sy / sx
    }

    override fun draw(x0: Int, y0: Int, x1: Int, y1: Int) {
        super.draw(x0, y0, x1, y1)

        // draw field
        for (y in 0 until sy) {
            val y2 = this.y + y * height / sy
            val y3 = this.y + (y + 1) * height / sy - 2
            for (x in 0 until sx) {
                val x2 = this.x + x * width / sx
                val x3 = this.x + (x + 1) * width / sx - 2
                val value = field[x + y * sx]
                val color = when {
                    isSnake(value) -> -1
                    value == food -> 0xff3333
                    else -> 0x333333
                } or black
                drawRect(x2, y2, x3 - x2, y3 - y2, color)
            }
        }

        // draw arrow in snake head
        // could be nicer, e.g., eyes and its tongue :)
        val snakeHeadChar = when {
            dx == -1 -> "<-"
            dx == +1 -> "->"
            dy == -1 -> "A"
            dy == +1 -> "V"
            else -> null
        }
        if (snakeHeadChar != null) drawText(
            this.x + (headX * width + width / 2) / sx, this.y + (headY * height + height / 2) / sy,
            font, snakeHeadChar, black, -1, -1, -1,
            AxisAlignment.CENTER, AxisAlignment.CENTER
        )

        if (gameOver) {
            drawText(
                x + width / 2, y + height / 2, font, "Game Over, Score: $snakeLength",
                -1, black, -1, -1,
                AxisAlignment.CENTER, AxisAlignment.CENTER
            )
        } else if (dx == 0 && dy == 0) {
            // draw instructions
            drawText(
                x + width / 2, y + height / 2, font, "Press WASD to control the snake",
                -1, black, -1, -1,
                AxisAlignment.CENTER, AxisAlignment.CENTER
            )
        } else if (isPaused) {
            drawText(
                x + width / 2, y + height / 2, font, "Press SPACE to continue",
                -1, black, -1, -1,
                AxisAlignment.CENTER, AxisAlignment.CENTER
            )
        }
    }

    override fun onUpdate() {
        super.onUpdate()
        if (Time.gameTimeN - lastStep > stepDelayNanos) {
            step()
        }
    }

    fun tryMove(dx: Int, dy: Int) {
        // check whether the movement would be valid
        if (!isSnake((headX + dx + sx) % sx, (headY + dy + sy) % sy)) {
            this.dx = dx
            this.dy = dy
            step()
        }
    }

    override fun onKeyTyped(x: Float, y: Float, key: Key) {
        when (key) {
            Key.KEY_W, Key.KEY_ARROW_UP -> tryMove(0, -1)
            Key.KEY_A, Key.KEY_ARROW_LEFT -> tryMove(-1, 0)
            Key.KEY_S, Key.KEY_ARROW_DOWN -> tryMove(0, 1)
            Key.KEY_D, Key.KEY_ARROW_RIGHT -> tryMove(1, 0)
            Key.KEY_SPACE, Key.KEY_ESCAPE -> {
                if (gameOver) {
                    startGame()
                } else {
                    isPaused = !isPaused
                }
                invalidateDrawing()
            }
            else -> super.onKeyTyped(x, y, key)
        }
    }
}

fun main() {
    disableRenderDoc()
    testUI3("Snake") {
        // container for layout
        PanelContainer(me.anno.games.Snake(), Padding.Zero, style)
    }
}