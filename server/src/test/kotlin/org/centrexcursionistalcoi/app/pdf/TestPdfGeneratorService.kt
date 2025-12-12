@file:Suppress("unused")

package org.centrexcursionistalcoi.app.pdf

import org.centrexcursionistalcoi.app.data.*
import org.centrexcursionistalcoi.app.data.ReferencedInventoryItem.Companion.referenced
import org.centrexcursionistalcoi.app.data.ReferencedInventoryItemType.Companion.referenced
import org.centrexcursionistalcoi.app.test.FakeUser
import org.centrexcursionistalcoi.app.utils.Zero
import org.centrexcursionistalcoi.app.utils.toUuid
import java.io.File
import kotlin.uuid.Uuid

class TestPdfGeneratorService {
    // Disable generation because it is only for testing, and requires manual verification
    // @Test
    fun generate() {
        val departmentImageFileId = "71eafe26-0c06-4f5b-a534-960e2901bb02".toUuid()
        val department = Department(
            id = "52c09ab7-fe66-4a1a-a202-1a6124614490".toUuid(),
            displayName = "Escalada",
            image = departmentImageFileId,
            members = emptyList(),
        )

        val memoryImageFileUuid = "b566d457-0226-49da-bb2f-f89971878c30".toUuid()
        val memory = LendingMemory(
            place = "Alcoi - Ull del Moro",
            members = listOf(FakeUser.MEMBER_NUMBER),
            externalUsers = "Pep Gimeno\nJoan Miró",
            text = "S'ha realitzat una activitat molt divertida. Jo què sé què més posar aquí, ha estat genial.",
            sport = Sports.ORIENTEERING,
            department = department.id,
            files = listOf(memoryImageFileUuid),
        )

        val inventoryItemTypeId = "f3ca1b24-886e-4b1a-88f3-934babbaec86".toUuid()
        val inventoryItemType = InventoryItemType(
            id = inventoryItemTypeId,
            displayName = "Inventory Item",
            description = "Description",
            categories = emptyList(),
            department = null,
            image = null,
        ).referenced(emptyList())

        File("document.pdf").outputStream().use { outputStream ->
            PdfGeneratorService.generateLendingPdf(
                memory = memory.referenced(listOf(FakeUser.member()), listOf(department)),
                itemsUsed = listOf(
                    InventoryItem(
                        id = Uuid.Zero,
                        variation = null,
                        type = inventoryItemTypeId,
                        nfcId = null,
                        manufacturerTraceabilityCode = null,
                    ).referenced(inventoryItemType)
                ),
                submittedBy = "Admin User",
                dateRange = Pair(
                    java.time.LocalDate.of(2025, 10, 8),
                    java.time.LocalDate.of(2025, 10, 9)
                ),
                photoProvider = { uuid ->
                    this::class.java.getResourceAsStream("/square.png")!!.readBytes()
                },
                outputStream = outputStream
            )
        }
    }
}
