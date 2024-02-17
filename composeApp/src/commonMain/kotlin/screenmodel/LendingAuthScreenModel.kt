package screenmodel

import backend.data.ext.InsuranceType
import backend.data.ext.Section
import backend.data.ext.Sport
import backend.wrapper.SupabaseWrapper
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray

class LendingAuthScreenModel : ScreenModel {
    val isLoading = MutableStateFlow(false)

    fun submit(
        sports: List<Sport>,
        insuranceType: InsuranceType,
        insuranceExpiration: LocalDate,
        sections: List<Section>
    ) = screenModelScope.async(Dispatchers.IO) {
        try {
            isLoading.emit(true)

            val user = SupabaseWrapper.auth.currentUserOrNull()!!
            SupabaseWrapper.postgrest
                .insert(
                    "lending_users",
                    buildJsonObject {
                        put("user_id", user.id)
                        put("year", insuranceExpiration.minus(1, DateTimeUnit.DAY).year)
                        putJsonArray("sports") {
                            for (sport in sports) add(sport.name)
                        }
                        put("insurance_type", insuranceType.name)
                        put("insurance_expiration", insuranceExpiration.toString())
                        putJsonArray("sections") {
                            for (section in sections) add(section.name)
                        }
                    }
                )
        } finally {
            isLoading.emit(false)
        }
    }
}
