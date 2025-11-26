package org.centrexcursionistalcoi.app.ui.page.main.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cea_app.composeapp.generated.resources.*
import kotlinx.coroutines.Job
import org.centrexcursionistalcoi.app.data.Department
import org.centrexcursionistalcoi.app.data.UserData
import org.jetbrains.compose.resources.stringResource

@Composable
fun DepartmentPendingJoinRequest(
    userData: UserData,
    department: Department,
    onApprove: () -> Job,
    onDeny: () -> Job,
) {
    var isLoading by remember { mutableStateOf(false) }

    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = userData.fullName,
                    modifier = Modifier.padding(start = 8.dp, top = 8.dp),
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = stringResource(Res.string.management_department_join_request),
                    modifier = Modifier.padding(start = 8.dp),
                )
                Text(
                    text = department.displayName,
                    modifier = Modifier.padding(start = 8.dp, bottom = 8.dp),
                    fontWeight = FontWeight.Bold,
                )
            }
            IconButton(
                enabled = !isLoading,
                onClick = {
                    isLoading = true
                    onDeny().invokeOnCompletion { isLoading = false }
                }
            ) {
                Icon(
                    Icons.Default.Close,
                    stringResource(Res.string.management_department_join_request_deny),
                    tint = Color(0xFFE32C2C),
                )
            }
            IconButton(
                enabled = !isLoading,
                onClick = {
                    isLoading = true
                    onApprove().invokeOnCompletion { isLoading = false }
                }
            ) {
                Icon(
                    Icons.Default.Check,
                    stringResource(Res.string.management_department_join_request_approve),
                    tint = Color(0xff4caf50),
                )
            }
        }
    }
}
