package me.anno.utils.test.files

import me.anno.utils.LOGGER
import java.io.File

fun main(){

    val folder = File("E:\\Projects\\Android")
    for(file in folder.listFiles()!!){
        cleanIntellijProject(file)
    }

    val folder2 = File("E:\\Projects\\Java")
    for(file in folder2.listFiles()!!){
        cleanEclipseProject(file)
        cleanIntellijProject(file)
        deleteClassFiles(file)
        deleteEmptyFolders(file)
    }

}

fun deleteClassFiles(file: File){
    if(file.isDirectory){
        for(f in file.listFiles()!!){
            deleteClassFiles(f)
        }
    } else {
        if(file.extension.equals("class", true)){
            file.delete()
        }
    }
}

fun cleanEclipseProject(file: File){

    if(!File(file,".classpath").exists()){
        LOGGER.info("not a project: $file")
        return
    }

    File(file,"bin").deleteRecursively()

}

fun cleanIntellijProject(file: File){

    if(!file.isDirectory) return

    if(file.listFiles()!!.none { it.extension == "iml" }){
        return
    }

    LOGGER.info(file.name)

    val files = listOf(".gradle","gradle","out","build","app/build","captures", "app/.externalNativeBuild")

    for(f in files){
        File(file, f).deleteRecursively()
    }

    val release = File(file,"app/release")
    if(release.exists()){
        if(release.listFiles()!!.size > 2){
            LOGGER.info("Problematic: $file")
        } else {
            release.deleteRecursively()
        }
    }

    deleteEmptyFolders(file)

}

fun deleteEmptyFolders(file: File){

    if(!file.isDirectory) return

    for(f in file.listFiles()!!){
        deleteEmptyFolders(f)
    }

    if(file.listFiles()!!.isEmpty()){
        LOGGER.info("deleting $file")
        file.deleteRecursively()
    }

}