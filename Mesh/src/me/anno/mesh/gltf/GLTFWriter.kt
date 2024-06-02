package me.anno.mesh.gltf

import me.anno.ecs.Component
import me.anno.ecs.Entity
import me.anno.ecs.components.anim.AnimMeshComponent
import me.anno.ecs.components.anim.AnimationCache
import me.anno.ecs.components.anim.Bone
import me.anno.ecs.components.anim.SkeletonCache
import me.anno.ecs.components.camera.Camera
import me.anno.ecs.components.light.DirectionalLight
import me.anno.ecs.components.light.LightComponent
import me.anno.ecs.components.light.PointLight
import me.anno.ecs.components.light.SpotLight
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.components.mesh.MeshComponentBase
import me.anno.ecs.components.mesh.material.Material
import me.anno.ecs.prefab.Prefab
import me.anno.gpu.CullMode
import me.anno.gpu.buffer.DrawMode
import me.anno.gpu.pipeline.Pipeline
import me.anno.gpu.texture.Filtering
import me.anno.utils.types.size
import me.anno.io.Streams.writeLE32
import me.anno.io.Streams.writeString
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import me.anno.io.files.Signature
import me.anno.io.files.inner.InnerFile
import me.anno.io.json.generic.JsonWriter
import me.anno.io.saveable.Saveable
import me.anno.maths.Maths.clamp
import me.anno.maths.Maths.min
import me.anno.mesh.gltf.writer.Accessor
import me.anno.mesh.gltf.writer.AnimationData
import me.anno.mesh.gltf.writer.BufferView
import me.anno.mesh.gltf.writer.MaterialData
import me.anno.mesh.gltf.writer.MeshData
import me.anno.mesh.gltf.writer.Sampler
import me.anno.mesh.gltf.writer.SkinData
import me.anno.utils.Color.b
import me.anno.utils.Color.black3
import me.anno.utils.Color.g
import me.anno.utils.Color.r
import me.anno.utils.Color.white4
import me.anno.utils.assertions.assertEquals
import me.anno.utils.structures.arrays.IntArrayList
import me.anno.utils.structures.lists.Lists.createArrayList
import me.anno.utils.structures.maps.Maps.nextId
import me.anno.utils.structures.tuples.IntPair
import me.anno.utils.types.Floats.toRadians
import me.anno.utils.types.Vectors.toLinear
import org.apache.logging.log4j.LogManager
import org.joml.AABBf
import org.joml.Matrix4x3f
import org.joml.Quaterniond
import org.joml.Quaternionf
import org.joml.Vector3d
import org.joml.Vector3f
import org.joml.Vector4f
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import kotlin.math.atan
import kotlin.math.max

/**
 * writes a GLTF file from an Entity or Mesh,
 * writes materials and textures, too
 * */
class GLTFWriter : JsonWriter(ByteArrayOutputStream(1024)) {

    companion object {
        private val LOGGER = LogManager.getLogger(GLTFWriter::class)
        private const val GL_FLOAT = 0x1406
        private const val GL_UNSIGNED_BYTE = 0x1401
        private const val GL_UNSIGNED_INT = 0x1405
        private const val GL_ARRAY_BUFFER = 34962
        private const val GL_ELEMENT_ARRAY_BUFFER = 34963
    }

    var allDepsToFolder = false
    var packedDepsToFolder = false
    var allDepsToBinary = false
    var packedDepsToBinary = true
    var maxNumBackPaths = 0

    private fun copyRaw(value: String) {
        // raw copy
        next()
        output.write(value.encodeToByteArray())
    }

    private val textures = HashMap<IntPair, Int>() // source, sampler
    private val images = HashMap<FileReference, Int>() // uris
    private val samplers = HashMap<Sampler, Int>()
    private val materials = HashMap<MaterialData, Int>() // material, isDoubleSided
    private val meshes = HashMap<MeshData, Int>() // mesh, materials[]
    private val cameras = HashMap<Camera, Int>()
    private val lights = HashMap<LightComponent, Int>()
    private val skins = HashMap<SkinData, Int>()
    private val meshCompToSkin = HashMap<AnimMeshComponent, Int>()
    private val animations = ArrayList<AnimationData>()

    private val binary = ByteArrayOutputStream(4096)
    private val json get() = output as ByteArrayOutputStream

    private val nodes = ArrayList<Saveable>()
    private val children = ArrayList<IntArrayList>()

    private val bufferViews = ArrayList<BufferView>()
    private val accessors = ArrayList<Accessor>()

    private fun countMeshes(entity: Entity): Int {
        return entity.components.count {
            it is MeshComponentBase && it.getMesh() != null
        }
    }

