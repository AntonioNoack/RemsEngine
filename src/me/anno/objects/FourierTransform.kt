package me.anno.objects

import me.anno.audio.AudioFXCache
import me.anno.audio.effects.Domain
import me.anno.audio.effects.Time
import me.anno.gpu.GFX.isFinalRendering
import me.anno.gpu.GFXx2D.getSize
import me.anno.gpu.GFXx2D.getSizeX
import me.anno.gpu.GFXx2D.getSizeY
import me.anno.io.FileReference
import me.anno.io.base.BaseWriter
import me.anno.objects.animation.Type
import me.anno.objects.modes.LoopingState
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.editor.SettingCategory
import me.anno.ui.style.Style
import me.anno.utils.Maths.clamp
import me.anno.utils.Maths.fract
import me.anno.utils.Maths.max
import me.anno.utils.Maths.mix
import me.anno.utils.files.LocalFile.toGlobalFile
import me.anno.video.FFMPEGMetadata
import me.anno.video.MissingFrameException
import org.joml.Matrix4fArrayList
import org.joml.Vector4fc
import kotlin.math.floor
import kotlin.math.sqrt

// todo option for gaussian smoothing
// option for sample rate
// todo option for logarithmic transform
// todo option for min and max index

class FourierTransform : Transform() {

    override fun getDefaultDisplayName(): String = "Fourier Transform"

    val meta get() = FFMPEGMetadata.getMeta(file, true)
    val forcedMeta get() = FFMPEGMetadata.getMeta(file, false)

    // todo why are soo few stripes displayed?
    // todo playing audio slowly and quickly is broken?

    // should be 60 fps, so sampleRate = 60 * bufferSize
    var sampleRate = 48000
    var bufferSize = 512

    // todo implement using half buffers...
    var enableHalfBuffers = true

    // todo support audio effects stack?
    var file = FileReference("")
    var getIndexParent: Transform? = null

    fun getIndexAndSize(): Int {

        var parent = parent
        if (parent == null) {
            lastWarning = "Missing parent"
            return -1
        }

        val parent0 = parent
        val targetParent = getIndexParent
        if (targetParent != null && targetParent !== parent) {
            while (parent != null) {
                val nextParent = parent.parent
                if (nextParent == targetParent) {
                    // found it :)
                    return getIndexAndSize(parent, nextParent)
                }
                parent = nextParent
            }
            lastWarning = "Index parent must be in direct line of inheritance"
        }

        return getIndexAndSize(this, parent0)

    }

    fun getIndexAndSize(child: Transform, parent: Transform): Int {
        val index = child.indexInParent
        val size = parent.drawnChildCount
        return getSize(kotlin.math.max(index, 0), size)
    }

    override fun onDraw(stack: Matrix4fArrayList, time: Double, color: Vector4fc) {

        lastWarning = null

        stack.next {

            // todo index = index in first parent with more than x children? we need references...
            // todo for that we need to drag transforms into other fields...

            val indexSize = getIndexAndSize()
            val index = getSizeX(indexSize)
            val size = getSizeY(indexSize)

            if (index < size) {
                // get the two nearest fourier transforms, and interpolate them
                val meta = forcedMeta
                if (meta != null) {
                    val bufferIndex = AudioFXCache.getIndex(time, bufferSize, sampleRate)
                    val bufferIndex0 = floor(bufferIndex).toLong()
                    val bufferTime0 = Time(AudioFXCache.getTime(bufferIndex0 - 1, bufferSize, sampleRate))
                    val bufferTime1 = Time(AudioFXCache.getTime(bufferIndex0 + 0, bufferSize, sampleRate))
                    val bufferTime2 = Time(AudioFXCache.getTime(bufferIndex0 + 1, bufferSize, sampleRate))
                    val buff0 = getBuffer(meta, bufferTime0, bufferTime1)
                    val buff1 = getBuffer(meta, bufferTime1, bufferTime2)
                    if (buff0 != null && buff1 != null) {

                        val relativeIndex0 = index.toFloat() / size
                        val relativeIndex1 = (index + 1f) / size
                        val bufferSize = buff0.first.size / 4 // half is mirrored, half is real/imaginary

                        var indexInBuffer0 = (relativeIndex0 * bufferSize).toInt()
                        var indexInBuffer1 = (relativeIndex1 * bufferSize).toInt()

                        if (indexInBuffer1 <= indexInBuffer0) indexInBuffer1 = indexInBuffer0 + 1
                        if (indexInBuffer0 >= bufferSize) indexInBuffer0 = bufferSize - 1
                        if (indexInBuffer1 > bufferSize) indexInBuffer1 = bufferSize

                        val fractIndex = fract(bufferIndex).toFloat()
                        val amplitude0 = getAmplitude(indexInBuffer0, indexInBuffer1, buff0)
                        val amplitude1 = getAmplitude(indexInBuffer0, indexInBuffer1, buff1)
                        val amplitude = mix(amplitude0, amplitude1, fractIndex)
                        val relativeAmplitude = amplitude / 32e3f
                        // todo interpolate all matching positions
                        stack.scale(1f, relativeAmplitude, 1f)
                    } else throw RuntimeException("null, why???")
                } else lastWarning = "Missing audio source"
            }

            super.onDraw(stack, time, color)

            drawChildren(stack, time, color)

            // todo change the transform based on the audio levels
            // todo multiplicative effect for children

            // todo choose left / right / average?
            // (for stereo music this may be really cool)

            // todo change rotation as well?
            // todo and translation :D

        }
    }

