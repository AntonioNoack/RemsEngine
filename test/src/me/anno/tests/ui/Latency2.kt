package me.anno.tests.ui

import me.anno.Time
import me.anno.config.DefaultConfig.style
import me.anno.engine.EngineBase
import me.anno.gpu.RenderDoc.disableRenderDoc
import me.anno.gpu.drawing.DrawTexts
import me.anno.input.Input
import me.anno.input.Key
import me.anno.language.translation.NameDesc
import me.anno.maths.Maths.MILLIS_TO_NANOS
import me.anno.ui.base.components.AxisAlignment
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.debug.TestDrawPanel
import me.anno.ui.debug.TestEngine
import me.anno.ui.input.EnumInput
import me.anno.utils.Color.black
import me.anno.utils.Color.white
import me.anno.utils.types.Strings.upperSnakeCaseToTitle

// compare this with browsers like Chrome and
// https://humanbenchmark.com/tests/reactiontime
// -> without v-sync, about as good as Chrome
// -> with v-sync, it's 30ms worse :/

enum class LatencyTestMode {
    VSYNC,
    UNLIMITED,
    LIMITED_60 // kind of mixed, but I think better than v-sync
}

fun main() {
    disableRenderDoc()
    var shownText = "Wait for screen to turn green!"
    var toBeChanged = 3f
    var firstTimeGreen = 0L
    var mode = LatencyTestMode.UNLIMITED
    TestEngine.testUI3("Input Latency") {
        val list = PanelListY(style)
        list.add(TestDrawPanel {
            EngineBase.enableVSync = mode == LatencyTestMode.VSYNC // with v-sync it's about 30ms worse
            EngineBase.maxFPS = if (mode == LatencyTestMode.LIMITED_60) 60 else 0
            it.backgroundColor = when {
                toBeChanged < 0f -> 0x336633 or black
                else -> 0x773333 or black
            }
            if (toBeChanged < 0f && firstTimeGreen == 0L) {
                firstTimeGreen = Time.nanoTime
                shownText = "Click!"
            } else if (Input.wasKeyPressed(Key.BUTTON_LEFT)) {
                if (toBeChanged < 0f) {
                    val time = Input.keysDown[Key.BUTTON_LEFT] ?: Time.nanoTime
                    // good reaction
                    val dtMillis = (time - firstTimeGreen) / MILLIS_TO_NANOS
                    shownText = "Reaction Time: $dtMillis"
                    toBeChanged = Math.random().toFloat() * 3f + 2f
                    firstTimeGreen = 0L
                } else {
                    // bad timing!
                    toBeChanged = 2f
                    shownText = "Wait for green before clicking!"
                }
            }
            toBeChanged -= Time.deltaTime.toFloat()
            it.clear()
            DrawTexts.drawSimpleTextCharByChar(
                it.x + it.width / 2, it.y + it.height / 2, 0,
                shownText, white, it.backgroundColor,
                AxisAlignment.CENTER,
                AxisAlignment.CENTER
            )
        }.fill(1f))
        val modeUI = EnumInput(
            NameDesc("Mode"), NameDesc(mode.name.upperSnakeCaseToTitle()),
            LatencyTestMode.entries.map { NameDesc(it.name.upperSnakeCaseToTitle()) },
            style
        ).setChangeListener { _, index, _ ->
            mode = LatencyTestMode.entries[index]
        }.apply { alignmentX = AxisAlignment.CENTER }
        list.add(modeUI)
        list
    }
}