package me.anno.audio.remfx

class RemFXContext {

    var amplitudeL0 = 0f
    var amplitudeR0 = 0f

    var deltaAmplitudeL = 0f
    var deltaAmplitudeR = 0f

    var bufferSize = 1024
    var sampleRate = 48000
    var sampleOffset = 0L
    var bufferOffset = 0

    val indices get() = bufferOffset until bufferOffset + bufferSize

}