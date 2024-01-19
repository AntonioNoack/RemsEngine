package me.anno.video.ffmpeg

import me.anno.language.translation.NameDesc

/**
 * https://trac.ffmpeg.org/wiki/Encode/H.264#FAQ
 * */
enum class FFMPEGEncodingType(val id: Int, val nameDesc: NameDesc, val internalName: String?) {
    DEFAULT(0, NameDesc("Other", "Default", "ffmpeg.flags.none"), null),
    FILM(1, NameDesc("Film", "Use for high quality movie content; lowers deblocking", "ffmpeg.flags.film"), "film"),
    ANIMATION(
        2, NameDesc(
            "Animation",
            "Good for cartoons; uses higher deblocking and more reference frames",
            "ffmpeg.flags.animation"
        ), "animation"
    ),
    GRAIN(
        3, NameDesc(
            "Grain",
            "Preserves the grain structure in old, grainy film material",
            "ffmpeg.flags.grain"
        ), "grain"
    ),
    STILL_IMAGE(4, NameDesc("Still Image", "Good for slideshow-like content", "ffmpeg.flags.stillImage"), "stillimage"),
    // FAST_DECODE(5,NameDesc("Fast Decode", "Allows faster decoding by disabling certain filters", "ffmpeg.flags.fastDecode"), "fastdecode"), // it's encoding flags -> irrelevant
    ZERO_LATENCY(
        6, NameDesc(
            "Low Latency",
            "Good for fast encoding and low-latency streaming",
            "ffmpeg.flags.zeroLatency"
        ), "zerolatency"
    );

    companion object {
        operator fun get(id: Int) = entries.firstOrNull { it.id == id } ?: DEFAULT
    }
}