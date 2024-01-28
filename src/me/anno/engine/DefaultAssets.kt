package me.anno.engine

import me.anno.ecs.components.mesh.Material
import me.anno.ecs.components.mesh.Material.Companion.defaultMaterial
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.shapes.CylinderModel
import me.anno.ecs.components.mesh.shapes.IcosahedronModel
import me.anno.ecs.components.mesh.shapes.PlaneModel
import me.anno.ecs.components.mesh.shapes.UVSphereModel
import me.anno.gpu.pipeline.PipelineStage
import me.anno.io.files.FileReference
import me.anno.io.files.FileRootRef
import me.anno.io.files.Reference
import me.anno.io.files.inner.InnerLinkFile
import me.anno.mesh.Shapes.flatCube
import me.anno.utils.Color.black

/**
 * some assets that are guaranteed to be always available;
 * however their exact topology is not guaranteed!!
 * */
object DefaultAssets {

    val assets = HashMap<String, HashSet<FileReference>>()

    fun init() {}

    init {
        registerMeshes()
        registerMaterials()
    }

    private fun registerMeshes() {
        register("meshes/Cube.json", "Mesh", flatCube.front.ref)
        val cylinderY11 = CylinderModel.createMesh(32, 2, top = true, bottom = true, null, 3f, Mesh())
        register("meshes/CylinderY.json", "Mesh", cylinderY11.ref)
        val uvSphere = UVSphereModel.createUVSphere(40, 20)
        register("meshes/UVSphere.json", "Mesh", uvSphere.ref)
        val icoSphere = IcosahedronModel.createIcosphere(3)
        register("meshes/IcoSphere.json", "Mesh", icoSphere.ref)
        val plane = PlaneModel.createPlane()
        register("meshes/PlaneY.json", "Mesh", plane.ref)
        register("materials/Default.json", "Material", defaultMaterial.ref)
    }

    private fun registerMaterials() {
        val mirror = Material()
        mirror.roughnessMinMax.set(0f)
        mirror.metallicMinMax.set(1f)
        register("materials/Mirror.json", "Material", mirror.ref)
        val glass = Material()
        glass.diffuseBase.w = 0.5f
        glass.roughnessMinMax.set(0f)
        glass.pipelineStage = PipelineStage.TRANSPARENT
        register("materials/Glass.json", "Material", glass.ref)
        val black = Material.diffuse(black)
        register("materials/Black.json", "Material", black.ref)
        val emissive = Material()
        emissive.emissiveBase.set(10f)
        register("materials/Emissive.json", "Material", emissive.ref)
    }

    fun register(name: String, type: String, file: FileReference) {
        val file1 = Reference.registerStatic(InnerLinkFile(name, name, FileRootRef, file))
        assets.getOrPut(type) { HashSet() }.add(file1)
    }
}