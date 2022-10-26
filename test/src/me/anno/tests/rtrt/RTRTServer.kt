package me.anno.tests.rtrt

import me.anno.ecs.components.cache.MeshCache
import me.anno.io.Streams.writeLE32
import me.anno.utils.OS.documents
import me.anno.utils.OS.downloads
import me.anno.utils.process.BetterProcessBuilder
import org.joml.Matrix4x3f
import java.net.Socket

fun main() {

    val updateMeshCommand = 'M'.code
    val updateInstanceCommand = 'I'.code
    val defineUniformsCommand = 'U'.code
    val defineShaderCommand = 'S'.code
    val defineTextureCommand = 'T'.code
    val renderCommand = 'X'.code
    val readResultCommand = 'R'.code

    // todo run VulkanRT server instance
    val server = downloads.getChild("RayTracingInVulkan/build/windows/bin/RayTracer.exe")
    val process = BetterProcessBuilder(server, 0, false).start()

    // listen for "ready"
    val input0 = process.inputStream.bufferedReader()
    while (true) {
        val line = input0.readLine() ?: break
        if (line == "ready") break
    }

    // open port for connection
    val port = 4536
    val socket = Socket("localhost", port)
    val input = socket.getInputStream()
    val output = socket.getOutputStream()

    // todo send rendering commands
    val mesh = MeshCache[documents.getChild("sphere.obj")]!!
    output.writeLE32(updateMeshCommand)
    output.writeLE32(0) // id

    // send mesh
    val pos = mesh.positions!!
    val idx = mesh.indices!!
    output.writeLE32(pos.size)
    for (f in pos) output.writeLE32(f.toRawBits())

    output.writeLE32(idx.size)
    for (i in idx) output.writeLE32(i)

    // todo server: load mesh, build BLAS

    output.writeLE32(updateInstanceCommand)
    output.writeLE32(0) // scene
    output.writeLE32(0) // instance
    output.writeLE32(0) // mesh
    output.writeLE32(0) // material

    // todo define shader materials


    // transform
    val m = Matrix4x3f()
    val t = FloatArray(12)
    m.get(t)
    for (i in 0 until 12) {
        output.writeLE32(t[i].toRawBits())
    }

    // todo send material shaders

    // send render command
    output.writeLE32(renderCommand)
    // invocation size
    output.writeLE32(1920)
    output.writeLE32(1080)
    output.writeLE32(1)

    output.writeLE32(readResultCommand)


}