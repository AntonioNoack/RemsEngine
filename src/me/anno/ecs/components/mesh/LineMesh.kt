package me.anno.ecs.components.mesh

import me.anno.ecs.Transform
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.engine.DefaultAssets
import me.anno.gpu.GFXState
import me.anno.gpu.buffer.Buffer
import me.anno.gpu.pipeline.Pipeline
import me.anno.gpu.shader.Shader
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import org.joml.AABBf

/**
 * render a mesh as if it was just lines;
 * make this an option in the default mesh class???
 * */
class LineMesh(var meshFile: FileReference) : PrefabSaveable(), IMesh {

    @Suppress("unused")
    constructor() : this(InvalidRef)
    constructor(mesh: Mesh) : this(mesh.ref)

    var materialOverrides: List<FileReference>? = null

    val mesh get() = MeshCache.getEntry(meshFile).waitFor() ?: DefaultAssets.flatCube

    override val numPrimitives: Long
        get() = mesh.numPrimitives // actual number is a little more complicated...

    override fun ensureBuffer() {
        mesh.ensureBuffer()
    }

    override fun getBounds(): AABBf {
        return mesh.getBounds()
    }

    override fun draw(pipeline: Pipeline?, shader: Shader, materialIndex: Int, drawLines: Boolean) {
        GFXState.drawLines.use(true) {
            mesh.draw(pipeline, shader, materialIndex, true)
        }
    }

    override fun drawInstanced(
        pipeline: Pipeline, shader: Shader, materialIndex: Int,
        instanceData: Buffer, drawLines: Boolean
    ) {
        GFXState.drawLines.use(true) {
            mesh.draw(pipeline, shader, materialIndex, true)
        }
    }

    override fun fill(pipeline: Pipeline, transform: Transform) {
        pipeline.addMesh(this, Pipeline.sampleMeshComponent, materialOverrides, transform)
    }

    override fun copyInto(dst: PrefabSaveable) {
        super.copyInto(dst)
        if (dst !is LineMesh) return
        dst.meshFile = meshFile
    }
}