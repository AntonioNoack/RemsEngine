package me.anno.input

import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import java.awt.datatransfer.UnsupportedFlavorException
import java.io.File
import java.io.IOException

class FileTransferable(val files: List<File>) : Transferable {

    override fun getTransferDataFlavors(): Array<DataFlavor?>? {
        return arrayOf(DataFlavor.javaFileListFlavor)
    }

    override fun isDataFlavorSupported(flavor: DataFlavor?): Boolean {
        return DataFlavor.javaFileListFlavor.equals(flavor)
    }

    @Throws(UnsupportedFlavorException::class, IOException::class)
    override fun getTransferData(flavor: DataFlavor?): Any? {
        if (isDataFlavorSupported(flavor)) return files
        throw UnsupportedFlavorException(flavor)
    }

}