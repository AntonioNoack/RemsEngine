package me.anno.mesh.blender

import me.anno.animation.LoopingState
import me.anno.ecs.components.anim.Animation
import me.anno.ecs.components.anim.AnimationState
import me.anno.ecs.components.anim.Bone
import me.anno.ecs.components.anim.BoneByBoneAnimation
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.prefab.Prefab
import me.anno.ecs.prefab.PrefabReadable
import me.anno.ecs.prefab.change.Path
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import me.anno.io.files.Signature
import me.anno.io.files.inner.InnerFolder
import me.anno.io.files.inner.InnerFolderCallback
import me.anno.io.files.inner.temporary.InnerTmpPrefabFile
import me.anno.maths.Maths.PIf
import me.anno.maths.Maths.sq
import me.anno.mesh.Shapes.flatCube
import me.anno.mesh.blender.BlenderMeshConverter.convertBMesh
import me.anno.mesh.blender.impl.BAction
import me.anno.mesh.blender.impl.BArmature
import me.anno.mesh.blender.impl.BArmatureModifierData
import me.anno.mesh.blender.impl.BBone
import me.anno.mesh.blender.impl.BCamera
import me.anno.mesh.blender.impl.BImage
import me.anno.mesh.blender.impl.BLamp
import me.anno.mesh.blender.impl.BMaterial
import me.anno.mesh.blender.impl.BMesh
import me.anno.mesh.blender.impl.BObject
import me.anno.mesh.blender.impl.BObjectType
import me.anno.mesh.blender.impl.BScene
import me.anno.utils.Clock
import me.anno.utils.structures.lists.Lists.castToList
import me.anno.utils.structures.lists.Lists.firstInstanceOrNull
import me.anno.utils.types.Strings.isNotBlank2
import org.apache.logging.log4j.LogManager
import org.joml.Matrix4f
import org.joml.Quaterniond
import org.joml.Quaternionf
import org.joml.Vector3d
import org.joml.Vector3f
import java.nio.ByteBuffer
import kotlin.math.PI
import kotlin.math.max
import kotlin.math.sqrt

/**
 * extract the relevant information from a blender file:
 *  - done meshes
 *  - done skeletons
 *  - done animations
 *  - done vertex weights
 *  - done embedded textures
 *  - done materials
 *  - done scene hierarchy
 * create a test scene with different layouts, and check that everything is in the right place
 * */
object BlenderReader {

    // postTransform=false may not be setting positions / translations correctly
    const val postTransform = true

    private val LOGGER = LogManager.getLogger(BlenderReader::class)

    fun readAsFolder(ref: FileReference, callback: InnerFolderCallback) {
        ref.readByteBuffer(false) { it, exc ->
            if (it != null) {
                callback.ok(readAsFolder(ref, it))
            } else callback.err(exc)
        }
    }

    private fun readImages(file: BlenderFile, folder: InnerFolder, clock: Clock): Map<String, FileReference> {
        val instances = file.instances["Image"] ?: return emptyMap()
        val texFolder = folder.createChild("textures", null) as InnerFolder
        val resultMap = HashMap<String, FileReference>()
        for (i in instances.indices) {
            val image = instances[i] as BImage
            val rawImageData = image.packedFiles?.first?.packedFile?.data
            val reference = if (rawImageData != null) {
                val name = image.id.realName
                val newName = if ('.' in name) name else {
                    val signature = Signature.findName(rawImageData)
                    if (signature != null) "${name}.${signature}"
                    else name
                }
                texFolder.createByteChild(newName, rawImageData)
            } else if (image.name.isNotBlank2()) {
                // prefer external files, if they exist?
                file.folder.getChild(image.name)
            } else InvalidRef
            resultMap[image.id.realName] = reference
        }
        clock.stop("reading images")
        return resultMap
    }

    private fun readMaterials(
        file: BlenderFile,
        folder: InnerFolder,
        clock: Clock,
        imageMap: Map<String, FileReference>
    ) {
        val instances = file.instances["Material"] ?: return
        val matFolder = folder.createChild("materials", null) as InnerFolder
        for (i in instances.indices) {
            val mat = instances[i] as BMaterial
            val prefab = Prefab("Material")
            BlenderMaterialConverter.defineDefaultMaterial(prefab, mat, imageMap)
            val name = mat.id.realName
            prefab.sealFromModifications()
            mat.fileRef = matFolder.createPrefabChild("$name.json", prefab)
        }
        clock.stop("read ${file.instances["Material"]?.size} materials")
    }

