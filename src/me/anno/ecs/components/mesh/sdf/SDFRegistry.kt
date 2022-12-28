package me.anno.ecs.components.mesh.sdf

import me.anno.ecs.components.mesh.sdf.modifiers.*
import me.anno.ecs.components.mesh.sdf.modifiers.random.SDFUVSeed
import me.anno.ecs.components.mesh.sdf.shapes.*
import me.anno.io.ISaveable.Companion.registerCustomClass

object SDFRegistry {

    // like on http://mercury.sexy/hg_sdf/

    fun init() {
        // shapes
        registerCustomClass(SDFBezierCurve())
        registerCustomClass(SDFBlob())
        registerCustomClass(SDFBoundingBox())
        registerCustomClass(SDFBox())
        registerCustomClass(SDFCone())
        registerCustomClass(SDFCylinder())
        registerCustomClass(SDFDeathStar())
        registerCustomClass(SDFDoor())
        registerCustomClass(SDFEllipsoid())
        registerCustomClass(SDFHeart())
        registerCustomClass(SDFHexPrism())
        registerCustomClass(SDFPlane())
        registerCustomClass(SDFPolygon())
        registerCustomClass(SDFPyramid())
        registerCustomClass(SDFRegular())
        registerCustomClass(SDFRoundCone())
        registerCustomClass(SDFSphere())
        registerCustomClass(SDFStairs())
        registerCustomClass(SDFStar())
        registerCustomClass(SDFTorus())
        registerCustomClass(SDFTriangle())
        registerCustomClass(SDFHyperCube())
        registerCustomClass(SDFHyperBBox()) // idk whether I want to keep that one
        // groups
        registerCustomClass(SDFGroup())
        // modifiers
        registerCustomClass(SDFArray())
        registerCustomClass(SDFColumn())
        registerCustomClass(SDFHalfSpace())
        registerCustomClass(SDFHexGrid())
        registerCustomClass(SDFMirror())
        registerCustomClass(SDFNoise())
        registerCustomClass(SDFOnion())
        registerCustomClass(SDFRotSym())
        registerCustomClass(SDFRoundness())
        registerCustomClass(SDFStretcher())
        registerCustomClass(SDFTriangleGrid())
        registerCustomClass(SDFTwist())
        registerCustomClass(SDFVoronoiArray())
        // physics
        registerCustomClass(SDFCollider())
        // random
        registerCustomClass(SDFUVSeed())
    }

}