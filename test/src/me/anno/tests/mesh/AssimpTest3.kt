package me.anno.tests.mesh

import me.anno.ecs.Entity
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.io.json.JsonFormatter
import me.anno.mesh.assimp.AnimatedMeshesLoader.readAsFolder
import me.anno.mesh.fbx.FBX6000.parseBinaryFBX6000
import me.anno.mesh.fbx.FBX6000.readBinaryFBX6000AsMeshes
import me.anno.utils.OS.desktop
import me.anno.utils.OS.downloads
import me.anno.utils.structures.lists.Lists.all2

fun main() {
    val source = downloads.getChild("3d/FemaleStandingPose/6.1.fbx")
    val json = JsonFormatter.format(shorten(parseBinaryFBX6000(source.inputStreamSync())))
    desktop.getChild("fbx6100.json").writeText(json)
    println(json)
    println(readAsFolder(source))
    val meshes = readBinaryFBX6000AsMeshes(source.inputStreamSync())
    val entity = Entity()
    for (mesh in meshes) entity.add(MeshComponent(mesh))
    testSceneWithUI(entity)
}

fun shorten(v: Any?): Any? {
    return when (v) {
        is List<*> -> shorten(v)
        is Map<*, *> -> shorten(v)
        else -> v
    }
}

fun shorten(v: List<*>): Any {
    return if (v.size > 5) {
        val clazz = v.first()?.javaClass
        if (v.all2 { it?.javaClass == clazz } &&
            (clazz == null || !clazz.name.startsWith("java.util."))) {
            if (clazz != null && clazz.name.startsWith("java.lang."))
                "[${clazz.name.substring(10)} x ${v.size}]"
            else
                "[${clazz?.name} x ${v.size}]"
        } else v.map { shorten(it) }
    } else v.map { shorten(it) }
}

fun shorten(map: Map<*, *>): Map<*, *> {
    return map.mapValues { (_, v) -> shorten(v) }
}