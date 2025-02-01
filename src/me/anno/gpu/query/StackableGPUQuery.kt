package me.anno.gpu.query

import me.anno.cache.ICacheData
import me.anno.utils.InternalAPI

abstract class StackableGPUQuery(
    private val sharedData: StackableQueryData,
    everyNthFrame: Int = 16
) : QueryBase(), ICacheData {

    private val timer0 = SimpleGPUQuery(sharedData.target, everyNthFrame)
    private val timer1 = SimpleGPUQuery(sharedData.target, everyNthFrame)

    fun start() {
        sharedData.stopLastTimer()
        currResult = 0L
        sharedData.startTimer(timer0)
        sharedData.currentlyActive.add(this)
    }

    fun stop() {
        sharedData.stopLastTimer()
        addSample(currResult)
        sharedData.currentlyActive.remove(this)
        if (sharedData.currentlyActive.isNotEmpty()) {
            sharedData.startTimer(timer1)
        }
    }

    @InternalAPI
    var currResult = 0L

    var frameCounter: Int
        get() = timer0.frameCounter
        set(value) {
            timer0.frameCounter = value
        }

    override fun destroy() {
        super.destroy()
        timer0.destroy()
        timer1.destroy()
    }
}