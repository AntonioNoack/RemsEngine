package me.anno.mesh.mitsuba

import me.anno.ecs.components.mesh.Material
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.prefab.Prefab
import me.anno.ecs.prefab.PrefabReadable
import me.anno.ecs.prefab.change.Path
import me.anno.gpu.CullMode
import me.anno.io.Streams.read0String
import me.anno.io.Streams.readLE16
import me.anno.io.Streams.readLE32
import me.anno.io.Streams.readLE32F
import me.anno.io.Streams.readLE64
import me.anno.io.Streams.readLE64F
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import me.anno.io.files.Reference.appendPath
import me.anno.io.files.inner.InnerFolder
import me.anno.io.files.inner.InnerFolderCallback
import me.anno.io.files.inner.InnerPrefabFile
import me.anno.image.thumbs.Thumbs
import me.anno.io.xml.generic.XMLNode
import me.anno.io.xml.generic.XMLReader
import me.anno.maths.Maths.hasFlag
import me.anno.mesh.assimp.StaticMeshesLoader.shininessToRoughness
import me.anno.utils.Color.rgba
import me.anno.utils.Color.toVecRGBA
import me.anno.utils.ColorParsing
import me.anno.utils.types.Floats.toDegrees
import me.anno.utils.types.Floats.toRadians
import me.anno.utils.types.InputStreams.skipN
import me.anno.utils.types.Ints.toIntOrDefault
import org.apache.logging.log4j.LogManager
import org.joml.Matrix4x3f
import org.joml.Quaterniond
import org.joml.Quaternionf
import org.joml.Vector3d
import org.joml.Vector3f
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStream
import java.util.zip.InflaterInputStream
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan
import kotlin.math.sign
import kotlin.math.tan

/**
 * reader for XML and serialized files of the Mitsuba Renderer
 * https://github.com/mitsuba-renderer/mitsuba3/blob/master/src/shapes/serialized.cpp
 * */
object MitsubaReader {

    private val LOGGER = LogManager.getLogger(MitsubaReader::class)

    private fun readHeader(file: InputStream, length: Long): Pair<Int, LongArray> {

        file.mark(length.toInt())

        if (file.readLE16() != 0x041c) throw IOException("Missing MAGIC")
        val version = file.readLE16()
        if (version !in 3..4) throw IOException("Unknown version $version")

        file.skipN(length - 8) // 4 bytes were already read, 4 bytes for mesh count

        val numMeshes = file.readLE32()
        if (numMeshes < 1 || numMeshes > 65536) {
            return version to longArrayOf(4)
        }

        file.reset()
        file.mark(length.toInt())
        val offsets = if (version == 3) {
            file.skipN(length - 4 - numMeshes * 4)
            LongArray(numMeshes) { file.readLE32().toLong().and(0xffffffff) + 4 }
        } else {
            file.skipN(length - 4 - numMeshes * 8)
            LongArray(numMeshes) { file.readLE64() + 4 }
        }

        for (i in 1 until numMeshes) {
            if (offsets[i - 1] > offsets[i]) {
                // illegal -> we assume a single mesh
                return version to longArrayOf(4)
            }
        }

        return version to offsets
    }

    fun readMeshesAsFolder(file: FileReference, callback: InnerFolderCallback) {
        file.inputStream { it, exc ->
            if (it != null) {
                val fileLength = file.length()
                val stream = if (it.markSupported() && fileLength > 30e6) it
                else ByteArrayInputStream(it.readBytes())
                val (version, offsets) = readHeader(stream, file.length())
                val folder = InnerFolder(file)
                val absolutePath = file.absolutePath
                for (i in offsets.indices) {

                    stream.reset()
                    stream.mark(fileLength.toInt())
                    stream.skipN(offsets[i])

                    val name = "$i.json"
                    val relativePath = folder.getSubName(name)
                    val mesh = readMeshData(stream, version)
                    val prefab = (mesh.ref as PrefabReadable).readPrefab()

                    InnerPrefabFile(appendPath(absolutePath, name), relativePath, folder, prefab)
                }
                stream.close()
                callback.ok(folder)
            } else callback.err(exc)
        }
    }

