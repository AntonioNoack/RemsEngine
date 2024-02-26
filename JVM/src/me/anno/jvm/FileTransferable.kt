package me.anno.jvm

import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import java.awt.datatransfer.UnsupportedFlavorException
import java.io.File

class FileTransferable(val files: List<File>) : Transferable {

    private val flavors = arrayOf(DataFlavor.javaFileListFlavor)

    override fun getTransferDataFlavors() = flavors
    override fun isDataFlavorSupported(flavor: DataFlavor?) = flavors.any { it == flavor }

    override fun getTransferData(flavor: DataFlavor?): Any {
        if (isDataFlavorSupported(flavor)) return files
        throw UnsupportedFlavorException(flavor)
    }
}