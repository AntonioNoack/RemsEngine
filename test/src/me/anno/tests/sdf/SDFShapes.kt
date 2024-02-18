package me.anno.tests.sdf

import me.anno.ecs.Entity
import me.anno.engine.ECSRegistry
import me.anno.engine.OfficialExtensions
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.extensions.ExtensionLoader
import me.anno.io.files.Reference.getReference
import me.anno.sdf.SDFComponent
import me.anno.sdf.shapes.SDFBezierCurve
import me.anno.sdf.shapes.SDFBlob
import me.anno.sdf.shapes.SDFBoundingBox
import me.anno.sdf.shapes.SDFBox
import me.anno.sdf.shapes.SDFCone
import me.anno.sdf.shapes.SDFCylinder
import me.anno.sdf.shapes.SDFDeathStar
import me.anno.sdf.shapes.SDFDoor
import me.anno.sdf.shapes.SDFEllipsoid
import me.anno.sdf.shapes.SDFHeart
import me.anno.sdf.shapes.SDFHexPrism
import me.anno.sdf.shapes.SDFHyperBBox
import me.anno.sdf.shapes.SDFHyperCube
import me.anno.sdf.shapes.SDFMesh
import me.anno.sdf.shapes.SDFPolygon
import me.anno.sdf.shapes.SDFPyramid
import me.anno.sdf.shapes.SDFRegular
import me.anno.sdf.shapes.SDFRoundCone
import me.anno.sdf.shapes.SDFSphere
import me.anno.sdf.shapes.SDFStairs
import me.anno.sdf.shapes.SDFStar
import me.anno.sdf.shapes.SDFTorus
import me.anno.sdf.shapes.SDFTriangle
import me.anno.utils.types.Strings.upperSnakeCaseToTitle
import me.anno.utils.types.Floats.toRadians
import org.joml.Vector3f

fun createShapesScene(): Entity {

    val scene = Entity()
    fun place(shape: SDFComponent, pos: Vector3f) {
        shape.position.set(pos)
        scene.add(shape)
    }

    place(SDFBox(), Vector3f(0f, 0f, 5f))
    place(SDFHyperCube().apply {
        rotation4d = rotation4d
            .rotateY((-35f).toRadians())
            .rotateX((47f).toRadians())
            .rotateZ((-33f).toRadians())
    }, Vector3f(5f, 0f, 5f))
    place(SDFBoundingBox(), Vector3f(0f, 0f, 10f))
    place(SDFHyperBBox().apply {
        rotation4d = rotation4d
            .rotateY((-35f).toRadians())
            .rotateX((47f).toRadians())
            .rotateZ((-33f).toRadians())
    }, Vector3f(5f, 0f, 10f))
    place(SDFSphere(), Vector3f())
    place(SDFCone(), Vector3f(5f, -1f, 0f))
    place(SDFRoundCone(), Vector3f(10f, -1f, 0f))
    place(SDFPyramid(), Vector3f(15f, -1f, 0f))
    place(SDFTorus(), Vector3f(20f, 0f, 0f))
    place(SDFCylinder(), Vector3f(25f, 0f, 0f))

    // todo heightmap

    // special shapes
    place(SDFHeart().apply { bound11() }, Vector3f(0f, 0f, 15f))
    place(SDFDeathStar(), Vector3f(5f, 0f, 15f))
    place(SDFDoor().apply { bound11() }, Vector3f(10f, 0f, 15f))
    place(SDFStairs().apply {
        stepCount = 15
        stepWidth = 0.28f
        stepHeight = 0.17f
        bound11(0.5f)
    }, Vector3f(15f - 15f * 0.28f * 0.5f, -1f, 15f))
    place(SDFStar().apply { bound11() }, Vector3f(20f, 0f, 15f))
    place(SDFPolygon().apply { bound11() }, Vector3f(25f, 0f, 15f))

    place(SDFHexPrism(), Vector3f(0f, 0f, 20f))
    place(SDFTriangle(), Vector3f(5f, 0f, 20f))
    place(SDFEllipsoid().apply { halfAxes.set(1f, 0.5f, 1f) }, Vector3f(10f, 0f, 20f))
    place(SDFBlob(), Vector3f(10f, 0f, 20f))
    place(SDFRegular().apply {
        name = "Octahedron"
        type = SDFRegular.Type.OCTAHEDRON
    }, Vector3f(15f, 0f, 20f))
    place(SDFRegular().apply {
        name = "Dodecahedron"
        type = SDFRegular.Type.DODECAHEDRON
    }, Vector3f(20f, 0f, 20f))
    place(SDFRegular().apply {
        name = "Icosahedron"
        type = SDFRegular.Type.ICOSAHEDRON
    }, Vector3f(25f, 0f, 20f))
    place(SDFBezierCurve(), Vector3f(30f, 0f, 20f))

    // place(SDFPlane().apply { limit = 20f }, Vector3f(0f, -1.2f, 0f))

    /**
     * This creates raytracers inside a raymarcher, which is a really bad idea (performance-wise).
     * This is only good for small meshes, and then you have to decide whether to use textures, or just code;
     * Symmetries aren't used ofc, so that might reduce performance by 2- or 4-fold as well.
     *
     * Using Susanne as the sample mesh, with 2 small spheres, reduced my performance to 60 fps on a RTX 3070 (BAD!!!).
     * */
    val sampleMesh = getReference("res://icon-lowpoly.obj")
    for ((index, type) in SDFMesh.SDFMeshTechnique.entries.withIndex()) {
        place(SDFMesh().apply {
            name = "SDFMesh - ${type.name.upperSnakeCaseToTitle()}"
            meshFile = sampleMesh
            smoothness = 0.01f
            technique = type
        }, Vector3f(20f + 5f * index, 0f, 10f))
    }

    return scene
}

/**
 * Scene, which shows most SDF shapes
 * */
fun main() {

    ECSRegistry.initMeshes()
    OfficialExtensions.register()
    ExtensionLoader.load()

    // todo bug: these elements cannot be properly clicked -> all have same clickId
    // todo bug: even when I select one, the gizmos isn't shown at the right place

    testSceneWithUI("SDF Shapes", createShapesScene())
}