    fun readMeshData(file: InputStream, version: Int): Mesh {

        val input = InflaterInputStream(file).buffered() // buffering is necessary; it makes reading 30x faster
        val flags = input.readLE32()
        val hasVertexNormals = flags.hasFlag(1)
        val hasUVs = flags.hasFlag(2)
        val hasVertexColors = flags.hasFlag(8)
        val hasFaceNormals = flags.hasFlag(0x10)
        val singlePrecision = flags.hasFlag(0x1000)
        // val doublePrecision = flags.hasFlag(0x2000)
        if (version == 4) { // version 3 has no names
            /*val sceneName = */input.read0String()
        }
        val numVertices = input.readLE64()
        val numTriangles = input.readLE64()

        fun readDouble() = input.readLE64F().toFloat()

        fun readNumbers(size: Int): FloatArray {
            val data = FloatArray(size)
            if (singlePrecision) {
                for (i in 0 until size) {
                    data[i] = input.readLE32F()
                }
            } else {
                for (i in 0 until size) {
                    data[i] = readDouble()
                }
            }
            return data
        }

        val vertices = readNumbers(3 * numVertices.toInt())
        val normals = if (hasVertexNormals) {
            readNumbers(3 * numVertices.toInt())
        } else if (hasFaceNormals) {
            readNumbers(3 * numTriangles.toInt())
        } else null
        val uvs = if (hasUVs) {
            val uvs = readNumbers(2 * numVertices.toInt())
            for (i in 1 until uvs.size step 2) uvs[i] = 1f - uvs[i]
            uvs
        } else null
        val vertexColors = if (hasVertexColors) {
            if (singlePrecision) {
                IntArray(numVertices.toInt()) {
                    val r = input.readLE32F()
                    val g = input.readLE32F()
                    val b = input.readLE32F()
                    rgba(r, g, b, 1f)
                }
            } else {
                IntArray(numVertices.toInt()) {
                    val r = readDouble()
                    val g = readDouble()
                    val b = readDouble()
                    rgba(r, g, b, 1f)
                }
            }
        } else null
        val indices = if (numVertices <= 0xffffffff) {
            IntArray(3 * numTriangles.toInt()) { input.readLE32() }
        } else {
            IntArray(3 * numTriangles.toInt()) { input.readLE64().toInt() }
        }

        val mesh = Mesh()
        mesh.positions = vertices
        mesh.color0 = if (hasVertexColors) vertexColors else null
        mesh.uvs = uvs
        mesh.normals = if (hasVertexNormals) normals else null
        mesh.indices = indices
        return mesh
    }

