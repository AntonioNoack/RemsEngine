package me.anno.tests.mesh.simplification

import me.anno.Engine
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.engine.OfficialExtensions
import me.anno.engine.raycast.RayQuery
import me.anno.jvm.HiddenOpenGLContext
import me.anno.utils.Clock
import me.anno.utils.OS.downloads
import org.joml.Vector3d
import org.joml.Vector3f

/**
 * First click onto dragon is slow... why???
 *   -> RTAS building takes a long time
 *   -> we made it async :)
 *   -> task-name-generation serialized the mesh 😄 -> optimized that
 * */
fun main() {

    val clock = Clock("FirstClickSlow")

    OfficialExtensions.initForTests()
    clock.stop("Loading Extensions")

    HiddenOpenGLContext.createOpenGL() // required for async generation
    clock.stop("Loading OpenGL")

    val comp = MeshComponent(downloads.getChild("3d/dragon.obj"))
    comp.getMesh()
    clock.stop("Loading Mesh")
    val query = RayQuery(
        Vector3d(-100.0, 0.0, 0.0),
        Vector3f(1f, 0f, 0f),
        200.0
    )
    comp.raycast(query)
    clock.stop("Raycast")

    Engine.requestShutdown()
}