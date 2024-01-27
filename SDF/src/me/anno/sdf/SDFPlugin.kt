package me.anno.sdf

import me.anno.engine.ui.render.RenderMode
import me.anno.engine.ui.render.Renderers
import me.anno.extensions.plugins.Plugin
import me.anno.gpu.shader.renderer.InheritedRenderer
import me.anno.io.Saveable
import me.anno.sdf.arrays.SDFArray2
import me.anno.sdf.arrays.SDFArrayMapper
import me.anno.sdf.arrays.SDFHexGrid
import me.anno.sdf.arrays.SDFTriangleGrid
import me.anno.sdf.arrays.SDFVoronoiArray
import me.anno.sdf.modifiers.SDFColumn
import me.anno.sdf.modifiers.SDFHalfSpace
import me.anno.sdf.modifiers.SDFMirror
import me.anno.sdf.modifiers.SDFNoise
import me.anno.sdf.modifiers.SDFOnion
import me.anno.sdf.modifiers.SDFRotSym
import me.anno.sdf.modifiers.SDFRoundness
import me.anno.sdf.modifiers.SDFStretcher
import me.anno.sdf.modifiers.SDFTwist
import me.anno.sdf.random.SDFRandomRotation
import me.anno.sdf.random.SDFRandomTranslation
import me.anno.sdf.random.SDFRandomUV
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
import me.anno.sdf.shapes.SDFHeightMap
import me.anno.sdf.shapes.SDFHexPrism
import me.anno.sdf.shapes.SDFHyperBBox
import me.anno.sdf.shapes.SDFHyperCube
import me.anno.sdf.shapes.SDFMesh
import me.anno.sdf.shapes.SDFPlane
import me.anno.sdf.shapes.SDFPolygon
import me.anno.sdf.shapes.SDFPyramid
import me.anno.sdf.shapes.SDFRegular
import me.anno.sdf.shapes.SDFRoundCone
import me.anno.sdf.shapes.SDFSphere
import me.anno.sdf.shapes.SDFStairs
import me.anno.sdf.shapes.SDFStar
import me.anno.sdf.shapes.SDFTorus
import me.anno.sdf.shapes.SDFTriangle

class SDFPlugin : Plugin() {
    companion object {
        val NumStepsRenderer = InheritedRenderer("Num SDF Steps", Renderers.previewRenderer)
        val NumStepsRenderMode = RenderMode(NumStepsRenderer)
        val SDFOnYRenderer = InheritedRenderer("SDF on Y", Renderers.previewRenderer)
        val SDFOnYRenderMode = RenderMode(SDFOnYRenderer)
    }

    override fun onEnable() {
        super.onEnable()
        // shapes
        Saveable.registerCustomClass(SDFBezierCurve())
        Saveable.registerCustomClass(SDFBlob())
        Saveable.registerCustomClass(SDFBoundingBox())
        Saveable.registerCustomClass(SDFBox())
        Saveable.registerCustomClass(SDFCone())
        Saveable.registerCustomClass(SDFCylinder())
        Saveable.registerCustomClass(SDFDeathStar())
        Saveable.registerCustomClass(SDFDoor())
        Saveable.registerCustomClass(SDFEllipsoid())
        Saveable.registerCustomClass(SDFHeart())
        Saveable.registerCustomClass(SDFHexPrism())
        Saveable.registerCustomClass(SDFPlane())
        Saveable.registerCustomClass(SDFPolygon())
        Saveable.registerCustomClass(SDFPyramid())
        Saveable.registerCustomClass(SDFRegular())
        Saveable.registerCustomClass(SDFRoundCone())
        Saveable.registerCustomClass(SDFSphere())
        Saveable.registerCustomClass(SDFStairs())
        Saveable.registerCustomClass(SDFStar())
        Saveable.registerCustomClass(SDFTorus())
        Saveable.registerCustomClass(SDFTriangle())
        Saveable.registerCustomClass(SDFHyperCube())
        Saveable.registerCustomClass(SDFHyperBBox()) // idk whether I want to keep that one
        Saveable.registerCustomClass(SDFMesh()) // not ready yet
        // groups
        Saveable.registerCustomClass(SDFGroup())
        // modifiers
        Saveable.registerCustomClass(SDFArrayMapper())
        Saveable.registerCustomClass(SDFColumn())
        Saveable.registerCustomClass(SDFHalfSpace())
        Saveable.registerCustomClass(SDFHexGrid())
        Saveable.registerCustomClass(SDFMirror())
        Saveable.registerCustomClass(SDFNoise())
        Saveable.registerCustomClass(SDFOnion())
        Saveable.registerCustomClass(SDFRotSym())
        Saveable.registerCustomClass(SDFRoundness())
        Saveable.registerCustomClass(SDFStretcher())
        Saveable.registerCustomClass(SDFTriangleGrid())
        Saveable.registerCustomClass(SDFTwist())
        Saveable.registerCustomClass(SDFVoronoiArray())
        Saveable.registerCustomClass(SDFHeightMap()) // not ready yet
        // physics
        Saveable.registerCustomClass(SDFCollider())
        // random
        Saveable.registerCustomClass(SDFRandomUV())
        Saveable.registerCustomClass(SDFRandomTranslation())
        Saveable.registerCustomClass(SDFRandomRotation())
        // arrays version 2
        Saveable.registerCustomClass(SDFArray2())
    }
}