    fun readSceneAsFolder(sceneMain: FileReference, inputStream: InputStream): InnerFolder {

        val folder = sceneMain.getParent() ?: InvalidRef

        val scene = XMLReader().read(inputStream) as XMLNode
        if (scene.type != "scene") throw IOException("Wrong type: ${scene.type}")

        val byId = HashMap<String, XMLNode>()
        val prefab = Prefab("Entity")
        val innerFolder = InnerFolder(sceneMain)
        innerFolder.createPrefabChild("Scene.json", prefab)

        fun registerIds(node: XMLNode) {
            for (child in node.children) {
                if (child is XMLNode) {
                    if (child.type != "ref") {
                        val id = child["id"]
                        if (id != null) byId[id] = child
                    }
                    registerIds(child)
                }
            }
        }
        registerIds(scene)

        var nextIdCtr = 0
        fun createId(node: XMLNode): String {
            var name: String
            do { // prevent duplicates
                name = "${nextIdCtr++}"
            } while (name in byId)
            node["id"] = name
            byId[name] = node
            return name
        }

        fun regIds(node: XMLNode) {
            when (node.type) {
                "bsdf", "shape", "textures", "camera" -> {
                    node["id"] ?: createId(node)
                }
            }
            for (child in node.children) {
                if (child is XMLNode) regIds(child)
            }
        }
        regIds(scene)

        fun resolveReferences(node: XMLNode) {
            var i = 0
            while (i < node.children.size) {
                val child = node.children[i++]
                child as? XMLNode ?: continue
                if (child.type == "ref") {
                    val resolved = byId[child["id"]]
                        ?: continue // throw NotImplementedError("Missing ${child["id"]}, available: ${byId.keys.sorted()}")
                    node.children.removeAt(i - 1)
                    val name = child["name"]
                    if (resolved["name"] != name) {
                        val clone = resolved.shallowClone()
                        clone["name"] = name
                        node.children.add(clone)
                        resolveReferences(clone)
                    } else {
                        node.children.add(resolved)
                    }
                } else resolveReferences(child)
            }
        }

        for ((_, v) in byId) {
            resolveReferences(v)
        }

        resolveReferences(scene)

        // register all materials
        val materialFolder = InnerFolder(innerFolder, "materials")
        val materials = HashMap<String, FileReference>()
        fun regMaterials(node: XMLNode) {
            if (node.type == "bsdf") {
                val id = node["id"]!!
                val self = if (node["type"] == "twosided") node.children.filterIsInstance<XMLNode>()
                    .firstOrNull { it.type == "bsdf" } ?: node else node
                val material = Material()

                if (node["type"] == "twosided") {
                    material.cullMode = CullMode.BOTH
                }

                val type = self["type"]
                if (type == "dielectric") {
                    material.diffuseBase.w = 0.25f
                }

                if (type == "conductor") {
                    material.metallicMinMax.set(1f)
                    material.roughnessMinMax.set(0.09f)
                }

                fun decodeColor(v: String): Vector3f? {
                    val vs = v.split(',', ' ', '\t')
                        .filter { it.isNotBlank() }
                        .map { it.toFloatOrNull() ?: 1f }
                    return when (vs.size) {
                        1 -> Vector3f(vs[0])
                        3 -> Vector3f(vs[0], vs[1], vs[2])
                        else -> null
                    }
                }

                fun readColor(type: String, v: String): Vector3f? {
                    return if (type == "spectrum" || type == "rgb") {
                        decodeColor(v)
                    } else null
                }

                if (type == "phong") {
                    // <rgb name="diffuseReflectance" value="0.2 0.2 0.2"/>
                    // <rgb name="specularReflectance" value="0.59 0.59 0.59"/>
                    // metallic = |diffuse|/(|diffuse + specular|)
                    // exponent -> metallic
                    val exponent0 = self.children.firstOrNull { it is XMLNode && it["name"] == "exponent" } as? XMLNode
                    val specular0 =
                        self.children.firstOrNull { it is XMLNode && it["name"] == "specularReflectance" } as? XMLNode
                    val diffuse0 =
                        self.children.firstOrNull { it is XMLNode && it["name"] == "diffuseReflectance" } as? XMLNode
                    val exponent = exponent0?.get("value")
                    val specular = specular0?.get("value")
                    val diffuse = diffuse0?.get("value")
                    val v = shininessToRoughness(exponent?.toFloatOrNull() ?: 50f)
                    if (specular != null || diffuse != null) {
                        val spec = (if (specular != null) readColor(specular0.type, specular) else null) ?: Vector3f(0f)
                        val diff = (if (diffuse != null) readColor(diffuse0.type, diffuse) else null) ?: Vector3f(1f)
                        material.metallicMinMax.set(spec.length() / (spec.length() + diff.length()))
                        material.roughnessMinMax.set(1f - v)
                        material.diffuseBase.set(
                            diff.x + spec.x,
                            diff.y + spec.y,
                            diff.z + spec.z,
                            1f
                        )
                    } else {
                        material.metallicMinMax.set(v)
                        material.roughnessMinMax.set(1f - v)
                    }
                }

                // set textures
                for (child in self.children) {
                    child as? XMLNode ?: continue
                    if (child.type == "textures") {
                        var file = ""
                        for (child1 in child.children) {
                            if (child1 is XMLNode &&
                                child1["name"] == "filename"
                            ) file = child1["value"] ?: file
                        }
                        if (file.isEmpty()) continue
                        val ref = folder.getChild(file)
                        when (child["name"]) {
                            "reflectance", "diffuseReflectance", "specularTransmittance" ->
                                material.diffuseMap = ref
                            else -> LOGGER.warn("texture of unknown type: ${child["name"]} by $child")
                        }
                    } else when (child["name"]) {
                        "alpha" -> {
                            val v = child["value"]?.toFloatOrNull()
                            if (v != null) {
                                if (type == "roughconductor") {
                                    material.roughnessMinMax.set(1f - v)
                                }
                            }
                            // this is not our alpha...
                            // <!-- rough aluminium -->
                            //	<bsdf type="roughconductor" id="roughAluMat">
                            //		<string name="material" value="Al"/>
                            //		<float name="alpha" value="0.15"/>
                            //		<string name="distribution" value="phong"/>
                            //	</bsdf>
                        }
                        "reflectance" -> {
                            val v = child["value"]
                            if (v != null) {
                                if (v.startsWith("#")) {
                                    val a = material.diffuseBase.w
                                    ColorParsing.parseColor(v)?.toVecRGBA(material.diffuseBase)
                                    material.diffuseBase.w = a
                                } else {
                                    val c = decodeColor(v)
                                    if (c != null) {
                                        material.diffuseBase.set(c, material.diffuseBase.w)
                                    }
                                }
                            }
                        }
                    }
                }

                val prefab2 = Prefab()
                prefab2.source = materialFolder.createPrefabChild("$id.json", prefab2)
                prefab2._sampleInstance = material
                material.prefab = prefab2

                materials[id] = prefab2.source
            }
            for (child in node.children) {
                if (child is XMLNode) regMaterials(child)
            }
        }
        regMaterials(scene)

        for (child in scene.children) {
            if (child is XMLNode) {
                when (child.type) {
                    "sensor" -> {

                        // <sensor type="perspective">
                        //		<float name="farClip" value="100"/>
                        //		<float name="focusDistance" value="6.20808"/>
                        //		<float name="fov" value="57.2848"/>
                        //		<string name="fovAxis" value="x"/>
                        //		<float name="nearClip" value="0.1"/>
                        //		<transform name="toWorld">
                        //			<lookat target="-0.837797, -4.84986, 0.200282" origin="-0.905029, -5.8466, 0.244796" up="-0.00295064, 0.0448131, 0.998991"/>
                        //		</transform>
                        //
                        //		<sampler type="independent">
                        //			<integer name="sampleCount" value="32"/>
                        //		</sampler>
                        //
                        //		<film type="multifilm">
                        //			<integer name="height" value="576"/>
                        //			<integer name="width" value="1024"/>
                        //			<rfilter type="box"/>
                        //		</film>
                        //	</sensor>

                        fun nodeToVec(txt: String?): Vector3f? {
                            val v = txt?.split(',', ' ')?.mapNotNull { it.trim().toFloatOrNull() }
                            return if (v != null && v.size == 3) Vector3f(v[0], v[1], v[2]) else null
                        }

                        // rotate world such that +y = up
                        // only works correctly, if there is a single camera
                        val transform = child.children.filterIsInstance<XMLNode>()
                            .firstOrNull { it.type == "transform" && it["name"] == "toWorld" }
                        val lookAt = transform?.children?.filterIsInstance<XMLNode>()
                            ?.firstOrNull { it.type == "lookat" }
                        if (lookAt != null) {
                            val up = nodeToVec(lookAt["up"])
                            if (up != null) {
                                up.normalize()
                                when {
                                    // is x ever up?
                                    abs(up.y) > 0.9f && up.y < 0f ->
                                        prefab["rotation"] = Quaterniond().rotateX(PI)
                                    abs(up.z) > 0.9f ->
                                        prefab["rotation"] = Quaterniond().rotateX(-sign(up.z) * PI / 2.0)
                                }
                            }
                        }

                        // add camera to scene
                        val cameraEntity = prefab.add(Path.ROOT_PATH, 'e', "Entity", child["id"] ?: createId(child))
                        val camera = prefab.add(cameraEntity, 'c', "Camera", "Camera")

                        if (lookAt != null) {
                            val origin = nodeToVec(lookAt["origin"])
                            if (origin != null) {
                                prefab[cameraEntity, "position"] = Vector3d(origin)
                                val target = nodeToVec(lookAt["target"])
                                if (target != null) {
                                    val up = nodeToVec(lookAt["up"]) ?: Vector3f(0f, 1f, 0f)
                                    prefab[cameraEntity, "rotation"] =
                                        Quaterniond(Quaternionf().lookAlong(target.sub(origin), up))
                                }
                            }
                        }

                        for (data in child.children) {
                            data as? XMLNode ?: continue
                            val value = data["value"]
                            val float = value?.toFloatOrNull()
                            when (data["name"]) {
                                "farClip" -> if (float != null) prefab[camera, "far"] = float.toDouble()
                                "nearClip" -> if (float != null) prefab[camera, "near"] = float.toDouble()
                                "fov" -> {
                                    if (float != null) {
                                        val xAxis = child.children.filterIsInstance<XMLNode>()
                                            .any { it["name"] == "fovAxis" && it["value"] == "x" }
                                        if (xAxis) {
                                            // <film type="multifilm">
                                            //	 <integer name="height" value="576"/>
                                            //	 <integer name="width" value="1024"/>
                                            //	 <rfilter type="box"/>
                                            // </film>
                                            val film = child.children.filterIsInstance<XMLNode>()
                                                .firstOrNull { it.type == "film" }
                                            if (film != null) {
                                                var width = 1f
                                                var height = 1f
                                                for (value1 in film.children) {
                                                    if (value1 is XMLNode && value1["value"] != null) {
                                                        when (value1["name"]) {
                                                            "width" -> width = value1["value"]?.toFloatOrNull() ?: width
                                                            "height" -> height =
                                                                value1["value"]?.toFloatOrNull() ?: height
                                                        }
                                                    }
                                                }
                                                val valueY = tan(atan(float.toRadians() / 2f) * height / width) * 2f
                                                prefab[camera, "fovY"] = valueY.toDegrees()
                                            }
                                        } else {
                                            prefab[camera, "fovY"] = float
                                        }
                                    }
                                }
                            }
                        }
                    }
                    "shape" -> {

                        // todo support emitters -> add lights somehow
                        // <shape type="serialized" id="Lamp1Luminaire-mesh_0">
                        //		<string name="filename" value="bidir.serialized"/>
                        //		<integer name="shapeIndex" value="7"/>
                        //		<transform name="toWorld">
                        //			<matrix value="0.199416 1.29956e-07 -5.68055e-15 -1.77091 0 -8.71675e-09 -0.199416 1.00249 -1.29956e-07 0.199416 -8.71675e-09 1.00405 0 0 0 1"/>
                        //		</transform>
                        //
                        //		<ref name="bsdf" id="Luminaire1Material"/>
                        //
                        //		<emitter type="area" id="Lamp1Luminaire-emission">
                        //			<rgb name="radiance" value="500.000000 500.000000 500.000000"/>
                        //			<float name="samplingWeight" value="1.000000"/>
                        //		</emitter>
                        //	</shape>

                        // read shape / open it to the scene tree :)
                        var meshRef: FileReference = InvalidRef
                        when (val type = child.attributes["type"]) {
                            "sphere" -> {
                                // spawn sphere
                                meshRef = Thumbs.sphereMesh.ref
                            }
                            "serialized" -> {
                                // read/reference serialized mesh
                                // <string name="filename" value="sponza.serialized"/>
                                // <integer name="shapeIndex" value="7"/>
                                var file = ""
                                var index = 0
                                for (v in child.children) {
                                    if (v is XMLNode) {
                                        when (v["name"]) {
                                            "filename" -> file = v["value"] ?: file
                                            "shapeIndex", "shape_index" -> index = v["value"].toIntOrDefault(index)
                                        }
                                    }
                                }
                                if (file.isNotBlank()) {
                                    meshRef = folder.getChild(file).getChild("$index.json")
                                }
                            }
                            "obj" -> {
                                var file = ""
                                for (v in child.children) {
                                    if (v is XMLNode && v["name"] == "filename") {
                                        file = v["value"] ?: file
                                    }
                                }
                                if (file.isNotBlank()) {
                                    meshRef = folder.getChild(file)
                                }
                            }
                            else -> LOGGER.warn("Unknown mesh type: $type")
                        }
                        /*<transform name="toWorld">
                            <matrix value="1 0 0 0.00724 0 1 1.39626e-07 0.093312 0 -1.39626e-07 1 0.00149 0 0 0 1"/>
                        </transform>*/
                        val matrix = Matrix4x3f()
                        val transformNode = child.children.filterIsInstance<XMLNode>().firstOrNull {
                            it.type == "transform" &&
                                    it["name"] == "toWorld"
                        }
                        if (transformNode != null) {
                            for (node in transformNode.children) {
                                if (node is XMLNode) {
                                    when (node.type) {
                                        "matrix" -> {
                                            val value = node["value"] ?: continue
                                            val v = value.split(' ')
                                                .filter { it.isNotBlank() }
                                                .map { it.trim().toFloat() }
                                            if (v.size == 16) {
                                                matrix.set(
                                                    v[0], v[4], v[8],
                                                    v[1], v[5], v[9],
                                                    v[2], v[6], v[10],
                                                    v[3], v[7], v[11]
                                                )
                                            }
                                        }
                                        "translate" -> {
                                            matrix.translate(
                                                node["x"]?.toFloatOrNull() ?: 0f,
                                                node["y"]?.toFloatOrNull() ?: 0f,
                                                node["z"]?.toFloatOrNull() ?: 0f
                                            )
                                        }
                                        "scale" -> {
                                            matrix.scale(
                                                node["x"]?.toFloatOrNull() ?: 1f,
                                                node["y"]?.toFloatOrNull() ?: 1f,
                                                node["z"]?.toFloatOrNull() ?: 1f
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        val material = child.children.filterIsInstance<XMLNode>()
                            .firstOrNull { it.type == "bsdf" }

                        val entity = prefab.add(Path.ROOT_PATH, 'e', "Entity", child["id"]!!)
                        prefab[entity, "position"] = Vector3d(matrix.getTranslation(Vector3f()))
                        prefab[entity, "rotation"] = Quaterniond(matrix.getUnnormalizedRotation(Quaternionf()))
                        prefab[entity, "scale"] = Vector3d(matrix.getScale(Vector3f()))

                        val mesh = prefab.add(entity, 'c', "MeshComponent", "Mesh")
                        prefab[mesh, "meshFile"] = meshRef
                        if (material != null) {
                            prefab[mesh, "materials"] = listOf(materials[material["id"]!!])
                        }
                    }
                    "integrator" -> {
                        // some general settings...
                    }
                }
            }
        }

        return innerFolder
    }

    fun readSceneAsFolder(file: FileReference, callback: InnerFolderCallback) {
        file.inputStream { it, exc ->
            if (it != null) {
                callback.ok(readSceneAsFolder(file, it))
            } else callback.err(exc)
        }
    }
}