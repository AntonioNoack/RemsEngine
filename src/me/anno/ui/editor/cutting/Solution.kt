package me.anno.ui.editor.cutting

import me.anno.gpu.GFXx2D.drawRectGradient
import me.anno.objects.Transform
import me.anno.objects.Video
import me.anno.ui.editor.TimelinePanel.Companion.centralTime
import me.anno.ui.editor.TimelinePanel.Companion.dtHalfLength
import me.anno.ui.editor.cutting.LayerView.Companion.maxStripes
import me.anno.utils.Maths.mix
import me.anno.video.FFMPEGMetadata
import org.joml.Vector4f
import kotlin.math.roundToInt

class Solution(
    val x0: Int, val y0: Int, val x1: Int, val y1: Int,
    val refTime: Double
) {
    val w = x1 - x0
    val stripes = Array(maxStripes) {
        ArrayList<Gradient>(w / 2)
    }

    // todo draw a stripe of the current image, or a symbol or sth...
    // todo if audio, draw audio levels
    // todo if video, draw a few frames in small

    val enableVideoPreview = false

    fun draw() {

        val t0 = System.nanoTime()

        val y = y0
        val h = y1 - y0

        val deltaX = ((refTime - centralTime) * w / (dtHalfLength * 2)).roundToInt()

        val metas = HashMap<Any, Any>()

        stripes.forEachIndexed { index, gradients ->
            val y0 = y + 3 + index * 3
            val h0 = h - 10
            gradients.forEach {

                val video = it.owner as? Video
                val meta = if(video == null || !enableVideoPreview) null
                else metas.getOrPut(video){ video.meta ?: Unit } as? FFMPEGMetadata

                val hasAudio = meta?.hasAudio ?: false
                val hasVideo = meta?.hasVideo ?: false

                val c0 = it.c0
                val c1 = it.c2

                val ix0 = it.x0 + deltaX
                val ix1 = it.x2 + deltaX

                val t1 = System.nanoTime()
                val budget = 20 * 1e6 // ms
                val hasBudgetLeft = (t1 - t0) < budget

                if (hasBudgetLeft && hasVideo && enableVideoPreview) {

                    video!!
                    meta!!

                    val frameWidth = (h * (1f + border) * video.w / video.h).roundToInt()

                    val frameOffset = (-centralTime / (2f * dtHalfLength) * w).toInt() % frameWidth

                    val frameIndex0 = (ix0 - frameOffset) / frameWidth
                    val frameIndex1 = (ix1 - frameOffset) / frameWidth

                    if (frameIndex0 != frameIndex1) {
                        // split into multiple frames

                        // middle frames
                        for (frameIndex in frameIndex0 + 1 until frameIndex1) {
                            val x0 = frameWidth * frameIndex + frameOffset
                            val x1 = x0 + frameWidth
                            val lerpedC0 = mix(c0, c1, (x0 - ix0).toFloat() / it.w)
                            val lerpedC1 = mix(c0, c1, (x1 - ix0).toFloat() / it.w)
                            draw(x0, x1, y0, h0, lerpedC0, lerpedC1, frameOffset, frameWidth, video, meta, 0f, 1f)
                        }

                        // first frame
                        val x1 = (frameIndex0 + 1) * frameWidth + frameOffset
                        val lerpedC1 = mix(c0, c1, (x1 - ix0).toFloat() / it.w)
                        val f0 = ((ix0 - frameOffset) % frameWidth).toFloat() / frameWidth
                        draw(ix0, x1, y0, h0, c0, lerpedC1, frameOffset, frameWidth, video, meta, f0, 1f)

                        // last frame
                        val x0 = frameIndex1 * frameWidth + frameOffset
                        val lerpedC0 = if (x0 == x1) lerpedC1
                        else mix(c0, c1, (x0 - ix0).toFloat() / it.w)
                        val x2Mod = (ix1 - frameOffset) % frameWidth
                        val f1 = if (x2Mod == 0) 1f else x2Mod.toFloat() / frameWidth
                        draw(x0, ix0 + it.w, y0, h0, lerpedC0, c1, frameOffset, frameWidth, video, meta, 0f, f1)

                    } else {

                        val f0 = ((ix0 - frameOffset) % frameWidth).toFloat() / frameWidth
                        val x2Mod = (ix1 - frameOffset) % frameWidth
                        val f1 = if (x2Mod == 0) 1f else x2Mod.toFloat() / frameWidth
                        draw(ix0, ix1 + 1, y0, h0, c0, c1, frameOffset, frameWidth, video, meta, f0, f1)

                    }

                } else draw(ix0, ix1 + 1, y0, h0, c0, c1)

            }
        }
    }

    fun keepResourcesLoaded() {

        val t0 = System.nanoTime()

        val h = y1 - y0

        stripes.forEach { gradients ->
            gradients.forEach {

                val video = it.owner as? Video
                val meta = video?.meta

                val hasVideo = meta?.hasVideo ?: false

                val ix0 = it.x0
                val ix1 = it.x2

                val t1 = System.nanoTime()
                val budget = 100 * 1e6 // ms
                val hasBudgetLeft = (t1 - t0) < budget

                if (hasBudgetLeft && hasVideo) {

                    video!!
                    meta!!

                    val frameWidth = (h * (1f + border) * video.w / video.h).roundToInt()

                    val frameOffset = (-centralTime / (2f * dtHalfLength) * w).toInt() % frameWidth

                    val frameIndex0 = (ix0 - frameOffset) / frameWidth
                    val frameIndex1 = (ix1 - frameOffset) / frameWidth

                    if (frameIndex0 != frameIndex1) {

                        // split into multiple frames

                        // middle frames
                        for (frameIndex in frameIndex0 + 1 until frameIndex1) {
                            val x0 = frameWidth * frameIndex + frameOffset
                            keepFrameLoaded(x0, frameOffset, frameWidth, video, meta, 0f, 1f)
                        }

                        // first frame
                        val f0 = ((ix0 - frameOffset) % frameWidth).toFloat() / frameWidth
                        keepFrameLoaded(ix0, frameOffset, frameWidth, video, meta, f0, 1f)

                        // last frame
                        val x0 = frameIndex1 * frameWidth + frameOffset
                        val x2Mod = (ix1 - frameOffset) % frameWidth
                        val f1 = if (x2Mod == 0) 1f else x2Mod.toFloat() / frameWidth
                        keepFrameLoaded(x0, frameOffset, frameWidth, video, meta, 0f, f1)

                    } else {

                        val f0 = ((ix0 - frameOffset) % frameWidth).toFloat() / frameWidth
                        val x2Mod = (ix1 - frameOffset) % frameWidth
                        val f1 = if (x2Mod == 0) 1f else x2Mod.toFloat() / frameWidth
                        keepFrameLoaded(ix0, frameOffset, frameWidth, video, meta, f0, f1)

                    }
                }
            }
        }
    }

    private val border = 0.1f // 10% border for video

    fun draw(x0: Int, x1: Int, y: Int, h: Int, c0: Vector4f, c1: Vector4f) {
        drawRectGradient(x0, y, x1 - x0, h, c0, c1)
    }

    fun draw(
        x0: Int, x1: Int, y: Int, h: Int,
        c0: Vector4f, c1: Vector4f,
        frameOffset: Int, frameWidth: Int,
        video: Video, meta: FFMPEGMetadata,
        fract0: Float, fract1: Float
    ) {
        val f0 = fract0 * (1f + border) - border * 0.5f
        val f1 = fract1 * (1f + border) - border * 0.5f
        if (f1 <= 0f || f0 >= 1f) {
            drawRectGradient(x0, y, x1 - x0, h, c0, c1)
        } else {
            // get time at frameIndex
            val centerX = x0 - ((x0 - frameOffset + 10 * frameWidth) % frameWidth) + frameWidth / 2
            val timeAtX = centralTime + dtHalfLength * mix(-1.0, +1.0, (centerX - this.x0).toDouble() / this.w)
            val localTime = video.getLocalTimeFromRoot(timeAtX)
            // get frame at time
            //val t0 = System.nanoTime()
            val videoWidth = (frameWidth / (1f + border)).toInt()
            val frame = video.getFrameAt(localTime, videoWidth, meta)
            //val t1 = System.nanoTime()
            if (frame == null) {
                drawRectGradient(x0, y, x1 - x0, h, c0, c1)
            } else {
                // draw frame
                drawRectGradient(
                    x0, y, x1 - x0, h, c0, c1, frame,
                    Vector4f(f0, 0f, f1, 1f)
                )
            }
            // val t2 = System.nanoTime()
            // getting: 0.15ms - improved to > 0.07ms
            // drawing: 0.02ms
            // println("get ${(t1-t0)*1e-6} vs draw ${(t2-t1)*1e-6}")
        }
    }

    private fun keepFrameLoaded(
        x0: Int,
        frameOffset: Int,
        frameWidth: Int,
        video: Video, meta: FFMPEGMetadata,
        fract0: Float, fract1: Float
    ) {
        val f0 = fract0 * (1f + border) - border * 0.5f
        val f1 = fract1 * (1f + border) - border * 0.5f
        if (!(f1 <= 0f || f0 >= 1f)) {
            // get time at frameIndex
            val centerX = x0 - ((x0 - frameOffset + 10 * frameWidth) % frameWidth) + frameWidth / 2
            val timeAtX = centralTime + dtHalfLength * mix(-1.0, +1.0, (centerX - this.x0).toDouble() / this.w)
            val localTime = video.getLocalTimeFromRoot(timeAtX)
            // get frame at time
            video.getFrameAt(localTime, frameWidth, meta)
        }
    }

}