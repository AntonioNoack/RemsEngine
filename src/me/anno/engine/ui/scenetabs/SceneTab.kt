package me.anno.engine.ui.scenetabs

import me.anno.config.DefaultConfig
import me.anno.ecs.Entity
import me.anno.ecs.prefab.Change
import me.anno.ecs.prefab.Prefab
import me.anno.ecs.prefab.PrefabInspector
import me.anno.io.files.FileReference
import me.anno.ui.base.text.TextPanel
import me.anno.utils.hpc.SyncMaster

class SceneTab : TextPanel {

    val syncMaster: SyncMaster
    val inspector: PrefabInspector
    val file: FileReference

    constructor(syncMaster: SyncMaster, fileReference: FileReference, classNameIfNull: String) : this(
        syncMaster,
        PrefabInspector(fileReference, classNameIfNull)
    )

    constructor(syncMaster: SyncMaster, prefab: Prefab) : this(syncMaster, PrefabInspector(prefab))

    constructor(syncMaster: SyncMaster, inspector: PrefabInspector) :
            super(inspector.reference.nameWithoutExtension, DefaultConfig.style) {
        this.inspector = inspector
        this.file = inspector.reference
        this.syncMaster = syncMaster
    }

    fun onStart() {
        syncMaster.nextSession()
        val rootEntity = inspector.root as? Entity
        rootEntity?.physics?.startWork(syncMaster)
    }

    fun onStop() {
        syncMaster.nextSession()
    }

    companion object {

        fun extractChanges(scene: Entity): List<Change> {
            TODO()
        }

    }

}