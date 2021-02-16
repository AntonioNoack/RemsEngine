package audacity.soundtouch

import org.apache.logging.log4j.LogManager
import kotlin.math.cos

fun main() {

    val logger = LogManager.getLogger("Test TDStretch")

    val tempo = 5f

    val stretch = TimeDomainStretch()
    stretch.setChannels(1)
    stretch.setTempo(tempo)

    logger.info("required: ${stretch.sampleReq}")

    val size = 9000
    val input = FloatArray(size) {
        cos(it * 0.1f * 3.1416f)
    }

    val backend = stretch.outputBuffer.backend
    for (i in 1 until 100) {
        stretch.putSamples(input)
        logger.info("${size * i} -> ${backend.size}, ${(backend.size * tempo) / (size * i)}, ${(backend.size * tempo + stretch.sampleReq) / (size * i)}")
        // stretch.outputBuffer.clear()
    }

}