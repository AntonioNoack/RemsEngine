package me.anno.engine.ui

import me.anno.ecs.Entity
import me.anno.ecs.EntityQuery.forAllComponentsInChildren
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.prefab.Prefab
import me.anno.ecs.prefab.PrefabCache
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.engine.GameEngineProject.Companion.currentProject
import me.anno.engine.RemsEngine
import me.anno.engine.ScenePrefab
import me.anno.engine.ui.AssetImport.deepCopyImport
import me.anno.engine.ui.AssetImport.shallowCopyImport
import me.anno.engine.ui.input.ComponentUI
import me.anno.engine.ui.render.PlayMode
import me.anno.engine.ui.scenetabs.ECSSceneTabs
import me.anno.io.files.FileReference
import me.anno.io.files.FileReference.Companion.getReference
import me.anno.io.files.InvalidRef
import me.anno.io.files.thumbs.Thumbs
import me.anno.io.json.saveable.JsonStringReader
import me.anno.io.json.saveable.JsonStringWriter
import me.anno.language.translation.NameDesc
import me.anno.studio.StudioBase
import me.anno.studio.StudioBase.Companion.workspace
import me.anno.ui.Style
import me.anno.ui.base.menu.Menu.askName
import me.anno.ui.base.menu.Menu.msg
import me.anno.ui.base.menu.Menu.openMenu
import me.anno.ui.base.menu.MenuOption
import me.anno.ui.editor.files.FileExplorer
import me.anno.ui.editor.files.FileExplorerEntry
import me.anno.ui.editor.files.FileExplorerOption
import me.anno.ui.editor.files.toAllowedFilename
import me.anno.utils.files.Files.findNextChild
import me.anno.utils.files.Files.findNextFile
import me.anno.utils.files.LocalFile.toGlobalFile
import me.anno.utils.structures.lists.Lists.all2
import org.apache.logging.log4j.LogManager
import kotlin.math.max


// done import mesh/material/... for modifications:
// done create material, mesh, animation etc folder
// done rename Scene.json to mesh file name.json

class ECSFileExplorer(file0: FileReference?, style: Style) : FileExplorer(file0, style) {

    override fun onDoubleClick(file: FileReference) {
        // open the file
        val prefab = PrefabCache[file]
        if (prefab != null) {
            ECSSceneTabs.open(file, PlayMode.EDITING, true)
        } else {
            switchTo(file)
            // msg(NameDesc("Could not open prefab!"))
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

        if (current.absolutePath.startsWith(projectFolder.absolutePath) &&
            !files.all { AssetImport.isPureFile(it) }
        ) {
            openMenu(windowStack, listOf(
                MenuOption(NameDesc("Shallow-Copy Import")) {
                    shallowCopyImport(current, files, this)
                },
                MenuOption(NameDesc("Deep-Copy Import")) {
                    deepCopyImport(current, files, this)
                },
                MenuOption(NameDesc("Link To Index")) {
                    val firstParent = files.first().getParent()
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
                },
                MenuOption(NameDesc("More Options")) {
                    super.onPasteFiles(x, y, files)
                }
            ))
        } else super.onPasteFiles(x, y, files)
    }

    private fun pastePrefab(data: String): Boolean {
        try {
            val read = JsonStringReader.read(data, StudioBase.workspace, true)
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

        @Suppress("MemberVisibilityCanBePrivate")
        fun addOptionToCreateFile(name: String, fileContent: String) {
            folderOptions.add(FileExplorerOption(NameDesc("Add $name")) { p, files ->
                val first = files.firstOrNull()
                if (first?.isDirectory == true) {
                    askName(p.windowStack, NameDesc("File Name"), name, NameDesc("Create File"), { -1 }, {
                        val name1 = if (it.endsWith(".json")) it.substring(0, it.length - 5) else it
                        val file = findNextFile(first, name1, "json", 1, 0.toChar(), 0)
                        if (file == InvalidRef) {
                            msg(p.windowStack, NameDesc("Directory is not writable"))
                        } else file.writeText(fileContent)
                        invalidateFileExplorers(p)
                    })
                } else LOGGER.warn("Not a directory")
            })
        }

        @Suppress("MemberVisibilityCanBePrivate")
        fun addOptionToCreateComponent(name: String, clazzName: String = name) {
            addOptionToCreateFile(name, Prefab(clazzName).toString())
        }

        @Suppress("MemberVisibilityCanBePrivate")
        fun addOptionToCreateComponent(name: String, clazzName: String, prefab: FileReference) {
            addOptionToCreateFile(name, Prefab(clazzName, prefab).toString())
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
            val assignMaterialToMeshes = FileExplorerOption(
                NameDesc(
                    "Override Material", "Changes the material of all selected files",
                    "ui.file.overrideMaterial"
                )
            ) { p, files ->
                // ask material
                val meshes = ArrayList<Triple<FileReference, Prefab, List<FileReference>>>()
                val materials = ArrayList<Pair<FileReference, Prefab>>()
                val filesToBeInvalidated = ArrayList<FileReference>()
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
                                filesToBeInvalidated.add(file)
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
                            prefab["materials"] = if (cancelled) oldMaterials
                            else (0 until size).map { materialFile }
                            file.writeText(JsonStringWriter.toText(prefab, workspace))
                        }
                        for ((file, prefab) in materials) {
                            prefab.prefab = materialFile
                            prefab.adds.clear() // not really doing anything
                            prefab.sets.clear()
                            file.writeText(JsonStringWriter.toText(prefab, workspace))
                        }
                        // and at the end, invalidate the thumbnail for these secondary source files
                        // todo ideally, invalidate all files that depend on ours
                        for (file in filesToBeInvalidated) {
                            Thumbs.invalidate(file)
                        }
                    }
                } else LOGGER.warn("No valid target was found")
            }
            fileOptions.add(openAsScene)
            fileOptions.add(assignMaterialToMeshes)
            folderOptions.add(openAsScene)
            // create camera, material, shader, prefab, mesh, script, etc
            addOptionToCreateComponent("Entity")
            addOptionToCreateComponent("Scene", "Entity", ScenePrefab)
            addOptionToCreateComponent("Material")
        }
    }
}