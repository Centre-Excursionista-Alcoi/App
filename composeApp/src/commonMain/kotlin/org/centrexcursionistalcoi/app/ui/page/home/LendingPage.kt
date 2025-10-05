package org.centrexcursionistalcoi.app.ui.page.home

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Job
import org.centrexcursionistalcoi.app.data.Sports
import org.centrexcursionistalcoi.app.data.displayName
import org.centrexcursionistalcoi.app.response.ProfileResponse
import org.centrexcursionistalcoi.app.ui.reusable.ColumnWidthWrapper
import org.centrexcursionistalcoi.app.ui.reusable.DropdownSelector

typealias LendingPageOnCreate = (fullName: String, nif: String, phoneNumber: String, sports: List<Sports>, address: String, postalCode: String, city: String, province: String, country: String) -> Job

@Composable
fun LendingPage(profile: ProfileResponse, onCreate: LendingPageOnCreate) {
    val lendingUser = profile.lendingUser
    if (lendingUser != null) {

    } else {
        LendingUserSignUpPage(onCreate)
    }
}

@Composable
fun LendingUserSignUpPage(
    onCreate: LendingPageOnCreate
) {
    ColumnWidthWrapper(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(8.dp)
    ) {
        Text(
            text = "Sign up required",
            style = MaterialTheme.typography.titleLarge,
        )
        Text(
            text = """
                Amb aquest formulari el soci/a certifica, que es coneixedor de les normes internes de les Seccions d'Escalada i Muntanyisme per la utilització de material tècnic de propietat del CEA. Que té la formació i coneixements necessaris per la utilització d'aquest tipus de material, sent, el soci/a, l'únic responsable del correcte ús i del correcte manteniment, així com l'obligatorietat de notificar, als responsables de les Seccions, de qualsevol desperfecte o mal ús d'aquest material.
            """.trimIndent(),
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            text = "A més, el soci/a es compromet a no fer ús del material per a activitats que no estiguen organitzades pel CEA o membres del CEA, així com a no cedir-lo a tercers.",
            style = MaterialTheme.typography.bodyMedium,
        )

        var fullName by remember { mutableStateOf("") }
        var nif by remember { mutableStateOf("") }
        var phoneNumber by remember { mutableStateOf("") }
        var sports by remember { mutableStateOf(emptyList<Sports>()) }
        var address by remember { mutableStateOf("") }
        var postalCode by remember { mutableStateOf("") }
        var city by remember { mutableStateOf("") }
        var province by remember { mutableStateOf("") }
        var country by remember { mutableStateOf("") }
        var conditionsAccepted by remember { mutableStateOf(false) }

        var isLoading by remember { mutableStateOf(false) }

        OutlinedTextField(
            value = fullName,
            onValueChange = { fullName = it },
            label = { Text("Full Name") },
            placeholder = { Text("Jordi Ferrandis i Carbonell") },
            singleLine = true,
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = nif,
            onValueChange = { nif = it },
            label = { Text("NIF") },
            placeholder = { Text("12345678A") },
            singleLine = true,
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = phoneNumber,
            onValueChange = { phoneNumber = it },
            label = { Text("Phone Number") },
            placeholder = { Text("+34123456789") },
            singleLine = true,
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth()
        )
        DropdownSelector(
            selection = sports,
            options = Sports.entries,
            onSelectionChange = { sports = it },
            label = "Sports",
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth(),
            itemToString = { it.displayName }
        )
        OutlinedTextField(
            value = address,
            onValueChange = { address = it },
            label = { Text("Address") },
            placeholder = { Text("Carrer de l'Exemple, 1") },
            singleLine = true,
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = postalCode,
            onValueChange = { postalCode = it },
            label = { Text("Postal Code") },
            placeholder = { Text("03801") },
            singleLine = true,
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = city,
            onValueChange = { city = it },
            label = { Text("City") },
            placeholder = { Text("Alcoi") },
            singleLine = true,
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = province,
            onValueChange = { province = it },
            label = { Text("Province") },
            placeholder = { Text("Alacant") },
            singleLine = true,
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = country,
            onValueChange = { country = it },
            label = { Text("Country") },
            placeholder = { Text("Spain") },
            singleLine = true,
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(conditionsAccepted, onCheckedChange = { conditionsAccepted = it }, enabled = !isLoading)
            Text("I accept the terms and conditions", Modifier.weight(1f).padding(start = 8.dp))
        }

        val valid = conditionsAccepted && fullName.isNotBlank() && nif.isNotBlank() && phoneNumber.isNotBlank() && sports.isNotEmpty() && address.isNotBlank() && postalCode.isNotBlank() && city.isNotBlank() && province.isNotBlank() && country.isNotBlank()

        OutlinedButton(
            onClick = {
                isLoading = true
                onCreate(fullName, nif, phoneNumber, sports, address, postalCode, city, province, country).invokeOnCompletion {
                    isLoading = false
                }
            },
            enabled = valid && !isLoading,
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
        ) {
            Text("Submit")
        }
    }
}