    private fun supportsLight(light: LightComponent): Boolean {
        return when (light) {
            is DirectionalLight, is SpotLight, is PointLight -> true
            else -> false
        }
    }

    private fun addChild(comp: Component, childIndices: IntArrayList) {
        val idx2 = nodes.size
        nodes.add(comp)
        childIndices.add(idx2)
    }

    private fun addEntity(entity: Entity): Int {
        val idx = nodes.size
        nodes.add(entity)
        val cm = countMeshes(entity)
        val childIndices = IntArrayList(entity.children.size + cm)
        children.add(childIndices)
        for (child in entity.children) {
            childIndices.add(addEntity(child))
        }
        for (comp in entity.components) {
            when (comp) {
                is MeshComponentBase -> {
                    val mesh = comp.getMesh()
                    if (mesh != null) {
                        addChild(comp, childIndices)
                        if (comp is AnimMeshComponent && comp.animations.isNotEmpty()) {
                            defineSkin(comp, childIndices)
                        }
                    }
                }
                is Camera -> addChild(comp, childIndices)
                is LightComponent -> {
                    if (supportsLight(comp)) {
                        addChild(comp, childIndices)
                    }
                }
            }
        }
        return idx
    }

    private fun defineSkin(comp: AnimMeshComponent, childIndices: IntArrayList) {
        val skeleton = SkeletonCache[comp.skeleton]
        if (skeleton != null) {
            // add bone/skeleton hierarchy
            val baseId = nodes.size
            defineBoneHierarchy(skeleton.bones, childIndices)
            meshCompToSkin[comp] = skins.nextId(SkinData(skeleton, baseId until nodes.size))
            for (state in comp.animations) {
                val animation = AnimationCache[state.source] ?: continue
                animations.add(AnimationData(skeleton, animation, baseId))
            }
        }
    }

    private fun defineBoneHierarchy(bones: List<Bone>, roots: IntArrayList) {
        val baseId = nodes.size
        for (boneId in bones.indices) {
            val bone = bones[boneId]
            nodes.add(bone)
            children.add(IntArrayList(4))
        }
        for (boneId in bones.indices) {
            val bone = bones[boneId]
            if (bone.parentId in bones.indices) {
                children[baseId + bone.parentId].add(baseId + boneId)
            } else {
                roots.add(baseId + boneId)
            }
        }
    }

    private fun <K> writeArray(name: String, map: Map<K, Int>, write: (K) -> Unit) {
        if (map.isNotEmpty()) {
            attr(name)
            writeArray {
                for ((k, _) in map.entries.sortedBy { it.value }) {
                    write(k)
                }
            }
        }
    }

    private fun minAABB(bounds: AABBf): String = "[${bounds.minX},${bounds.minY},${bounds.minZ}]"
    private fun maxAABB(bounds: AABBf): String = "[${bounds.maxX},${bounds.maxY},${bounds.maxZ}]"

    private fun aabb(bounds: FloatArray, i: Int, n: Int): String =
        (i until i + n).joinToString(",", "[", "]") { bounds[it].toString() }

    private fun alignBinary(alignment: Int) {
        val remainder = binary.size() % alignment
        if (remainder > 0) {
            for (i in remainder until alignment) {
                binary.write(0)
            }
        }
        assertEquals(0, binary.size() % alignment)
    }

    private fun createPositionsView(positions: FloatArray, bounds: AABBf): Int {
        alignBinary(4)
        val start = binary.size()
        for (v in positions) {
            // NaN is not supported
            binary.writeLE32(if (v.isNaN()) 0 else v.toRawBits())
        }
        return addBuffer(
            "VEC3", GL_FLOAT, positions.size / 3, false,
            minAABB(bounds), maxAABB(bounds),
            start, GL_ARRAY_BUFFER, 0
        )
    }

    private fun createColorView(data: IntArray): Int {
        alignBinary(4)
        val pos = binary.size()
        for (v in data) {
            binary.write(v.r())
            binary.write(v.g())
            binary.write(v.b())
            binary.write(255) // unused
        }
        return addBuffer(
            "VEC3", GL_UNSIGNED_BYTE, data.size, true, null, null,
            pos, GL_ARRAY_BUFFER, 4
        )
    }

