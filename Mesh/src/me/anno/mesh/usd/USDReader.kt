package me.anno.mesh.usd

import me.anno.ecs.prefab.Prefab
import me.anno.ecs.prefab.change.Path
import me.anno.io.files.FileReference
import me.anno.io.files.inner.InnerFolder
import me.anno.utils.async.Callback
import me.anno.utils.types.Arrays.startsWith
import org.joml.Matrix4f
import org.joml.Matrix4x3f
import org.joml.Quaternionf
import org.joml.Vector3f

class USDReader(val src: FileReference) {

    companion object {
        fun readAsFolder(src: FileReference, callback: Callback<InnerFolder>) {
            src.readText { text, err ->
                if (text != null) {
                    try {
                        val reader = USDReader(src)
                        callback.ok(reader.read(text, src.absolutePath))
                    } catch (e: Exception) {
                        callback.err(e)
                    }
                } else callback.err(err)
            }
        }
    }

    private val innerFolder = InnerFolder(src)
    private val meshFolder = InnerFolder(innerFolder, "meshes")
    private val materialFolder = InnerFolder(innerFolder, "materials")
    private val materialFiles = HashMap<String, FileReference>()
    private val stageCache = HashMap<String, USDPrim>()
    private val resolvedRefCache = HashMap<USDReference, USDPrim>()
    private lateinit var rootStage: USDPrim

    fun read(text: String, path: String): InnerFolder {

        println("Reading $text")

        val parsed = USDAParser(text, path).parse()
        rootStage = applySubLayers(parsed)
        indexPrims(rootStage)

        readStage(rootStage)

        innerFolder.sealPrefabs()
        return innerFolder
    }

    private fun applySubLayers(root: USDPrim): USDPrim {

        val subLayers = root.properties["subLayers"] as? List<String> ?: return root
        val mergedRoot = USDPrim("Root", "Root")

        // load in order
        for (layerPath in subLayers) {

            val file = src.getSibling(layerPath)
            val text = file.readBytesSync()

            val stage = readAnyUSD(text, file.absolutePath)

            // recursive: sublayers can have sublayers
            val resolved = applySubLayers(stage)

            mergedRoot.children.addAll(resolved.children)
        }

        // finally add local file (strongest layer)
        mergedRoot.children.addAll(root.children)

        return mergedRoot
    }

    fun readAnyUSD(bytes: ByteArray, path: String): USDPrim {
        return if (bytes.startsWith("PXR-USDC".toByteArray(), 0)) {
            USDCReader(bytes, path).read()
        } else {
            USDAParser(bytes.decodeToString(), path).parse()
        }
    }

    private fun mergeChildren(dst: MutableList<USDPrim>, src: List<USDPrim>) {
        // todo apply this...
        val map = dst.associateBy { it.name }.toMutableMap()
        for (child in src) {
            map[child.name] = child
        }
        dst.clear()
        dst.addAll(map.values)
    }

    private fun loadStage(file: String): USDPrim {
        return stageCache.getOrPut(file) {
            val file1 = src.getSibling(file)
            val text = file1.readTextSync()
            val stage = USDAParser(text, file1.absolutePath).parse()
            indexPrims(stage)
            stage
        }
    }

    private fun resolveReference(ref: USDReference): USDPrim {
        return resolvedRefCache.getOrPut(ref) {
            val stage = if (ref.file != null) loadStage(ref.file) else rootStage
            val index = primIndexByStage[stage]!!
            index[ref.path] ?: error("Missing referenced prim ${ref.path}")
        }
    }

    private fun readStage(root: USDPrim) {
        val scenePrefab = Prefab("Entity")

        for (child in root.children) {
            readPrim(child, scenePrefab, Path.ROOT_PATH, "")
        }

        innerFolder.createPrefabChild("Scene.json", scenePrefab)
    }

