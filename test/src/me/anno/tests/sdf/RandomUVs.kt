package me.anno.tests.sdf

import me.anno.ecs.Entity
import me.anno.ecs.components.mesh.Material
import me.anno.engine.ECSRegistry
import me.anno.engine.ui.render.SceneView
import me.anno.gpu.RenderDoc.disableRenderDoc
import me.anno.sdf.TwoDims
import me.anno.sdf.arrays.SDFArrayMapper
import me.anno.sdf.arrays.SDFHexGrid
import me.anno.sdf.arrays.SDFTriangleGrid
import me.anno.sdf.modifiers.PositionMapper
import me.anno.sdf.random.SDFRandomUV
import me.anno.sdf.shapes.SDFSphere
import me.anno.utils.OS
import org.joml.Matrix2d

fun main() {

    ECSRegistry.init()
    val entity = Entity()

    val material = Material()
    material.diffuseMap = OS.pictures.getChild("normal bricks.png")
    material.linearFiltering = false
    val matList = listOf(material.ref)

    fun add(array: PositionMapper, y: Float) {
        val group = Entity()
        val shape = SDFSphere()
        shape.forMorphing = true
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

    disableRenderDoc()
    SceneView.testSceneWithUI("SDFRandomUV", entity)
}