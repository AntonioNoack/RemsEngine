package me.anno.tests.utils

import me.anno.ecs.Component
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.ProceduralMesh
import me.anno.ecs.components.mesh.utils.MeshJoiner
import me.anno.ecs.systems.Updatable
import me.anno.engine.OfficialExtensions
import me.anno.engine.ui.render.RenderState
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.maths.Maths.clamp
import me.anno.maths.Maths.sq
import me.anno.maths.chunks.spherical.SphereTriangle
import me.anno.maths.chunks.spherical.SphericalHierarchy
import me.anno.maths.noise.PerlinNoise
import me.anno.mesh.Shapes.tetrahedron
import me.anno.utils.Color.black
import me.anno.utils.Color.mixARGB
import org.joml.Matrix4x3f
import org.joml.Vector3d
import kotlin.math.abs

fun main() {

    OfficialExtensions.initForTests()

    // simple test for spherical hierarchy

    // test dynamic chunk system with dynamic lods
    // iterate over triangle chunks & children until they become too small
    // make sure, that we cover all triangles

    // spherical world is intended for much more detailed SphericalTriangle than here,
    // so don't worry about performance too much

    val baseShape = tetrahedron.front
    val sphereWorld = SphericalHierarchy(1.0, baseShape)

    val singleTriMesh = Mesh()
    val tripleDetail = true
    singleTriMesh.positions = FloatArray(if (tripleDetail) 12 else 9)
    if (tripleDetail) singleTriMesh.indices = intArrayOf(0, 1, 3, 1, 2, 3, 2, 0, 3)

    val noiseMap = PerlinNoise(1234L, 8, 0.5f, -1f, 1f)
    val meshBuilder = object : MeshJoiner<SphereTriangle>(true, false, false) {

        override fun getVertexColor(element: SphereTriangle): Int {
            val c = element.globalCenter
            val v = abs(noiseMap[c.x.toFloat(), c.y.toFloat(), c.z.toFloat()])
            val waterLevel = 0.1f
            return if (v < waterLevel) {
                // mix blue color for river
                val bright = 0x0096FF
                val dark = 0x0000FF
                mixARGB(dark, bright, v / waterLevel)
            } else {
                // mix green color
                mixARGB(0x057a24, 0x7abd64, clamp(2f * v / (1f - waterLevel)))
            } or black
        }

        private fun put(i: Int, local: Vector3d) {
            val positions = singleTriMesh.positions!!
            positions[i] = local.x.toFloat()
            positions[i + 1] = local.y.toFloat()
            positions[i + 2] = local.z.toFloat()
        }

        override fun getMesh(element: SphereTriangle): Mesh {
            put(0, element.localA)
            put(3, element.localB)
            put(6, element.localC)
            return singleTriMesh
        }

        override fun getTransform(element: SphereTriangle, dst: Matrix4x3f) {
            dst.set(element.localToGlobal)
        }
    }

    // iterate over some level of world, over all triangles
    val targetDepth = 4
    val elements = ArrayList<SphereTriangle>(sphereWorld.triangles.size * (1 shl (2 * targetDepth)))

    // display mesh
    val staticTest = true
    if (staticTest) {
        sphereWorld.forEach { triangle ->
            if (triangle.level == targetDepth) {
                elements.add(triangle)
                false
            } else {
                triangle.generateChildren()
                true
            }
        }
        val mesh = meshBuilder.join(elements)
        // todo on click raycast & find triangle & place tree there
        // baseShape.copy(mesh)
        testSceneWithUI("SphericalWorld", mesh)
    } else {
        val relativeDetail = 0.1
        val detailFactor = sq(relativeDetail)
        val oldPosition = Vector3d()
        val proceduralMesh = object : ProceduralMesh(), Updatable {
            override fun update(instances: Collection<Component>) {
                if (RenderState.cameraPosition.distanceSquared(RenderState.prevCameraPosition) > 0.0) {
                    generateMesh(getMesh())
                    oldPosition.set(RenderState.cameraPosition)
                    if (changed) invalidateMesh()
                }
            }

            var changed = false
            var nextIndex = 0
            fun add(triangle: SphereTriangle) {
                val index = nextIndex
                if (index >= elements.size) {
                    elements.add(triangle)
                } else {
                    if (elements[index] != triangle) {
                        elements[index] = triangle
                        changed = true
                    }
                }
                this.nextIndex++
            }

            override fun generateMesh(mesh: Mesh) {
                val oldSize = elements.size
                nextIndex = 0
                changed = false
                sphereWorld.forEach { triangle ->
                    // calculate distance to triangle
                    val distanceSq = triangle.globalCenter.distanceSquared(RenderState.cameraPosition)
                    // check whether it is small enough
                    if (sq(triangle.size) < distanceSq * detailFactor ||
                        (triangle.globalA.dot(RenderState.cameraDirection) > 0.0 &&
                                triangle.globalB.dot(RenderState.cameraDirection) > 0.0 &&
                                triangle.globalC.dot(RenderState.cameraDirection) > 0.0)
                    ) {
                        // add children as well to hide seams
                        // todo only add children if that edge has a change in level
                        // todo for that, get neighbors
                        triangle.ensureChildren()
                        add(triangle.childXX!!)
                        add(triangle.childAB!!)
                        add(triangle.childBC!!)
                        add(triangle.childCA!!)
                        add(triangle)
                        false
                    } else true
                }
                // only invalidate, if mesh actually changes
                if (oldSize != elements.size) changed = true
                for (i in elements.lastIndex downTo nextIndex)
                    elements.removeAt(i)
                if (changed) {
                    meshBuilder.join(mesh, elements)
                }
            }
        }
        testSceneWithUI("SphericalWorld", proceduralMesh)
    }
}