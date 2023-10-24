package me.anno.tests.bench

import me.anno.engine.ECSRegistry
import me.anno.utils.Clock
import org.apache.logging.log4j.LogManager

fun main() {
    LogManager.disableLogger("ISaveable")
    val clock = Clock()
    ECSRegistry.init() // ~2s
    clock.stop("First Time")
    ECSRegistry.hasBeenInited = false
    ECSRegistry.init() // ~0.01s
    clock.stop("Second Time")
}