package audacity.soundtouch

import kotlin.math.cos

fun main() {

    val tempo = 5f

    val stretch = TimeDomainStretch()
    stretch.setChannels(1)
    stretch.setTempo(tempo)

    println("required: ${stretch.sampleReq}")

    val size = 9000
    val input = FloatArray(size) {
        cos(it * 0.1f * 3.1416f)
    }

    val backend = stretch.outputBuffer.backend
    for(i in 1 until 100){
        stretch.putSamples(input)
        println("${size*i} -> ${backend.size}, ${(backend.size*tempo)/(size*i)}, ${(backend.size*tempo + stretch.sampleReq)/(size*i)}")
        // stretch.outputBuffer.clear()
    }

}