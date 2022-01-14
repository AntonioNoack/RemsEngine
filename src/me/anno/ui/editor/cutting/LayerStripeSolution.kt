package me.anno.ui.editor.cutting

import me.anno.audio.AudioFXCache
import me.anno.audio.AudioFXCache.SPLITS
import me.anno.audio.effects.SoundPipeline.Companion.bufferSize
import me.anno.config.DefaultStyle.black
import me.anno.gpu.drawing.DrawGradients.drawRectGradient
import me.anno.gpu.drawing.DrawRectangles
import me.anno.gpu.drawing.DrawStriped.drawRectStriped
import me.anno.objects.Transform
import me.anno.objects.Video
import me.anno.objects.modes.LoopingState
import me.anno.studio.rems.RemsStudio.nullCamera
import me.anno.ui.editor.TimelinePanel.Companion.centralTime
import me.anno.ui.editor.TimelinePanel.Companion.dtHalfLength
import me.anno.ui.editor.cutting.LayerView.Companion.maxLines
import me.anno.maths.Maths.clamp
import me.anno.maths.Maths.max
import me.anno.maths.Maths.mixARGB
import me.anno.maths.Maths.nonNegativeModulo
import me.anno.video.FFMPEGMetadata
import org.joml.Vector4f
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToInt

