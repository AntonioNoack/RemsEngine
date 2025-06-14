package me.anno.engine.ui

import me.anno.ecs.Component
import me.anno.ecs.Entity
import me.anno.ecs.EntityQuery.forAllComponentsInChildren
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.components.mesh.material.Material
import me.anno.ecs.prefab.Prefab
import me.anno.ecs.prefab.PrefabCache
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.engine.EngineBase.Companion.workspace
import me.anno.engine.Events.addEvent
import me.anno.engine.projects.GameEngineProject
import me.anno.engine.projects.GameEngineProject.Companion.currentProject
import me.anno.engine.ui.AssetImport.deepCopyImport
import me.anno.engine.ui.AssetImport.shallowCopyImport
import me.anno.engine.ui.ECSTreeView.Companion.optionToMenu
import me.anno.engine.ui.input.ComponentUI
import me.anno.engine.ui.render.PlayMode
import me.anno.engine.ui.scenetabs.ECSSceneTabs
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import me.anno.io.files.Reference.getReference
import me.anno.io.json.saveable.JsonStringReader
import me.anno.language.translation.NameDesc
import me.anno.ui.Panel
import me.anno.ui.Style
import me.anno.ui.base.menu.ComplexMenuGroup
import me.anno.ui.base.menu.Menu
import me.anno.ui.base.menu.Menu.askRename
import me.anno.ui.base.menu.Menu.menuSeparator1
import me.anno.ui.base.menu.MenuOption
import me.anno.ui.editor.files.FileExplorer
import me.anno.ui.editor.files.FileExplorerEntry
import me.anno.ui.editor.files.FileExplorerOption
import me.anno.ui.editor.files.FileNames.toAllowedFilename
import me.anno.utils.async.Callback.Companion.mapCallback
import me.anno.utils.files.Files.findNextChild
import me.anno.utils.files.Files.findNextFile
import me.anno.utils.files.LocalFile.toGlobalFile
import me.anno.utils.structures.lists.Lists.all2
import org.apache.logging.log4j.LogManager
import kotlin.math.max
import kotlin.reflect.KClass

class ECSFileExplorer(file0: FileReference?, isY: Boolean, style: Style) : FileExplorer(file0, isY, style) {

    constructor(file0: FileReference, style: Style) : this(file0, true, style)

    override fun onDoubleClick(file: FileReference) {
        // open the file
        PrefabCache.getPrefabAsync(file) { prefab, err ->
            err?.printStackTrace()
            addEvent { onDoubleClick1(file, prefab) }
        }
    }

    private fun onDoubleClick1(file: FileReference, prefab: Prefab?) {
        if (prefab != null) {
            ECSSceneTabs.open(file, PlayMode.EDITING, true)
        } else {
            switchTo(file)
        }
    }

    override fun getFileOptions(): List<FileExplorerOption> {
        return fileOptions + super.getFileOptions()
    }

    override fun getFolderOptions(): List<FileExplorerOption> {
        return folderOptions
    }

    override fun onPaste(x: Float, y: Float, data: String, type: String) {
        LOGGER.info("Pasted $type : $data")
        when (type) {
            "PrefabSaveable" -> if (!pastePrefab(data)) {
                LOGGER.warn("Could not parse prefab, $data")
            }
            else -> {
                if (!pastePrefab(data)) {
                    if (data.length < 2048) {
                        val ref = data.toGlobalFile()
                        if (ref.exists) {
                            switchTo(ref)
                        }// else super.onPaste(x, y, data, type)
                    }// else super.onPaste(x, y, data, type)
                }
            }
        }
    }

    override fun onPasteFiles(x: Float, y: Float, files: List<FileReference>) {

        // if current folder is inside project, then import all these assets

        // when dragging over a current folder, do that operation on that folder
        val entry = content2d.children.firstOrNull { it.contains(x, y) } as? FileExplorerEntry
        val current = if (entry == null) folder else getReference(entry.path)

        val projectFolder = currentProject?.location ?: return
        if (current.isSameOrSubFolderOf(projectFolder) &&
            files.any { file -> AssetImport.getPureTypeOrNull(file) == null }
        ) {
            pasteFiles(
                files, folder,
            )
        } else super.onPasteFiles(x, y, files)
    }

