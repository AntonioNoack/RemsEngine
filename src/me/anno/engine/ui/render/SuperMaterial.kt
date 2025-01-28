package me.anno.engine.ui.render

import me.anno.ecs.annotations.ExtendableEnum
import me.anno.ecs.components.mesh.material.Material
import me.anno.engine.DefaultAssets
import me.anno.language.translation.NameDesc

/**
 * Extendable enum for the material-overrides selectable in the editor.
 * */
class SuperMaterial(
    override val nameDesc: NameDesc,
    val material: Material?
) : ExtendableEnum {

    constructor(name: String, material: Material?) :
            this(NameDesc(name), material)

    init {
        material?.name = nameDesc.name
        Companion.values.add(this)
    }

    override val id: Int = values.indexOf(this)

    override val values: List<ExtendableEnum>
        get() = SuperMaterial.values

    @Suppress("unused")
    companion object {

        val values = ArrayList<SuperMaterial>()

        val NONE = SuperMaterial("None", null)

        val GLASS = SuperMaterial("Glass", DefaultAssets.glassMaterial)
        val SILVER = SuperMaterial("Silver", DefaultAssets.silverMaterial)
        val STEEL = SuperMaterial("Steel", DefaultAssets.steelMaterial)
        val GOLDEN = SuperMaterial("Golden", DefaultAssets.goldenMaterial)
        val WHITE = SuperMaterial("White", DefaultAssets.whiteMaterial)
        val UV = SuperMaterial("UV Debug", DefaultAssets.uvDebugMaterial)
    }
}