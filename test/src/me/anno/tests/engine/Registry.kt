package me.anno.tests.engine

import me.anno.engine.ECSRegistry
import me.anno.utils.types.Floats.f3
import org.apache.logging.log4j.LogManager

fun main() {
    val t0 = System.nanoTime()
    ECSRegistry.init()
    val t1 = System.nanoTime()
    ECSRegistry.hasBeenInited = false
    ECSRegistry.init()
    val t2 = System.nanoTime()
    LogManager.getLogger(ECSRegistry::class).info(
        "Used 1st ${((t1 - t0) * 1e-9).f3()} / " +
                "2nd ${((t2 - t1) * 1e-9).f3()} s"
    )
}