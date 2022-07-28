package me.anno.tests

import me.anno.Engine
import me.anno.ecs.components.mesh.Material
import me.anno.ecs.prefab.PrefabCache
import me.anno.engine.ECSRegistry
import me.anno.utils.OS
import org.apache.logging.log4j.LogManager

fun main() {
    ECSRegistry.init()
    val prefab = PrefabCache[OS.documents.getChild("cube bricks.glb")]!!
    val logger = LogManager.getLogger(Material::class)
    for (change in prefab.adds) {
        logger.info(change)
    }
    for (change in prefab.sets) {
        logger.info(change)
    }
    val instance = prefab.createInstance()
    logger.info(instance)
    Engine.requestShutdown()
}