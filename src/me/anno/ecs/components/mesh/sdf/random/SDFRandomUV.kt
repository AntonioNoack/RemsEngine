package me.anno.ecs.components.mesh.sdf.random

import me.anno.ecs.Entity
import me.anno.ecs.components.mesh.Material
import me.anno.ecs.components.mesh.TypeValue
import me.anno.ecs.components.mesh.sdf.TwoDims
import me.anno.ecs.components.mesh.sdf.VariableCounter
import me.anno.ecs.components.mesh.sdf.modifiers.PositionMapper
import me.anno.ecs.components.mesh.sdf.arrays.SDFArrayMapper
import me.anno.ecs.components.mesh.sdf.arrays.SDFHexGrid
import me.anno.ecs.components.mesh.sdf.arrays.SDFTriangleGrid
import me.anno.ecs.components.mesh.sdf.shapes.SDFSphere
import me.anno.engine.ECSRegistry
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.gpu.GFXBase
import me.anno.utils.OS.pictures
import org.joml.Matrix2d
import org.joml.Vector4f

// notice: to disable color interpolation, disable linear filtering inside the material :)
class SDFRandomUV : SDFRandom() {

    override fun buildShader(
        builder: StringBuilder,
        posIndex: Int,
        nextVariableId: VariableCounter,
        uniforms: HashMap<String, TypeValue>,
        functions: HashSet<String>,
        seed: String
    ): String? {
        builder.append("uv=nextRandF2(").append(seed).append(");\n")
        return null
    }

    override fun calcTransform(pos: Vector4f, seed: Int) {}

    override val className get() = "SDFRandomUV"

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {

            ECSRegistry.init()
            val entity = Entity()

            val material = Material()
            material.diffuseMap = pictures.getChild("normal bricks.png")
            material.linearFiltering = false
            val matList = listOf(material.ref)

            fun add(array: PositionMapper, y: Float) {
                val group = Entity()
                val shape = SDFSphere()
                shape.position.y = y
                shape.addChild(array)
                shape.addChild(SDFRandomUV())
                shape.sdfMaterials = matList
                group.addChild(shape)
                entity.add(group)
            }

            val hexGrid = SDFHexGrid()
            hexGrid.cellSize = 3f
            add(hexGrid, -10f)

            val cubeGrid = SDFArrayMapper()
            cubeGrid.cellSize.set(3f)
            cubeGrid.count.set(10, 1, 10)
            add(cubeGrid, 0f)

            val triGrid = SDFTriangleGrid()
            triGrid.dims = TwoDims.XZ
            triGrid.cellSize.set(3f)
            add(triGrid, 10f)

            val m = Matrix2d(1.0, 1.0, 0.6, -0.6)
            println(Matrix2d(m).transpose())
            println(m.invert().transpose())

            GFXBase.disableRenderDoc()
            testSceneWithUI(entity)
        }
    }

}