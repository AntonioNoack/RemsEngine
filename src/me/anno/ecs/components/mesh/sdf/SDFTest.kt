package me.anno.ecs.components.mesh.sdf

import me.anno.Engine
import me.anno.config.DefaultStyle.deepDark
import me.anno.ecs.components.mesh.TypeValue
import me.anno.ecs.components.mesh.sdf.modifiers.*
import me.anno.ecs.components.mesh.sdf.shapes.*
import me.anno.gpu.GFX
import me.anno.gpu.shader.BaseShader
import me.anno.gpu.shader.ShaderLib
import me.anno.image.ImageWriter
import me.anno.input.Input
import me.anno.ui.debug.TestDrawPanel
import me.anno.utils.pooling.JomlPools
import me.anno.utils.types.Floats.toRadians
import org.joml.Matrix3f
import org.joml.Quaternionf
import org.joml.Vector3f
import kotlin.concurrent.thread
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tan

// todo sdf fractals
// todo sdf material properties
// todo sdf ecs integration & bounds

fun testCPU(finalShape: SDFComponent, camPosition: Vector3f, fovFactor: Float) {
    // test the cpu implementation by using it to calculate a frame as well
    val w = 96 * 6
    val h = 64 * 6
    val bgc = deepDark
    val camScaleX = fovFactor * w.toFloat() / h
    ImageWriter.writeRGBImageInt(w, h, "sdf.png", 32) { x, y, _ ->
        val dir = JomlPools.vec3f.create()
        dir.set(
            +(x.toFloat() / w * 2f - 1f) * camScaleX,
            -(y.toFloat() / h * 2f - 1f) * fovFactor, -1f
        )
        val distance = finalShape.raycast(camPosition, dir, 0.1f, 100f, 200, 0.5f)
        dir.mul(distance).add(camPosition)
        if (distance.isFinite()) {
            val normal = finalShape.calcNormal(dir, dir)
            if (normal.x in -1f..1f) {
                val color = ((normal.x * 100f).toInt() + 155) * 0x10101
                JomlPools.vec3f.sub(1)
                color
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
    tree.buildShader(shapeDependentShader, 0, VariableCounter(1), "res", uniforms, functions)
    return uniforms to BaseShader("raycasting", ShaderLib.simplestVertexShader, ShaderLib.uvList, "" +
            uniforms.entries.joinToString("") { (k, v) -> "uniform ${v.type.glslName} $k;\n" } +
            "uniform mat3 camMatrix;\n" +
            "uniform vec2 camScale;\n" +
            "uniform vec3 camPosition;\n" +
            "uniform vec2 distanceBounds;\n" +
            "uniform vec3 sunDir;\n" +
            "uniform int maxSteps;\n" +
            "uniform float sdfReliability, sdfNormalEpsilon, sdfMaxRelativeError;\n" + // [0,1.5], can be 1.0 in most cases; higher = faster convergence
            "uniform vec3 depthParams;\n" + // near, far, reversedZ
            "#define Infinity 1e20\n" +
            functions.joinToString("") +
            "vec2 map(in vec3 pos0){\n" +
            "   vec2 res = vec2(1e20,-1.0);\n" +
            // here comes the shape dependant shader
            shapeDependentShader.toString() +
            "   return res;\n" +
            "}\n" +
            SDFComposer.raycasting +
            SDFComposer.normal +
            "void main(){\n" +
            // define ray origin and ray direction from projection matrix
            "   vec3 dir = vec3((uv*2.0-1.0)*camScale, -1.0);\n" +
            "   dir = normalize(camMatrix * dir);\n" +
            "   vec2 ray = raycast(camPosition, dir);\n" +
            "   if(ray.y < 0.0) discard;\n" +
            "   vec3 hit = camPosition + ray.x * dir;\n" +
            "   vec3 normal = calcNormal(hit, sdfNormalEpsilon);\n" +
            "   gl_FragColor = vec4(vec3(dot(normal,sunDir)*.4+.8), 1.0);\n" +
            "}")
}

fun testGPU(finalShape: SDFComponent, camPosition: Vector3f, fovFactor: Float) {
    val (uniforms, shaderBase) = createTestShader(finalShape)
    val group1 = (finalShape as? SDFGroup)?.children?.getOrNull(0)
    val group2 = (finalShape as? SDFGroup)?.children?.getOrNull(1)
    println(shaderBase.fragmentSource)
    val camRotation = Quaternionf()
    val camMatrix = Matrix3f()
    TestDrawPanel.testDrawing {
        val dt = Engine.deltaTime
        val dt5 = 5f * dt
        val time = Engine.gameTimeF
        val progressTime = 3f * time / ((group1 ?: group2)?.children?.size ?: 1)
        camRotation.transformInverse(camPosition)
        if (Input.isKeyDown('w')) camPosition.z -= dt5
        if (Input.isKeyDown('s')) camPosition.z += dt5
        if (Input.isKeyDown('a')) camPosition.x -= dt5
        if (Input.isKeyDown('d')) camPosition.x += dt5
        if (Input.isKeyDown('q')) camPosition.y -= dt5
        if (Input.isKeyDown('e')) camPosition.y += dt5
        camRotation.transform(camPosition)
        GFX.clip(it.x, it.y, it.w, it.h) {
            it.clear()
            val shader = shaderBase.value
            shader.use()
            shader.v2f("camScale", fovFactor * it.w.toFloat() / it.h, fovFactor)
            shader.v3f("camPosition", camPosition)
            shader.v2f("distanceBounds", 0.01f, 1e3f)
            shader.v1i("maxSteps", 100)
            shader.v1f("sdfMaxRelativeError", 0.001f)
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
                .rotateY(it.mx * 2f)
                .rotateX(it.my * 2f)
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
        /*obj1.add(SDFTwist().apply {
            strength = 1f
            // "cheap bend": src = x, dst = z
            source = Vector3f(1f, 0f, 0f)
            destination = Vector3f(0f, 0f, 1f)
            center.set(0f, -0.5f, 0f)
            // dynamicSource = true
            // dynamicDestination = true
        })*/
        // obj1.dynamicSmoothness = true
        val obj2 = SDFSphere()
        val obj3 = SDFTorus()
        val obj4 = SDFCylinder()
        obj4.smoothness = 0.1f
        obj4.dynamicSmoothness = true
        val obj5 = SDFHexPrism()
        obj5.smoothness = 0.1f
        obj5.dynamicSmoothness = true
        obj5.dynamicRotation = true
        obj5.scale = 0.5f
        obj5.add(SDFStretcher(0.3f, 0f, 0f))
        val obj6 = SDFStairs()
        obj6.boundZ(-1f, +1f)
        obj6.add(SDFRoundness())
        val obj7 = SDFHeart()
        obj7.add(SDFOnion(0.2f, 1))
        obj7.boundZ(-0.1f, +0.1f)
        val obj8 = SDFDoor()
        obj8.add(SDFMirror(Vector3f(), Vector3f(1f, 0f, 1f)))
        // obj8.add(SDFMirror(Vector3f(), Vector3f(0f, 1f, 0f)))
        obj8.add(SDFMirror(Vector3f(), Vector3f(-1f, 0f, 1f)))
        // obj8.add(SDFOnion(0.2f, 1))
        obj8.boundZ(+0.5f, +1f)
        val group = SDFGroup()
        group.addChild(obj0)
        group.addChild(obj1)
        group.addChild(obj2)
        group.addChild(obj4)
        group.addChild(obj3)
        group.addChild(obj5)
        group.addChild(obj6)
        group.addChild(obj7)
        group.addChild(obj8)
        //group.add(SDFOnion())
        //group.add(SDFHalfSpace())
        return group
    }

    val group1 = createGroup()
    group1.position.y += 1f
    // group1.dynamicRotation = true
    val group2 = createGroup()
    group2.position.y -= 1f
    // group2.dynamicRotation = true
    val finalShape = SDFGroup()
    // finalShape.add(group1)
    finalShape.addChild(group2)
    finalShape.type = SDFGroup.CombinationMode.UNION
    finalShape.smoothness = 0.5f
    val array = SDFArray()
    array.cellSize.set(4f)
    // array.count.set(4, 1, 5)
    group2.add(array)
    val hexGrid = SDFHexGrid()
    hexGrid.lim1.set(2f)
    hexGrid.lim2.set(2f)
    hexGrid.cellSize = 4f
    // group2.add(hexGrid)
    return finalShape
}

fun main() {

    fun dynamicThread(run: () -> Unit) {
        thread {
            while (!Engine.shutdown) {
                run(run)
                Thread.sleep(3)
            }
        }
    }

    // render test of shapes
    // we could try to recreate some basic samples from IQ with our nodes :)

    // this would ideally test our capabilities
    val camPosition = Vector3f(0f, 3f, 5f)
    val fovDegrees = 90f

    val finalShape = createShape()
    val fovFactor = tan(fovDegrees.toRadians() * 0.5f)
    // testCPU(finalShape, camPosition, fovFactor)
    testGPU(finalShape, camPosition, fovFactor)
}