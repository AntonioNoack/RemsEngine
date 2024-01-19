package me.anno.tests.audio

import me.anno.utils.OS.downloads
import me.anno.utils.Sleep
import me.anno.utils.hpc.HeavyProcessing
import me.anno.utils.types.Floats.f3
import me.anno.utils.types.Strings.formatTime
import me.anno.io.MediaMetadata.Companion.getMeta
import me.anno.video.ffmpeg.FFMPEGStream
import java.nio.ShortBuffer
import kotlin.math.abs
import kotlin.math.pow

// a try to find all GTA V deaths... works less than ideal...
fun main() {

    val sourceFile = downloads.getChild("gtav_mirrorworld.m4a")
    val findStart = 16 * 60 + 53.0
    val findEnd = 16 * 60 + 56.0
    val meta = getMeta(sourceFile, false)!!

    val sampleRate = 16000

    fun getData(start: Double, duration: Double): ShortBuffer {
        val sequence = FFMPEGStream.getAudioSequence(sourceFile, start, duration, sampleRate)
        return Sleep.waitUntilDefined(true) { sequence.value?.data }
    }

    val sourceData = getData(0.0, meta.audioDuration)
    val findStartIndex = (findStart * sampleRate).toInt() * 2
    val findEndIndex = (findEnd * sampleRate).toInt() * 2
    val findLength = (findEndIndex - findStartIndex) / 2

    val findData = ShortArray(findLength)
    for (i in 0 until findLength) {
        findData[i] = sourceData[findStartIndex + i.shl(1)]
    }

    fun cross(i: Int): Double {
        var cross = 0L
        var input = findLength * 4 * 256L // input normalization
        for (j in 0 until findLength) {
            val s0 = sourceData[i + j + j]
            cross += s0 * findData[j]
            input += s0 * s0
        }
        return cross.toDouble() / input.toDouble()
    }

    val cross0 = cross(findStartIndex)

    println("starting search, zero: $cross0")

    val factor = 0.5.pow(1.0 / findLength)
    println("factor: $factor")

    // calculate correlation and find maxima :)
    HeavyProcessing.processBalanced(
        0, sourceData.capacity().shr(1) - findLength, false,
    ) { i0, i1 ->
        var max = cross0
        val min = cross0 * 0.3
        var recentMax = 0.0
        var recentMaxTime = 0
        for (k in i0 until i1) {
            val i = k + k
            val cross = abs(cross(i))
            if (cross > max && cross > min) {
                recentMax = cross
                recentMaxTime = i
                max = cross
            } else if (recentMax > 0.0 && i < recentMaxTime + findLength) {
                // wait for higher maximum
            } else if (recentMax > 0.0) {
                println("max ${(recentMax / cross0).f3()} at ${(recentMaxTime / (2.0 * sampleRate)).formatTime(0)}")
                recentMax = 0.0
            } else {
                max *= factor
            }
        }
        if (recentMax > 0.0) {
            println("max ${(recentMax / cross0).f3()} at ${(recentMaxTime / (2.0 * sampleRate)).formatTime(0)}")
        }
    }


}