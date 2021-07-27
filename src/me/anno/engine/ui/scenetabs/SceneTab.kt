package me.anno.engine.ui.scenetabs

import me.anno.config.DefaultConfig
import me.anno.ecs.Entity
import me.anno.ecs.prefab.Change
import me.anno.ecs.prefab.EntityPrefab
import me.anno.ecs.prefab.PrefabInspector
import me.anno.io.files.FileReference
import me.anno.ui.base.text.TextPanel
import me.anno.utils.hpc.SyncMaster

class SceneTab : TextPanel {

    val syncMaster: SyncMaster
    val inspector: PrefabInspector
    val file: FileReference

    constructor(syncMaster: SyncMaster, fileReference: FileReference) : this(syncMaster, PrefabInspector(fileReference))

    constructor(syncMaster: SyncMaster, prefab: EntityPrefab) : this(syncMaster, PrefabInspector(prefab))

    constructor(syncMaster: SyncMaster, inspector: PrefabInspector) :
            super(inspector.reference.nameWithoutExtension, DefaultConfig.style) {
        this.inspector = inspector
        this.file = inspector.reference
        this.syncMaster = syncMaster
    }

    constructor(syncMaster: SyncMaster, ref: FileReference, scene: Entity) : this(
        syncMaster,
        ref,
        extractChanges(scene)
    )

    constructor(syncMaster: SyncMaster, ref: FileReference, scene: List<Change>) :
            super(ref.nameWithoutExtension, DefaultConfig.style) {
        // todo doesn't use changes???...
        inspector = PrefabInspector(ref)
        this.file = ref
        this.syncMaster = syncMaster
    }

    fun onStart() {
        syncMaster.nextSession()
        inspector.root.physics?.startWork(syncMaster)
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