    private fun createIndicesView(data: IntArray, mode: DrawMode, cullMode: CullMode): Int {
        alignBinary(4)
        val pos = binary.size()
        val reverseIndices = cullMode == CullMode.BACK
        if (reverseIndices && (mode == DrawMode.TRIANGLES || mode == DrawMode.TRIANGLE_STRIP)) {
            for (i in data.lastIndex downTo 0) {
                binary.writeLE32(data[i])
            }
        } else {
            for (i in data.indices) {
                binary.writeLE32(data[i])
            }
        }
        return addBuffer(
            "SCALAR", GL_UNSIGNED_INT, data.size, false, null, null,
            pos, GL_ELEMENT_ARRAY_BUFFER, 0
        )
    }

    private fun createNormalsView(data: FloatArray): Int {
        alignBinary(4)
        val pos = binary.size()
        val v = Vector3f()
        val bounds = AABBf()
        val one = 1f.toRawBits()
        for (i in data.indices step 3) {
            v.set(data[i], data[i + 1], data[i + 2])
            v.normalize()
            if (v.isFinite) {
                binary.writeLE32(v.x.toRawBits())
                binary.writeLE32(v.y.toRawBits())
                binary.writeLE32(v.z.toRawBits())
                bounds.union(v.x, v.y, v.z)
            } else {
                binary.writeLE32(0)
                binary.writeLE32(one)
                binary.writeLE32(0)
                bounds.union(0f, 1f, 0f)
            }
        }
        return addBuffer(
            "VEC3", GL_FLOAT, data.size / 3, false, minAABB(bounds), maxAABB(bounds),
            pos, GL_ARRAY_BUFFER, 0
        )
    }

    private fun createVectorView(data: FloatArray, type: String, numComp: Int, target: Int): Int {
        alignBinary(if (numComp == 3) 16 else 4 * numComp)
        val bounds = FloatArray(8)
        bounds.fill(Float.POSITIVE_INFINITY, 0, 4)
        bounds.fill(Float.NEGATIVE_INFINITY, 4, 8)
        val pos = binary.size()
        var k = 0
        for (i in 0 until data.size / numComp) {
            for (j in 0 until numComp) {
                val value = data[k++]
                bounds[j] = min(bounds[j], value)
                bounds[j + 4] = max(bounds[j + 4], value)
                binary.writeLE32(value)
            }
        }
        return addBuffer(
            type, GL_FLOAT, data.size / numComp, false,
            aabb(bounds, 0, numComp), aabb(bounds, 4, numComp),
            pos, target, 0
        )
    }

    private fun addBuffer(
        type: String, componentType: Int, count: Int, normalized: Boolean,
        min: String?, max: String?, pos: Int, target: Int, byteStride: Int
    ): Int {
        accessors.add(Accessor(bufferViews.size, type, componentType, count, normalized, min, max))
        bufferViews.add(BufferView(pos, binary.size() - pos, target, byteStride))
        return accessors.lastIndex
    }

    private fun createWeightView(data: FloatArray): Int {
        return createVectorView(data, "VEC4", 4, GL_ARRAY_BUFFER)
    }

    private fun createJointView(data: ByteArray, weights: FloatArray): Int {
        alignBinary(4)
        val pos = binary.size()
        for (i in data.indices) {
            binary.write(
                if (weights[i] != 0f) data[i].toInt()
                else 0 // zero weights must have zero indices in GLTF
            )
        }
        return addBuffer(
            "VEC4", GL_UNSIGNED_BYTE, data.size / 4, false, null, null,
            pos, GL_ARRAY_BUFFER, 0
        )
    }

    private fun createInverseBindMatrixView(data: List<Bone>): Int {
        alignBinary(64)
        val pos = binary.size()
        val tmp = FloatArray(16)
        tmp[15] = 1f // last row must be 0,0,0,1
        for (v in data) {
            v.inverseBindPose.get(tmp)
            for (i in tmp.indices) {
                val j = i.and(3).shl(2) or i.shr(2) // transpose the matrix
                binary.writeLE32(tmp[j])
            }
        }
        return addBuffer(
            "MAT4", GL_FLOAT, data.size, false, null, null,
            pos, 0, 0
        )
    }

    private fun createUVView(data: FloatArray): Int {
        alignBinary(8)
        val pos = binary.size()
        val bounds = AABBf()
        for (i in data.indices step 2) {
            var u = data[i]
            var v = 1f - data[i + 1]
            if (u.isNaN()) u = 0f
            if (v.isNaN()) v = 0f
            binary.writeLE32(u.toRawBits())
            binary.writeLE32(v.toRawBits())
            bounds.union(u, v, 0f)
        }
        val min = "[${bounds.minX},${bounds.minY}]"
        val max = "[${bounds.maxX},${bounds.maxY}]"
        return addBuffer(
            "VEC2", GL_FLOAT, data.size / 2, false, min, max,
            pos, GL_ARRAY_BUFFER, 0
        )
    }