    private fun readPrim(prim: USDPrim, prefab: Prefab, path: Path, usdPath: String) {
        val entityPath = prefab.add(path, 'e', "Entity", prim.name)
        val currentPath = "$usdPath/${prim.name}"

        // --- instancing ---
        if (prim.references.isNotEmpty()) {

            val ref = prim.references.first() // USD allows multiple, start simple
            val targetPrim = resolveReference(ref)

            val instancePath = prefab.add(path, 'e', "Entity", prim.name)

            // recursively build referenced prim here (flattened)
            readPrim(targetPrim, prefab, instancePath, ref.path)

            // apply local overrides (transform!)
            applyTransform(prim, prefab, instancePath)

            return
        }

        when (prim.type) {

            "Mesh" -> {
                val meshFile = createMesh(prim)

                val meshPath = prefab.add(entityPath, 'c', "MeshComponent", prim.name)
                prefab[meshPath, "meshFile"] = meshFile

                // --- material binding ---
                val matPath = prim.relationships["material:binding"]
                if (matPath != null) {
                    val mat = materialFiles[matPath]
                    if (mat != null) {
                        prefab[meshPath, "materials"] = listOf(mat)
                    }
                }
            }

            "Material" -> {
                createMaterial(prim, currentPath)
            }
        }

        for (child in prim.children) {
            readPrim(child, prefab, entityPath, currentPath)
        }
    }

    private fun createMesh(prim: USDPrim): FileReference {

        val prefab = Prefab("Mesh")

        val points = (prim.properties["points"] as? List<Float>)?.toFloatArray()
        val counts = (prim.properties["faceVertexCounts"] as? List<Float>)?.map { it.toInt() }?.toIntArray()
        val faceIndices = (prim.properties["faceVertexIndices"] as? List<Float>)?.map { it.toInt() }?.toIntArray()

        if (points == null || counts == null || faceIndices == null) {
            return meshFolder.createPrefabChild("${prim.name}.json", prefab)
        }

        // triangulate
        val triIndices = triangulate(counts, faceIndices)

        // positions (expand!)
        val positions = FloatArray(triIndices.size * 3)
        for (i in triIndices.indices) {
            val j = triIndices[i] * 3
            positions[i * 3 + 0] = points[j + 0]
            positions[i * 3 + 1] = points[j + 1]
            positions[i * 3 + 2] = points[j + 2]
        }

        prefab["positions"] = positions
        prefab["indices"] = IntArray(triIndices.size) { it } // now non-indexed

        // --- UVs ---
        val uvValues = (prim.properties["primvars:st"] as? List<Float>)?.toFloatArray()
        val uvIndices = (prim.properties["primvars:st:indices"] as? List<Float>)?.map { it.toInt() }?.toIntArray()

        if (uvValues != null) {
            prefab["uvs"] = expandPrimvarVec2(uvValues, uvIndices, triIndices)
        }

        // --- normals ---
        val nValues = (prim.properties["normals"] as? List<Float>)?.toFloatArray()
        val nIndices = (prim.properties["normals:indices"] as? List<Float>)?.map { it.toInt() }?.toIntArray()

        if (nValues != null) {
            prefab["normals"] = expandPrimvarVec3(nValues, nIndices, triIndices)
        }

        return meshFolder.createPrefabChild("${prim.name}.json", prefab)
    }

    private fun applyTransform(prim: USDPrim, prefab: Prefab, path: Path) {

        val order = prim.properties["xformOpOrder"] as? List<*>
        if (order == null) {
            // fallback (rare but possible)
            applyFallbackTransform(prim, prefab, path)
            return
        }

        val transform = Matrix4x3f()

        for (entry in order) {
            val key = entry.toString()

            when {
                key.startsWith("xformOp:translate") -> {
                    val v = prim.properties[key] as? List<Float> ?: continue
                    transform.translate(v[0], v[1], v[2])
                }

                key.startsWith("xformOp:scale") -> {
                    val v = prim.properties[key] as? List<Float> ?: continue
                    transform.scale(v[0], v[1], v[2])
                }

                key.startsWith("xformOp:rotateXYZ") -> {
                    val v = prim.properties[key] as? List<Float> ?: continue
                    transform.rotateXYZ(
                        Math.toRadians(v[0].toDouble()).toFloat(),
                        Math.toRadians(v[1].toDouble()).toFloat(),
                        Math.toRadians(v[2].toDouble()).toFloat()
                    )
                }

                key.startsWith("xformOp:orient") -> {
                    val v = prim.properties[key] as? List<Float> ?: continue
                    val q = Quaternionf(v[0], v[1], v[2], v[3])
                    transform.rotate(q)
                }

                key.startsWith("xformOp:transform") -> {
                    val m = prim.properties[key] as? List<List<Float>> ?: continue
                    val mat = Matrix4f(
                        m[0][0], m[0][1], m[0][2], m[0][3],
                        m[1][0], m[1][1], m[1][2], m[1][3],
                        m[2][0], m[2][1], m[2][2], m[2][3],
                        m[3][0], m[3][1], m[3][2], m[3][3]
                    )
                    transform.mul(Matrix4x3f().set(mat))
                }
            }
        }

        writeTransform(prefab, path, transform)
    }

