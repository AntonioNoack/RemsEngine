package me.anno.objects.meshes

import de.javagl.jgltf.model.io.GltfModelReader
import me.anno.animation.AnimatedProperty
import me.anno.animation.Type
import me.anno.cache.instances.LastModifiedCache
import me.anno.cache.instances.MeshCache.getMesh
import me.anno.config.DefaultConfig
import me.anno.gpu.GFX.isFinalRendering
import me.anno.io.FileReference
import me.anno.io.ISaveable
import me.anno.io.base.BaseWriter
import me.anno.language.translation.Dict
import me.anno.mesh.assimp.AnimatedMeshesLoader
import me.anno.mesh.gltf.ExternalCameraImpl
import me.anno.mesh.gltf.GltfLogger
import me.anno.mesh.gltf.GltfViewerLwjgl
import me.anno.objects.GFXTransform
import me.anno.objects.Transform
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.editor.SettingCategory
import me.anno.ui.style.Style
import me.anno.utils.Maths.pow
import me.anno.utils.files.LocalFile.toGlobalFile
import me.anno.video.MissingFrameException
import me.karl.renderer.AnimatedModelRenderer
import org.apache.logging.log4j.LogManager
import org.joml.Matrix4fArrayList
import org.joml.Vector4fc
import java.util.*

class Mesh(var file: FileReference, parent: Transform?) : GFXTransform(parent) {

    // todo lerp animations

    // todo types of lights
    // todo shadows, ...
    // todo types of shading/rendering?

    companion object {

        // var daeEngine: RenderEngine? = null
        val gltfReader = GltfModelReader()
        var daeRenderer: AnimatedModelRenderer? = null

        init {
            GltfLogger.setup()
            LogManager.disableLogger("MatrixOps")
            LogManager.disableLogger("RenderCommandUtils")
            LogManager.disableLogger("GlContextLwjgl")
            LogManager.disableLogger("GltfRenderData")
            LogManager.disableLogger("DefaultRenderedGltfModel")
        }

    }

    val animationIndex = AnimatedProperty.int()

    // for the start it is nice to be able to import meshes like a torus into the engine :)

    constructor() : this(FileReference(""), null)

    var lastFile: FileReference? = null
    var extension = ""
    var powerOf10Correction = 0

    fun loadModel(file: FileReference, key: String, load: (MeshData) -> Unit, getData: (MeshData) -> Any?): MeshData? {
        val meshData1 = getMesh(file, key, 1000, true) {
            val meshData = MeshData()
            try {
                load(meshData)
            } catch (e: Exception) {
                e.printStackTrace()
                meshData.lastWarning = e.message ?: e.javaClass.name
            }
            meshData
        } as? MeshData
        if (isFinalRendering && meshData1 == null) throw MissingFrameException(file)
        val renderData = if (meshData1 != null) getData(meshData1) else null
        lastWarning = if (renderData == null) meshData1?.lastWarning ?: "Loading" else null
        return if (renderData == null) null else meshData1
    }

