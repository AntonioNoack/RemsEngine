package me.anno.ecs.components.light

import me.anno.config.DefaultStyle.white4
import me.anno.ecs.components.mesh.Material
import me.anno.ecs.components.mesh.Mesh
import me.anno.engine.ui.render.RenderView
import me.anno.engine.ui.render.RenderView.Companion.stack
import me.anno.mesh.vox.meshing.BlockBuffer
import me.anno.mesh.vox.meshing.BlockSide
import me.anno.mesh.vox.meshing.VoxelMeshBuildInfo
import me.anno.ui.editor.sceneView.Grid
import me.anno.utils.structures.arrays.FloatArrayList
import org.joml.Vector3f

class PointLight : LightComponent() {

    override fun getLightPrimitive(): Mesh = cubeMesh

    /*override fun onDrawGUI(view: RenderView) {
        super.onDrawGUI(view)
        stack.pushMatrix()
        stack.scale(1f / 3f)
        Grid.drawBuffer(stack, white4, Grid.sphereBuffer)
        stack.popMatrix()
    }*/

    override val className: String = "PointLight"

    companion object {

        val cutoff = 0.1
        val falloff = "max(0.0, 1.0/(1.0+9.0*dot(dir,dir)) - $cutoff)*${1.0 / (1.0 - cutoff)}"

        val cubeMesh = Mesh()

        init {

            val vertices = FloatArrayList(6 * 2 * 3 * 3)
            val base = VoxelMeshBuildInfo(intArrayOf(0, -1), vertices, null, null)

            base.color = -1
            base.setOffset(-0.5f, -0.5f, -0.5f)

            for (side in BlockSide.values) {
                BlockBuffer.addQuad(base, side, 1, 1, 1)
            }

            cubeMesh.positions = vertices.toFloatArray()
            cubeMesh.material = Material().apply {
                shader = getShader(PointLight())
                shaderOverrides["uColor"] = Material.TypeValue(Material.GLSLType.V3F, Vector3f(.3f, .3f, 0.7f))
            }

        }

    }

}