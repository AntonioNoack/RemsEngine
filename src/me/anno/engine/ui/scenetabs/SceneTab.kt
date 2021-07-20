package me.anno.engine.ui.scenetabs

import me.anno.config.DefaultConfig
import me.anno.ecs.Entity
import me.anno.ecs.prefab.Change
import me.anno.ecs.prefab.EntityPrefab
import me.anno.ecs.prefab.PrefabInspector
import me.anno.io.files.FileReference
import me.anno.ui.base.text.TextPanel

class SceneTab : TextPanel {

    val inspector: PrefabInspector
    val file: FileReference

    constructor(fileReference: FileReference): this(PrefabInspector(fileReference))

    constructor(prefab: EntityPrefab): this(PrefabInspector(prefab))

    constructor(inspector: PrefabInspector):
            super(inspector.reference.nameWithoutExtension, DefaultConfig.style){
        this.inspector = inspector
        this.file = inspector.reference
    }

    constructor(ref: FileReference, scene: Entity) : this(ref, extractChanges(scene))

    constructor(ref: FileReference, scene: List<Change>) :
            super(ref.nameWithoutExtension, DefaultConfig.style) {
        this.file = ref
        inspector = PrefabInspector(ref)
    }

    companion object {

        fun extractChanges(scene: Entity): List<Change> {
            TODO()
        }

    }

}