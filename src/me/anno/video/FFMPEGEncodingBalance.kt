package me.anno.video

import me.anno.language.translation.NameDesc
import kotlin.math.abs

/**
 * https://trac.ffmpeg.org/wiki/Encode/H.264#FAQ
 * */
enum class FFMPEGEncodingBalance(val nameDesc: NameDesc, val internalName: String, val value: Float) {
    F4(NameDesc("Fastest"), "ultrafast", 0f),
    F3(NameDesc("Even Faster²"), "superfast", 0.1f),
    F2(NameDesc("Even Faster"), "veryfast", 0.2f),
    F1(NameDesc("Faster"), "faster", 0.3f),
    F0(NameDesc("Fast"), "fast", 0.4f),
    M0(NameDesc("Medium"), "medium", 0.5f),
    S1(NameDesc("Small"), "slow", 0.667f),
    S2(NameDesc("Smaller"), "slower", 0.833f),
    S3(NameDesc("Smallest"), "veryslow", 1f);
    companion object {
        operator fun get(value: Float) = values().minBy { abs(value - it.value) }!!
    }
}