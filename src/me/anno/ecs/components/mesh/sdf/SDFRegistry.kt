package me.anno.ecs.components.mesh.sdf

import me.anno.ecs.components.mesh.sdf.modifiers.*
import me.anno.ecs.components.mesh.sdf.shapes.*
import me.anno.io.ISaveable.Companion.registerCustomClass

object SDFRegistry {

    fun init() {
        // shapes
        registerCustomClass(SDFBoundingBox())
        registerCustomClass(SDFBox())
        registerCustomClass(SDFCylinder())
        registerCustomClass(SDFDoor())
        registerCustomClass(SDFHeart())
        registerCustomClass(SDFHexPrism())
        registerCustomClass(SDFPlane())
        registerCustomClass(SDFPolygon())
        registerCustomClass(SDFPyramid())
        registerCustomClass(SDFSphere())
        registerCustomClass(SDFStairs())
        registerCustomClass(SDFStar())
        registerCustomClass(SDFTorus())
        registerCustomClass(SDFTriangle())
        // groups
        registerCustomClass(SDFGroup())
        // modifiers
        registerCustomClass(SDFArray())
        registerCustomClass(SDFHalfSpace())
        registerCustomClass(SDFHexGrid())
        registerCustomClass(SDFMirror())
        registerCustomClass(SDFOnion())
        registerCustomClass(SDFRoundness())
        registerCustomClass(SDFStretcher())
        registerCustomClass(SDFTwist())
    }

}