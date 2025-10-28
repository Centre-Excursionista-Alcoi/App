package org.centrexcursionistalcoi.app.transfer

import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable

class ByteArrayTransferable(val bytes: ByteArray, val flavor: DataFlavor = PNGDataFlavor): Transferable {
    override fun getTransferData(flavor: DataFlavor?): Any {
        if (flavor == this.flavor) {
            return bytes
        }
        throw UnsupportedOperationException("Unsupported flavor: $flavor")
    }

    override fun isDataFlavorSupported(flavor: DataFlavor?): Boolean {
        return flavor == this.flavor
    }

    override fun getTransferDataFlavors(): Array<DataFlavor> {
        return arrayOf(flavor)
    }
}

val PNGDataFlavor = DataFlavor("image/png; class=kotlin.ByteArray", "PNG Image")