class LayerStripeSolution(
    val x0: Int,
    var y0: Int,
    val x1: Int,
    var y1: Int,
    private val referenceTime: Double
) {

    private val stripeStride = 5

    val w = x1 - x0
    val lines = Array(maxLines) {
        ArrayList<Gradient>(w / 2)
    }

    private val relativeVideoBorder = 0.1f
    private val stripeColorSelected = 0x33ffffff
    private val stripeColorWarning = 0x33ffff77
    private val stripeColorError = 0xffff7777.toInt()

    // draw a stripe of the current image, or a symbol or sth...
    // done shader for the stripes (less draw calls)
    // if video, draw a few frames in small
    // if audio, draw audio levels

    fun draw(selectedTransform: Transform?, draggedTransform: Transform?) {
        iteratorOverGradients(selectedTransform, draggedTransform, true, ::drawStripes, ::drawGradient, ::drawVideo)
    }

    fun keepResourcesLoaded() {
        iteratorOverGradients(null, null, false, { _, _, _, _, _, _ -> }, { _, _, _, _, _, _ -> }, ::keepFrameLoaded)
    }

    private fun iteratorOverGradients(
        selectedTransform: Transform?, draggedTransform: Transform?,
        drawAudio: Boolean,
        drawStripes: (x0: Int, x1: Int, y: Int, h: Int, offset: Int, color: Int) -> Unit,
        drawGradient: (x0: Int, x1: Int, y: Int, h: Int, c0: Int, c1: Int) -> Unit,
        drawVideo: (
            x0: Int, x1: Int, y: Int, h: Int,
            c0: Int, c1: Int,
            frameOffset: Int, frameWidth: Int,
            video: Video, meta: FFMPEGMetadata,
            fract0: Float, fract1: Float
        ) -> Unit
    ) {

        val y = y0
        val h = y1 - y0

        val xTimeCorrection = ((referenceTime - centralTime) * w / (dtHalfLength * 2)).roundToInt()

        val metas = HashMap<Any, Any>()

        val timeOffset = (-centralTime / (2f * dtHalfLength) * w).toInt()

        for ((lineIndex, gradients) in lines.withIndex()) {

            val y0 = y + 3 + lineIndex * 3
            val h0 = h - 10

            for (gradient in gradients) {

                val tr = gradient.owner as? Transform
                val isStriped = selectedTransform === tr || draggedTransform === tr

                val video = tr as? Video
                val meta = if (video == null) null else metas.getOrPut(video) { video.meta ?: Unit } as? FFMPEGMetadata

                val hasAudio = meta?.hasAudio ?: false
                val hasVideo = meta?.hasVideo ?: false

                val c0 = gradient.c0
                val c1 = gradient.c1

                // bind gradients to edge, because often the stripe continues
                val ix0 = if (gradient.x0 == x0) x0 else gradient.x0 + xTimeCorrection
                val ix1 = if (gradient.x1 + 1 >= x1) x1 else gradient.x1 + xTimeCorrection + 1

                if (hasVideo) {

                    video!!
                    meta!!

                    val frameWidth = (h * (1f + relativeVideoBorder) * meta.videoWidth / meta.videoHeight).roundToInt()

                    var frameOffset = timeOffset % frameWidth
                    if (frameOffset < 0) frameOffset += frameWidth

                    val frameIndex0 = floor((ix0 - frameOffset).toFloat() / frameWidth).toInt()
                    val frameIndex1 = floor((ix1 - frameOffset).toFloat() / frameWidth).toInt()

                    fun getFraction(x: Int, allow0: Boolean): Float {
                        var lx = (x + frameWidth - frameOffset) % frameWidth
                        if (lx > frameWidth) lx -= frameWidth
                        if (lx < 0) lx += frameWidth
                        if (lx == 0 && !allow0) return 1f
                        return lx.toFloat() / frameWidth
                    }

                    fun getLerpedColor(x: Int) =
                        mixARGB(c0, c1, (x - ix0).toFloat() / gradient.w)

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

                } else {
                    drawGradient(ix0, ix1, y0, h0, c0, c1)
                }

                if (hasAudio) {

                    val audio = video!!

                    // todo get auto levels for pixels, which equal ranges of audio frames -> min, max, avg?, with weight?
                    val identifier = audio.toString()

                    // todo get used camera instead of nullCamera
                    val camera = nullCamera!!

                    val color = if (hasVideo) 0xaa777777.toInt() else 0xff777777.toInt()
                    val mix = 0.5f
                    val fineColor = mixARGB(color, 0x77ff77, mix) or black
                    val okColor = mixARGB(color, 0xffff77, mix) or black
                    val criticalColor = mixARGB(color, 0xff7777, mix) or black

                    val offset = (if (hasVideo) 0.75f else 0.5f) * h
                    val scale = if (hasVideo) h / 128e3f else h / 65e3f

                    val tStart = getTimeAt(ix0)
                    val dt = getTimeAt(ix0 + SPLITS) - tStart

                    val timeStartIndex = floor(tStart / dt).toLong()
                    val timeEndIndex = ceil(getTimeAt(ix1) / dt).toLong()

                    for (timeIndex in timeStartIndex until timeEndIndex) {

                        val t0 = timeIndex * dt
                        val t1 = (timeIndex + 1) * dt
                        val xi = getXAt(t0).roundToInt()

                        // get min, max, avg, of audio at this time point
                        // time0: Time,
                        // time1: Time,
                        // speed: Double,
                        // domain: Domain,
                        // async: Boolean
                        val range = AudioFXCache.getRange(bufferSize, t0, t1, identifier, audio, camera)
                        if (range != null && drawAudio) {
                            for (dx in 0 until SPLITS) {
                                val x = xi + dx
                                if (x in ix0 until ix1) {
                                    val minV = range[dx * 2 + 0]
                                    val maxV = range[dx * 2 + 1]
                                    val amplitude = max(abs(minV.toInt()), abs(maxV.toInt()))
                                    val colorMask = when {
                                        amplitude < 5000 -> black
                                        amplitude < 28000 -> fineColor
                                        amplitude < 32000 -> okColor
                                        else -> criticalColor
                                    }
                                    val min = minV * scale + offset
                                    val max = maxV * scale + offset
                                    if (max >= min) {
                                        val y01 = this.y0 + min.toInt()
                                        val y11 = this.y0 + max.toInt()
                                        DrawRectangles.drawRect(x, y01, 1, y11 + 1 - y01, colorMask)
                                    }
                                }
                            }
                        }

                    }
                }

                val hasError = tr?.lastWarning != null
                if (isStriped || hasError) {
                    // check if the video element has an error
                    // if so, add red stripes
                    val color = if (hasError) stripeColorError else stripeColorSelected
                    drawStripes(ix0, ix1, y0, h0, timeOffset, color)
                }

            }
        }
    }

    private fun drawGradient(x0: Int, x1: Int, y: Int, h: Int, c0: Int, c1: Int) {
        drawRectGradient(x0, y, x1 - x0, h, c0, c1)
    }

    private fun drawStripes(x0: Int, x1: Int, y: Int, h: Int, offset: Int, color: Int) {
        drawRectStriped(x0, y, x1 - x0, h, offset, stripeStride, color)
    }

    private fun getTimeAt(x: Int) = centralTime + dtHalfLength * ((x - x0).toDouble() / w * 2.0 - 1.0)
    private fun getXAt(time: Double) = (time - centralTime) / dtHalfLength * 0.5 * w + (x0 + w / 2)

    private fun clampTime(localTime: Double, video: Video): Double {
        return if (video.isLooping.value == LoopingState.PLAY_ONCE) clamp(localTime, 0.0, video.lastDuration)
        else localTime
    }

    private fun getCenterX(x0: Int, frameOffset: Int, frameWidth: Int) =
        x0 - nonNegativeModulo(x0 - frameOffset, frameWidth) + frameWidth / 2

    // todo bug: it's invisible, if video is not loaded
    // todo bug: when clicked, it changes the frames shortly

    private fun drawVideo(
        x0: Int, x1: Int, y: Int, h: Int,
        c0: Int, c1: Int,
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
            val localTime = clampTime(video.getLocalTimeFromRoot(timeAtX, false), video)
            // get frame at time
            val videoWidth = (frameWidth / (1f + relativeVideoBorder)).toInt()
            val frame = video.getFrameAtLocalTime(localTime, videoWidth, meta)
            if (frame == null || !frame.isCreated) {
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

    @Suppress("UNUSED_PARAMETER")
    private fun keepFrameLoaded(
        x0: Int, x1: Int, y: Int, h: Int,
        c0: Int, c1: Int,
        frameOffset: Int, frameWidth: Int,
        video: Video, meta: FFMPEGMetadata,
        fract0: Float, fract1: Float
    ) {
        val f0 = fract0 * (1f + relativeVideoBorder) - relativeVideoBorder * 0.5f
        val f1 = fract1 * (1f + relativeVideoBorder) - relativeVideoBorder * 0.5f
        if (f1 > 0f && f0 < 1f) {
            // get time at frameIndex
            val centerX = getCenterX(x0, frameOffset, frameWidth)
            val timeAtX = getTimeAt(centerX)
            val localTime = clampTime(video.getLocalTimeFromRoot(timeAtX, false), video)
            // get frame at time
            val videoWidth = (frameWidth / (1f + relativeVideoBorder)).toInt()
            video.getFrameAtLocalTime(localTime, videoWidth, meta)
        }
    }

}