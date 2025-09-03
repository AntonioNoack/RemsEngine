package me.anno.audio

import me.anno.Time
import me.anno.cache.ICacheData

class AudioData(
    var timeLeft: ShortArray,
    var timeRight: ShortArray
) : ICacheData {

    operator fun component1() = timeLeft
    operator fun component2() = timeRight

    var isDestroyed = 0L
    override fun destroy() {
        // LOGGER.info("Destroying ${hashCode()} $key")
        // printStackTrace()
        // GFX.checkIsGFXThread()
        // todo why is it being destroyed twice????
        /*if (isDestroyed > 0L){
            Engine.shutdown()
            throw IllegalStateException("Cannot destroy twice, now $gameTime, then: $isDestroyed!")
        }*/
        isDestroyed = Time.nanoTime
        /*FAPool.returnBuffer(timeLeft)
        FAPool.returnBuffer(freqLeft)
        FAPool.returnBuffer(timeRight)
        FAPool.returnBuffer(freqRight)*/
    }
}