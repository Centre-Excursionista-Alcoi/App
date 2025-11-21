package org.centrexcursionistalcoi.app.ui.page.main.profile

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cea_app.composeapp.generated.resources.*
import org.centrexcursionistalcoi.app.response.ProfileResponse
import org.centrexcursionistalcoi.app.ui.reusable.InformationCard
import org.jetbrains.compose.resources.stringResource

@Composable
fun PersonalInformationCard(profile: ProfileResponse) {
    InformationCard(
        title = stringResource(Res.string.personal_info),
        modifier = Modifier.fillMaxWidth().padding(8.dp),
    ) {
        OutlinedTextField(
            value = profile.fullName,
            onValueChange = {},
            label = { Text(stringResource(Res.string.personal_info_full_name)) },
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            readOnly = true,
        )
        OutlinedTextField(
            value = profile.email,
            onValueChange = {},
            label = { Text(stringResource(Res.string.personal_info_email)) },
            modifier = Modifier.fillMaxWidth(),
            readOnly = true,
        )
    }
}
