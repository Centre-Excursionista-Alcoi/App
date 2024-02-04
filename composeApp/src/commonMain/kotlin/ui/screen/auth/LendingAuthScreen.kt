package ui.screen.auth

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import backend.data.ext.InsuranceType
import backend.data.user.UserData
import backend.supabase
import dev.icerock.moko.resources.compose.stringResource
import io.github.jan.supabase.gotrue.auth
import kotlinx.datetime.LocalDate
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject
import resources.MR
import ui.reusable.form.FormCheckbox
import ui.reusable.form.FormCheckboxList
import ui.reusable.form.FormColumn
import ui.reusable.form.FormDatePicker
import ui.reusable.form.FormField
import ui.reusable.form.FormSelect
import ui.screen.BaseScreen
import utils.toLocalDate

class LendingAuthScreen : BaseScreen({ stringResource(MR.strings.lending_auth_title) }, true) {
    @Composable
    @OptIn(ExperimentalMaterial3Api::class)
    override fun ScreenContent() {
        val user = supabase.auth.currentUserOrNull() ?: return
        val data = Json.decodeFromJsonElement<UserData>(user.userMetadata!!.jsonObject)

        var sports by remember {
            mutableStateOf(listOf(false, false, false, false, false, false))
        }
        var insuranceType by remember { mutableStateOf(InsuranceType.FEMECV) }
        var insuranceExpiration by remember { mutableStateOf<LocalDate?>(null) }
        var sections by remember {
            mutableStateOf(listOf(false, false, false, false, false, false, false))
        }
        var checkRules by remember { mutableStateOf(false) }
        var checkPreparation by remember { mutableStateOf(false) }
        var checkResponsibility by remember { mutableStateOf(false) }
        var checkMemory by remember { mutableStateOf(false) }

        Scaffold { paddingValues ->
            FormColumn(
                modifier = Modifier.padding(paddingValues).padding(8.dp),
                contentModifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text(
                    text = stringResource(MR.strings.lending_auth_title),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = stringResource(MR.strings.lending_auth_message),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(16.dp))

                FormField(
                    value = TextFieldValue(data.fullName),
                    onValueChange = {},
                    label = stringResource(MR.strings.lending_auth_name),
                    readOnly = true,
                    modifier = Modifier.fillMaxWidth(),
                    supportingText = stringResource(MR.strings.lending_auth_name_info)
                )
                FormField(
                    value = TextFieldValue(user.email!!),
                    onValueChange = {},
                    label = stringResource(MR.strings.lending_auth_email),
                    readOnly = true,
                    modifier = Modifier.fillMaxWidth()
                )
                FormField(
                    value = TextFieldValue(data.phone),
                    onValueChange = {},
                    label = stringResource(MR.strings.lending_auth_phone),
                    readOnly = true,
                    modifier = Modifier.fillMaxWidth()
                )
                FormField(
                    value = TextFieldValue(data.birthday.toLocalDate().toString()),
                    onValueChange = {},
                    label = stringResource(MR.strings.lending_auth_birthday),
                    readOnly = true,
                    modifier = Modifier.fillMaxWidth()
                )
                FormField(
                    value = TextFieldValue(data.city),
                    onValueChange = {},
                    label = stringResource(MR.strings.lending_auth_city),
                    readOnly = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = stringResource(MR.strings.lending_auth_check_info),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                )

                Text(
                    text = stringResource(MR.strings.lending_auth_sports),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                )
                Text(
                    text = stringResource(MR.strings.lending_auth_sports_message),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                )
                FormCheckboxList(
                    sports,
                    { sports = it },
                    listOf(
                        MR.strings.lending_auth_sports_climbing_1,
                        MR.strings.lending_auth_sports_climbing_2,
                        MR.strings.lending_auth_sports_via_ferrata,
                        MR.strings.lending_auth_sports_canyoning,
                        MR.strings.lending_auth_sports_trekking,
                        MR.strings.lending_auth_sports_mountaineering
                    ).map { { stringResource(it) } },
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = stringResource(MR.strings.lending_auth_insurance),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                )
                Text(
                    text = stringResource(MR.strings.lending_auth_insurance_message),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                )
                FormSelect(
                    value = insuranceType,
                    onValueChanged = { insuranceType = it },
                    options = InsuranceType.entries,
                    label = stringResource(MR.strings.lending_auth_insurance),
                    modifier = Modifier.fillMaxWidth(),
                    toStringConverter = { stringResource(it.labelRes) }
                )
                FormDatePicker(
                    value = insuranceExpiration,
                    onValueChange = { insuranceExpiration = it },
                    label = stringResource(MR.strings.lending_auth_insurance_expire),
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = stringResource(MR.strings.lending_auth_sections),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                )
                Text(
                    text = stringResource(MR.strings.lending_auth_sections_message),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                )
                FormCheckboxList(
                    sections,
                    { sections = it },
                    listOf(
                        MR.strings.lending_auth_sections_mountaineering,
                        MR.strings.lending_auth_sections_climbing,
                        MR.strings.lending_auth_sections_speleology,
                        MR.strings.lending_auth_sections_orientation,
                        MR.strings.lending_auth_sections_nordic_walking,
                        MR.strings.lending_auth_sections_mountain_trail,
                        MR.strings.lending_auth_sections_btt
                    ).map { { stringResource(it) } },
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = stringResource(MR.strings.lending_auth_verification),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                )
                FormCheckbox(
                    checked = checkRules,
                    onCheckedChange = { checkRules = it },
                    text = stringResource(MR.strings.lending_auth_verification_rules)
                )
                FormCheckbox(
                    checked = checkPreparation,
                    onCheckedChange = { checkPreparation = it },
                    text = stringResource(MR.strings.lending_auth_verification_preparation)
                )
                FormCheckbox(
                    checked = checkResponsibility,
                    onCheckedChange = { checkResponsibility = it },
                    text = stringResource(MR.strings.lending_auth_verification_responsibility)
                )
                FormCheckbox(
                    checked = checkMemory,
                    onCheckedChange = { checkMemory = it },
                    text = stringResource(MR.strings.lending_auth_verification_memory)
                )

                TextButton(
                    onClick = { /*TODO*/ },
                    modifier = Modifier
                        .align(Alignment.End)
                        .padding(horizontal = 16.dp)
                        .padding(top = 24.dp),
                    enabled = sports.any { it } &&
                        insuranceExpiration != null &&
                        sections.any { it } &&
                        checkRules &&
                        checkPreparation &&
                        checkResponsibility &&
                        checkMemory
                ) {
                    Text(stringResource(MR.strings.lending_auth_submit))
                }
            }
        }
    }
}
