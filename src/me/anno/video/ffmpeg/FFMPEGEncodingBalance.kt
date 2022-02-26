package me.anno.video.ffmpeg

import me.anno.language.translation.NameDesc
import kotlin.math.abs

/**
 * https://trac.ffmpeg.org/wiki/Encode/H.264#FAQ
 * */
enum class FFMPEGEncodingBalance(val nameDesc: NameDesc, val internalName: String, val value: Float) {

    F4(NameDesc("Fastest", "Worst", "encoding.speed.f4"), "ultrafast", 0f),
    F3(NameDesc("Even Faster²", "Even worse²", "encoding.speed.f3"), "superfast", 0.1f),
    F2(NameDesc("Even Faster", "Even worse", "encoding.speed.f2"), "veryfast", 0.2f),
    F1(NameDesc("Faster", "Worse", "encoding.speed.f1"), "faster", 0.3f),
    F0(NameDesc("Fast", "Slightly bad maybe", "encoding.speed.f0"), "fast", 0.4f),
    M0(NameDesc("Medium", "Medium", "encoding.speed.m0"), "medium", 0.5f),
    S1(NameDesc("Small", "Good", "encoding.speed.s1"), "slow", 0.667f),
    S2(NameDesc("Smaller", "Better", "encoding.speed.s2"), "slower", 0.833f),
    S3(NameDesc("Smallest", "Best", "encoding.speed.s3"), "veryslow", 1f);

    companion object {
        operator fun get(value: Float) = values().minByOrNull { abs(value - it.value) }!!
    }
}