    private fun getTextureIndex(source: FileReference, sampler: Int): Int {
        return textures.nextId(IntPair(images.nextId(source), sampler))
    }

    private fun writeSampler(sampler: Sampler) {
        val (filtering, clamping) = sampler
        writeObject {
            attr("magFilter")
            write(filtering.mag)
            attr("minFilter")
            write(filtering.min)
            attr("wrapS")
            write(clamping.mode)
            attr("wrapT")
            write(clamping.mode)
        }
    }

    private fun writeSkin(skinData: SkinData) {
        val (skeleton, nodes) = skinData
        writeObject {
            attr("inverseBindMatrices")
            write(createInverseBindMatrixView(skeleton.bones))
            attr("joints")
            writeArrayByIndices(0, nodes.size) {
                write(nodes.first + it)
            }
        }
    }

    private fun writeBufferView(bufferView: BufferView) {
        writeObject {
            attr("buffer")
            write(0)
            attr("byteOffset")
            write(bufferView.offset)
            attr("byteLength")
            write(bufferView.length)
            if (bufferView.byteStride != 0) {
                attr("byteStride")
                write(bufferView.byteStride)
            }
            if (bufferView.target != 0) {
                attr("target")
                write(bufferView.target)
            }
        }
    }

    private fun writeEntityAttributes(node: Entity) {
        if (countMeshes(node) == 1) {
            val mesh = node.components
                .filterIsInstance<MeshComponentBase>()
                .firstNotNullOfOrNull {
                    val mesh = it.getMesh() as? Mesh
                    if (mesh != null) Pair(it, mesh) else null
                }
            if (mesh != null) {
                attr("mesh")
                write(meshes.nextId(getMeshData(mesh.first, mesh.second)))
                val skin = meshCompToSkin[mesh.first]
                if (skin != null) {
                    attr("skin")
                    write(skin)
                }
            }
        }

        val camera = node.components
            .filterIsInstance<Camera>()
            .firstNotNullOfOrNull { cameras[it] }

        if (camera != null) {
            attr("camera")
            write(camera)
        }

        val name = node.name
        if (name.isNotEmpty()) {
            attr("name")
            write(name)
        }

        val translation = node.transform.localPosition
        if (translation != Vector3d()) {
            attr("translation")
            write(translation)
        }

        val rotation = node.transform.localRotation
        if (rotation != Quaterniond()) {
            attr("rotation")
            write(rotation)
        }

        val scale = node.transform.localScale
        if (scale.x != 1.0 || scale.y != 1.0 || scale.z != 1.0) {
            attr("scale")
            write(scale)
        }
    }

    private fun writePbrMetallicRoughness(material: Material, sampler: Int) {
        writeObject {
            if (material.diffuseMap.exists) {
                attr("baseColorTexture")
                writeObject {
                    attr("index")
                    write(getTextureIndex(material.diffuseMap, sampler))
                }
            }
            val color = material.diffuseBase
            if (color != white4) {
                attr("baseColorFactor")
                val color1 = if (color.x in 0f..1f && color.y in 0f..1f && color.z in 0f..1f && color.w in 0f..1f) {
                    material.diffuseBase
                } else {
                    Vector4f(clamp(color.x), clamp(color.y), clamp(color.z), clamp(color.w))
                }
                write(color1)
            }
            attr("metallicFactor")
            write(material.metallicMinMax.y)
            attr("roughnessFactor")
            write(material.roughnessMinMax.y)
        }
    }

    private fun findSampler(material: Material): Int {
        return if (
            material.emissiveMap.exists ||
            material.diffuseMap.exists ||
            material.normalMap.exists ||
            material.occlusionMap.exists
        ) {
            samplers.nextId(
                Sampler(
                    if (material.linearFiltering) Filtering.TRULY_LINEAR
                    else Filtering.TRULY_NEAREST,
                    material.clamping
                )
            )
        } else -1
    }

    private fun writeMaterial(data: MaterialData) {
        val (material, isDoubleSided) = data
        // https://github.com/KhronosGroup/glTF/blob/main/specification/2.0/schema/material.schema.json
        writeObject {
            val sampler = findSampler(material)
            attr("pbrMetallicRoughness")
            writePbrMetallicRoughness(material, sampler)
            if (isDoubleSided) {
                attr("doubleSided")
                write(true)
            }
            writeTextureProperty(material.emissiveMap, "emissiveTexture", sampler)
            if (material.emissiveBase != black3) {
                attr("emissiveFactor")
                write(material.emissiveBase)
            }
            writeTextureProperty(material.normalMap, "normalTexture", sampler)
            writeTextureProperty(material.occlusionMap, "occlusionTexture", sampler)
        }
    }

