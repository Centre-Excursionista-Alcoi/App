package org.centrexcursionistalcoi.app.transfer

import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable

class TextTransferable(private val text: String) : Transferable {
    override fun getTransferData(flavor: DataFlavor?): Any {
        return text
    }

    override fun isDataFlavorSupported(flavor: DataFlavor?): Boolean {
        return flavor == DataFlavor.stringFlavor
    }

    override fun getTransferDataFlavors(): Array<DataFlavor> {
        return arrayOf(DataFlavor.stringFlavor)
    }
}
