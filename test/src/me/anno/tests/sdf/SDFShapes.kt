package me.anno.tests.sdf

import me.anno.ecs.Entity
import me.anno.engine.ECSRegistry
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.io.files.FileReference.Companion.getReference
import me.anno.sdf.SDFComponent
import me.anno.sdf.SDFRegistry
import me.anno.sdf.shapes.*
import me.anno.utils.types.Floats.toRadians
import org.joml.Vector3f

/**
 * Scene, which shows most SDF shapes
 * */
fun main() {

    ECSRegistry.initMeshes()
    SDFRegistry.init()

    // todo bug: these elements cannot be properly clicked -> all have same clickId
    // todo bug: even when I select one, the gizmos isn't shown at the right place
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
    place(SDFRegular().apply { type = SDFRegular.Type.OCTAHEDRON }, Vector3f(15f, 0f, 20f))
    place(SDFRegular().apply { type = SDFRegular.Type.DODECAHEDRON }, Vector3f(20f, 0f, 20f))
    place(SDFRegular().apply { type = SDFRegular.Type.ICOSAHEDRON }, Vector3f(25f, 0f, 20f))
    place(SDFBezierCurve(), Vector3f(30f, 0f, 20f))

    // place(SDFPlane().apply { limit = 20f }, Vector3f(0f, -1.2f, 0f))

    /**
     * This creates raytracers inside a raymarcher, which is a really bad idea (performance-wise).
     * This is only good for small meshes, and then you have to decide whether to use textures, or just code;
     * Symmetries aren't used ofc, so that might reduce performance by 2- or 4-fold as well.
     *
     * Using Susanne as the sample mesh, with 2 small spheres, reduced my performance to 60 fps on a RTX 3070 (BAD!!!).
     * */
    val sampleMesh = getReference("res://icon.obj")
    place(SDFMesh().apply {
        meshFile = sampleMesh
        normalEpsilon = 1e-5f
        smoothness = 0.01f
    }, Vector3f(20f, 0f, 10f))

    place(SDFMesh().apply {
        meshFile = sampleMesh
        normalEpsilon = 1e-5f
        smoothness = 0.01f
        useTextures = false
    }, Vector3f(25f, 0f, 10f))

    testSceneWithUI("SDF Shapes", scene)
}