    private fun writeTextureProperty(texture: FileReference, attrName: String, sampler: Int) {
        if (texture.exists) {
            attr(attrName)
            writeObject {
                attr("index")
                write(getTextureIndex(texture, sampler))
            }
        }
    }

    @Suppress("SpellCheckingInspection")
    private fun writeCamera(camera: Camera) {
        writeObject {
            attr("type")
            val type = if (camera.isPerspective) "perspective" else "orthographic"
            write(type)
            attr(type)
            writeObject {
                if (camera.isPerspective) {
                    attr("aspectRatio")
                    write(1f) // mmmh...
                    attr("yfov")
                    write(camera.fovY.toRadians())
                    attr("zfar")
                    write(camera.far)
                    attr("znear")
                    write(camera.near)
                } else {
                    attr("xmag")
                    write(camera.fovOrthographic)
                    attr("ymag")
                    write(camera.fovOrthographic)
                    attr("zfar")
                    write(camera.far)
                    attr("znear")
                    write(camera.near)
                }
            }
        }
    }

    private fun writeMesh(data: MeshData) {
        val (mesh, materialOverrides, animations) = data
        val hasSkeleton = animations.isNotEmpty()
        writeObject {

            mesh.ensureNorTanUVs()

            val pos = mesh.positions!!
            val bounds = mesh.getBounds()
            val posI = createPositionsView(pos, bounds)

            val normal = mesh.normals
            val norI = if (normal != null) createNormalsView(normal) else null

            val uv = mesh.uvs
            val uvI = if (uv != null) createUVView(uv) else null

            val color = mesh.color0
            val colorI = if (color != null) createColorView(color) else null

            val weights = mesh.boneWeights
            val joints = mesh.boneIndices
            val hasBones = weights != null && joints != null && hasSkeleton
            val weightsI = if (hasBones && weights != null) createWeightView(weights) else null
            val jointsI = if (hasBones && joints != null && weights != null) createJointView(joints, weights) else null

            attr("primitives")
            writeArray {
                fun writeMeshAttributes() {
                    attr("attributes")
                    writeObject {

                        attr("POSITION")
                        write(posI)

                        if (norI != null) {
                            attr("NORMAL")
                            write(norI)
                        }

                        if (uvI != null) {
                            attr("TEXCOORD_0")
                            write(uvI)
                        }

                        if (colorI != null) {
                            attr("COLOR_0")
                            write(colorI)
                        }

                        if (weightsI != null) {
                            attr("WEIGHTS_0")
                            write(weightsI)
                        }

                        if (jointsI != null) {
                            attr("JOINTS_0")
                            write(jointsI)
                        }

                    }
                }

                // for each material add a primitive
                val matIds = mesh.materialIds
                val ownHelpers = mesh.helperMeshes == null
                val helpers = if (matIds != null) {
                    if (ownHelpers) mesh.createHelperMeshes(matIds, false)
                    mesh.helperMeshes
                } else null

                fun getMaterial(i: Int): Material {
                    return Pipeline.getMaterial(materialOverrides, mesh.materials, i)
                }
                if (helpers != null) {
                    for ((i, helper) in helpers.withIndex()) {
                        helper ?: continue
                        val material = getMaterial(i)
                        val cullMode = mesh.cullMode * material.cullMode
                        writeMeshHelper(mesh.drawMode, helper, material, cullMode, ::writeMeshAttributes)
                    }
                    if (ownHelpers) {
                        // because they have no buffers
                        mesh.helperMeshes = null
                    }
                } else {
                    val material = getMaterial(0)
                    val cullMode = material.cullMode * material.cullMode
                    writeMesh1(mesh.drawMode, mesh.indices, material, cullMode, ::writeMeshAttributes)
                }
            }
        }
    }

    private fun writeMesh1(
        mode: DrawMode,
        indices: IntArray?,
        material: Material,
        cullMode: CullMode,
        writeMeshAttributes: () -> Unit
    ) {
        writeObject {
            attr("mode")
            write(mode.id) // triangles / triangle-strip, ...

            attr("material")
            write(materials.nextId(MaterialData(material, cullMode)))

            if (indices != null) {
                attr("indices")
                write(createIndicesView(indices, mode, cullMode))
            }

            writeMeshAttributes()
        }
    }