    override fun onDraw(stack: Matrix4fArrayList, time: Double, color: Vector4fc) {

        val file = file
        if (file.hasValidName() && LastModifiedCache[file].exists) {

            if (file !== lastFile) {
                extension = file.extension.lowercase(Locale.getDefault())
                lastFile = file
            }

            // todo decide on file magic instead
            when (extension) {
                /*"dae" -> {

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
                            try {
                                meshData.daeScene = SceneLoader.loadScene(URI(file), URI(file))
                                // for consistency between Obj and Dae files
                                meshData.daeScene?.lightDirection?.set(1f, 0f, 0f)
                            } catch (e: Exception) {
                                e.printStackTrace()
                                meshData.lastWarning = e.message
                            }
                            GFX.check()
                        }
                        meshData
                    } as? MeshData

                    if (isFinalRendering && data == null) throw MissingFrameException(file)

                    // stack.scale(0.01f, -0.01f, 0.01f)
                    if (data?.daeScene != null){
                        stack.next {
                            if (powerOf10Correction != 0)
                                stack.scale(pow(10f, powerOf10Correction.toFloat()))
                            data.drawDae(stack, time, color)
                        }
                    } else super.onDraw(stack, time, color)

                }
                "fbx" -> {

                    // load the 3D model
                    val data = loadModel(file, "Mesh-FBX", { meshData ->
                        val reader = FBXReader(file.inputStream().buffered())
                        val geometries = reader.fbxObjects.filterIsInstance<FBXGeometry>()
                        // join all geometries
                        // todo assign the materials, and correct the material indices... (and then don't join them)
                        // todo or import them in a hierarchy and set the mesh selectors (by index or similar)
                        val m0 = Material()
                        meshData.fbxData = geometries.map {
                            val buffer =
                                it.generateMesh("coords", "normals", "materialIndex", true, 1, maxWeightsDefault)
                            FBXData(it, mapOf(m0 to buffer))
                        }
                    }) { it.fbxData }

                    if (data?.fbxData != null) {
                        stack.next {
                            if (powerOf10Correction != 0)
                                stack.scale(pow(10f, powerOf10Correction.toFloat()))
                            data.drawFBX(stack, time, color)
                        }
                    } else super.onDraw(stack, time, color)

                }
                "obj" -> {
                    // load the 3D model
                    val data = loadModel(file, "Mesh-OBJ", { meshData ->
                        val attributes = listOf(
                            Attribute("coords", 3),
                            Attribute("uvs", 2),
                            Attribute("normals", 3)
                        )
                        // load the model...
                        // assume it's obj first...
                        val obj = OBJReader(file.file)
                        // generate mesh data from this obj somehow...
                        meshData.objData = obj.pointsByMaterial.mapValues {
                            val buffer = StaticBuffer(attributes, it.value.size)
                            it.value.forEach { v -> buffer.put(v) }
                            buffer
                        }
                    }) { it.objData }

                    if (data?.objData != null) {
                        stack.next {
                            if (powerOf10Correction != 0)
                                stack.scale(pow(10f, powerOf10Correction.toFloat()))
                            data.drawObj(stack, time, color)
                        }
                    } else super.onDraw(stack, time, color)

                }
                // assimp can load gltf files <3
                // -> we can give up/remove jGLTF*/
                /*"gltf", "glb" -> {
                    val data = loadModel(file, "Mesh-GLTF", {

                        val model = gltfReader.read(file.toUri())
                        val viewer = GltfViewerLwjgl()
                        val camera = ExternalCameraImpl()
                        viewer.setup(camera, model)
                        it.gltfData = GlTFData(viewer, model, camera)

                    }) { it.gltfData }

                    if (data?.gltfData != null) {
                        stack.next {
                            if (powerOf10Correction != 0)
                                stack.scale(pow(10f, powerOf10Correction.toFloat()))
                            data.drawGlTF(stack, time, color, animationIndex[time])
                        }
                    } else super.onDraw(stack, time, color)

                }*/
                else -> {

                    // load the 3D model
                    val data = loadModel(file, "Assimp", { meshData ->
                        // load the model...
                        // assume it's obj first...
                        val reader = AnimatedMeshesLoader
                        val meshes = reader.load(file.toString(), file.getParent().toString())
                        meshData.assimpModel = meshes
                    }) { it.assimpModel }

                    if (data?.assimpModel != null) {
                        stack.next {
                            if (powerOf10Correction != 0)
                                stack.scale(pow(10f, powerOf10Correction.toFloat()))
                            data.drawAssimp(this, stack, time, color, animationIndex[time])
                        }
                    } else super.onDraw(stack, time, color)

                }
                /*else -> {
                    lastWarning = "Extension '$extension' is not supported"
                }*/
            }

        } else {
            lastWarning = "Missing file"
            super.onDraw(stack, time, color)
        }

    }

    override fun createInspector(
        list: PanelListY,
        style: Style,
        getGroup: (title: String, description: String, dictSubPath: String) -> SettingCategory
    ) {
        super.createInspector(list, style, getGroup)
        list += vi("File", "", null, file, style) { file = it }
        list += vi(
            "Scale Correction, 10^N",
            "Often file formats are incorrect in size by a factor of 100. Use +/- 2 to correct this issue easily",
            Type.INT, powerOf10Correction, style
        ) { powerOf10Correction = it }
        // todo use names instead
        list += vi("Animation Index", "", animationIndex, style)
    }

    override fun save(writer: BaseWriter) {
        super.save(writer)
        writer.writeFile("file", file)
        writer.writeInt("powerOf10", powerOf10Correction)
        writer.writeObject(this, "animationIndex", animationIndex)
    }

    override fun readObject(name: String, value: ISaveable?) {
        when (name) {
            "animationIndex" -> animationIndex.copyFrom(value)
            else -> super.readObject(name, value)
        }
    }

    override fun readString(name: String, value: String) {
        when (name) {
            "file" -> file = value.toGlobalFile()
            else -> super.readString(name, value)
        }
    }

    override fun readInt(name: String, value: Int) {
        when (name) {
            "powerOf10" -> powerOf10Correction = value
            else -> super.readInt(name, value)
        }
    }

    override fun getClassName() = "Mesh"
    override val defaultDisplayName = Dict["Mesh", "obj.mesh"]
    override val symbol = DefaultConfig["ui.symbol.mesh", "\uD83D\uDC69"]

}