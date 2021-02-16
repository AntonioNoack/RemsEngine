package me.anno.objects.meshes

import me.anno.cache.instances.MeshCache.getMesh
import me.anno.config.DefaultConfig
import me.anno.gpu.GFX
import me.anno.gpu.GFX.isFinalRendering
import me.anno.gpu.buffer.Attribute
import me.anno.gpu.buffer.StaticBuffer
import me.anno.io.base.BaseWriter
import me.anno.language.translation.Dict
import me.anno.mesh.fbx.model.FBXGeometry
import me.anno.mesh.fbx.model.FBXShader.maxWeightsDefault
import me.anno.mesh.fbx.structure.FBXReader
import me.anno.mesh.obj.Material
import me.anno.mesh.obj.OBJReader
import me.anno.objects.GFXTransform
import me.anno.objects.Transform
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.editor.SettingCategory
import me.anno.ui.editor.files.hasValidName
import me.anno.ui.style.Style
import me.anno.utils.files.LocalFile.toGlobalFile
import me.anno.video.MissingFrameException
import me.karl.main.SceneLoader
import me.karl.renderer.AnimatedModelRenderer
import me.karl.utils.URI
import org.joml.Matrix4fArrayList
import org.joml.Vector4f
import org.joml.Vector4fc
import java.io.File

class Mesh(var file: File, parent: Transform?) : GFXTransform(parent) {

    companion object {
        // var daeEngine: RenderEngine? = null
        var daeRenderer: AnimatedModelRenderer? = null
    }

    // todo types of lights
    // todo shadows, ...
    // todo types of shading/rendering?

    // for the start it is nice to be able to import meshes like a torus into the engine :)

    constructor() : this(File(""), null)

    var lastFile: File? = null
    var extension = ""

    override fun onDraw(stack: Matrix4fArrayList, time: Double, color: Vector4fc) {

        val file = file
        if (file.hasValidName()) {

            if (file !== lastFile) {
                extension = file.extension.toLowerCase()
                lastFile = file
            }

            when (extension) {// todo decide on file magic instead
                "dae" -> {

                    GFX.check()
                    if (daeRenderer == null) {
                        daeRenderer = AnimatedModelRenderer()
                    }
                    GFX.check()

                    // load the 3D model
                    val data = getMesh(file, "Mesh-DAE", 1000, true) {
                        val meshData = MeshData()
                        GFX.addGPUTask(10) {
                            GFX.check()
                            meshData.daeScene = SceneLoader.loadScene(URI(file), URI(file))
                            GFX.check()
                        }
                        Thread.sleep(100) // wait for the texture to load
                        meshData
                    } as? MeshData

                    if (isFinalRendering && data == null) throw MissingFrameException(file)

                    // stack.scale(0.01f, -0.01f, 0.01f)
                    if (data?.daeScene != null) data.drawDae(stack, time, color) ?: super.onDraw(stack, time, color)

                }
                "fbx" -> {
                    // load the 3D model
                    val data = getMesh(file, "Mesh-FBX", 1000, true) {
                        val fbxGeometry =
                            FBXReader(file.inputStream().buffered()).fbxObjects.filterIsInstance<FBXGeometry>().first()
                        val meshData = MeshData()
                        meshData.objData = mapOf(
                            Material() to fbxGeometry.generateMesh(
                                "xyz",
                                "normals",
                                "materialIndex",
                                true,
                                1,
                                maxWeightsDefault
                            )
                        )
                        meshData.fbxGeometry = fbxGeometry
                        meshData
                    } as? MeshData

                    if (isFinalRendering && data == null) throw MissingFrameException(file)

                    stack.scale(0.01f, -0.01f, 0.01f)
                    data?.drawFBX(stack, time, color) ?: super.onDraw(stack, time, color)
                }
                "obj" -> {
                    // load the 3D model
                    val data = getMesh(file, "Mesh-OBJ", 1000, true) {
                        val attributes = listOf(
                            Attribute("coords", 3),
                            Attribute("uvs", 2),
                            Attribute("normals", 3)
                        )
                        // load the model...
                        // assume it's obj first...
                        val obj = OBJReader(file)
                        // generate mesh data from this obj somehow...
                        val meshData = MeshData()
                        meshData.objData = obj.pointsByMaterial.mapValues {
                            val buffer = StaticBuffer(attributes, it.value.size)
                            it.value.forEach { v -> buffer.put(v) }
                            buffer
                        }
                        meshData
                    } as? MeshData

                    if (isFinalRendering && data == null) throw MissingFrameException(file)

                    data?.drawObj(stack, time, color) ?: super.onDraw(stack, time, color)
                }
            }

        } else super.onDraw(stack, time, color)

    }

    override fun createInspector(
        list: PanelListY,
        style: Style,
        getGroup: (title: String, description: String, dictSubPath: String) -> SettingCategory
    ) {
        super.createInspector(list, style, getGroup)
        list += vi("File", "", null, file, style) { file = it }
    }

    override fun save(writer: BaseWriter) {
        super.save(writer)
        writer.writeFile("file", file)
    }

    override fun readString(name: String, value: String) {
        when (name) {
            "file" -> file = value.toGlobalFile()
            else -> super.readString(name, value)
        }
    }

    override fun getClassName() = "Mesh"
    override fun getDefaultDisplayName() = Dict["Mesh", "obj.mesh"]
    override fun getSymbol() = DefaultConfig["ui.symbol.mesh", "\uD83D\uDC69"]

}