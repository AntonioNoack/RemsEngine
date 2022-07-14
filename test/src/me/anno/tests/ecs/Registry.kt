package me.anno.tests.ecs

import me.anno.engine.ECSRegistry
import me.anno.engine.ui.render.ECSShaderLib
import me.anno.gpu.shader.BaseShader
import me.anno.utils.types.Floats.f3
import org.apache.logging.log4j.LogManager

fun main() {
    val tx = System.nanoTime()
    ECSShaderLib.pbrModelShader = BaseShader()
    val t0 = System.nanoTime()
    ECSRegistry.init()
    val t1 = System.nanoTime()
    ECSRegistry.hasBeenInited = false
    ECSRegistry.init()
    val t2 = System.nanoTime()
    LogManager.getLogger(ECSRegistry::class).info(
        "Used 1st ${((t1 - t0) * 1e-9).f3()} / " +
                "2nd ${((t2 - t1) * 1e-9).f3()} s, " +
                "base: ${((t0 - tx) * 1e-9).f3()}"
    )
}