package me.anno.mesh.blender

import me.anno.ecs.components.anim.Bone
import me.anno.ecs.prefab.Prefab
import me.anno.ecs.prefab.change.Path
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import me.anno.io.files.Signature
import me.anno.io.zip.InnerFolder
import me.anno.io.zip.InnerFolderCallback
import me.anno.io.zip.InnerTmpFile
import me.anno.maths.Maths.sq
import me.anno.mesh.blender.BlenderMeshConverter.convertBMesh
import me.anno.mesh.blender.impl.*
import me.anno.utils.Clock
import me.anno.utils.structures.lists.Lists.firstInstanceOrNull
import org.apache.logging.log4j.LogManager
import org.joml.*
import java.nio.ByteBuffer
import kotlin.math.PI
import kotlin.math.max

/**
 * extract the relevant information from a blender file:
 *  - done meshes
 *  - todo skeletons
 *  - todo animations
 *  - todo vertex weights
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
                callback(readAsFolder(ref, it), null)
            } else callback(null, exc)
        }
    }

    private fun readImages(file: BlenderFile, folder: InnerFolder, clock: Clock) {
        val imagesInFile = file.instances["Image"] ?: return
        val texFolder = folder.createChild("textures", null) as InnerFolder
        for (image in imagesInFile) {
            image as BImage
            val rawImageData = image.packedFiles.first?.packedFile?.data
            if (rawImageData != null) {
                val name = image.id.realName
                val newName = if ('.' in name) name else {
                    val signature = Signature.findName(rawImageData)
                    if (signature != null) "${name}.${signature}"
                    else name
                }
                image.reference = texFolder.createByteChild(newName, rawImageData)
            } else if (image.name.isNotBlank()) {
                // prefer external files, if they exist?
                image.reference = file.folder.getChild(image.name)
            }
        }
        clock.stop("reading images")
    }

    private fun readMaterials(file: BlenderFile, folder: InnerFolder, clock: Clock) {
        val materialsInFile = file.instances["Material"] ?: return
        val matFolder = folder.createChild("materials", null) as InnerFolder
        for (i in materialsInFile.indices) {
            val mat = materialsInFile[i] as BMaterial
            val prefab = Prefab("Material")
            BlenderMaterialConverter.defineDefaultMaterial(prefab, mat)
            val name = mat.id.realName
            prefab.sealFromModifications()
            mat.fileRef = matFolder.createPrefabChild("$name.json", prefab)
        }
        clock.stop("read ${file.instances["Material"]?.size} materials")
    }

    private fun readMeshes(file: BlenderFile, folder: InnerFolder, clock: Clock) {
        if ("Mesh" !in file.instances) return
        val meshFolder = folder.createChild("meshes", null) as InnerFolder
        for (mesh in file.instances["Mesh"]!!) {
            mesh as BMesh
            val name = mesh.id.realName
            val prefab = convertBMesh(mesh) ?: continue
            mesh.fileRef = meshFolder.createPrefabChild("$name.json", prefab)
        }
        clock.stop("read meshes")
    }

    private fun extractHierarchy(file: BlenderFile): Prefab {
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
                    createObject(prefab, bObject, path, false)
                }
                if (postTransform) {
                    prefab[Path.ROOT_PATH, "rotation"] = Quaterniond().rotateX(-PI / 2)
                }
            } else {
                // there must be a root
                paths[roots.first()] = Path.ROOT_PATH
                createObject(prefab, roots.first(), Path.ROOT_PATH, true)
            }

            for (obj in objects) {
                obj as BObject
                makeObject(prefab, obj, paths)
            }
        }
        return prefab
    }

    fun readAsFolder(ref: FileReference, nio: ByteBuffer): InnerFolder {

        // todo 1: find equivalent meshes, and replace them for speed
        // todo 2: read positions, indices, and normals without instantiation

        // transform: +x, +z, -y
        // because we want y up, and Blender has z up

        val clock = Clock()
        clock.stop("read bytes")

        clock.stop("put into other array")
        val binaryFile = BinaryFile(nio)
        val folder = InnerFolder(ref)
        val file = BlenderFile(binaryFile, ref.getParent() ?: InvalidRef)
        clock.stop("read blender file")
        // data.printTypes()

        readImages(file, folder, clock)
        readMaterials(file, folder, clock)
        readMeshes(file, folder, clock)

        val prefab = extractHierarchy(file)
        prefab.sealFromModifications()
        folder.createPrefabChild("Scene.json", prefab)
        clock.stop("read hierarchy")
        return folder
    }

    fun makeObject(prefab: Prefab, obj: BObject, paths: HashMap<BObject, Path>): Path {
        return paths.getOrPut(obj) {
            val name = obj.id.realName
            val parent = makeObject(prefab, obj.parent!!, paths)
            val childIndex = prefab.adds.count { it.path == parent && it.type == 'e' }
            val path = Path(parent, name, childIndex, 'e')
            createObject(prefab, obj, path, false)
            path
        }
    }

    fun createSkeleton(armature: BArmature): Prefab {
        val prefab = Prefab("Skeleton")
        val blenderBones = ArrayList<BBone>()
        val boneToIndex = HashMap<BBone, Int>()
        fun index(bone: BBone) {
            blenderBones.add(bone)
            boneToIndex[bone] = boneToIndex.size
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
            val parentIndex = boneToIndex[bone.parent] ?: -1
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
        return prefab
    }

    fun createObject(prefab: Prefab, obj: BObject, path: Path, isRoot: Boolean) {
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
        val rotation = localMatrix.getUnnormalizedRotation(Quaterniond())
        if (!postTransform) rotation.set(rotation.x, rotation.z, -rotation.y, rotation.w)
        if (isRoot && postTransform) rotation.rotateLocalX(-PI / 2)
        if (rotation.w != 1.0)
            prefab.setUnsafe(path, "rotation", rotation)
        val scale = localMatrix.getScale(Vector3d())
        if (!postTransform) scale.set(scale.x, scale.z, -scale.y)
        if (scale.x != 1.0 || scale.y != 1.0 || scale.z != 1.0)
            prefab.setUnsafe(path, "scale", scale)
        when (BObject.objectTypeById[obj.type.toInt()]) {
            BObject.BObjectType.OB_EMPTY -> { // done
            }
            BObject.BObjectType.OB_MESH -> {
                // add mesh component
                val armatureObject = obj.modifiers
                    .firstInstanceOrNull<BArmatureModifierData>()
                    ?.armatureObject
                val armatureModifier = armatureObject?.data as? BArmature
                // todo find all animations
                if (armatureModifier != null) {
                    val skeleton = createSkeleton(armatureModifier)
                    val c = prefab.add(path, 'c', "AnimRenderer", obj.id.realName)
                    // todo create proper location for skeleton
                    prefab[c, "meshFile"] = (obj.data as BMesh).fileRef
                    prefab[c, "skeleton"] = InnerTmpFile.InnerTmpPrefabFile(skeleton)
                    LOGGER.debug("Armature Pose: {}", armatureObject.pose)
                    LOGGER.debug("Armature Action: {}", armatureObject.action)
                    LOGGER.debug("Object Action: {}", obj.action)
                } else {
                    val c = prefab.add(path, 'c', "MeshComponent", obj.id.realName)
                    prefab[c, "meshFile"] = (obj.data as BMesh).fileRef
                }
                LOGGER.debug("Modifiers for mesh {}/{}: {}", obj.id.realName, path.nameId, obj.modifiers)
                // materials would be nice... but somehow they are always null
            }
            BObject.BObjectType.OB_CAMERA -> {
                val cam = obj.data as? BCamera
                if (cam != null) {
                    val c = prefab.add(path, 'c', "Camera", obj.id.realName)
                    prefab.setUnsafe(c, "near", cam.near.toDouble())
                    prefab.setUnsafe(c, "far", cam.far.toDouble())
                }
            }
            BObject.BObjectType.OB_LAMP -> {
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
                        prefab.setUnsafe(c, "autoUpdate", false)
                    }
                } else LOGGER.warn("obj.data of a lamp was not a lamp: ${obj.data?.run { this::class.simpleName }}")
            }
            BObject.BObjectType.OB_ARMATURE -> {
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