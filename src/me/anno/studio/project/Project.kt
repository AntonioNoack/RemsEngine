package me.anno.studio.project

import me.anno.io.Saveable

class Project: Saveable(){

    // todo do we need multiple targets per project?
    // todo do we need a target at all? -> yes
    // todo include the scene? or do we store it in different files?
    // todo a project always is a folder
    // todo zip this folder all the time to not waste SSD life time?
    // todo -> we need to be able to show contents of zip files then

    var targetWidth = 1920
    var targetHeight = 1080
    var targetFPS = 24f

    override fun getClassName() = "Project"
    override fun getApproxSize() = 1000
    override fun isDefaultValue() = false

}