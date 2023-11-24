package me.anno.tests.ui

import me.anno.config.DefaultConfig
import me.anno.gpu.RenderDoc.disableRenderDoc
import me.anno.lua.ui.LuaAnimTextPanel
import me.anno.maths.Maths
import me.anno.maths.noise.FullNoise
import me.anno.ui.anim.AnimTextPanel
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.debug.TestStudio.Companion.testUI
import me.anno.utils.Color
import me.anno.utils.Color.mixARGB
import me.anno.utils.Color.withAlpha
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

class AnimTextPanelTest(useLua: Boolean) : PanelListY(DefaultConfig.style) {
    init {
        val green = 0x8fbc8f or Color.black
        val blue = 0x7777ff or Color.black
        val fontSize = 50f

        val font = AnimTextPanel("", DefaultConfig.style).font
            .withSize(fontSize)
            .withBold(true)

        add(AnimTextPanel.SimpleAnimTextPanel("Rainbow Text", DefaultConfig.style) { p, time, index, cx, cy ->
            p.font = font
            val s = time * 5f + index / 3f
            AnimTextPanel.translate(0f, sin(s) * 5f)
            AnimTextPanel.rotate(sin(s) * 0.1f, cx, cy)
            AnimTextPanel.hsluv(time * 2f - index / 2f)
        })
        // test of a panel with a lua script :3
        // excellent for fast and quick development; bad for allocations and GC
        if (useLua) add(
            LuaAnimTextPanel(
                "Lua Rainbow Text", "" +
                        "s = time*5+index/3\n" +
                        "translate(0,math.sin(s)*5)\n" +
                        "rotate(math.sin(s)*0.1)\n" +
                        "return hsluv(time*2-index/2)", DefaultConfig.style
            ).apply { this.font = font })
        add(AnimTextPanel.SimpleAnimTextPanel("Growing Text", DefaultConfig.style) { p, time, index, _, _ ->
            p.font = font
            val growTime = 0.4f
            val dissolveTime = 1.0f
            val phase = time - index * 0.03f
            val s = Maths.smoothStep((phase) / growTime)
            AnimTextPanel.translate(0f, (1f - s) * p.font.size / 2f)
            AnimTextPanel.scale(1f, s)
            green.withAlpha(min(1f, 20f * (dissolveTime - phase)))
        }.apply { periodMillis = 1500 }) // total time
        add(AnimTextPanel.SimpleAnimTextPanel("Special Department", DefaultConfig.style) { p, time, index, _, _ ->
            p.font = font
            val phase = time * 4f - index * 0.15f
            val s = Maths.clamp(sin(phase) * 2f + 1f, -1f, +1f)
            AnimTextPanel.scale(1f, s)
            if (s < 0f) AnimTextPanel.hsluv(0f, 1f - s) else blue
        })
        val burnPalette = intArrayOf(
            0, 0x66 shl 24, 0xaa shl 24,
            Color.black, Color.black, Color.black, Color.black, Color.black,
            0x533637 or Color.black,
            0xdd4848 or Color.black,
            0xf6b24c or Color.black,
            0xfffab3 or Color.black, 0
        )
        add(AnimTextPanel.SimpleAnimTextPanel("Burning", DefaultConfig.style) { p, time, index, cx, cy ->
            p.font = font
            val phase = time * 10f - index * 0.75f
            val index1 = Maths.clamp(burnPalette.lastIndex - phase, 0f, burnPalette.size - 0.001f)
            val scale = Maths.max(1f, 2f - phase / 2f)
            AnimTextPanel.scale(sqrt(scale), scale)
            AnimTextPanel.rotate(1f - scale, cx, cy)
            // smooth index
            val index1i = Maths.clamp(index1.toInt(), 0, burnPalette.size - 2)
            mixARGB(burnPalette[index1i], burnPalette[index1i + 1], Maths.clamp(index1 - index1i))
        }.apply { periodMillis = 1200 })
        val noise = FullNoise(1234L)
        val sketchPalette = intArrayOf(
            0xa6dee9 or Color.black,
            0xc5c5c8 or Color.black,
            0xbecbd2 or Color.black,
            0x7c99a9 or Color.black
        )
        add(AnimTextPanel.SimpleAnimTextPanel("Sketchy", DefaultConfig.style) { p, time, index, cx, cy ->
            p.font = font
            val seed = AnimTextPanel.limitFps(time, 3f) * 3f
            val pos = index * 5f + seed
            val y = seed * 0.3f
            val scale = 5f
            AnimTextPanel.translate(
                (noise[pos, y] - 0.5f) * scale,
                (noise[pos, y + 0.1f] - 0.5f) * scale
            )
            AnimTextPanel.rotate(noise[pos, y + 3f] - 0.5f, cx, cy)
            sketchPalette[(noise[pos] * 1e5).toInt() % sketchPalette.size] // choose a random color
        })
        add(AnimTextPanel.SimpleAnimTextPanel("Sketchy²", DefaultConfig.style) { p, time, index, cx, cy ->
            p.font = font
            val seed = AnimTextPanel.limitFps(time, 3f) * 3f
            val pos = index * 5f + seed
            val y = seed * 0.3f
            val scale = 5f
            AnimTextPanel.translate(
                (noise[pos, y] - 0.5f) * scale,
                (noise[pos, y + 0.1f] - 0.5f) * scale
            )
            AnimTextPanel.rotate(noise[pos, y + 3f] - 0.5f, cx, cy)
            val scale2 = 1.5f
            AnimTextPanel.perspective(
                cx, cy,
                (noise[pos, y + 7f] - 0.5f) * scale2,
                (noise[pos, y + 9.3f] - 0.5f) * scale2
            )
            sketchPalette[(noise[pos] * 1e5).toInt() % sketchPalette.size] // choose a random color
        })
    }
}

fun main() {
    // inspired by https://www.youtube.com/watch?v=3QXGM84ZfSw
    disableRenderDoc()
    testUI("AnimTextPanel") {
        AnimTextPanelTest(true)
    }
}