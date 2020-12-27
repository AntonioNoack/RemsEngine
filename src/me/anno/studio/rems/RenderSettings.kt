package me.anno.studio.rems

import me.anno.config.DefaultConfig
import me.anno.config.DefaultStyle.black
import me.anno.objects.Transform
import me.anno.objects.animation.Type
import me.anno.studio.rems.RemsStudio.project
import me.anno.studio.rems.RemsStudio.targetDuration
import me.anno.studio.rems.RemsStudio.targetOutputFile
import me.anno.studio.rems.Rendering.renderPart
import me.anno.studio.StudioBase.Companion.addEvent
import me.anno.ui.base.buttons.TextButton
import me.anno.ui.base.TextPanel
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.editor.SettingCategory
import me.anno.ui.editor.frames.FrameSizeInput
import me.anno.ui.input.EnumInput
import me.anno.ui.input.FileInput
import me.anno.ui.input.FloatInput
import me.anno.ui.input.IntInput
import me.anno.ui.style.Style
import me.anno.utils.Maths.mixARGB
import kotlin.concurrent.thread
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

object RenderSettings : Transform() {

    override fun getDefaultDisplayName(): String = "Render Settings"

    override fun createInspector(
        list: PanelListY,
        style: Style,
        getGroup: (title: String, id: String) -> SettingCategory
    ) {

        val project = project!!

        list += TextPanel(getDefaultDisplayName(), style).apply { focusTextColor = textColor }
        list += vi("Duration", "Video length in seconds", Type.FLOAT_PLUS, targetDuration, style) {
            project.targetDuration = it
            save()
        }

        list += vi(
            "Relative Frame Size (%)", "For rendering tests, in percent",
            Type.FLOAT_PERCENT, project.targetSizePercentage, style
        ) {
            project.targetSizePercentage = it
            save()
        }

        list += FrameSizeInput("Frame Size", "${project.targetWidth}x${project.targetHeight}", style)
            .setChangeListener { w, h ->
                project.targetWidth = max(1, w)
                project.targetHeight = max(1, h)
                save()
            }
            .setTooltip("Size of resulting video")

        var framesRates = DefaultConfig["rendering.frameRates", "60"]
            .split(',')
            .mapNotNull { it.trim().toDoubleOrNull() }
            .toMutableList()

        if (framesRates.isEmpty()) framesRates = arrayListOf(60.0)
        if (project.targetFPS !in framesRates) framesRates.add(0, project.targetFPS)
        list += EnumInput("Frame Rate", true, project.targetFPS.toString(), framesRates.map { it.toString() }, style)
            .setChangeListener { value, _, _ ->
                project.targetFPS = value.toDouble()
                save()
            }
            .setTooltip("The fps of the video, or how many frame are shown per second")

        list += IntInput("Video Quality", project.targetVideoQuality, Type.VIDEO_QUALITY_CRF, style)
            .setChangeListener {
                project.targetVideoQuality = it.toInt()
                save()
            }
            .setTooltip("0 = lossless, 23 = default, 51 = worst; worse results have smaller file sizes")

        list += IntInput("Motion-Blur-Steps", project.motionBlurSteps, Type.INT_PLUS, style)
            .setChangeListener {
                project.motionBlurSteps = it.toInt()
                save()
            }
            .setTooltip("0,1 = no motion blur, e.g. 16 = decent motion blur, sub-frames per frame")

        list += FloatInput("Shutter-Percentage", project.shutterPercentage, Type.FLOAT_PLUS, style)
            .setChangeListener {
                project.shutterPercentage = it.toFloat()
                save()
            }
            .setTooltip("[Motion Blur] 1 = full frame is used; 0.1 = only 1/10th of a frame time is used")

        // todo file selector from OS...
        val fileInput = FileInput("Output File", style, targetOutputFile)
        val originalColor = fileInput.base2.textColor
        fun updateFileInputColor() {
            val file = project.targetOutputFile
            fileInput.base2.run {
                textColor = mixARGB(originalColor, when {
                    file.isDirectory -> 0xff0000 or black
                    file.exists() -> 0xffff00 or black
                    else -> 0x00ff00 or black
                }, 0.5f)
                focusTextColor = textColor
            }
        }

        updateFileInputColor()
        fileInput.setChangeListener {
            val file = it//File(it)
            project.targetOutputFile = file
            updateFileInputColor()
            save()
        }
        list += fileInput

        list += TextButton("Render at 100%", false, style)
            .setSimpleClickListener { renderPart(1) }
            .setTooltip("Create video at full resolution")
        list += TextButton("Render at 50%", false, style)
            .setSimpleClickListener { renderPart(2) }
            .setTooltip("Create video at half resolution")
        list += TextButton("Render at 25%", false, style)
            .setSimpleClickListener { renderPart(4) }
            .setTooltip("Create video at quarter resolution")
        list += TextButton("Render at Set%", false, style)
            .setSimpleClickListener { renderSetPercent() }
            .setTooltip("Create video at your custom set relative resolution")

    }

    fun renderSetPercent() {
        Rendering.render(
            max(2, (project!!.targetWidth * project!!.targetSizePercentage / 100).roundToInt()),
            max(2, (project!!.targetHeight * project!!.targetSizePercentage / 100).roundToInt())
        )
    }

    var lastSavePoint = 0L
    var wasChanged = false
    fun save() {
        val time = System.nanoTime()
        if (abs(time - lastSavePoint) > 500_000_000L) {// 500ms, saving 2/s
            actuallySave()
        } else {
            wasChanged = true
            thread {
                Thread.sleep(500) // save
                if (wasChanged) {
                    addEvent {
                        if (wasChanged) {// yes, checking twice makes sense
                            actuallySave()
                        }
                    }
                }
            }
        }
    }

    fun actuallySave() {
        wasChanged = false
        project!!.saveConfig()
    }

}