    private val missingMeshPrefab: Mesh
        get() {
            val mesh = flatCube.front
            mesh.ref // ensure prefab
            return mesh
        }

    private fun readMeshes(file: BlenderFile, folder: InnerFolder, clock: Clock): InnerFolder {
        val meshFolder = folder.createChild("meshes", null) as InnerFolder
        val instances = file.instances["Mesh"] ?: emptyList()
        for (i in instances.indices) {
            val mesh = instances[i] as BMesh
            val name = mesh.id.realName
            val prefab = convertBMesh(mesh) ?: missingMeshPrefab.prefab!!
            mesh.fileRef = meshFolder.createPrefabChild("$name.json", prefab)
        }
        clock.stop("read ${instances.size} meshes")
        return meshFolder
    }

    private fun readAnimation(
        action: BAction,
        givenBones: List<Bone>,
        skeleton: FileReference,
        fps: Float
    ): Animation? {
        val curves = action.curves // animated values
        if (curves.any { it.path.startsWith("pose.bones[\"") }) {

            val numFrames = (curves.maxOfOrNull { it.lastKeyframeIndex } ?: 0f).toInt() + 1
            val curveByName = curves.associateBy { it.path to it.arrayIndex }

            val numBones = givenBones.size
            val translations = FloatArray(numFrames * numBones * 3)
            val rotations = FloatArray(numFrames * numBones * 4)

            for (i in rotations.indices step 4) {
                rotations[i + 3] = 1f
            }

            val tmp = Vector3f()
            for (boneIdx in givenBones.indices) {
                val bone = givenBones[boneIdx]
                val boneName = bone.name
                val keyT = "pose.bones[\"$boneName\"].location"
                if ((keyT to 0) in curveByName) {
                    val x = curveByName[keyT to 0]!!
                    val y = curveByName[keyT to 1]!!
                    val z = curveByName[keyT to 2]!!
                    var idx = boneIdx * 3
                    for (fi in 0 until numFrames) {
                        val kf = fi.toFloat()
                        // todo is translation missing scale? the pelvis isn't moving
                        translations[idx] = x.getValueAt(kf)
                        translations[idx + 1] = y.getValueAt(kf)
                        translations[idx + 2] = z.getValueAt(kf)
                        idx += numBones * 3
                    }
                }
                val keyR = "pose.bones[\"$boneName\"].rotation_quaternion"
                if ((keyR to 0) in curveByName) {
                    val w = curveByName[keyR to 0]!!
                    val x = curveByName[keyR to 1]!!
                    val y = curveByName[keyR to 2]!!
                    val z = curveByName[keyR to 3]!!
                    var idx = boneIdx * 4
                    for (fi in 0 until numFrames) {
                        val kf = fi.toFloat()
                        // some things are nearly too easy ^^
                        tmp.set(x.getValueAt(kf), y.getValueAt(kf), z.getValueAt(kf))
                        bone.bindPose.transformDirection(tmp)
                        tmp.normalize(tmp.length())
                        val wf = w.getValueAt(kf)
                        val invLen = 1f / sqrt(tmp.lengthSquared() + wf * wf)
                        rotations[idx] = tmp.x * invLen
                        rotations[idx + 1] = tmp.y * invLen
                        rotations[idx + 2] = tmp.z * invLen
                        rotations[idx + 3] = wf * invLen
                        idx += numBones * 4
                    }
                }
            }

            val animation = BoneByBoneAnimation()
            animation.translations = translations
            animation.rotations = rotations
            animation.boneCount = givenBones.size
            animation.frameCount = numFrames
            animation.skeleton = skeleton
            animation.duration = numFrames / fps
            return animation
        } else return null
        // LOGGER.debug("Action[{}]: {}", i, action)
    }

    private fun extractHierarchy(file: BlenderFile, fps: Float, meshes: InnerFolder): Prefab {
        val prefab = Prefab("Entity")
        if ("Object" in file.instances) {

            val objects = file.instances["Object"]!!
            val roots = ArrayList<BObject>()

            for (obj in objects) {
                obj as BObject
                val parent = obj.parent
                if (parent == null) roots.add(obj)
            }

            val paths = HashMap<BObject, Path>()
            if (roots.size > 1) {
                // add a pseudo root
                for (index in roots.indices) {
                    val bObject = roots[index]
                    val name = bObject.id.realName
                    val path = Path(Path.ROOT_PATH, name, index, 'e')
                    paths[bObject] = path
                    createObject(prefab, bObject, path, false, fps)
                }
                if (postTransform) {
                    prefab[Path.ROOT_PATH, "rotation"] = Quaterniond().rotateX(-PI / 2)
                }
            } else {
                // there must be a root
                paths[roots.first()] = Path.ROOT_PATH
                createObject(prefab, roots.first(), Path.ROOT_PATH, true, fps)
            }

            for (obj in objects) {
                obj as BObject
                makeObject(prefab, obj, paths, fps, meshes)
            }
        }
        return prefab
    }

