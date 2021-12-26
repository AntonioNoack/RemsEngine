package me.anno.objects.meshes

import me.anno.animation.AnimatedProperty
import me.anno.animation.Type
import me.anno.cache.instances.MeshCache.getMesh
import me.anno.config.DefaultConfig
import me.anno.ecs.Entity
import me.anno.ecs.components.anim.BoneByBoneAnimation
import me.anno.ecs.components.anim.ImportedAnimation
import me.anno.ecs.components.anim.Skeleton
import me.anno.ecs.components.mesh.AnimRenderer
import me.anno.ecs.components.mesh.Material
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.gpu.GFX.isFinalRendering
import me.anno.gpu.OpenGL
import me.anno.gpu.shader.BaseShader.Companion.cullFaceColoringGeometry
import me.anno.gpu.shader.BaseShader.Companion.lineGeometry
import me.anno.input.Input
import me.anno.io.ISaveable
import me.anno.io.ISaveable.Companion.registerCustomClass
import me.anno.io.base.BaseWriter
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import me.anno.language.translation.Dict
import me.anno.language.translation.NameDesc
import me.anno.mesh.assimp.AnimGameItem
import me.anno.mesh.assimp.AnimatedMeshesLoader
import me.anno.mesh.assimp.StaticMeshesLoader
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
import me.anno.utils.files.LocalFile.toGlobalFile
import me.anno.utils.maths.Maths.pow
import me.anno.video.MissingFrameException
import org.joml.Matrix4fArrayList
import org.joml.Vector4fc

class MeshTransform(var file: FileReference, parent: Transform?) : GFXTransform(parent) {

    // todo lerp animations

    // todo types of lights
    // todo shadows, ...
    // todo types of shading/rendering?

    // todo info field with the amount of vertices, triangles, and such :)

    companion object {

        init {
            registerCustomClass(Mesh())
            registerCustomClass(Entity())
            registerCustomClass(MeshComponent())
            registerCustomClass(AnimRenderer())
            registerCustomClass(Skeleton())
            registerCustomClass(Material())
            registerCustomClass(ImportedAnimation())
            registerCustomClass(BoneByBoneAnimation())
        }

        fun loadModel(
            file: FileReference,
            key: String,
            instance: Transform?,
            load: (MeshData) -> Unit,
            getData: (MeshData) -> Any?
        ): MeshData? {
            val meshData1 = getMesh(file, key, 1000, true) { _, _ ->
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

        fun loadVOX(file: FileReference, instance: Transform?): MeshData? {
            return loadModel(file, "vox", instance, {
                val reader = VOXReader().read(file)
                val entity = reader.toEntityPrefab(file).createInstance() as Entity
                // alternatively for testing:
                // reader.toEntityPrefab().createInstance()
                // works :)
                it.assimpModel = AnimGameItem(entity)
            }, { it.assimpModel })
        }

        fun loadAssimpAnimated(file: FileReference, instance: Transform?): MeshData? {
            return loadModel(file, "Assimp", instance, { meshData ->
                val reader = AnimatedMeshesLoader
                val meshes = reader.load(file)
                meshData.assimpModel = meshes
            }) { it.assimpModel }
        }

        fun loadAssimpStatic(file: FileReference, instance: Transform?): MeshData? {
            return loadModel(file, "Assimp-Static", instance, { meshData ->
                val reader = StaticMeshesLoader()
                val meshes = reader.load(file)
                meshData.assimpModel = meshes
            }) { it.assimpModel }
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
        if (file.hasValidName() && file.exists) {

            if (file !== lastFile) {
                extension = file.lcExtension
                lastFile = file
            }

            // todo decide on file magic instead
            val data = when (extension) {
                "vox" -> {
                    // vox is not supported by assimp -> custom loader
                    loadVOX(file, this)
                }
                else -> {
                    // load the 3D model
                    loadAssimpAnimated(file, this)
                }
            }

            drawAssimp(data, stack, time, color)
            lastModel = data?.assimpModel

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
                        false, this, stack, time, color,
                        animation[time], true, centerMesh, normalizeScale, false
                    )
                    Input.isKeyDown('l') -> {// line debugging
                        OpenGL.geometryShader.use(lineGeometry) {
                            data.drawAssimp(
                                false, this, stack, time, color,
                                animation[time], true, centerMesh, normalizeScale, false
                            )
                        }
                    }
                    Input.isKeyDown('n') -> {// normal debugging
                        OpenGL.geometryShader.use(cullFaceColoringGeometry) {
                            data.drawAssimp(
                                false, this, stack, time, color,
                                animation[time], true, centerMesh, normalizeScale, false
                            )
                        }
                    }
                    else -> data.drawAssimp(
                        false, this, stack, time, color,
                        animation[time], true, centerMesh, normalizeScale, true
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

    // was Mesh before
    // mmh, but the game engine is kinda more important...
    // and the mesh support was very limited before anyways -> we shouldn't worry too much,
    // because we don't have users at the moment anyways
    override val className get() = "MeshTransform"

    override val defaultDisplayName get() = Dict["Mesh", "obj.mesh"]
    override val symbol get() = DefaultConfig["ui.symbol.mesh", "\uD83D\uDC69"]

}