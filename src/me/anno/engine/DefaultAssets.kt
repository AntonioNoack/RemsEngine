package me.anno.engine

import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.material.Material
import me.anno.ecs.components.mesh.material.Material.Companion.defaultMaterial
import me.anno.ecs.components.mesh.shapes.CylinderModel
import me.anno.ecs.components.mesh.shapes.IcosahedronModel
import me.anno.ecs.components.mesh.shapes.PlaneModel
import me.anno.ecs.components.mesh.shapes.UVSphereModel
import me.anno.gpu.pipeline.PipelineStage
import me.anno.io.files.FileReference
import me.anno.io.files.FileRootRef
import me.anno.io.files.Reference
import me.anno.io.files.inner.InnerLinkFile
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
    val mirrorMaterial = Material.metallic(-1, 0f)
    val goldenMaterial = Material.metallic(0xf5ba6c, 0.2f)
    val glassMaterial = Material.metallic(white, 0f).apply {
        diffuseBase.w = 0.5f
        pipelineStage = PipelineStage.TRANSPARENT
    }
    val blackMaterial = Material.diffuse(0)
    val emissiveMaterial = Material().apply { emissiveBase.set(10f) }
    val uvDebugMaterial = Material().apply { diffuseMap = uvCheckerTexture }

    fun init() {}

    init {
        registerMeshes()
        registerMaterials()
        registerTextures()
    }

    private fun registerMeshes() {
        register("meshes/Cube.json", "Mesh", flatCube.ref)
        register("meshes/SmoothCube.json", "Mesh", smoothCube.ref)
        register("meshes/CylinderY.json", "Mesh", cylinderY11.ref)
        register("meshes/UVSphere.json", "Mesh", uvSphere.ref)
        register("meshes/IcoSphere.json", "Mesh", icoSphere.ref)
        register("meshes/PlaneY.json", "Mesh", plane.ref)
    }

    private fun registerMaterials() {
        register("materials/Default.json", "Material", defaultMaterial.ref)
        register("materials/Mirror.json", "Material", mirrorMaterial.ref)
        register("materials/Golden.json", "Material", goldenMaterial.ref)
        register("materials/Glass.json", "Material", glassMaterial.ref)
        register("materials/Black.json", "Material", blackMaterial.ref)
        register("materials/Emissive.json", "Material", emissiveMaterial.ref)
        register("materials/UVDebug.json", "Material", uvDebugMaterial.ref)
    }

    private fun registerTextures() {
        register("textures/UVChecker.png", "Texture", uvCheckerTexture)
        register("textures/Icon.png", "Texture", iconTexture)
    }

    fun register(name: String, type: String, file: FileReference) {
        val file1 = Reference.registerStatic(InnerLinkFile(name, name, FileRootRef, file))
        assets.getOrPut(type) { HashSet() }.add(file1)
    }
}