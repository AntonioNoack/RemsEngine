package me.anno.ui.debug

import me.anno.Engine
import me.anno.gpu.GFX
import me.anno.gpu.WindowX
import me.anno.gpu.drawing.DrawTexts.drawSimpleTextCharByChar
import kotlin.math.max
import kotlin.math.roundToInt

object FPSPanel {

    val text = "theFPS, min: theFPS".toCharArray()

    fun add(nanos: Long, color: Int) {
        FrameTimes.putValue(nanos * 1e-9f, color)
    }

    fun showFPS(window: WindowX) {

        val x0 = max(0, window.width - FrameTimes.width)
        val y0 = max(0, window.height - FrameTimes.height)

        FrameTimes.setPosSize(x0, y0, FrameTimes.width, FrameTimes.height)
        FrameTimes.draw()

        GFX.loadTexturesSync.push(true)

        val maxTime = FrameTimes.timeContainer.maxValue
        formatNumber(text, 0, 6, Engine.currentFPS)
        formatNumber(text, 13, 6, 1f / maxTime)

        drawSimpleTextCharByChar(x0, y0, 2, text)

        GFX.loadTexturesSync.pop()

    }

    fun getChar(digit: Int) = ((digit % 10) + '0'.code).toChar()

    fun formatNumber(chars: CharArray, index: Int, space: Int, number: Float) {

        if (number < 0) {
            chars[index] = '-'
            formatNumber(chars, index + 1, space - 1, -number)
            return
        }

        if (number >= 999.5f) {
            formatNumber(chars, index, space, number.roundToInt())
        }

        val numberX = (number * 10).roundToInt()
        chars[index + space - 2] = '.'
        chars[index + space - 1] = getChar(numberX)
        formatNumber(chars, index, space - 2, numberX / 10)

    }

    fun formatNumber(chars: CharArray, index: Int, space: Int, number: Int) {

        if (number < 0) {
            chars[index] = '-'
            formatNumber(chars, index + 1, space - 1, -number)
            return
        }

        var limit = 1
        for (i in 0 until space) {
            limit *= 10
        }

        if (number >= limit) {
            // all 9, maybe an x for extra much?
            chars.fill('x', index, index + space)
            return
        }

        // can print it :)
        printNumber(chars, index, space, number)

    }

    fun printNumber(chars: CharArray, index: Int, space: Int, number: Int) {

        // can print it :)
        chars[index + space - 1] = getChar(number)

        if (space > 1) {
            // we need to print more
            if (number >= 10) {
                printNumber(chars, index, space - 1, number / 10)
            } else {
                chars.fill(' ', index, index + space - 1)
            }
        }

    }

}