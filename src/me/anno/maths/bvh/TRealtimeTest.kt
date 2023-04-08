package me.anno.maths.bvh

import me.anno.config.DefaultConfig.style
import me.anno.engine.ECSRegistry
import me.anno.language.translation.NameDesc
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.custom.CustomList
import me.anno.ui.debug.TestStudio.Companion.testUI3
import me.anno.ui.input.EnumInput
import me.anno.utils.Clock
import me.anno.utils.LOGGER
import org.apache.logging.log4j.LogManager
import org.joml.Quaternionf
import org.joml.Vector3f

fun main() {
    ECSRegistry.init()
    val clock = Clock()
    val (tlas, cameraPosition, cameraRotation, fovZFactor) = createSampleTLAS(16)
    clock.stop("Loading & Generating TLAS", 0.0)
    run(tlas, cameraPosition, cameraRotation, fovZFactor)
    LOGGER.debug("Finished")
}

fun run(
    bvh: TLASNode,
    pos: Vector3f, rot: Quaternionf,
    fovZFactor: Float
) {
    LogManager.disableLogger("WorkSplitter")
    testUI3 {

        val main = PanelListY(style)
        val controls = createControls(pos, rot, bvh, main)

        val list = CustomList(false, style)

        var scale = 4
        list.add(createCPUPanel(scale, pos, rot, fovZFactor, bvh, controls, true))
        list.add(createCPUPanel(scale, pos, rot, fovZFactor, bvh, controls, false))

        // gpu is fast enough :)
        scale = 1
        val useComputeShader = true
        list.add(createGPUPanel(scale, pos, rot, fovZFactor, bvh, controls, useComputeShader, false))
        list.add(createGPUPanel(scale, pos, rot, fovZFactor, bvh, controls, useComputeShader, true))
        list.add(createGPUPanel(scale, pos, rot, fovZFactor, bvh, controls, useComputeShader = false, false))

        main.add(list)
        main.add(typeInput())
        list.weight = 1f
        main

    }
}

fun typeInput() = EnumInput(
    NameDesc("Draw Mode"),
    NameDesc(drawMode.name),
    DrawMode.values().map { NameDesc(it.name) },
    style
).setChangeListener { _, index, _ ->
    drawMode = DrawMode.values()[index]
}