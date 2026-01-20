package org.centrexcursionistalcoi.app.ui.dialog

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import cea_app.composeapp.generated.resources.*
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month
import org.centrexcursionistalcoi.app.data.UserInsurance
import org.centrexcursionistalcoi.app.data.fetchFilePath
import org.centrexcursionistalcoi.app.platform.PlatformOpenFileLogic
import org.centrexcursionistalcoi.app.platform.PlatformShareLogic
import org.centrexcursionistalcoi.app.process.Progress
import org.centrexcursionistalcoi.app.process.ProgressNotifier
import org.centrexcursionistalcoi.app.ui.icons.materialsymbols.MaterialSymbols
import org.centrexcursionistalcoi.app.ui.icons.materialsymbols.Share
import org.centrexcursionistalcoi.app.ui.reusable.LinearLoadingIndicator
import org.centrexcursionistalcoi.app.viewmodel.FileProviderModel
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
private fun InsuranceInfoText(labelRes: StringResource, value: String) {
    Text(
        text = buildAnnotatedString {
            withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                append(stringResource(labelRes) + ": ")
            }
            append(value)
        },
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

@Composable
fun InsuranceDialog(
    insurance: UserInsurance,
    fpm: FileProviderModel = viewModel { FileProviderModel() },
    onDismissRequest: () -> Unit
) {
    InsuranceDialog(
        insurance = insurance,
        loadingProgress = fpm.progress,
        onShareFile = { fpm.shareFile(pathProvider = it) },
        onOpenFile = { fpm.openFile(pathProvider = it) },
        onDismissRequest = onDismissRequest
    )
}

private val femecvLicenseCardDrawable = mapOf(
    2026 to Res.drawable.femecv_2026
)

@Composable
private fun InsuranceDialog(
    insurance: UserInsurance,
    loadingProgress: StateFlow<Progress?>,
    onShareFile: (pathProvider: suspend (ProgressNotifier) -> String) -> Unit,
    onOpenFile: (pathProvider: suspend (ProgressNotifier) -> String) -> Unit,
    onDismissRequest: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(stringResource(Res.string.insurance)) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                val femecvLicense = insurance.femecvLicense
                if (femecvLicense != null) {
                    val isYearly = femecvLicense.validFrom.month == Month.JANUARY && femecvLicense.validFrom.day == 1
                            && femecvLicense.validTo.month == Month.DECEMBER && femecvLicense.validTo.day == 31
                    val cardDrawable = femecvLicenseCardDrawable[femecvLicense.validFrom.year]
                    if (isYearly && cardDrawable != null) {
                        // Only yearly licenses have card
                        Image(
                            painter = painterResource(cardDrawable),
                            contentDescription = null,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }

                InsuranceInfoText(Res.string.insurance_company, insurance.insuranceCompany)
                InsuranceInfoText(Res.string.insurance_policy_number, insurance.policyNumber)
                InsuranceInfoText(Res.string.insurance_start_date, insurance.validFrom.toString())
                InsuranceInfoText(Res.string.insurance_end_date, insurance.validTo.toString())

                val documentId = insurance.files["documentId"]
                val hasDocument = insurance.documentId != null && documentId != null
                if (hasDocument) {
                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)) {
                        if (PlatformShareLogic.isSupported) {
                            IconButton(
                                onClick = {
                                    onShareFile { insurance.fetchFilePath(documentId) }
                                },
                            ) {
                                Icon(MaterialSymbols.Share, stringResource(Res.string.share))
                            }
                        }
                        if (PlatformOpenFileLogic.isSupported) {
                            OutlinedButton(
                                onClick = {
                                    onOpenFile { insurance.fetchFilePath(documentId) }
                                },
                                modifier = Modifier.weight(1f).padding(start = 8.dp)
                            ) {
                                Text(stringResource(Res.string.insurance_view_document))
                            }
                        }
                    }
                }

                loadingProgress.LinearLoadingIndicator()
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismissRequest,
            ) {
                Text(stringResource(Res.string.close))
            }
        },
    )
}

@Preview
@Composable
fun InsuranceDialog_Generic_Preview() {
    InsuranceDialog(
        insurance = UserInsurance(
            id = Uuid.random(),
            userSub = "user-sub",
            insuranceCompany = "Insurance Co.",
            policyNumber = "POL123456789",
            validFrom = LocalDate(2023, 1, 1),
            validTo = LocalDate(2024, 1, 1),
            documentId = Uuid.random(),
        ),
        loadingProgress = MutableStateFlow(null),
        onShareFile = {},
        onOpenFile = {},
        onDismissRequest = {}
    )
}

@Preview
@Composable
fun InsuranceDialog_FEMECV2026_Preview() {
    InsuranceDialog(
        insurance = UserInsurance(
            id = Uuid.random(),
            userSub = "user-sub",
            insuranceCompany = "FEMECV",
            policyNumber = "POL123456789",
            validFrom = LocalDate(2026, 1, 1),
            validTo = LocalDate(2026, 12, 31),
            documentId = Uuid.random(),
        ),
        loadingProgress = MutableStateFlow(null),
        onShareFile = {},
        onOpenFile = {},
        onDismissRequest = {}
    )
}
