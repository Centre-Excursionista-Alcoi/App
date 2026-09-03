package org.centrexcursionistalcoi.app.ui.page.main

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cea_app.composeapp.generated.resources.Res
import cea_app.composeapp.generated.resources.forma_pv
import cea_app.composeapp.generated.resources.icon_monochrome
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import org.centrexcursionistalcoi.app.data.Department
import org.centrexcursionistalcoi.app.response.ProfileResponse
import org.centrexcursionistalcoi.app.ui.dialog.AddInsuranceDialog
import org.centrexcursionistalcoi.app.ui.dialog.CreateInsuranceRequest
import org.centrexcursionistalcoi.app.ui.page.main.profile.DepartmentsListCard
import org.centrexcursionistalcoi.app.ui.page.main.profile.FEMECVAccountCard
import org.centrexcursionistalcoi.app.ui.page.main.profile.InsurancesListCard
import org.centrexcursionistalcoi.app.ui.page.main.profile.NoInsurancesCard
import org.centrexcursionistalcoi.app.ui.page.main.profile.PersonalInformationCard
import org.centrexcursionistalcoi.app.ui.reusable.AdaptiveVerticalGrid
import org.centrexcursionistalcoi.app.ui.reusable.LoadingBox
import org.centrexcursionistalcoi.app.viewmodel.ProfilePageModel
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.viewmodel.koinViewModel

// See: https://en.wikipedia.org/wiki/ISO/IEC_7810
private const val TD1_ASPECT_RATIO = 1.586f

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ColumnScope.ProfilePage(
    model: ProfilePageModel = koinViewModel(),
) {
    val profile by model.profile.collectAsState()
    val departments by model.departments.collectAsState()
    val profileValue = profile
    if (profileValue == null) {
        LoadingBox()
        return
    }

    ProfilePage(
        profile = profileValue,
        onCreateInsurance = model::createInsurance,
        onFEMECVConnectRequested = model::connectFEMECV,
        onFEMECVDisconnectRequested = model::disconnectFEMECV,
        departments = departments,
        onJoinDepartmentRequested = model::requestJoinDepartment,
        onLeaveDepartmentRequested = model::leaveDepartment,
    )
}

@Composable
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private fun ColumnScope.ProfilePage(
    profile: ProfileResponse,
    onCreateInsurance: CreateInsuranceRequest,
    onFEMECVConnectRequested: (username: String, password: CharArray) -> Deferred<Throwable?>,
    onFEMECVDisconnectRequested: () -> Job,
    departments: List<Department>?,
    onJoinDepartmentRequested: (Department) -> Job,
    onLeaveDepartmentRequested: (Department) -> Job,
) {
    val activeInsurances = remember(profile) { profile.activeInsurances() }

    var addingInsurance by remember { mutableStateOf(false) }
    if (addingInsurance) AddInsuranceDialog(
        onCreate = onCreateInsurance,
        onDismissRequest = { addingInsurance = false }
    )

    CompositionLocalProvider(LocalContentColor provides Color.White) {
        Box(
            modifier = Modifier
                .width(400.dp)
                .aspectRatio(TD1_ASPECT_RATIO)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xffa9a587))
        ) {
            Image(
                painter = painterResource(Res.drawable.icon_monochrome),
                contentDescription = null,
                alpha = .3f,
                modifier = Modifier.align(Alignment.Center).fillMaxHeight(.7f),
            )

            Row(
                modifier = Modifier.align(Alignment.TopStart).padding(12.dp).fillMaxHeight(.3f),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Image(
                    painter = painterResource(Res.drawable.forma_pv),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(Color.White),
                )
                Text(
                    text = "Centre Excursionista\nd'Alcoi",
                    style = MaterialTheme.typography.titleLargeEmphasized,
                    fontSize = 18.sp,
                )
            }

            Column(modifier = Modifier.align(Alignment.BottomStart).padding(12.dp)) {
                Text(
                    text = profile.fullName,
                    style = MaterialTheme.typography.titleLargeEmphasized,
                    fontSize = 20.sp,
                )
                Text(
                    text = profile.memberNumber.toString(),
                    style = MaterialTheme.typography.titleLargeEmphasized,
                    fontSize = 22.sp,
                )
            }
        }
    }

    AdaptiveVerticalGrid(
        contentPadding = PaddingValues(8.dp),
        modifier = Modifier.fillMaxWidth().weight(1f).padding(16.dp)
    ) {
        item("personal_info") {
            PersonalInformationCard(profile)
        }
        if (activeInsurances.isEmpty()) item("no_insurances") {
            NoInsurancesCard(
                onAddInsuranceRequested = { addingInsurance = true }
            )
        } else item("insurances_list") {
            InsurancesListCard(
                activeInsurances = activeInsurances,
                onAddInsuranceRequested = { addingInsurance = true },
            )
        }

        item("femecv_sync") {
            FEMECVAccountCard(
                profile = profile,
                onConnectRequested = onFEMECVConnectRequested,
                onDisconnectRequested = onFEMECVDisconnectRequested,
            )
        }

        item("departments") {
            DepartmentsListCard(
                profile.sub,
                departments,
                onJoinDepartmentRequested,
                onLeaveDepartmentRequested,
            )
        }
    }
}