    private fun writeMeshHelper(
        mode: DrawMode,
        helper: Mesh.HelperMesh,
        material: Material?,
        cullMode: CullMode,
        writeMeshAttributes: () -> Unit
    ) {
        writeObject {
            attr("mode")
            write(mode.id) // triangles, triangle-strip, ...

            if (material != null) {
                attr("material")
                write(materials.nextId(MaterialData(material, cullMode)))
            }

            val indices = helper.indices
            attr("indices")
            write(createIndicesView(indices, mode, cullMode))

            writeMeshAttributes()
        }
    }

    private fun writeTexture(texture: IntPair) {
        val (source, sampler) = texture
        writeObject {
            attr("source")
            write(source)
            attr("sampler")
            write(sampler)
        }
    }

    private fun writeAccessor(acc: Accessor) {
        writeObject {
            attr("bufferView")
            write(acc.view)
            attr("type")
            write(acc.type)
            attr("componentType")
            write(acc.componentType)
            attr("count")
            write(acc.count)
            if (acc.normalized) {
                attr("normalized")
                write(true)
            }
            if (acc.min != null && acc.max != null) {
                attr("min")
                copyRaw(acc.min)
                attr("max")
                copyRaw(acc.max)
            }
        }
    }

    private fun writeImage(src: FileReference) {
        writeObject {
            val dstParent = dstParent
            // if contains inaccessible assets, pack them, or write them to same directory
            val sameFolder = src.getParent() == dstParent
            val packed = src is InnerFile
            if ((packed && packedDepsToFolder) || (!sameFolder && allDepsToFolder)) {
                // copy the file
                val newFile = dstParent.getChild(src.name)
                newFile.writeFile(src) {}
                writeURI(newFile.absolutePath)
            } else {
                val path = src.relativePathTo(dstParent, maxNumBackPaths)
                if ((packed && packedDepsToBinary) || (!sameFolder && allDepsToBinary) || path == null) {
                    appendFile(src)
                } else {
                    writeURI(path)
                }
            }
        }
    }

    private fun writeURI(uri: String) {
        attr("uri")
        write(uri)
    }

    private fun appendFile(src: FileReference) {
        // "bufferView": 3,
        // "mimeType" : "image/jpeg"
        attr("bufferView")
        write(bufferViews.size)
        val pos0 = binary.size()
        src.inputStreamSync().copyTo(binary) // must be sync, or we'd need to unpack this loop
        val pos1 = binary.size()
        bufferViews.add(BufferView(pos0, pos1 - pos0, 0, 0))
        val ext = when (Signature.findNameSync(src)) {
            "png" -> "image/png"
            "jpg" -> "image/jpeg"
            else -> null
        }
        if (ext != null) {
            attr("mimeType")
            write(ext)
        }
    }

    private fun writeMeshCompAttributes(node: MeshComponent) {
        val name = node.name
        if (name.isNotEmpty()) {
            attr("name")
            write(name)
        }
        val mesh = node.getMesh()
        if (mesh != null) {
            attr("mesh")
            write(meshes.nextId(getMeshData(node, mesh)))
            val skin = meshCompToSkin[node]
            if (skin != null) {
                attr("skin")
                write(skin)
            }
        }
    }

    private fun writeCameraAttributes(node: Camera) {
        val name = node.name
        if (name.isNotEmpty()) {
            attr("name")
            write(name)
        }
        attr("camera")
        write(cameras.nextId(node))
    }

    private fun writeLightAttributes(node: LightComponent) {
        val name = node.name
        if (name.isNotEmpty()) {
            attr("name")
            write(name)
        }
        val id = lights.nextId(node)
        attr("extensions")
        writeObject {
            attr("KHR_lights_punctual")
            writeObject {
                attr("light")
                write(id)
            }
        }
    }

    private fun writeBoneAttributes(bone: Bone) {
        val m = bone.relativeTransform // correct??
        attr("name")
        write(bone.name)
        attr("translation")
        write(m.getTranslation(Vector3f()))
        attr("rotation")
        write(m.getUnnormalizedRotation(Quaternionf()))
        val sc = m.getScale(Vector3f())
        if (sc.distanceSquared(1f, 1f, 1f) > 1e-3f) {
            attr("scale")
            write(sc)
        }
    }

    private fun writeNode(i: Int, node: Any) {
        writeObject {
            when (node) {
                is Entity -> writeEntityAttributes(node)
                is MeshComponent -> writeMeshCompAttributes(node)
                is Camera -> writeCameraAttributes(node)
                is LightComponent -> writeLightAttributes(node)
                is Bone -> writeBoneAttributes(node)
                else -> LOGGER.warn("Unknown node type $node")
            }

            val childrenI = children.getOrNull(i)
            if (childrenI != null && !childrenI.isEmpty()) {
                attr("children")
                writeArrayByIndices(0, childrenI.size) {
                    write(childrenI[it])
                }
            }
        }
    }

