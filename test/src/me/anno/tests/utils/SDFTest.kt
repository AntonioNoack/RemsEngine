package me.anno.tests.utils

import me.anno.Time
import me.anno.config.DefaultStyle.deepDark
import me.anno.ecs.components.mesh.TypeValue
import me.anno.gpu.GFX
import me.anno.gpu.shader.BaseShader
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.ShaderLib
import me.anno.gpu.shader.builder.Variable
import me.anno.image.ImageWriter
import me.anno.input.Input
import me.anno.sdf.*
import me.anno.sdf.arrays.SDFArrayMapper
import me.anno.sdf.arrays.SDFHexGrid
import me.anno.sdf.modifiers.*
import me.anno.sdf.shapes.*
import me.anno.ui.debug.TestDrawPanel.Companion.testDrawing
import me.anno.utils.Color.rgba
import me.anno.utils.pooling.JomlPools
import me.anno.utils.structures.arrays.IntArrayList
import me.anno.utils.types.Floats.toRadians
import org.joml.Matrix3f
import org.joml.Quaternionf
import org.joml.Vector3f
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tan

// todo sdf fractals
// todo sdf material properties

fun testCPU(finalShape: SDFComponent, camPosition: Vector3f, fovFactor: Float) {
    // test the cpu implementation by using it to calculate a frame as well
    val w = 96 * 6
    val h = 64 * 6
    val bgc = deepDark
    val camScaleX = fovFactor * w.toFloat() / h
    val seeds = IntArrayList(8)
    ImageWriter.writeRGBImageInt(w, h, "sdf.png", 32) { x, y, _ ->
        val dir = JomlPools.vec3f.create()
        dir.set(
            +(x.toFloat() / w * 2f - 1f) * camScaleX,
            -(y.toFloat() / h * 2f - 1f) * fovFactor, -1f
        )
        val distance = finalShape.raycast(camPosition, dir, 0.1f, 100f, 200, seeds)
        dir.mul(distance).add(camPosition)
        if (distance.isFinite()) {
            val normal = finalShape.calcNormal(dir, dir).mul(0.5f).add(0.5f, 0.5f, 0.5f)
            if (normal.x in -1f..1f) {
                rgba(normal.x, normal.y, normal.z, 1f)
                /*val color = ((normal.x * 100f).toInt() + 155) * 0x10101
                JomlPools.vec3f.sub(1)
                color*/
            } else 0xff0000
        } else {
            JomlPools.vec3f.sub(1)
            bgc
        }
    }
}

fun createTestShader(tree: SDFComponent): Pair<HashMap<String, TypeValue>, BaseShader> {
    val functions = LinkedHashSet<String>()
    val uniforms = HashMap<String, TypeValue>()
    val shapeDependentShader = StringBuilder()
    val seeds = ArrayList<String>()
    tree.buildShader(shapeDependentShader, 0, VariableCounter(1), 0, uniforms, functions, seeds)
    return uniforms to BaseShader(
        "raycasting",
        ShaderLib.coordsList,
        ShaderLib.coordsUVVertexShader,
        ShaderLib.uvList,
        listOf(
            Variable(GLSLType.M3x3, "camMatrix"),
            Variable(GLSLType.V2F, "camScale"),
            Variable(GLSLType.V3F, "camPosition"),
            Variable(GLSLType.V2F, "distanceBounds"),
            Variable(GLSLType.V3F, "sunDir"),
            Variable(GLSLType.V1I, "maxSteps"),
            Variable(GLSLType.V1F, "sdfReliability"),
            Variable(GLSLType.V1F, "sdfNormalEpsilon"),
            // [0,1.5], can be 1.0 in most cases; higher = faster convergence
            Variable(GLSLType.V1F, "sdfMaxRelativeError"),
            // near, far, reversedZ
            Variable(GLSLType.V3F, "depthParams"),
        ) + uniforms.entries.map { (k, v) ->
            Variable(v.type, k)
        },
        "" +
                "#define Infinity 1e20\n" +
                functions.joinToString("") +
                "vec4 map(in vec3 pos0){\n" +
                "   vec4 res0;vec2 uv=vec2(0.0);\n" +
                // here comes the shape dependant shader
                shapeDependentShader.toString() +
                "   return res0;\n" +
                "}\n" +
                SDFComposer.raycasting +
                SDFComposer.normal +
                "void main(){\n" +
                // define ray origin and ray direction from projection matrix
                "   vec3 dir = vec3((uv*2.0-1.0)*camScale, -1.0);\n" +
                "   dir = me.anno.tests.normalize(camMatrix * dir);\n" +
                "   vec2 ray = raycast(camPosition, dir);\n" +
                "   if(ray.y < 0.0) discard;\n" +
                "   vec3 hit = camPosition + ray.x * dir;\n" +
                "   vec3 normal = calcNormal(hit, ray.x * sdfNormalEpsilon, ray.x);\n" +
                // "   gl_FragColor = vec4(vec3(me.anno.tests.dot(normal,sunDir)*.4+.8), 1.0);\n" +
                "   gl_FragColor = vec4(normal*.5+.5, 1.0);\n" +
                "}"
    )
}

