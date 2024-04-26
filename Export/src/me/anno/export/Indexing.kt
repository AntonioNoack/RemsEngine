package me.anno.export

import me.anno.export.idea.IdeaLibrary
import me.anno.export.idea.IdeaModule
import me.anno.export.idea.IdeaProject
import me.anno.io.files.FileReference
import me.anno.ui.base.progress.ProgressBar
import java.util.zip.ZipInputStream

object Indexing {
    fun indexZipFile(sources: MutableMap<String, ByteArray>, src: FileReference, progress: ProgressBar) {
        val input = ZipInputStream(src.inputStreamSync())
        while (!progress.isCancelled) {
            val entry = input.nextEntry ?: break
            sources[entry.name] = input.readBytes()
            progress.total++
        }
        input.close()
    }

    fun indexFolder(
        sources: MutableMap<String, ByteArray>,
        src: FileReference, path: String, progress: ProgressBar
    ) {
        if (src.isDirectory) {
            for (child in src.listChildren()) {
                indexFolder(
                    sources, child,
                    if (path.isEmpty()) child.name
                    else "$path/${child.name}", progress
                )
            }
        } else {
            sources[path] = src.readBytesSync()
            progress.total++
        }
    }

    fun indexProject(
        project: IdeaProject,
        sources: HashMap<String, ByteArray>,
        settings: ExportSettings,
        progress: ProgressBar
    ) {

        // modules have dependencies,
        //  so find them (list of source projects?),
        //  read their dependency list,
        //  and apply it ourselves (-> we need to find the build folders, too)

        val doneModules = HashSet<String>()
        val doneJars = HashSet<FileReference>()

        fun checkLibrary(library: IdeaLibrary) {
            for (jar in library.jars) {
                if (doneJars.add(jar)) {
                    indexZipFile(sources, jar, progress)
                }
            }
        }

        fun checkModule(name: String, module: IdeaModule) {
            if (name !in settings.excludedModules && doneModules.add(name)) {
                for (subName in module.moduleDependencies) {
                    checkModule(subName, project.modules[subName]!!)
                }
                for (libName in module.libraryDependencies) {
                    checkLibrary(project.libraries[libName]!!)
                }
                // todo we should not include the .jar, if it is an engine build... how?
                val moduleOutFolder = project.projectDir.getChild("out/production/$name")
                indexFolder(sources, moduleOutFolder, "", progress)
            }
        }
        for ((name, module) in project.modules) {
            checkModule(name, module)
        }
    }

}