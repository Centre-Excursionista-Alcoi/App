package org.centrexcursionistalcoi.app.ui.dialog

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
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
import androidx.compose.ui.unit.dp
import cea_app.composeapp.generated.resources.*
import io.ktor.http.ContentType
import org.centrexcursionistalcoi.app.data.UserInsurance
import org.centrexcursionistalcoi.app.data.filePath
import org.centrexcursionistalcoi.app.platform.PlatformOpenFileLogic
import org.centrexcursionistalcoi.app.platform.PlatformShareLogic
import org.jetbrains.compose.resources.StringResource
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
    onDismissRequest: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(stringResource(Res.string.insurance_add_title)) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
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
                                    val path = insurance.filePath(documentId)
                                    PlatformShareLogic.share(path, ContentType.Application.Pdf)
                                },
                            ) {
                                Icon(Icons.Default.Share, stringResource(Res.string.share))
                            }
                        }
                        if (PlatformOpenFileLogic.isSupported) {
                            OutlinedButton(
                                onClick = {
                                    val path = insurance.filePath(documentId)
                                    PlatformOpenFileLogic.open(path, ContentType.Application.Pdf)
                                },
                                modifier = Modifier.weight(1f).padding(start = 8.dp)
                            ) {
                                Text(stringResource(Res.string.insurance_view_document))
                            }
                        }
                    }
                }
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
