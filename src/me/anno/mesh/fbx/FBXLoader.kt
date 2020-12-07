package me.anno.mesh.fbx

import me.anno.mesh.Mesh
import me.anno.mesh.Model
import me.anno.mesh.Point
import me.anno.mesh.fbx.model.FBXGeometry
import me.anno.mesh.fbx.model.FBXMaterial
import me.anno.mesh.fbx.model.FBXModel
import me.anno.mesh.fbx.structure.FBXReader
import org.apache.logging.log4j.LogManager
import org.joml.Vector2f
import org.joml.Vector3f
import java.io.InputStream
import kotlin.math.max

object FBXLoader {

    fun loadFBX(input: InputStream): List<Model> {

        // our format does not yet support animations, so we need either a better format,
        // or just use it for other stuff; or append them later somehow...

        val fbxFile = FBXReader(input)

        val fbxModels = fbxFile.fbxObjects.filterIsInstance<FBXModel>()
        val models = ArrayList<Model>(fbxModels.size)
        for (model in fbxModels) {

            model.printProperties()

            val geometries = model.children.filterIsInstance<FBXGeometry>()
            if (geometries.isEmpty()) continue

            val materials = model.children.filterIsInstance<FBXMaterial>()

            if (geometries.size > 2) {
                println(model)
                throw RuntimeException("More than one geometry inside a model!")
            }

            val fbxGeometry = geometries[0]

            val mesh = fbxGeometry.generateMesh(
                "coords", "normals", "materialIndex",
                true, 1, 0
            )

            val stride = mesh.stride
            val attr = mesh.attributes
            val coordsIndex = attr.first { it.name == "coords" }.offset.toInt()
            val normalIndex = attr.first { it.name == "normals" }.offset.toInt()
            val materialIndex = attr.first { it.name == "materialIndex" }.offset.toInt()
            val uvIndex = attr.firstOrNull { it.name == "uvs" }?.offset?.toInt() ?: -1
            val buffer = mesh.nioBuffer!!

            val byteCount = buffer.position()
            val pointCount = byteCount / stride

            var maxMaterialId = 0
            for (i0 in 0 until byteCount step stride) {
                val material = buffer.getInt(i0 + materialIndex)
                maxMaterialId = max(maxMaterialId, material)
            }

            val pointsByMaterial = Array(maxMaterialId + 1) { ArrayList<Point>(pointCount) }
            for (i0 in 0 until byteCount step stride) {
                val x = buffer.getFloat(i0 + coordsIndex)
                val y = buffer.getFloat(i0 + coordsIndex + 4)
                val z = buffer.getFloat(i0 + coordsIndex + 8)
                val position = Vector3f(x, y, z)
                val nx = buffer.getFloat(i0 + normalIndex)
                val ny = buffer.getFloat(i0 + normalIndex + 4)
                val nz = buffer.getFloat(i0 + normalIndex + 8)
                val normal = Vector3f(nx, ny, nz)// .normalize()
                val uv = if (uvIndex >= 0) {
                    val u = buffer.getFloat(i0 + uvIndex)
                    val v = buffer.getFloat(i0 + uvIndex + 4)
                    Vector2f(u, v)
                } else null
                val material = buffer.getInt(i0 + materialIndex)
                val points = pointsByMaterial[material]
                val point = Point(position, normal, uv)
                points.add(point)
            }

            pointsByMaterial.forEach {
                frontToBack(it)
            }

            val meshes = pointsByMaterial.mapIndexed { index, it ->
                Mesh(materials.getOrNull(index)?.name ?: "M$index", it)
            }

            models += Model(model.name, meshes).apply {
                localTranslation.set(model.localTranslation)
                localRotation.set(model.localRotation)
                localScale.set(model.localScale)
                pivot.set(model.rotationPivot)
            }

        }

        if(models.isEmpty()){
            LOGGER.warn(fbxFile.root.toString().trim())
        }

        LOGGER.info(models.toString())

        return models

    }

    // fbx files are GL_BACK, obj is GL_FRONT
    // todo we could just flip them, if their normal disagrees...
    fun frontToBack(list: MutableList<Point>) {
        for (i in 0 until list.size step 3) {
            val tmp = list[i]
            list[i] = list[i + 1]
            list[i + 1] = tmp
        }
    }

    private val LOGGER = LogManager.getLogger(FBXLoader::class)

}