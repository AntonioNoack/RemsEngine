package me.anno.studio.rems

import me.anno.animation.Type
import me.anno.config.DefaultConfig
import me.anno.config.DefaultStyle.black
import me.anno.gpu.GFX
import me.anno.language.translation.NameDesc
import me.anno.objects.Transform
import me.anno.studio.rems.RemsStudio.editorTime
import me.anno.studio.rems.RemsStudio.project
import me.anno.studio.rems.RemsStudio.targetDuration
import me.anno.studio.rems.RemsStudio.targetHeight
import me.anno.studio.rems.RemsStudio.targetOutputFile
import me.anno.studio.rems.RemsStudio.targetWidth
import me.anno.studio.rems.Rendering.renderAudio
import me.anno.studio.rems.Rendering.renderFrame
import me.anno.studio.rems.Rendering.renderPart
import me.anno.studio.rems.Rendering.renderSetPercent
import me.anno.ui.base.buttons.TextButton
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.base.text.TextPanel
import me.anno.ui.editor.SettingCategory
import me.anno.ui.editor.frames.FrameSizeInput
import me.anno.ui.input.EnumInput
import me.anno.ui.input.FileInput
import me.anno.ui.input.FloatInput
import me.anno.ui.input.IntInput
import me.anno.ui.style.Style
import me.anno.utils.Maths.mixARGB
import me.anno.utils.process.DelayedTask
import me.anno.video.FFMPEGEncodingBalance
import me.anno.video.FFMPEGEncodingType
import kotlin.math.max

object RenderSettings : Transform() {

    // render queue?

    override val defaultDisplayName: String = "Render Settings"

    override fun createInspector(
        list: PanelListY,
        style: Style,
        getGroup: (title: String, description: String, dictSubPath: String) -> SettingCategory
    ) {

        val project = project!!

        list += TextPanel(defaultDisplayName, style)
            .apply { focusTextColor = textColor }
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

        list += EnumInput(
            "Frame Rate",
            true,
            project.targetFPS.toString(),
            framesRates.map { NameDesc(it.toString()) },
            style
        )
            .setChangeListener { value, _, _ ->
                project.targetFPS = value.toDouble()
                save()
            }
            .setTooltip("The fps of the video, or how many frame are shown per second")

        list += IntInput("Video Quality", "VideoQuality", project.targetVideoQuality, Type.VIDEO_QUALITY_CRF, style)
            .setChangeListener {
                project.targetVideoQuality = it.toInt()
                save()
            }
            .setTooltip("0 = lossless, 23 = default, 51 = worst; worse results have smaller file sizes")

        // todo still cannot be animated... why???
        // todo why is the field not showing up?
        val mbs = vi(
            "Motion-Blur-Steps",
            "0,1 = no motion blur, e.g. 16 = decent motion blur, sub-frames per frame",
            project.motionBlurSteps, style
        ) as IntInput
        val mbsListener = mbs.changeListener
        mbs.setChangeListener {
            mbsListener(it)
            save()
        }
        list += mbs

        val shp = vi(
            "Shutter-Percentage",
            "[Motion Blur] 1 = full frame is used; 0.1 = only 1/10th of a frame time is used",
            project.shutterPercentage, style
        ) as FloatInput
        val shpListener = shp.changeListener
        shp.setChangeListener {
            shpListener(it)
            save()
        }
        list += shp

        list += EnumInput(
            "Encoding Speed / Compression",
            "How much time is spent on compressing the video into a smaller file",
            "ui.ffmpeg.encodingSpeed",
            project.ffmpegBalance.nameDesc,
            FFMPEGEncodingBalance.values().map { it.nameDesc },
            style
        ).setChangeListener { _, index, _ -> project.ffmpegBalance = FFMPEGEncodingBalance.values()[index]; save() }

        list += EnumInput(
            "Encoding Type", "Helps FFMPEG with the encoding process", "ui.ffmpeg.flags.input",
            project.ffmpegFlags.nameDesc, FFMPEGEncodingType.values().map { it.nameDesc }, style
        ).setChangeListener { _, index, _ -> project.ffmpegFlags = FFMPEGEncodingType.values()[index]; save() }

        val fileInput = FileInput("Output File", style, targetOutputFile)
        val originalColor = fileInput.base2.textColor
        fun updateFileInputColor() {
            val file = project.targetOutputFile
            fileInput.base2.run {
                textColor = mixARGB(
                    originalColor, when {
                        file.isDirectory -> 0xff0000 or black
                        file.exists -> 0xffff00 or black
                        else -> 0x00ff00 or black
                    }, 0.5f
                )
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

        val callback = { GFX.requestAttentionMaybe() }

        list += TextButton("Render at 100%", false, style)
            .setSimpleClickListener { renderPart(1, true, callback) }
            .setTooltip("Create video at full resolution")
        list += TextButton("Render at 50%", false, style)
            .setSimpleClickListener { renderPart(2, true, callback) }
            .setTooltip("Create video at half resolution")
        list += TextButton("Render at 25%", false, style)
            .setSimpleClickListener { renderPart(4, true, callback) }
            .setTooltip("Create video at quarter resolution")
        list += TextButton("Render at Set%", false, style)
            .setSimpleClickListener { renderSetPercent(true, callback) }
            .setTooltip("Create video at your custom set relative resolution")
        list += TextButton("Render Audio only", false, style)
            .setSimpleClickListener { renderAudio(true, callback) }
            .setTooltip("Only creates an audio file; no video is rendered nor saved.")
        list += TextButton("Render Current Frame", false, style)
            .setSimpleClickListener { renderFrame(targetWidth, targetHeight, editorTime, true, callback) }
            .setTooltip("Only creates an audio file; no video is rendered nor saved.")

    }

    private val savingTask = DelayedTask { actuallySave() }

    fun save() {
        savingTask.update()
    }

    private fun actuallySave() {
        save()
        project!!.saveConfig()
    }

}