fun testGPU(finalShape: SDFComponent, camPosition: Vector3f, fovFactor: Float) {
    val (uniforms, shaderBase) = createTestShader(finalShape)
    val group1 = (finalShape as? SDFGroup)?.children?.getOrNull(0)
    val group2 = (finalShape as? SDFGroup)?.children?.getOrNull(1)
    println(shaderBase.fragmentShader)
    val camRotation = Quaternionf()
    val camMatrix = Matrix3f()
    testDrawing("SDFs on GPU") {
        val dt = Time.deltaTime.toFloat()
        val dt5 = 5f * dt
        val time = Time.gameTime.toFloat()
        val progressTime = 3f * time / ((group1 ?: group2)?.children?.size ?: 1)
        camRotation.transformInverse(camPosition)
        if (Input.isKeyDown('w')) camPosition.z -= dt5
        if (Input.isKeyDown('s')) camPosition.z += dt5
        if (Input.isKeyDown('a')) camPosition.x -= dt5
        if (Input.isKeyDown('d')) camPosition.x += dt5
        if (Input.isKeyDown('q')) camPosition.y -= dt5
        if (Input.isKeyDown('e')) camPosition.y += dt5
        camRotation.transform(camPosition)
        GFX.clip(it.x, it.y, it.width, it.height) {
            it.clear()
            val shader = shaderBase.value
            shader.use()
            shader.v2f("camScale", fovFactor * it.width.toFloat() / it.height, fovFactor)
            shader.v3f("camPosition", camPosition)
            shader.v2f("distanceBounds", 0.01f, 1e3f)
            shader.v1i("maxSteps", 100)
            shader.v1f("sdfMaxRelativeError", fovFactor / it.height) // is this correct???
            shader.v1f("sdfReliability", 0.7f)
            shader.v1f("sdfNormalEpsilon", 0.005f)
            shader.v3f("sunDir", 0.7f, 0f, 0.5f)
            if (group1 is SDFGroup) {
                for (mapper in group1.positionMappers) {
                    if (mapper is SDFTwist) {
                        // mapper.strength = 3f * sin(time * 3f)
                        // mapper.source = mapper.source.rotateX(dt)
                        // mapper.destination = mapper.destination.rotateX(dt)
                    }
                }
                /*group1.distanceMappers.filterIsInstance<SDFOnion>().forEach {
                    it.rings = ((sin(time) * .5f + .5f) * 20 + 1).toInt()
                }*/
                group1.progress = (sin(progressTime) * .5f + .5f) * (group1.children.size - 1f)
                if (group1.dynamicRotation) group1.rotation.rotateY(dt)
                for (child in group1.children) {
                    /*if (child is SDFSmoothShape && child !is SDFCylinder) {
                        child.smoothness = sin(Engine.gameTimeF.toFloat()) * .5f + .5f
                    }*/
                    /*if (child is SDFHexPrism) {
                        child.rotation.rotateY(-dt)
                    }*/
                }
            }
            if (group2 is SDFGroup) {
                group2.progress = (cos(progressTime) * .5f + .5f) * (group2.children.size - 1f)
                if (group2.dynamicRotation) group2.rotation.rotateY(-dt * 3f)
            }
            camRotation.identity()
                .rotateY(it.mx / it.height * 2f)
                .rotateX(it.my / it.height * 2f)
            camMatrix.identity().rotate(camRotation)
            shader.m3x3("camMatrix", camMatrix)
            for ((key, value) in uniforms) {
                value.bind(shader, key)
            }
            GFX.flat01.draw(shader)
        }
    }
}

