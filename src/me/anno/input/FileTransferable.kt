package me.anno.input

import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import java.awt.datatransfer.UnsupportedFlavorException
import java.io.File
import java.io.IOException

class FileTransferable(val files: List<File>) : Transferable {

    private val tdf = arrayOf(DataFlavor.javaFileListFlavor)

    override fun getTransferDataFlavors() = tdf
    override fun isDataFlavorSupported(flavor: DataFlavor?) = tdf.any { it == flavor }

    override fun getTransferData(flavor: DataFlavor?): Any {
        if (isDataFlavorSupported(flavor)) return files
        throw UnsupportedFlavorException(flavor)
    }

}