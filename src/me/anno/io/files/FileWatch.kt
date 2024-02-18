package me.anno.io.files

import me.anno.utils.InternalAPI

// file change watcher
// to do why are meshes disappearing, but not re-appearing?
// to do also watch mesh files, e.g., for parallel tinkering e.g., in Blender with meshes :)
// to do if we can work together with Blender nicely, we can skip a lot of own mesh generation :)
// https://docs.oracle.com/javase/tutorial/essential/io/notification.html
// http://jnotify.sourceforge.net/

object FileWatch {

    @InternalAPI
    var watchDogAddImpl: ((file: FileReference) -> Unit)? = null

    @InternalAPI
    var watchDogRemoveImpl: ((file: FileReference) -> Unit)? = null

    /**
     * will be called, when e.g., a Mesh, loaded from a file, is created,
     * because updates of that file are of interest to us
     * */
    fun addWatchDog(file: FileReference) {
        watchDogAddImpl?.invoke(file)
    }

    /**
     * will be called, when e.g., a Mesh, loaded from file, is freed
     * */
    fun removeWatchDog(file: FileReference) {
        watchDogRemoveImpl?.invoke(file)
    }
}