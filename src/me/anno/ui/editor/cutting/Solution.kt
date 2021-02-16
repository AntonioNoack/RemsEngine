package me.anno.ui.editor.cutting

import me.anno.gpu.GFXx2D.drawRectGradient
import me.anno.gpu.GFXx2D.drawRectStriped
import me.anno.objects.Transform
import me.anno.objects.Video
import me.anno.objects.modes.LoopingState
import me.anno.ui.editor.TimelinePanel.Companion.centralTime
import me.anno.ui.editor.TimelinePanel.Companion.dtHalfLength
import me.anno.ui.editor.cutting.LayerView.Companion.maxLines
import me.anno.utils.Maths.clamp
import me.anno.utils.Maths.mix
import me.anno.utils.Maths.nonNegativeModulo
import me.anno.video.FFMPEGMetadata
import org.joml.Vector4f
import org.joml.Vector4fc
import kotlin.math.floor
import kotlin.math.roundToInt

class Solution(
    val x0: Int, val y0: Int, val x1: Int, val y1: Int,
    private val referenceTime: Double
) {

    val stripeStride = 5

    val w = x1 - x0
    val lines = Array(maxLines) {
        ArrayList<Gradient>(w / 2)
    }

    private val relativeVideoBorder = 0.1f
    private val stripeColor = Vector4f(1f, 1f, 1f, 0.2f)

    // draw a stripe of the current image, or a symbol or sth...
    // done shader for the stripes (less draw calls)
    // todo if audio, draw audio levels
    // if video, draw a few frames in small

    fun draw(selectedTransform: Transform?, draggedTransform: Transform?) {
        iteratorOverGradients(selectedTransform, draggedTransform, ::drawStripes, ::drawGradient, ::drawVideo)
    }

    fun iteratorOverGradients(
        selectedTransform: Transform?, draggedTransform: Transform?,
        drawStripes: (x0: Int, x1: Int, y: Int, h: Int, offset: Int) -> Unit,
        drawGradient: (x0: Int, x1: Int, y: Int, h: Int, c0: Vector4fc, c1: Vector4fc) -> Unit,
        drawVideo: (
            x0: Int, x1: Int, y: Int, h: Int,
            c0: Vector4fc, c1: Vector4fc,
            frameOffset: Int, frameWidth: Int,
            video: Video, meta: FFMPEGMetadata,
            fract0: Float, fract1: Float
        ) -> Unit
    ) {

        val y = y0
        val h = y1 - y0

        val deltaX = ((referenceTime - centralTime) * w / (dtHalfLength * 2)).roundToInt()

        val metas = HashMap<Any, Any>()

        val timeOffset = (-centralTime / (2f * dtHalfLength) * w).toInt()

        for ((lineIndex, gradients) in lines.withIndex()) {

            val y0 = y + 3 + lineIndex * 3
            val h0 = h - 10

            for (gradient in gradients) {

                val tr = gradient.owner as? Transform
                val isStriped = selectedTransform === tr || draggedTransform === tr

                val video = tr as? Video
                val meta = if (video == null) null
                else metas.getOrPut(video) { video.meta ?: Unit } as? FFMPEGMetadata

                val hasAudio = meta?.hasAudio ?: false
                val hasVideo = meta?.hasVideo ?: false

                val c0 = gradient.c0
                val c1 = gradient.c1

                val ix0 = gradient.x0 + deltaX
                val ix1 = gradient.x1 + deltaX + 1

                if (hasVideo) {

                    video!!
                    meta!!

                    val frameWidth = (h * (1f + relativeVideoBorder) * video.w / video.h).roundToInt()

                    val frameOffset =
                        (timeOffset % frameWidth + frameWidth) % frameWidth

                    val frameIndex0 = floor((ix0 - frameOffset).toFloat() / frameWidth).toInt()
                    val frameIndex1 = floor((ix1 - frameOffset).toFloat() / frameWidth).toInt()

                    fun getFraction(x: Int, allow0: Boolean): Float {
                        var lx = (x + frameWidth - frameOffset) % frameWidth
                        if (lx > frameWidth) lx -= frameWidth
                        if (lx < 0) lx += frameWidth
                        if (lx == 0 && !allow0) return 1f
                        return lx.toFloat() / frameWidth
                    }

                    fun getLerpedColor(x: Int) = mix(c0, c1, (x - ix0).toFloat() / gradient.w)

                    if (frameIndex0 != frameIndex1) {
                        // split into multiple frames

                        // middle frames
                        for (frameIndex in frameIndex0 + 1 until frameIndex1) {
                            val x0 = frameWidth * frameIndex + frameOffset
                            val x1 = x0 + frameWidth
                            val c0x = getLerpedColor(x0)
                            val c1x = getLerpedColor(x1)
                            drawVideo(x0, x1, y0, h0, c0x, c1x, frameOffset, frameWidth, video, meta, 0f, 1f)
                        }

                        // first frame
                        val x1 = (frameIndex0 + 1) * frameWidth + frameOffset
                        if (x1 > ix0) {
                            val f0 = getFraction(ix0, true)
                            val lerpedC1 = getLerpedColor(x1 - 1)
                            drawVideo(ix0, x1, y0, h0, c0, lerpedC1, frameOffset, frameWidth, video, meta, f0, 1f)
                        }

                        // last frame
                        val x0 = frameIndex1 * frameWidth + frameOffset
                        if (x0 < ix1) {
                            val lerpedC0 = getLerpedColor(x0)
                            val f1 = getFraction(ix1, false)
                            drawVideo(x0, ix1, y0, h0, lerpedC0, c1, frameOffset, frameWidth, video, meta, 0f, f1)
                        }

                    } else {

                        val f0 = getFraction(ix0, true)
                        val f1 = getFraction(ix1, false)
                        drawVideo(ix0, ix1, y0, h0, c0, c1, frameOffset, frameWidth, video, meta, f0, f1)

                    }

                } else drawGradient(ix0, ix1, y0, h0, c0, c1)

                if (isStriped) {
                    drawStripes(ix0, ix1, y0, h0, timeOffset)
                }

            }
        }
    }

    fun keepResourcesLoaded() {
        iteratorOverGradients(null, null, { _, _, _, _, _ -> }, { _, _, _, _, _, _ -> }, ::keepFrameLoaded)
    }

    private fun drawGradient(x0: Int, x1: Int, y: Int, h: Int, c0: Vector4fc, c1: Vector4fc) {
        drawRectGradient(x0, y, x1 - x0, h, c0, c1)
    }

    private fun drawStripes(x0: Int, x1: Int, y: Int, h: Int, offset: Int) {
        drawRectStriped(x0, y, x1 - x0, h, offset, stripeStride, stripeColor)
    }

    private fun getTimeAt(x: Int) = centralTime + dtHalfLength * mix(-1.0, +1.0, (x - this.x0).toDouble() / this.w)

    private fun clampTime(localTime: Double, video: Video): Double {
        return if (video.isLooping.value == LoopingState.PLAY_ONCE) clamp(localTime, 0.0, video.lastDuration)
        else localTime
    }

    private fun getCenterX(x0: Int, frameOffset: Int, frameWidth: Int) =
        x0 - nonNegativeModulo(x0 - frameOffset, frameWidth) + frameWidth / 2

    private fun drawVideo(
        x0: Int, x1: Int, y: Int, h: Int,
        c0: Vector4fc, c1: Vector4fc,
        frameOffset: Int, frameWidth: Int,
        video: Video, meta: FFMPEGMetadata,
        fract0: Float, fract1: Float
    ) {
        val f0 = fract0 * (1f + relativeVideoBorder) - relativeVideoBorder * 0.5f
        val f1 = fract1 * (1f + relativeVideoBorder) - relativeVideoBorder * 0.5f
        if (f1 <= 0f || f0 >= 1f) {
            drawRectGradient(x0, y, x1 - x0, h, c0, c1)
        } else {
            // get time at frameIndex
            val centerX = getCenterX(x0, frameOffset, frameWidth)
            val timeAtX = getTimeAt(centerX)
            val localTime = clampTime(video.getLocalTimeFromRoot(timeAtX), video)
            // get frame at time
            val videoWidth = (frameWidth / (1f + relativeVideoBorder)).toInt()
            val frame = video.getFrameAtLocalTime(localTime, videoWidth, meta)
            if (frame == null) {
                drawRectGradient(x0, y, x1 - x0, h, c0, c1)
            } else {
                // draw frame
                drawRectGradient(
                    x0, y, x1 - x0, h, c0, c1, frame,
                    Vector4f(f0, 0f, f1, 1f)
                )
            }
        }
    }

    private fun keepFrameLoaded(
        x0: Int, x1: Int, y: Int, h: Int,
        c0: Vector4fc, c1: Vector4fc,
        frameOffset: Int, frameWidth: Int,
        video: Video, meta: FFMPEGMetadata,
        fract0: Float, fract1: Float
    ) {
        val f0 = fract0 * (1f + relativeVideoBorder) - relativeVideoBorder * 0.5f
        val f1 = fract1 * (1f + relativeVideoBorder) - relativeVideoBorder * 0.5f
        if (!(f1 <= 0f || f0 >= 1f)) {
            // get time at frameIndex
            val centerX = getCenterX(x0, frameOffset, frameWidth)
            val timeAtX = getTimeAt(centerX)
            val localTime = clampTime(video.getLocalTimeFromRoot(timeAtX), video)
            // get frame at time
            val videoWidth = (frameWidth / (1f + relativeVideoBorder)).toInt()
            video.getFrameAtLocalTime(localTime, videoWidth, meta)
        }
    }

}