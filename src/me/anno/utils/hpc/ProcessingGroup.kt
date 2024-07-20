package me.anno.utils.hpc

import me.anno.utils.types.Floats.roundToIntOr
import kotlin.math.max
import kotlin.math.roundToInt

class ProcessingGroup(name: String, numThreads: Int) : ProcessingQueue(name, numThreads) {

    constructor(name: String, threadFraction: Float) : this(
        name,
        max(1, (HeavyProcessing.numThreads * threadFraction).roundToIntOr())
    )

    private var hasBeenStarted = false
    override fun start(name: String, force: Boolean) {
        if (hasBeenStarted && !force) return
        hasBeenStarted = true
        // thread 0 is typically working itself ^^
        for (index in 1 until max(2, numThreads)) {
            super.start("$name-$index", true)
        }
    }

}