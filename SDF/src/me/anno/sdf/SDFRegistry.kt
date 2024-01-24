package me.anno.sdf

import me.anno.engine.ui.render.RenderMode
import me.anno.engine.ui.render.Renderers.previewRenderer
import me.anno.gpu.shader.renderer.InheritedRenderer
import me.anno.io.Saveable.Companion.registerCustomClass
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

@Suppress("unused")
object SDFRegistry {

    val NumStepsRenderer = InheritedRenderer("Num SDF Steps", previewRenderer)
    val NumStepsRenderMode = RenderMode(NumStepsRenderer)
    val SDFOnYRenderer = InheritedRenderer("SDF on Y", previewRenderer)
    val SDFOnYRenderMode = RenderMode(SDFOnYRenderer)

    @JvmStatic
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
        registerCustomClass(SDFMesh()) // not ready yet
        // groups
        registerCustomClass(SDFGroup())
        // modifiers
        registerCustomClass(SDFArrayMapper())
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
        registerCustomClass(SDFHeightMap()) // not ready yet
        // physics
        registerCustomClass(SDFCollider())
        // random
        registerCustomClass(SDFRandomUV())
        registerCustomClass(SDFRandomTranslation())
        registerCustomClass(SDFRandomRotation())
        // arrays version 2
        registerCustomClass(SDFArray2())
    }
}