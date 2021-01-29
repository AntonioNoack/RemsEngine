package me.anno.studio.rems

import me.anno.installer.Installer
import me.anno.language.translation.NameDesc
import me.anno.studio.StudioBase.Companion.addEvent
import me.anno.ui.base.menu.Menu
import me.anno.ui.base.menu.MenuOption
import me.anno.utils.files.Files.openInExplorer
import me.anno.utils.OS
import me.anno.utils.files.OpenInBrowser.openInBrowser
import org.apache.logging.log4j.LogManager
import java.io.File
import java.io.IOException
import java.net.URI
import java.net.URL
import kotlin.concurrent.thread

object CheckVersion {

    fun checkVersion() {
        thread {
            val latestVersion = checkVersion(URL("https://remsstudio.phychi.com/version.php?isWindows=${if(OS.isWindows) 1 else 0}"))
            if (latestVersion > -1) {
                if(latestVersion > RemsStudio.versionNumber){
                    val mega = latestVersion / 10000
                    val major = (latestVersion / 100) % 100
                    val minor = latestVersion % 100
                    val name = "RemsStudio $mega.$major.$minor.${if (OS.isWindows) "exe" else "jar"}"
                    val dst = File(OS.documents, name)
                    if(!dst.exists()){
                        LOGGER.info("Found newer version: $name")
                        // wait for everything to be loaded xD
                        addEvent {
                            Menu.openMenu(
                                NameDesc("New Version Available!", "", "ui.newVersion"), listOf(
                                    MenuOption(NameDesc("See Download Options", "", "ui.newVersion.openLink")) {
                                        URI("https", "remsstudio.phychi.com", "/", "s=download").toURL().openInBrowser()
                                    },
                                    MenuOption(NameDesc("Download with Browser", "", "ui.newVersion.openLink")) {
                                        URI("https", "remsstudio.phychi.com", "/download/$name", "").toURL().openInBrowser()
                                    },
                                    MenuOption(NameDesc("Download to ~/Documents", "", "ui.newVersion.download")) {
                                        // download the file
                                        // RemsStudio_v1.00.00.jar ?
                                        Installer.download(name, dst) {
                                            Menu.openMenu(
                                                listOf(
                                                    MenuOption(NameDesc("Downloaded file to %1", "", "")
                                                        .with("%1", dst.toString())){
                                                        dst.openInExplorer()
                                                    }
                                                )
                                            )
                                        }
                                    }
                                )
                            )
                        }
                    } else {
                        LOGGER.warn("Newer version available, but not used! $dst")
                    }
                } else {
                    LOGGER.info("The newest version is in use: ${RemsStudio.versionName}")
                }
            }
        }
    }


    fun checkVersion(url: URL): Int {
        try {
            val reader = url.openStream().bufferedReader()
            while(true){
                val line = reader.readLine() ?: break
                val index = line.indexOf(':')
                if(index > 0){
                    val key = line.substring(0,index).trim()
                    val value = line.substring(index+1).trim()
                    if(key.equals("VersionId", true)){
                        return value.toIntOrNull() ?: continue
                    }
                }
            }
        } catch (e: IOException){
            if(url.protocol.equals("https", true)){
                return checkVersion(URL(url.toString().replace("https://", "http://")))
            } else {
                LOGGER.warn("${e.javaClass.name}: ${e.message ?: ""}")
            }
        }
        return -1
    }

    private val LOGGER = LogManager.getLogger(CheckVersion::class)

}