    fun getAmplitude(index0: Int, index1: Int, buffer: Pair<FloatArray, FloatArray>): Float {
        return (getAmplitude(index0, index1, buffer.first) + getAmplitude(index0, index1, buffer.second)) * 0.5f
    }

    fun getAmplitude(index0: Int, index1: Int, buffer: FloatArray): Float {
        var sum = 0f
        if (index1 <= index0) throw IllegalArgumentException()
        for (index in index0 until index1) {
            val real = buffer[index * 2]
            val imag = buffer[index * 2 + 1]
            sum += real * real + imag * imag
        }
        return sqrt(sum / (index1 - index0))
    }

    var loopingState = LoopingState.PLAY_LOOP
    // todo option to change the domain? may be nice, but otherwise, it's really fast...

    fun getKey(sampleIndex0: Long, half: Boolean): AudioFXCache.PipelineKey {
        val fraction = if (half) 0.5 else 0.0
        val t0 = Time(AudioFXCache.getTime(sampleIndex0 + 0, fraction, bufferSize, sampleRate))
        val t1 = Time(AudioFXCache.getTime(sampleIndex0 + 1, fraction, bufferSize, sampleRate))
        return getKey(t0, t1)
    }

    fun getKey(sampleTime0: Time, sampleTime1: Time): AudioFXCache.PipelineKey {
        return AudioFXCache.PipelineKey(
            file, sampleTime0, sampleTime1, bufferSize, false, "",
            loopingState, null
        )
    }

    fun getBuffer(
        meta: FFMPEGMetadata,
        sampleTime0: Time,
        sampleTime1: Time
    ): Pair<FloatArray, FloatArray>? {
        val data = AudioFXCache.getBuffer0(meta, getKey(sampleTime0, sampleTime1), false)
        if (isFinalRendering && data == null) throw MissingFrameException(file)
        if (data == null) return null
        return data.getBuffersOfDomain(Domain.FREQUENCY_DOMAIN)
    }

    override fun createInspector(
        list: PanelListY,
        style: Style,
        getGroup: (title: String, description: String, dictSubPath: String) -> SettingCategory
    ) {
        super.createInspector(list, style, getGroup)
        val fourier = getGroup("Fourier Transform", "", "")
        fourier.add(vi("Audio File", "", null, file, style) { file = it })
        fourier.add(
            vi(
                "Sample Rate",
                "What the highest frequency should be. Higher frequencies may be reflected",
                sampleRateType,
                sampleRate,
                style
            ) { sampleRate = max(64, it) })
        fourier.add(
            vi(
                "Buffer Size",
                "Should be at least twice the buffer size",
                bufferSizeType,
                bufferSize,
                style
            ) { bufferSize = max(64, it) })
    }

    override fun drawChildrenAutomatically(): Boolean = false

    override fun save(writer: BaseWriter) {
        super.save(writer)
        writer.writeFile("file", file)
        writer.writeInt("sampleRate", sampleRate)
        writer.writeInt("bufferSize", bufferSize)
    }

    override fun readString(name: String, value: String) {
        when (name) {
            "file" -> file = value.toGlobalFile()
            else -> super.readString(name, value)
        }
    }

    override fun readInt(name: String, value: Int) {
        when (name) {
            "sampleRate" -> sampleRate = max(64, value)
            "bufferSize" -> bufferSize = max(64, value)
            else -> super.readInt(name, value)
        }
    }

    override fun getClassName(): String = "FourierTransform"

    companion object {
        val sampleRateType =
            Type(24000, 1, 1024f, false, true, { clamp(it.toString().toDouble().toInt(), 64, 96000) }, { it })
        val bufferSizeType =
            Type(512, 1, 512f, false, true, { clamp(it.toString().toDouble().toInt(), 64, 65536) }, { it })
    }

}