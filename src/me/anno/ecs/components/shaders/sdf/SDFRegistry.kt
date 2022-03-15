package me.anno.ecs.components.shaders.sdf

import me.anno.ecs.components.shaders.sdf.modifiers.SDFArray
import me.anno.ecs.components.shaders.sdf.modifiers.SDFHexGrid
import me.anno.ecs.components.shaders.sdf.modifiers.SDFStretcher
import me.anno.ecs.components.shaders.sdf.shapes.*
import me.anno.io.ISaveable.Companion.registerCustomClass

object SDFRegistry {

    fun init(){
        registerCustomClass(SDFBox())
        registerCustomClass(SDFBoundingBox())
        registerCustomClass(SDFCylinder())
        registerCustomClass(SDFHexPrism())
        registerCustomClass(SDFPlane())
        registerCustomClass(SDFSphere())
        registerCustomClass(SDFTorus())
        registerCustomClass(SDFGroup())
        registerCustomClass(SDFArray())
        registerCustomClass(SDFHexGrid())
        registerCustomClass(SDFStretcher())
    }

}