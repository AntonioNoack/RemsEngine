package me.anno.engine

import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.material.Material
import me.anno.ecs.components.mesh.material.Material.Companion.defaultMaterial
import me.anno.ecs.components.mesh.material.Material.Companion.noVertexColors
import me.anno.ecs.components.mesh.shapes.CylinderModel
import me.anno.ecs.components.mesh.shapes.IcosahedronModel
import me.anno.ecs.components.mesh.shapes.PlaneModel
import me.anno.ecs.components.mesh.shapes.UVSphereModel
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.gpu.pipeline.PipelineStage
import me.anno.io.files.FileReference
import me.anno.io.files.FileRootRef
import me.anno.io.files.Reference
import me.anno.io.files.inner.InnerLinkFile
import me.anno.io.files.inner.InnerPrefabFile
import me.anno.io.files.inner.temporary.InnerTmpPrefabFile
import me.anno.mesh.Shapes
import me.anno.utils.Color.white
import me.anno.utils.InternalAPI
import me.anno.utils.OS.res
import org.joml.Vector2f

/**
 * some assets that are guaranteed to be always available;
 * however their exact topology is not guaranteed!!
 * */
object DefaultAssets {

    // registry
    @InternalAPI
    val assets = HashMap<String, HashSet<FileReference>>()

    // meshes
    val flatCube = Shapes.flatCube.front
    val smoothCube = Shapes.smoothCube.front
    val cylinderY11 = CylinderModel.createCylinder(32, 2, top = true, bottom = true, null, 3f, Mesh())
    val uvSphere = UVSphereModel.createUVSphere(40, 20)
    val icoSphere = IcosahedronModel.createIcosphere(3)
    val plane = PlaneModel.createPlaneXZ(2, 2, Vector2f(1f))

    // textures
    val uvCheckerTexture = res.getChild("textures/UVChecker.png")
    val iconTexture = res.getChild("icon.png")

    // materials
    val whiteMaterial = Material().noVertexColors()
    val mirrorMaterial = Material.metallic(white, 0f).noVertexColors()
    val silverMaterial = Material.metallic(0xe5e5e5, 0f).noVertexColors()
    val steelMaterial = Material.metallic(0x4c4c4c, 0.2f).noVertexColors()
    val goldenMaterial = Material.metallic(0xf5ba6c, 0.2f).noVertexColors()
    val glassMaterial = Material.metallic(white, 0f).noVertexColors().apply {
        diffuseBase.w = 0.5f
        pipelineStage = PipelineStage.TRANSPARENT
    }
    val blackMaterial = Material.diffuse(0)
    val emissiveMaterial = Material().noVertexColors().apply { emissiveBase.set(10f) }
    val uvDebugMaterial = Material().noVertexColors().apply { diffuseMap = uvCheckerTexture }

    fun init() {}

    init {
        registerMeshes()
        registerMaterials()
        registerTextures()
    }

    private fun registerMeshes() {
        register("meshes/Cube.json", "Mesh", flatCube)
        register("meshes/SmoothCube.json", "Mesh", smoothCube)
        register("meshes/CylinderY.json", "Mesh", cylinderY11)
        register("meshes/UVSphere.json", "Mesh", uvSphere)
        register("meshes/IcoSphere.json", "Mesh", icoSphere)
        register("meshes/PlaneY.json", "Mesh", plane)
    }

    private fun registerMaterials() {
        register("materials/Default.json", "Material", defaultMaterial)
        register("materials/White.json", "Material", whiteMaterial)
        register("materials/Mirror.json", "Material", mirrorMaterial)
        register("materials/Golden.json", "Material", goldenMaterial)
        register("materials/Silver.json", "Material", silverMaterial)
        register("materials/Steel.json", "Material", steelMaterial)
        register("materials/Glass.json", "Material", glassMaterial)
        register("materials/Black.json", "Material", blackMaterial)
        register("materials/Emissive.json", "Material", emissiveMaterial)
        register("materials/UVDebug.json", "Material", uvDebugMaterial)
    }

    private fun registerTextures() {
        register("textures/UVChecker.png", "Texture", uvCheckerTexture)
        register("textures/Icon.png", "Texture", iconTexture)
    }

    fun register(name: String, type: String, file: FileReference) {
        val file1 = Reference.registerStatic(InnerLinkFile(name, name, FileRootRef, file))
        assets.getOrPut(type, ::HashSet).add(file1)
    }

    fun register(name: String, type: String, value: PrefabSaveable) {
        val file = value.ref
        if (file is InnerTmpPrefabFile) {
            // replace the link in favor of us always using the proper static path in the future
            val prefab = file.prefab
            val newFile = Reference.registerStatic(InnerPrefabFile(name, name, FileRootRef, prefab))
            prefab.sourceFile = newFile // just in case, not strictly needed
        } else register(name, type, file)
    }
}