    private val shallowCopyDesc =
        NameDesc("Shallow-Copy Import", "Import the file as a prefab, but just link dependencies", "")
    private val deepCopyDesc =
        NameDesc("Deep-Copy Import", "Import the file and any dependencies as prefabs, except for images", "")
    private val linkToIndexDesc = NameDesc(
        "Link To Index",
        "Creates a .url file, which will be used when the project is opened to index assets", ""
    )

    override fun getPasteOptions(files: List<FileReference>, folder: FileReference): List<MenuOption> {
        val baseOptions = super.getPasteOptions(files, folder)
        val projectFolder = currentProject?.location ?: return baseOptions
        if (files.none { file -> AssetImport.getPureTypeOrNull(file) == null }) return baseOptions
        // when dragging over a current folder, do that operation on that folder
        val entry = content2d.children.firstOrNull { it.contains(x, y) } as? FileExplorerEntry
        val current = if (entry == null) folder else getReference(entry.path)
        if (!current.isSameOrSubFolderOf(projectFolder)) return baseOptions
        return baseOptions + listOf(
            menuSeparator1,
            MenuOption(shallowCopyDesc) { shallowCopyImport(current, files, this) },
            MenuOption(deepCopyDesc) { deepCopyImport(current, files, this) },
            MenuOption(linkToIndexDesc) { linkToIndex(current, files) }
        )
    }

    private fun linkToIndex(current: FileReference, files: List<FileReference>) {
        val firstParent = files.first().getParent().nullIfUndefined()
        val name = if (files.size == 1) files.first().nameWithoutExtension
        else if (files.all2 { it.getParent() == firstParent }) firstParent?.nameWithoutExtension ?: "Root"
        else files.first().nameWithoutExtension
        val newFile = current.findNextChild(name, "url", 3, '-')
        // http://www.lyberty.com/encyc/articles/tech/dot_url_format_-_an_unofficial_guide.html
        newFile.writeText(
            "[InternetShortcut]\r\n" +
                    files.joinToString(",") { "URL=file://${it.toLocalPath()}\r\n" }
        )
        LOGGER.debug("Created url link file {}", newFile)
    }

