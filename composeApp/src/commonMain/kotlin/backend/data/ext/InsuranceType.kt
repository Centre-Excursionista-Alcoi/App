package backend.data.ext

import app.composeapp.generated.resources.Res
import app.composeapp.generated.resources.lending_auth_insurance_federative_femecv
import app.composeapp.generated.resources.lending_auth_insurance_federative_other
import app.composeapp.generated.resources.lending_auth_insurance_private
import org.jetbrains.compose.resources.StringResource

enum class InsuranceType(val labelRes: StringResource) {
    FEMECV(Res.string.lending_auth_insurance_federative_femecv),
    FEDERATIVE(Res.string.lending_auth_insurance_federative_other),
    PRIVATE(Res.string.lending_auth_insurance_private)
}