    fun readAsFolder(ref: FileReference, nio: ByteBuffer): InnerFolder {

        // todo 1: find equivalent meshes, and replace them for speed
        // todo 2: read positions, indices, and normals without instantiation

        // transform: +x, +z, -y
        // because we want y up, and Blender has z up

        val clock = Clock(LOGGER)
        clock.stop("read bytes")

        clock.stop("put into other array")
        val binaryFile = BinaryFile(nio)
        val folder = InnerFolder(ref)
        val file = BlenderFile(binaryFile, ref.getParent())
        clock.stop("read blender file")
        // file.printStructs()

        var fps = 30f
        for (scene in file.instances["Scene"] ?: emptyList()) {
            scene as BScene
            LOGGER.debug("Scene: {}", scene)
            val renderData = scene.renderData
            fps = renderData.frsSec / renderData.frsSecBase
        }

        val imageMap = readImages(file, folder, clock)
        readMaterials(file, folder, clock, imageMap)
        val meshes = readMeshes(file, folder, clock)
        // readAnimations(file, folder, clock)

        val prefab = extractHierarchy(file, fps, meshes)
        prefab.sealFromModifications()
        folder.createPrefabChild("Scene.json", prefab)
        clock.stop("read hierarchy")
        return folder
    }

    private fun makeObject(
        prefab: Prefab, obj: BObject, paths: HashMap<BObject, Path>, fps: Float,
        meshes: InnerFolder
    ): Path {
        return paths.getOrPut(obj) {
            val name = obj.id.realName
            val parent = makeObject(prefab, obj.parent!!, paths, fps, meshes)
            val childIndex = prefab.adds[parent]?.count { it.type == 'e' } ?: 0
            val path = Path(parent, name, childIndex, 'e')
            createObject(prefab, obj, path, false, fps)
            path
        }
    }

    private fun createSkeleton(
        armature: BArmature,
        vertexGroups: List<String>,
        srcBoneIndices: IntArray
    ): Pair<Prefab, ByteArray> {
        val prefab = Prefab("Skeleton")
        val blenderBones = ArrayList<BBone>()
        val boneToIndex = HashMap<String, Int>()
        fun index(bone: BBone) {
            val name = bone.name ?: return
            blenderBones.add(bone)
            boneToIndex[name] = boneToIndex.size
            for (child in bone.children) {
                index(child)
            }
        }
        for (bone in armature.bones) {
            index(bone)
        }
        if (blenderBones.size > 256) {
            LOGGER.warn("Cannot handle more than 256 bones")
        }
        prefab["bones"] = blenderBones.mapIndexed { index, bone ->
            val parentIndex = boneToIndex[bone.parent?.name] ?: -1
            val newBone = Bone(index, parentIndex, bone.name!!)
            val data = bone.restPose // todo is this the bind pose? (probably not)
            newBone.setBindPose(
                newBone.bindPose.set(
                    // this looks to be correct :)
                    data[0], data[1], data[2],
                    data[4], data[5], data[6],
                    data[8], data[9], data[10],
                    data[12], data[13], data[14]
                )
            )
            newBone
        }
        val mappedIndices = mapBoneIndices(vertexGroups, boneToIndex, srcBoneIndices)
        return Pair(prefab, mappedIndices)
    }

    private fun mapBoneIndices(
        vertexGroups: List<String>,
        boneToIndex: Map<String, Int>,
        vertexGroupIndices: IntArray
    ): ByteArray {
        val boneMapping = ByteArray(vertexGroups.size)
        for (vgIndex in boneMapping.indices) {
            val newBoneIndex = boneToIndex[vertexGroups[vgIndex]]
            // LOGGER.info("Mapping ${vertexGroups[vgIndex]} to $newBoneIndex")
            boneMapping[vgIndex] = (newBoneIndex ?: 0).toByte()
        }
        val dstBoneIndices = ByteArray(vertexGroupIndices.size)
        for (i in vertexGroupIndices.indices) {
            val srcIndex = vertexGroupIndices[i]
            if (srcIndex in boneMapping.indices) {
                dstBoneIndices[i] = boneMapping[srcIndex]
            } else LOGGER.warn("Index out of bounds: $srcIndex !in ${boneMapping.indices}")
        }
        return dstBoneIndices
    }