    private fun pastePrefab(data: String): Boolean {
        try {
            val read = JsonStringReader.read(data, workspace, true)
            val saveable = read.getOrNull(0) ?: return false
            when (saveable) {
                is Prefab -> {
                    // find the name of the root element
                    var name = saveable.instanceName
                    name = name?.toAllowedFilename()
                    name = name ?: saveable.className
                    name = name.toAllowedFilename() ?: "Something"
                    // make .json lowercase
                    if (!name.endsWith(".json", true)) {
                        name += ".json"
                    }
                    val file = findNextFile(folder.getChild(name), 1, '-')
                    file.writeText(data)
                    invalidate()
                    return true
                }
                is PrefabSaveable -> {
                    var name = saveable.name.toAllowedFilename()
                    name = name ?: saveable.defaultDisplayName.toAllowedFilename()
                    name = name ?: saveable.className
                    name = name.toAllowedFilename() ?: "Something"
                    // make .json lowercase
                    if (!name.endsWith(".json", true)) {
                        name += ".json"
                    }
                    val file = findNextFile(folder.getChild(name), 1, '-')
                    file.writeText(data)
                    invalidate()
                    return true
                }
                else -> throw RuntimeException("Unknown class ${saveable.className}")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    companion object {

        private val LOGGER = LogManager.getLogger(ECSFileExplorer::class)

        val fileOptions = ArrayList<FileExplorerOption>()
        val folderOptions = ArrayList<FileExplorerOption>()

        fun askPlacePrefab(p: Panel, folder: FileReference, name: String, clazzName: String) {
            val ext = GameEngineProject.encoding.extension
            askRename(
                p.windowStack, NameDesc("File Name"), "$name.$ext",
                NameDesc("Create File"), folder
            ) { file ->
                if (file != InvalidRef) {
                    val encoding = GameEngineProject.encoding.getForExtension(file)
                    file.writeBytes(encoding.encode(Prefab(clazzName), InvalidRef))
                    invalidateFileExplorers(p)
                }
            }
        }

        fun getFirstFolder(files: List<FileReference>): FileReference {
            val folder = files.firstNotNullOfOrNull {
                if (it.isDirectory) it else {
                    val parent = it.getParent()
                    if (parent.isDirectory) parent else null
                }
            } ?: InvalidRef
            if (folder == InvalidRef) LOGGER.warn("Not directory selected")
            return folder
        }

        @Suppress("MemberVisibilityCanBePrivate")
        fun addOptionToCreateFile(name: String, clazzName: String) {
            folderOptions.add(FileExplorerOption(NameDesc("Add $name")) { p, files ->
                val folder = getFirstFolder(files)
                if (folder != InvalidRef) {
                    askPlacePrefab(p, folder, name, clazzName)
                }
            })
        }

        @Suppress("MemberVisibilityCanBePrivate")
        fun addOptionToCreateComponent(name: String, clazzName: String = name) {
            addOptionToCreateFile(name, clazzName)
        }

        fun <V : PrefabSaveable> addComplexButtonToCreate(name: String, clazz: KClass<V>) {
            folderOptions.add(FileExplorerOption(NameDesc("Add $name")) { p, files ->
                val first = getFirstFolder(files)
                if (first != InvalidRef) {
                    fun onChoseType(sample: PrefabSaveable) {
                        askPlacePrefab(
                            p, first, sample.className,
                            sample.className
                        )
                    }
                    Menu.openComplexMenu(
                        p.windowStack, NameDesc("Choose Type"),
                        getOptionsByClass(null, clazz)
                            .groupBy(ECSTreeView.Companion::getMenuGroup)
                            .map { (group, options) ->
                                if (options.size > 1) {
                                    ComplexMenuGroup(
                                        NameDesc("Add $group"), true,
                                        options.map { optionToMenu(it, ::onChoseType) })
                                } else {
                                    optionToMenu(options.first(), ::onChoseType)
                                }
                            }
                            .sortedBy(ECSTreeView::getSorting)
                    )
                }
            })
        }

        init {
            val openAsScene = FileExplorerOption(
                NameDesc(
                    "Open As Scene",
                    "Show the file in a new scene tab",
                    "ui.file.openInSceneTab"
                )
            ) { p, files ->
                for (file in files) {
                    ECSSceneTabs.open(
                        file, PlayMode.EDITING,
                        setActive = (file == files.first())
                    )
                }
                invalidateFileExplorers(p)
            }
            val extendPrefab = FileExplorerOption(
                NameDesc("Extend Prefab", "Create a child prefab with this file as its base", ""),
            ) { p, files ->
                val defaultExtension = GameEngineProject.encoding.extension
                files.mapCallback({ _, src, cb ->
                    PrefabCache.getPrefabAsync(src) { prefab, err ->
                        if (prefab != null) {
                            askRename(
                                p.windowStack, NameDesc("Extend ${prefab.clazzName} \"${src.name}\""),
                                "${src.nameWithoutExtension}.$defaultExtension",
                                NameDesc("Extend"), src.getParent()
                            ) { dst ->
                                if (src == dst) {
                                    LOGGER.warn("Cannot extend into same file")
                                } else {
                                    // todo add file to index? or is that done automatically??
                                    val newPrefab = Prefab(prefab.clazzName, src)
                                    GameEngineProject.save(dst, newPrefab)
                                }
                                cb.ok(Unit)
                            }
                        } else if (err != null) {
                            LOGGER.warn("Failed loading $src as prefab", err)
                        }
                    }
                }, { _, _ -> })
            }
            val assignMaterialToMeshes = FileExplorerOption(
                NameDesc(
                    "Override Material", "Changes the material of all selected files",
                    "ui.file.overrideMaterial"
                )
            ) { p, files ->
                // ask material
                val meshes = ArrayList<Triple<FileReference, Prefab, List<FileReference>>>()
                val materials = ArrayList<Pair<FileReference, Prefab>>()
                val invalidFiles = HashSet<FileReference>()
                fun processMesh(file: FileReference, prefab: Prefab) {
                    val oldMaterials = (prefab["materials"] as? List<*>)
                        ?.filterIsInstance<FileReference>()
                        ?: emptyList()
                    meshes.add(Triple(file, prefab, oldMaterials))
                }

                fun processMaterial(file: FileReference, prefab: Prefab) {
                    materials.add(file to prefab)
                }
                for (file in files) {
                    if (file.isDirectory) continue
                    val prefab = PrefabCache[file, false] ?: continue
                    when (prefab.clazzName) {
                        "Entity" -> {
                            // iterate through, find MeshComponents, extract their mesh, and associated materials
                            var addedAny = false
                            val sample = prefab.getSampleInstance() as Entity
                            sample.forAllComponentsInChildren(MeshComponent::class) {
                                val meshFile = it.meshFile
                                val prefab1 = PrefabCache[meshFile, false]
                                if (prefab1 != null && prefab1.clazzName == "Mesh") {
                                    processMesh(meshFile, prefab1)
                                    addedAny = true
                                }
                            }
                            if (addedAny) {
                                invalidFiles.add(file)
                            }
                        }
                        "Mesh" -> processMesh(file, prefab)
                        "Material" -> processMaterial(file, prefab)
                    }
                }
                if (meshes.isNotEmpty() || materials.isNotEmpty()) {
                    ComponentUI.chooseFileFromProject("Material", InvalidRef, p, p.style) { materialFile, cancelled ->
                        for ((file, prefab, oldMaterials) in meshes) {
                            val size = max(1, oldMaterials.size)
                            prefab["materials"] =
                                if (cancelled) oldMaterials
                                else (0 until size).map { materialFile }
                            val encoding = GameEngineProject.encoding.getForExtension(file)
                            file.writeBytes(encoding.encode(prefab, workspace))
                        }
                        for ((file, prefab) in materials) {
                            prefab.parentPrefabFile = materialFile
                            prefab.adds.clear() // not really doing anything
                            prefab.sets.clear()
                            val encoding = GameEngineProject.encoding.getForExtension(file)
                            file.writeBytes(encoding.encode(prefab, workspace))
                        }
                        // and at the end, invalidate the thumbnail for these secondary source files
                        GameEngineProject.invalidateThumbnails(invalidFiles)
                    }
                } else LOGGER.warn("No valid target was found")
            }

            fun flattenFiles(files: List<FileReference>): List<FileReference> {
                return files.flatMap { file ->
                    if (file.isDirectory) flattenFiles(file.listChildren())
                    else listOf(file)
                }
            }

            val clearHistory = FileExplorerOption(NameDesc("Clear History")) { _, files ->
                for (file in flattenFiles(files)) {
                    val prefab = PrefabCache[file] ?: continue
                    prefab.history = null
                    val encoding = GameEngineProject.encoding.getForExtension(file)
                    file.writeBytes(encoding.encode(prefab, workspace))
                }
            }

            val deepCopyImport = FileExplorerOption(NameDesc("Deep-Copy Import")) { fe, files ->
                deepCopyImport(files.first().getParent(), files, fe as FileExplorer?)
            }

            fileOptions.add(openAsScene)
            fileOptions.add(extendPrefab)
            fileOptions.add(assignMaterialToMeshes)
            fileOptions.add(deepCopyImport)
            fileOptions.add(clearHistory)
            folderOptions.add(openAsScene)
            // todo create shader (MaterialGraph), post-processing (ShaderGraph), render mode (RenderGraph),
            //  mesh(?), visual script, Kotlin-script??, etc
            // todo add same options as when right-clicking on tree-view
            // todo create folders for these, because there's too many
            addOptionToCreateComponent("Entity")
            addComplexButtonToCreate("Component", Component::class)
            addComplexButtonToCreate("Material", Material::class)
        }
    }
}