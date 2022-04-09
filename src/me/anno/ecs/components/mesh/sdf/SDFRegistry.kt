package me.anno.ecs.components.mesh.sdf

import me.anno.ecs.components.mesh.sdf.modifiers.*
import me.anno.ecs.components.mesh.sdf.shapes.*
import me.anno.io.ISaveable.Companion.registerCustomClass

object SDFRegistry {

    // todo option to display sdf values on x/y/z axis for debugging shapes
    // like on http://mercury.sexy/hg_sdf/

    fun init() {
        // shapes
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
        // groups
        registerCustomClass(SDFGroup())
        // modifiers
        registerCustomClass(SDFArray())
        registerCustomClass(SDFColumn())
        registerCustomClass(SDFHalfSpace())
        registerCustomClass(SDFHexGrid())
        registerCustomClass(SDFMirror())
        registerCustomClass(SDFOnion())
        registerCustomClass(SDFRotSym())
        registerCustomClass(SDFRoundness())
        registerCustomClass(SDFStretcher())
        registerCustomClass(SDFTriangleArray())
        registerCustomClass(SDFTwist())
        registerCustomClass(SDFVoronoiArray())
    }

}