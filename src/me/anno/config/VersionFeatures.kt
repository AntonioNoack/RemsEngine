package me.anno.config

import me.anno.io.utils.StringMap

abstract class VersionFeatures(val oldVersion: Int){

    abstract fun addNewPackages(config: StringMap)
    fun addVersion(introductionVersion: Int, modify: () -> Unit){
        if(oldVersion < introductionVersion){
            modify()
        }
    }

}