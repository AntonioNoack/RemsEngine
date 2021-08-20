package me.anno.objects.meshes

import me.anno.animation.AnimatedProperty
import me.anno.animation.Type
import me.anno.cache.instances.LastModifiedCache
import me.anno.cache.instances.MeshCache.getMesh
import me.anno.config.DefaultConfig
import me.anno.ecs.prefab.Prefab
import me.anno.gpu.GFX.isFinalRendering
import me.anno.gpu.RenderState
import me.anno.gpu.shader.BaseShader.Companion.cullFaceColoringGeometry
import me.anno.gpu.shader.BaseShader.Companion.lineGeometry
import me.anno.input.Input
import me.anno.io.ISaveable
import me.anno.io.base.BaseWriter
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import me.anno.language.translation.Dict
import me.anno.language.translation.NameDesc
import me.anno.mesh.assimp.AnimGameItem
import me.anno.mesh.assimp.AnimatedMeshesLoader
import me.anno.mesh.vox.VOXReader
import me.anno.objects.GFXTransform
import me.anno.objects.Transform
import me.anno.ui.base.Panel
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.base.groups.UpdatingContainer
import me.anno.ui.base.text.TextPanel
import me.anno.ui.editor.SettingCategory
import me.anno.ui.input.EnumInput
import me.anno.ui.style.Style
import me.anno.utils.Maths.pow
import me.anno.utils.files.LocalFile.toGlobalFile
import me.anno.video.MissingFrameException
import org.joml.Matrix4fArrayList
import org.joml.Vector4fc
import java.util.*

class Mesh(var file: FileReference, parent: Transform?) : GFXTransform(parent) {

    // todo lerp animations

    // todo types of lights
    // todo shadows, ...
    // todo types of shading/rendering?

    // todo info field with the amount of vertices, triangles, and such :)

    companion object {

        // var daeEngine: RenderEngine? = null
        // val gltfReader = GltfModelReader()
        // var daeRenderer: AnimatedModelRenderer? = null

        /*init {
            GltfLogger.setup()
            LogManager.disableLogger("MatrixOps")
            LogManager.disableLogger("RenderCommandUtils")
            LogManager.disableLogger("GlContextLwjgl")
            LogManager.disableLogger("GltfRenderData")
            LogManager.disableLogger("DefaultRenderedGltfModel")
        }*/

        fun loadModel(
            file: FileReference,
            key: String,
            instance: Transform?,
            load: (MeshData) -> Unit,
            getData: (MeshData) -> Any?
        ): MeshData? {
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
            instance?.lastWarning = if (renderData == null) meshData1?.lastWarning ?: "Loading" else null
            return if (renderData == null) null else meshData1
        }

    }

    val animation = AnimatedProperty.string()

    var centerMesh = true
    var normalizeScale = true

    // for the start it is nice to be able to import meshes like a torus into the engine :)

    constructor() : this(InvalidRef, null)