    private fun applyFallbackTransform(prim: USDPrim, prefab: Prefab, path: Path) {

        val t = prim.properties["xformOp:translate"] as? List<Float>
        val r = prim.properties["xformOp:rotateXYZ"] as? List<Float>
        val s = prim.properties["xformOp:scale"] as? List<Float>

        if (t != null) prefab[path, "position"] = Vector3f(t[0], t[1], t[2])

        if (r != null) {
            prefab[path, "rotation"] = Quaternionf()
                .rotateXYZ(
                    Math.toRadians(r[0].toDouble()).toFloat(),
                    Math.toRadians(r[1].toDouble()).toFloat(),
                    Math.toRadians(r[2].toDouble()).toFloat()
                )
        }

        if (s != null) prefab[path, "scale"] = Vector3f(s[0], s[1], s[2])
    }

    private fun writeTransform(prefab: Prefab, path: Path, transform: Matrix4x3f) {

        val translation = transform.getTranslation(Vector3f())
        val rotation = transform.getUnnormalizedRotation(Quaternionf())
        val scale = transform.getScale(Vector3f())

        if (translation.lengthSquared() > 0f) {
            prefab[path, "position"] = Vector3f(translation)
        }

        if (rotation.w != 1f) {
            prefab[path, "rotation"] = Quaternionf(rotation)
        }

        if (scale.distanceSquared(1f, 1f, 1f) > 1e-6f) {
            prefab[path, "scale"] = Vector3f(scale)
        }
    }

    private fun createMaterial(prim: USDPrim, path: String): FileReference {

        val prefab = Prefab("Material")

        val color = prim.properties["diffuseColor"] as? List<Float>
        if (color != null && color.size >= 3) {
            prefab["diffuseBase"] = Vector3f(color[0], color[1], color[2])
        }

        val file = materialFolder.createPrefabChild("${prim.name}.json", prefab)
        materialFiles[path] = file
        return file
    }

    private val primIndexByStage = HashMap<USDPrim, Map<String, USDPrim>>()
    private fun indexPrims(root: USDPrim): Map<String, USDPrim> {

        val map = HashMap<String, USDPrim>()

        fun recurse(prim: USDPrim, path: String) {
            val currentPath = "$path/${prim.name}"
            map[currentPath] = prim
            for (child in prim.children) {
                recurse(child, currentPath)
            }
        }

        recurse(root, "")
        primIndexByStage[root] = map
        return map
    }

    private fun triangulate(
        counts: IntArray,
        indices: IntArray
    ): IntArray {

        val result = ArrayList<Int>()
        var offset = 0

        for (c in counts) {
            // fan triangulation
            for (i in 1 until c - 1) {
                result.add(indices[offset])
                result.add(indices[offset + i])
                result.add(indices[offset + i + 1])
            }
            offset += c
        }

        return result.toIntArray()
    }

    private fun expandPrimvarVec2(
        values: FloatArray,
        indices: IntArray?,
        faceIndices: IntArray
    ): FloatArray {

        val result = FloatArray(faceIndices.size * 2)

        for (i in faceIndices.indices) {

            val srcIndex = indices?.get(i) ?: faceIndices[i]
            val j = srcIndex * 2

            result[i * 2 + 0] = values[j + 0]
            result[i * 2 + 1] = 1f - values[j + 1]
        }

        return result
    }

    private fun expandPrimvarVec3(
        values: FloatArray,
        indices: IntArray?,
        faceIndices: IntArray
    ): FloatArray {
        val result = FloatArray(faceIndices.size * 3)
        for (i in faceIndices.indices) {

            val srcIndex = indices?.get(i) ?: faceIndices[i]
            val j = srcIndex * 3

            result[i * 3 + 0] = values[j + 0]
            result[i * 3 + 1] = values[j + 1]
            result[i * 3 + 2] = values[j + 2]
        }
        return result
    }
}