fun createShape(): SDFComponent {

    fun createGroup(): SDFGroup {
        val obj0 = SDFBoundingBox()
        obj0.thickness = 0.1f
        obj0.dynamicThickness = true
        obj0.smoothness = 0.1f
        obj0.dynamicSmoothness = true
        val obj1 = SDFBox()
        obj1.smoothness = 0.3f
        /*obj1.addChild(SDFTwist().apply {
            strength = 1f
            // "cheap bend": src = x, dst = z
            source = Vector3f(1f, 0f, 0f)
            destination = Vector3f(0f, 0f, 1f)
            center.set(0f, -0.5f, 0f)
            // dynamicSource = true
            // dynamicDestination = true
        })*/
        // obj1.dynamicSmoothness = true
        // val obj2 = SDFSphere()
        val obj3 = SDFTorus()
        val obj4 = SDFCylinder()
        obj4.smoothness = 0.1f
        obj4.dynamicSmoothness = true
        // todo hexagon is slightly broken on cpu side
        val obj5 = SDFHexPrism()
        obj5.smoothness = 0.1f
        obj5.dynamicSmoothness = true
        obj5.dynamicRotation = true
        obj5.scale = 0.5f
        obj5.addChild(SDFStretcher(0.3f, 0f, 0f))
        // todo stairs are broken on cpu side
        val obj6 = SDFStairs()
        obj6.boundZ(-1f, +1f)
        obj6.addChild(SDFRoundness())
        val obj7 = SDFHeart()
        obj7.addChild(SDFOnion(0.2f, 1))
        obj7.boundZ(-0.1f, +0.1f)
        val obj8 = SDFDoor()
        obj8.addChild(SDFMirror(Vector3f(), Vector3f(1f, 0f, 1f)))
        // obj8.addChild(SDFMirror(Vector3f(), Vector3f(0f, 1f, 0f)))
        obj8.addChild(SDFMirror(Vector3f(), Vector3f(-1f, 0f, 1f)))
        // obj8.addChild(SDFOnion(0.2f, 1))
        obj8.boundZ(+0.5f, +1f)
        val group = SDFGroup()
        //group.addChild(obj0)
        //group.addChild(obj1)
        //group.addChild(obj2)
        group.addChild(obj3)
        //group.addChild(obj4)
        // group.addChild(obj5)
        // group.addChild(obj6)
        /*group.addChild(obj7)
        group.addChild(obj8)*/
        //group.addChild(SDFOnion())
        //group.addChild(SDFHalfSpace())
        return group
    }

    val group1 = createGroup()
    group1.position.y += 1f
    // group1.dynamicRotation = true
    val group2 = createGroup()
    group2.position.y -= 1f
    // group2.dynamicRotation = true
    val finalShape = SDFGroup()
    // finalShape.addChild(group1)
    finalShape.addChild(group2)
    finalShape.combinationMode = CombinationMode.UNION
    finalShape.smoothness = 0.5f
    val array = SDFArrayMapper()
    array.cellSize.set(4f)
    // array.count.set(4, 1, 5)
    // group2.addChild(array)
    val hexGrid = SDFHexGrid()
    // hexGrid.lim1.set(2f)
    // hexGrid.lim2.set(2f)
    hexGrid.cellSize = 4f
    // group2.addChild(hexGrid)
    return finalShape
}

fun main() {

    // render test of shapes
    // we could try to recreate some basic samples from IQ with our nodes :)

    // this would ideally test our capabilities
    val camPosition = Vector3f(0f, 0f, 4f)
    val fovDegrees = 90f

    val finalShape = createShape()
    val fovFactor = tan(fovDegrees.toRadians() * 0.5f)
    testCPU(finalShape, camPosition, fovFactor)
    testGPU(finalShape, camPosition, fovFactor)
}