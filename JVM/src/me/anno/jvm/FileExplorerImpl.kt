package me.anno.jvm

import me.anno.io.files.FileFileRef
import me.anno.io.files.FileReference
import me.anno.jvm.utils.BetterProcessBuilder
import me.anno.utils.OS
import org.apache.logging.log4j.LogManager

object FileExplorerImpl {

    private val LOGGER = LogManager.getLogger(FileExplorerImpl::class)

    fun createLink(src: FileReference, dst: FileReference, tmp: FileReference?): FileReference? {
        var tmp1 = tmp
        when {
            OS.isWindows -> {
                if (tmp1 == null) tmp1 = FileFileRef.createTempFile("create-link", ".ps1")
                tmp1.writeText(
                    "" + // param ( [string]$SourceExe, [string]$DestinationPath )
                            "\$WshShell = New-Object -comObject WScript.Shell\n" +
                            "\$Shortcut = \$WshShell.CreateShortcut(\"${src.absolutePath}\")\n" +
                            "\$Shortcut.TargetPath = \"${dst.absolutePath}\"\n" +
                            "\$Shortcut.Save()"
                )
                // PowerShell.exe -ExecutionPolicy Unrestricted -command "C:\temp\TestPS.ps1"
                val builder = BetterProcessBuilder("PowerShell.exe", 16, false)
                builder.add("-ExecutionPolicy")
                builder.add("Unrestricted")
                builder.add("-command")
                builder.add(tmp1.absolutePath)
                builder.startAndPrint().waitFor()
            }
            OS.isLinux || OS.isMacOS -> {
                // create symbolic link
                // ln -s target_file link_name
                val builder = BetterProcessBuilder("ln", 3, false)
                builder.add("-s") // symbolic link
                builder.add(dst.absolutePath)
                builder.add(src.absolutePath)
                builder.startAndPrint()
            }
            else -> LOGGER.warn("Don't know how to create links")
        }
        return tmp1
    }
}