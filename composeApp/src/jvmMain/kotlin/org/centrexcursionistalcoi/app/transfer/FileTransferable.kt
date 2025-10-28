package org.centrexcursionistalcoi.app.transfer

import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import java.io.File

class FileTransferable(private val files: List<File>) : Transferable {
    constructor(file: File) : this(listOf(file))

    override fun getTransferData(flavor: DataFlavor?): Any {
        if (flavor == DataFlavor.javaFileListFlavor) {
            return files
        }
        throw UnsupportedOperationException("Unsupported flavor: $flavor")
    }

    override fun isDataFlavorSupported(flavor: DataFlavor?): Boolean {
        return flavor == DataFlavor.javaFileListFlavor
    }

    override fun getTransferDataFlavors(): Array<DataFlavor> {
        return arrayOf(DataFlavor.javaFileListFlavor)
    }
}