    private fun writeScenes() {
        attr("scenes")
        writeArray {
            writeObject {  // scenes[0]
                attr("nodes")
                writeArray {
                    write(0) // only root nodes
                }
            }
        }
    }

    private fun writeHeader() {
        attr("asset")
        writeObject {
            attr("generator")
            write("Rem's Engine")
            attr("version")
            write("2.0")
        }
    }

    private fun writeBuffers() {
        attr("buffers")
        writeArray {
            writeObject {
                attr("byteLength")
                write(binary.size())
            }
        }
    }

    private fun writeSceneIndex() {
        attr("scene")
        write(0)
    }

    private fun getMeshData(scene: MeshComponentBase, mesh: Mesh): MeshData {
        val animations = if (scene is AnimMeshComponent && SkeletonCache[scene.skeleton] != null) {
            scene.animations.map { it.source }.filter { it.exists }
        } else emptyList()
        return MeshData(mesh, scene.materials, animations)
    }

    private fun defineSceneHierarchy(scene: Saveable, callback: (Exception?) -> Unit) {
        when (scene) {
            is Prefab -> defineSceneHierarchy(scene.getSampleInstance(), callback)
            is Entity -> {
                scene.validateTransform()
                addEntity(scene)
                callback(null)
            }
            is Mesh -> {
                nodes.add(MeshComponent(scene))
                meshes[MeshData(scene, emptyList(), emptyList())] = 0
                callback(null)
            }
            is MeshComponentBase -> {
                val mesh = scene.getMesh()
                if (mesh is Mesh) {
                    nodes.add(scene)
                    meshes[getMeshData(scene, mesh)] = 0
                    callback(null)
                } else callback(IllegalArgumentException("Missing mesh"))
            }
            is Camera -> {
                nodes.add(scene)
                cameras[scene] = 0
                callback(null)
            }
            is LightComponent -> {
                if (supportsLight(scene)) {
                    nodes.add(scene)
                    lights[scene] = 0
                    callback(null)
                } else callback(warnUnsupportedType(scene))
            }
            else -> callback(warnUnsupportedType(scene))
        }
    }

    private fun warnUnsupportedType(scene: Saveable): Exception {
        return IllegalArgumentException("Unsupported type ${scene.className}")
    }

    fun write(scene: Saveable, dst: FileReference) {
        defineSceneHierarchy(scene) { err ->
            if (err == null) {
                writeAll(dst)
            } else err.printStackTrace()
        }
    }

    private fun writeExtensions() {
        if (lights.isEmpty()) return // no extensions needed
        attr("extensions")
        writeObject {
            attr("KHR_lights_punctual")
            writeLights()
        }
    }

    private fun writeLights() {
        writeObject {
            writeArray("lights", lights, ::writeLight)
        }
    }

    /**
     * docs: https://github.com/KhronosGroup/glTF/blob/main/extensions/2.0/Khronos/KHR_lights_punctual/README.md
     * */
    private fun writeLight(light: LightComponent) {
        writeObject {
            val color = light.color.toLinear(Vector3f())
            val intensity = max(1e-38f, color.max())
            color.div(intensity)
            attr("color")
            write(color)
            attr("intensity")
            write(intensity)
            val type = when (light) {
                is DirectionalLight -> "directional"
                is PointLight -> "point"
                is SpotLight -> "spot"
                else -> "?"
            }
            attr("type")
            write(type)
            when (light) {
                is SpotLight -> {
                    attr("outerConeAngle")
                    write(atan(light.coneAngle))
                    writeLightRange()
                }
                is PointLight -> {
                    writeLightRange()
                }
                // directional light doesn't support cutoff in GLTF
            }
        }
    }

    private fun writeLightRange() {
        attr("range")
        write(1f)
    }

    private fun writeAnimationChannel(node: Int, name: String, sampler: Int) {
        writeObject {
            attr("target")
            writeObject {
                attr("node")
                write(node)
                attr("path")
                write(name)
            }
            attr("sampler")
            write(sampler)
        }
    }