    fun createObject(prefab: Prefab, obj: BObject, path: Path, isRoot: Boolean, fps: Float) {
        if (path != Path.ROOT_PATH) {
            prefab.add(
                path.parent ?: Path.ROOT_PATH,
                path.type, "Entity", path.nameId
            )
        }
        // add position relative to parent
        // par * self = ws
        // -> (par)-1 * (par * self) = self
        val parentMatrix = obj.parent?.finalWSMatrix ?: Matrix4f()
        val localMatrix = Matrix4f(parentMatrix).invert().mul(obj.finalWSMatrix)
        // if(path == Path.ROOT_PATH) localMatrix.rotateX(-PI.toFloat() * 0.5f)
        val translation = localMatrix.getTranslation(Vector3d())
        if (!postTransform) translation.set(translation.x, translation.z, -translation.y)
        if (translation.x != 0.0 || translation.y != 0.0 || translation.z != 0.0)
            prefab.setUnsafe(path, "position", translation)
        val rotation = localMatrix.getUnnormalizedRotation(Quaternionf())
        if (!postTransform) rotation.set(rotation.x, rotation.z, -rotation.y, rotation.w)
        if (isRoot && postTransform) rotation.rotateLocalX(-PIf * 0.5f)
        if (rotation.w != 1f)
            prefab.setUnsafe(path, "rotation", Quaterniond(rotation))
        val scale = localMatrix.getScale(Vector3d())
        if (!postTransform) scale.set(scale.x, scale.z, -scale.y)
        if (scale.x != 1.0 || scale.y != 1.0 || scale.z != 1.0)
            prefab.setUnsafe(path, "scale", scale)
        val typeId = obj.type.toInt()
        when (BObjectType.entries.firstOrNull { it.id == typeId }) {
            BObjectType.OB_EMPTY -> { // done
            }
            BObjectType.OB_MESH -> {
                // add mesh component
                val armatureObject = obj.modifiers
                    .firstInstanceOrNull(BArmatureModifierData::class)
                    ?.armatureObject
                val armatureModifier = armatureObject?.data as? BArmature
                // todo find all animations
                val blenderMesh = obj.data as BMesh
                val meshFile = blenderMesh.fileRef
                if (meshFile !is PrefabReadable) {
                    LOGGER.warn("${path.nameId} wasn't found")
                    return
                }
                val meshPrefab = meshFile.readPrefab()
                val boneIndices = meshPrefab["rawBoneIndices"] as? IntArray
                if (armatureModifier != null && boneIndices != null) {

                    // todo proper file for this
                    val subPrefab = Prefab("Mesh", meshFile)
                    val meshFile2 = InnerTmpPrefabFile(subPrefab)

                    // create skeleton and map vertex indices
                    val vertexGroups = blenderMesh.vertexGroupNames?.map { it.name ?: "" }
                    if (vertexGroups != null) {
                        val (skeleton, mappedBoneIndices) = createSkeleton(armatureModifier, vertexGroups, boneIndices)
                        subPrefab["boneIndices"] = mappedBoneIndices

                        // todo create proper location for skeleton
                        val skeletonRef = InnerTmpPrefabFile(skeleton)
                        val c = prefab.add(path, 'c', "AnimMeshComponent", obj.id.realName)

                        prefab[c, "meshFile"] = meshFile2
                        meshPrefab[c, "skeleton"] = skeletonRef
                        if (LOGGER.isDebugEnabled()) {
                            LOGGER.debug("Armature Pose: {}", armatureObject.pose)
                            LOGGER.debug("Armature Action: {}", armatureObject.action)
                            LOGGER.debug("Object Action: {}", obj.action)
                            LOGGER.debug("Object AnimData: {}", obj.animData)
                            LOGGER.debug("Armature AnimData: {}", armatureObject.animData) // this is defined :3
                        }
                        val action =
                            armatureObject.animData?.action ?: obj.animData?.action // obj.animData just in case
                        if (action != null) {
                            val animation =
                                readAnimation(action, skeleton["bones"].castToList(Bone::class), skeletonRef, fps)
                            if (animation != null) {
                                val animState = AnimationState(animation.ref, 1f, 0f, 1f, LoopingState.PLAY_LOOP)
                                prefab[c, "animations"] = listOf(animState)
                            }
                        }
                    } else {
                        LOGGER.warn("Vertex groups were null :/")
                        val c = prefab.add(path, 'c', "MeshComponent", obj.id.realName)
                        prefab[c, "meshFile"] = meshFile
                    }
                } else {
                    val c = prefab.add(path, 'c', "MeshComponent", obj.id.realName)
                    prefab[c, "meshFile"] = meshFile
                }
                LOGGER.debug("Modifiers for mesh {}/{}: {}", obj.id.realName, path.nameId, obj.modifiers)
                // materials would be nice... but somehow they are always null
            }
            BObjectType.OB_CAMERA -> {
                val cam = obj.data as? BCamera
                if (cam != null) {
                    val c = prefab.add(path, 'c', "Camera", obj.id.realName)
                    prefab.setUnsafe(c, "near", cam.near.toDouble())
                    prefab.setUnsafe(c, "far", cam.far.toDouble())
                }
            }
            BObjectType.OB_LAMP -> {
                val light = obj.data as? BLamp
                if (light != null) {
                    val name = obj.id.realName
                    val path1 = prefab.add(path, 'e', "Entity", name)
                    val extraSize = Vector3d(1.0)
                    val c = when (light.type) {
                        0 -> {
                            extraSize.set(light.pointRadius.toDouble())
                            prefab.add(path1, 'c', "PointLight", name)
                        }
                        1 -> { // sun
                            prefab.add(path1, 'c', "DirectionalLight", name)
                        }
                        2 -> {
                            extraSize.set(light.spotRadius.toDouble())
                            prefab.add(path1, 'c', "SpotLight", name)
                        }
                        // AreaLight
                        4 -> {
                            when (light.areaShape.toInt()) {
                                0 -> {
                                    // square
                                    extraSize.set(light.areaSizeX * 10.0)
                                    prefab.add(path1, 'c', "RectangleLight", name).apply {
                                        prefab.setUnsafe(this, "width", 0.1f)
                                        prefab.setUnsafe(this, "height", 0.1f)
                                    }
                                }
                                1 -> {
                                    // rectangle
                                    val w = light.areaSizeX
                                    val h = light.areaSizeY
                                    extraSize.set(max(w, h) * 10.0)
                                    prefab.add(path1, 'c', "RectangleLight", name).apply {
                                        prefab.setUnsafe(this, "width", 0.1f * w / max(w, h))
                                        prefab.setUnsafe(this, "height", 0.1f * h / max(w, h))
                                    }
                                }
                                4 -> {
                                    // circle
                                    extraSize.set(light.areaSizeX * 10.0)
                                    prefab.add(path1, 'c', "CircleLight", name).apply {
                                        prefab.setUnsafe(this, "radius", 0.1f)
                                    }
                                }
                                5 -> {
                                    // ellipse
                                    val w = light.areaSizeX * 10.0
                                    val h = light.areaSizeY * 10.0
                                    extraSize.set(w, max(w, h), h)
                                    prefab.add(path1, 'c', "CircleLight", name).apply {
                                        prefab.setUnsafe(this, "radius", 0.1f)
                                    }
                                }
                                else -> null
                            }
                        }
                        else -> null // deprecated or not supported
                    }
                    prefab[path1, "scale"] = extraSize
                    if (c != null) {
                        // scale energy by 1/scale², because we follow the 1/distance² law in light-local space
                        val e = light.energy * 0.01f / sq(max(extraSize.x, extraSize.z).toFloat())
                        prefab.setUnsafe(c, "color", Vector3f(light.r, light.g, light.b).mul(e))
                        prefab.setUnsafe(c, "shadowMapCascades", light.cascadeCount)
                        prefab.setUnsafe(c, "shadowMapPower", light.cascadeExponent.toDouble())
                        prefab.setUnsafe(c, "autoUpdate", 0)
                    }
                } else LOGGER.warn("obj.data of a lamp was not a lamp: ${obj.data?.run { this::class.simpleName }}")
            }
            BObjectType.OB_ARMATURE -> {
                val armature = obj.data as BArmature
                LOGGER.debug("Found armature, {}", armature)
                LOGGER.debug(armature.bones)
            }
            // todo volumes?
            // todo curves?
            else -> {
                // nothing to do
            }
        }
    }
}