package me.anno.tests.game

import me.anno.Engine
import me.anno.config.DefaultConfig.style
import me.anno.config.DefaultStyle.black
import me.anno.gpu.GFX
import me.anno.gpu.drawing.DrawRectangles.drawRect
import me.anno.gpu.drawing.DrawTexts.drawText
import me.anno.maths.Maths.MILLIS_TO_NANOS
import me.anno.maths.Maths.max
import me.anno.ui.Panel
import me.anno.ui.base.Font
import me.anno.ui.base.constraints.AspectRatioConstraint
import me.anno.ui.base.constraints.AxisAlignment
import me.anno.ui.debug.TestStudio.Companion.testUI
import org.lwjgl.glfw.GLFW
import java.util.*

fun main() {
    GFX.disableRenderDoc()
    testUI {
        object : Panel(style) {

            // create a simple snake game
            // personal high score: 16, then it just is way too fast currently xD

            // this was coded in ~ 45 minutes currently ^^

            val sx = 9
            val sy = 9

            var gameOver = false
            var lastStep = Engine.gameTime
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
                    field[dstIndex] = ++snakeStep
                    if (oldValue == food) {
                        generateFood()
                    }
                }
            }

            val font = Font("Verdana", 20)

            init {
                startGame()
                alignmentX = AxisAlignment.CENTER // not working :/
                alignmentY = AxisAlignment.CENTER
                add(AspectRatioConstraint(sy.toFloat() / sx.toFloat()))
            }

            override fun onDraw(x0: Int, y0: Int, x1: Int, y1: Int) {
                super.onDraw(x0, y0, x1, y1)

                // draw field
                for (y in 0 until sy) {
                    val y2 = this.y + y * h / sy
                    val y3 = this.y + (y + 1) * h / sy - 2
                    for (x in 0 until sx) {
                        val x2 = this.x + x * w / sx
                        val x3 = this.x + (x + 1) * w / sx - 2
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
                    this.x + (headX * w + w / 2) / sx, this.y + (headY * h + h / 2) / sy,
                    font, snakeHeadChar, black, -1, -1, -1,
                    AxisAlignment.CENTER, AxisAlignment.CENTER
                )

                if (gameOver) {
                    drawText(
                        x + w / 2, y + h / 2, font, "Game Over, Score: $snakeLength",
                        -1, black, -1, -1,
                        AxisAlignment.CENTER, AxisAlignment.CENTER
                    )
                } else if (dx == 0 && dy == 0) {
                    // draw instructions
                    drawText(
                        x + w / 2, y + h / 2, font, "Press WASD to control the snake",
                        -1, black, -1, -1,
                        AxisAlignment.CENTER, AxisAlignment.CENTER
                    )
                } else if (isPaused) {
                    drawText(
                        x + w / 2, y + h / 2, font, "Press SPACE to continue",
                        -1, black, -1, -1,
                        AxisAlignment.CENTER, AxisAlignment.CENTER
                    )
                }
            }

            override fun tickUpdate() {
                super.tickUpdate()
                if (Engine.gameTime - lastStep > stepDelayNanos) {
                    step()
                    invalidateDrawing()
                    lastStep = Engine.gameTime
                }
            }

            override fun onKeyTyped(x: Float, y: Float, key: Int) {
                when (key) {
                    GLFW.GLFW_KEY_W, GLFW.GLFW_KEY_UP -> {
                        // check whether the movement would be valid
                        if (!isSnake(headX, (headY - 1 + sy) % sy)) {
                            dx = 0
                            dy = -1
                            invalidateDrawing()
                        }
                    }
                    GLFW.GLFW_KEY_A, GLFW.GLFW_KEY_LEFT -> {
                        // check whether the movement would be valid
                        if (!isSnake((headX - 1 + sx) % sx, headY)) {
                            dx = -1
                            dy = 0
                            invalidateDrawing()
                        }
                    }
                    GLFW.GLFW_KEY_S, GLFW.GLFW_KEY_DOWN -> {
                        if (!isSnake(headX, (headY + 1) % sy)) {
                            dx = 0
                            dy = 1
                            invalidateDrawing()
                        }
                    }
                    GLFW.GLFW_KEY_D, GLFW.GLFW_KEY_RIGHT -> {
                        if (!isSnake((headX + 1) % sx, headY)) {
                            dx = 1
                            dy = 0
                            invalidateDrawing()
                        }
                    }
                    GLFW.GLFW_KEY_SPACE, GLFW.GLFW_KEY_ESCAPE -> {
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
        }.setWeight(1f)
    }

}