    private fun writeAnimation(animData: AnimationData) {
        writeObject {
            // we could write one shared buffer for all data...
            // -> that's error-prone, so let's just be safe
            val skeleton = animData.skeleton
            val bones = skeleton.bones
            val baseId = animData.baseId
            val animation = animData.animation
            val frameTimes = FloatArray(animation.numFrames)
            val dt = animation.duration / frameTimes.size
            for (i in frameTimes.indices) {
                frameTimes[i] = dt * i
            }
            val inputAccessor = createVectorView(frameTimes, "SCALAR", 1, 0)
            val accessor0 = accessors.size
            var ci = 0
            attr("channels")
            writeArray {
                val tmp = Matrix4x3f()
                val matrices = frameTimes.indices.map { frameIndex ->
                    val result = createArrayList(bones.size) { Matrix4x3f() }
                    // todo this matrix or the inverse-bind-matrices are incorrect
                    //  -> we need the local position, rotation and scale, not the global offset
                    //  -> inverse of AnimationLoader.readAnimationFrame
                    // skinning = (parent * local) * binePose^-1 | we want local
                    // -> skinning * bindPose = (parent * local)
                    val skinning = animation.getMappedMatrices(frameIndex, result, skeleton.ref)!!
                    for (boneId in bones.indices) {
                        val bone = bones[boneId]
                        val dst = skinning[boneId].mul(bone.bindPose, result[boneId])
                        if (bone.parentId in bones.indices) {
                            result[bone.parentId].invert(tmp).mul(dst, dst)
                        }
                    }
                    result
                }
                val tmp3 = Vector3f()
                val tmp4 = Quaternionf()
                for (boneId in bones.indices) {
                    val pos = FloatArray(frameTimes.size * 3)
                    val rot = FloatArray(frameTimes.size * 4)
                    val sca = FloatArray(frameTimes.size * 3)
                    for (frameIndex in frameTimes.indices) {
                        val matrix = matrices[frameIndex][boneId]
                        matrix.getTranslation(tmp3).get(pos, frameIndex * 3)
                        matrix.getUnnormalizedRotation(tmp4).get(rot, frameIndex * 4)
                        matrix.getScale(tmp3).get(sca, frameIndex * 3)
                    }
                    createVectorView(pos, "VEC3", 3, 0)
                    createVectorView(rot, "VEC4", 4, 0)
                    createVectorView(sca, "VEC3", 3, 0)
                    val node = boneId + baseId
                    writeAnimationChannel(node, "translation", ci++)
                    writeAnimationChannel(node, "rotation", ci++)
                    writeAnimationChannel(node, "scale", ci++)
                }
            }
            attr("samplers")
            writeArray { // lots of repetitive data :/
                for (i in 0 until ci) {
                    writeObject {
                        attr("input")
                        write(inputAccessor)
                        attr("interpolation")
                        write("LINEAR") // ok?, or should be use cubic?
                        attr("output")
                        write(accessor0 + i)
                    }
                }
            }
        }
    }

    fun <V> writeArray(name: String, elements: List<V>, writeElement: (V) -> Unit) {
        if (elements.isNotEmpty()) {
            attr(name)
            writeArray(elements, writeElement)
        }
    }

    private var dstParent: FileReference = InvalidRef
    private fun writeAll(dst: FileReference) {
        dstParent = dst.getParent()
        writeObject {
            writeHeader()
            writeSceneIndex()
            writeScenes()
            attr("nodes")
            writeArrayIndexed(nodes, ::writeNode)
            writeArray("cameras", cameras, ::writeCamera)
            writeArray("meshes", meshes, ::writeMesh)
            writeArray("materials", materials, ::writeMaterial)
            writeArray("textures", textures, ::writeTexture)
            writeArray("samplers", samplers, ::writeSampler)
            writeArray("skins", skins, ::writeSkin)
            writeArray("animations", animations, ::writeAnimation)
            writeArray("images", images, ::writeImage)
            writeArray("accessors", accessors, ::writeAccessor)
            writeArray("bufferViews", bufferViews, ::writeBufferView)
            writeBuffers()
            writeExtensions()
        }
        finish()
        writeChunks(dst)
    }

    private fun ensureAlignment(bos: ByteArrayOutputStream, paddingByte: Int) {
        while (bos.size().and(3) != 0) {
            bos.write(paddingByte)
        }
    }

    private fun writeChunks(dst: FileReference) {

        ensureAlignment(json, ' '.code)
        ensureAlignment(binary, 0)

        val version = 2
        val totalFileSize = 12 + 8 + 8 + json.size() + binary.size()

        val out = dst.outputStream()
        // header
        out.writeString("glTF")
        out.writeLE32(version)
        out.writeLE32(totalFileSize)
        // chunks
        out.writeChunk(json, "JSON")
        out.writeChunk(binary, "BIN${0.toChar()}")
        out.close()
    }

    private fun OutputStream.writeChunk(data: ByteArrayOutputStream, type: String) {
        writeLE32(data.size())
        writeString(type)
        data.writeTo(this)
    }
}