    var lastFile: FileReference? = null
    var extension = ""
    var powerOf10Correction = 0

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
                "vox" -> {

                    // vox is not supported by assimp -> custom loader
                    val data = loadModel(file, "vox", this, { meshData ->
                        val reader = VOXReader()
                        reader.read(file)
                        // reader.toEntity()
                        // alternatively for testing:
                        // reader.toEntityPrefab().createInstance()
                        // works :)
                        val entity = reader.toEntity()
                        meshData.assimpModel = AnimGameItem(
                            entity, reader.meshes,
                            emptyList(), emptyMap()
                        )
                    }) { it.assimpModel }

                    drawAssimp(data, stack, time, color)

                    lastModel = data?.assimpModel

                }
                else -> {

                    // load the 3D model
                    val data = loadModel(file, "Assimp", this, { meshData ->
                        val reader = AnimatedMeshesLoader
                        val meshes = reader.load(file)
                        meshData.assimpModel = meshes
                    }) { it.assimpModel }

                    drawAssimp(data, stack, time, color)

                    lastModel = data?.assimpModel

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

    fun drawAssimp(data: MeshData?, stack: Matrix4fArrayList, time: Double, color: Vector4fc) {
        if (data?.assimpModel != null) {
            stack.next {

                if (powerOf10Correction != 0)
                    stack.scale(pow(10f, powerOf10Correction.toFloat()))

                // todo option to center the mesh
                // todo option to normalize its size
                // (see thumbnail generator)

                when {
                    isFinalRendering -> data.drawAssimp(
                        this, stack, time, color,
                        animation[time], true, centerMesh, normalizeScale
                    )
                    Input.isKeyDown('l') -> {// line debugging
                        RenderState.geometryShader.use(lineGeometry) {
                            data.drawAssimp(
                                this, stack, time, color,
                                animation[time], true, centerMesh, normalizeScale
                            )
                        }
                    }
                    Input.isKeyDown('n') -> {// normal debugging
                        RenderState.geometryShader.use(cullFaceColoringGeometry) {
                            data.drawAssimp(
                                this, stack, time, color,
                                animation[time], true, centerMesh, normalizeScale
                            )
                        }
                    }
                    else -> data.drawAssimp(
                        this, stack, time, color,
                        animation[time], true, centerMesh, normalizeScale
                    )
                }

            }
        } else super.onDraw(stack, time, color)
    }

    var lastModel: AnimGameItem? = null

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

        list += vi(
            "Normalize Scale", "A quicker fix than manually finding the correct scale",
            null, normalizeScale, style
        ) { normalizeScale = it }
        list += vi(
            "Center Mesh", "If your mesh is off-center, this corrects it",
            null, centerMesh, style
        ) { centerMesh = it }

        // the list of available animations depends on the model
        // but still, it's like an enum: only a certain set of animations is available
        // and the user wouldn't know perfectly which
        val map = HashMap<AnimGameItem?, Panel>()
        list += UpdatingContainer(100, {
            map.getOrPut(lastModel) {
                val model = lastModel
                val animations = model?.animations
                if (animations != null && animations.isNotEmpty()) {
                    var currentValue = animation[lastLocalTime]
                    val noAnimName = "No animation"
                    if (currentValue !in animations.keys) {
                        currentValue = noAnimName
                    }
                    val options = listOf(NameDesc(noAnimName)) + animations.map { NameDesc(it.key) }
                    EnumInput(
                        NameDesc("Animation"),
                        NameDesc(currentValue),
                        options, style
                    ).setChangeListener { value, _, _ ->
                        putValue(animation, value, true)
                    }
                } else TextPanel("No animations found!", style)
            }
        }, style)

    }

    override fun save(writer: BaseWriter) {
        super.save(writer)
        writer.writeFile("file", file)
        writer.writeInt("powerOf10", powerOf10Correction)
        writer.writeObject(this, "animation", animation)
        writer.writeBoolean("normalizeScale", normalizeScale, true)
        writer.writeBoolean("centerMesh", centerMesh, true)
    }

    override fun readBoolean(name: String, value: Boolean) {
        when (name) {
            "normalizeScale" -> normalizeScale = value
            "centerMesh" -> centerMesh = value
            else -> super.readBoolean(name, value)
        }
    }

    override fun readObject(name: String, value: ISaveable?) {
        when (name) {
            "animation" -> animation.copyFrom(value)
            else -> super.readObject(name, value)
        }
    }

    override fun readString(name: String, value: String?) {
        when (name) {
            "file" -> file = value?.toGlobalFile() ?: InvalidRef
            else -> super.readString(name, value)
        }
    }

    override fun readFile(name: String, value: FileReference) {
        when (name) {
            "file" -> file = value
            else -> super.readFile(name, value)
        }
    }

    override fun readInt(name: String, value: Int) {
        when (name) {
            "powerOf10" -> powerOf10Correction = value
            else -> super.readInt(name, value)
        }
    }

    override val className get() = "Mesh"
    override val defaultDisplayName = Dict["Mesh", "obj.mesh"]
    override val symbol = DefaultConfig["ui.symbol.mesh", "\uD83D\uDC69"]

}