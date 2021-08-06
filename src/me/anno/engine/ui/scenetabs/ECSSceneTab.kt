package me.anno.engine.ui.scenetabs

import me.anno.config.DefaultConfig
import me.anno.config.DefaultStyle.black
import me.anno.ecs.Entity
import me.anno.ecs.prefab.Prefab
import me.anno.ecs.prefab.PrefabInspector
import me.anno.input.MouseButton
import me.anno.io.files.FileReference
import me.anno.ui.base.text.TextPanel
import me.anno.utils.hpc.SyncMaster

class ECSSceneTab(val syncMaster: SyncMaster, val inspector: PrefabInspector, val file: FileReference) :
    TextPanel(inspector.reference.nameWithoutExtension, DefaultConfig.style) {

    constructor(syncMaster: SyncMaster, fileReference: FileReference, classNameIfNull: String) : this(
        syncMaster,
        PrefabInspector(fileReference, classNameIfNull)
    )

    constructor(syncMaster: SyncMaster, prefab: Prefab) : this(syncMaster, PrefabInspector(prefab))

    constructor(syncMaster: SyncMaster, inspector: PrefabInspector) :
            this(syncMaster, inspector, inspector.reference)

    fun onStart() {
        syncMaster.nextSession()
        val rootEntity = inspector.root as? Entity
        rootEntity?.physics?.startWork(syncMaster)
    }

    fun onStop() {
        syncMaster.nextSession()
    }

    override fun onMouseClicked(x: Float, y: Float, button: MouseButton, long: Boolean) {
        if (button.isLeft) {
            ECSSceneTabs.open(this)
        } else {
            super.onMouseClicked(x, y, button, long)
        }
    }

    override fun tickUpdate() {
        super.tickUpdate()
        backgroundColor = if (ECSSceneTabs.currentTab == this) 0xff777777.toInt()
        else originalBGColor
    }

    fun save() {
        inspector.save()
    }

}