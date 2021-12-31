package audacity.soundtouch

import me.anno.utils.structures.arrays.FloatArrayList
import kotlin.math.min

/**
 * https://github.com/audacity/audacity/blob/cce2c7b8830a7bb651d225863b792d23f336323f/lib-src/soundtouch/source/SoundTouch/FIFOSampleBuffer.cpp
 * */
class SampleBuffer {

    private var channels = 0
    private var samplesInBuffer = 0

    private var bufferPos = 0

    val backend = FloatArrayList(1024)

    fun ptrBegin() = FloatPtrArrList(backend, bufferPos * channels)

    fun ptrEnd(@Suppress("UNUSED_PARAMETER") slackCapacity: Int): FloatPtr {
        // ensureCapacity(samplesInBuffer + slackCapacity)
        return FloatPtrArrList(backend, samplesInBuffer * channels)
    }

    /*fun ensureCapacity(samples: Int){
        // managed automatically
    }*/

    fun numSamples() = samplesInBuffer

    fun putSamples(samples: Int) {
        // ensureCapacity(samplesInBuffer + samples)
        samplesInBuffer += samples
    }

    fun putSamples(ptr: FloatArray) {
        val samples = ptr.size / channels
        val values = samples * channels
        for (i in 0 until values) {
            backend += ptr[i]
        }
        samplesInBuffer += samples
    }

    fun putSamples(ptr: FloatArray, samples: Int) {
        val values = samples * channels
        for (i in 0 until values) {
            backend += ptr[i]
        }
        samplesInBuffer += samples
    }

    fun putSamples(ptr: FloatPtr, samples: Int) {
        val values = samples * channels
        for (i in 0 until values) {
            backend += ptr[i]
        }
        samplesInBuffer += samples
    }

    fun setChannels(c: Int) {
        channels = c
    }

    fun clear() {
        samplesInBuffer = 0
        bufferPos = 0
    }

    fun receiveSamples(output: FloatPtr, maxSamples: Int): Int {
        val num = min(maxSamples, samplesInBuffer)
        val ptrBegin = ptrBegin()
        for (i in 0 until channels * num) {
            output[i] = ptrBegin[i]
        }
        return receiveSamples(num)
    }

    fun receiveSamples(maxSamples: Int): Int {
        if (maxSamples >= samplesInBuffer) {
            val tmp = samplesInBuffer
            samplesInBuffer = 0
            return tmp
        }

        samplesInBuffer -= maxSamples
        bufferPos += maxSamples

        return maxSamples
    }

    fun isEmpty() = samplesInBuffer == 0

}