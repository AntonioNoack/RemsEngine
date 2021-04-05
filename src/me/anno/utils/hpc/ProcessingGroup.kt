package me.anno.utils.hpc

import kotlin.math.max
import kotlin.math.roundToInt

class ProcessingGroup(name: String, val threads: Int) : ProcessingQueue(name) {

    constructor(name: String, threadFraction: Float) : this(
        name,
        max(1, (HeavyProcessing.threads * threadFraction).roundToInt())
    )

    private var hasBeenStarted = false
    override fun start(name: String, force: Boolean) {
        if (hasBeenStarted && !force) return
        hasBeenStarted = true
        for (index in 0 until threads) {
            super.start("$name-$index", true)
        }
    }

}