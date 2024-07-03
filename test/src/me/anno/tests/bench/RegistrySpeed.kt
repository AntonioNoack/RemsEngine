package me.anno.tests.bench

import me.anno.Engine
import me.anno.engine.ECSRegistry
import me.anno.engine.OfficialExtensions
import me.anno.utils.Clock
import org.apache.logging.log4j.LogManager

fun main() {
    LogManager.disableLogger("Saveable")
    val clock = Clock("RegistrySpeed")
    OfficialExtensions.initForTests() // ~1.6s on Ryzen 5 2600, ~0.8s on Ryzen 9 7950x3d
    clock.stop("First Time")
    ECSRegistry.hasBeenInited = false
    ECSRegistry.init() // ~0.01s
    clock.stop("Second Time")
    Engine.requestShutdown()
}