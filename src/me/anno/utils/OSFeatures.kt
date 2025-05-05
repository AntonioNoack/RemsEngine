package me.anno.utils

/**
 * Information about the environment we're shipped in.
 * */
object OSFeatures {

    /**
     * Whether sleeping is supported.
     * Desktop and Android support it, Web doesn't.
     * */
    var canSleep = true

    /**
     * Whether code can run concurrently.
     * Desktop and Android support it, Web and my C++ port don't.
     * */
    var hasMultiThreading = true

    /**
     * Can host TCP/UDP servers
     * */
    var canHostServers = true

    /**
     * Whether UDP is supported.
     * */
    var supportsNetworkUDP = true

    /**
     * Whether a log file may be easily created.
     * */
    var supportsContinuousLogFiles = true

    /**
     * Whether files are case-sensitive.
     * Windows isn't case-sensitive, Linux and Android are.
     *
     * Please implement your algorithms in a portable way, so always use the correct case to access files!
     * */
    var filesAreCaseSensitive = !OS.isWindows

    /**
     * Whether FFMPEG can be executed from the command line.
     * */
    var supportsFFMPEG = true

    /**
     * Whether RenderDoc may be called explicitly.
     * */
    var mayLoadRenderDocExplicitly = true

    /**
     * Whether accessing files typically takes hundreds of milliseconds.
     * This is the case for the web.
     * */
    var fileAccessIsHorriblySlow = false
}