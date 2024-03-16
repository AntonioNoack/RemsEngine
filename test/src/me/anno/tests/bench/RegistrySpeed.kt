package me.anno.tests.bench

import me.anno.Engine
import me.anno.engine.ECSRegistry
import me.anno.engine.OfficialExtensions
import me.anno.extensions.ExtensionLoader
import me.anno.utils.Clock
import org.apache.logging.log4j.LogManager

fun main() {
    LogManager.disableLogger("Saveable")
    val clock = Clock()
    OfficialExtensions.register()
    ExtensionLoader.load()
    ECSRegistry.init() // ~2s
    clock.stop("First Time")
    ECSRegistry.hasBeenInited = false
    ECSRegistry.init() // ~0.01s
    clock.stop("Second Time")
    Engine.requestShutdown()
}