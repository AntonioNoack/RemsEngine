package me.anno.studio.rems

import me.anno.config.VersionFeatures
import me.anno.io.utils.StringMap
import me.anno.studio.StudioBase

class RemsVersionFeatures(oldVersion: Int): VersionFeatures(oldVersion) {

    constructor(config: StringMap): this(config["version", -1])

    override fun addNewPackages(config: StringMap) {

        // when new stuff is added, it can be forced upon the user
        // DefaultConfig["createNewInstancesList"].removeAll { it is NewType }
        // DefaultConfig["createNewInstancesList"].add("newName" to NewType())
        addVersion(10002){

        }

        config["version"] = StudioBase.instance.versionNumber

    }

}