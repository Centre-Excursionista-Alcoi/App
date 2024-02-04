package backend.data.ext

import dev.icerock.moko.resources.StringResource
import resources.MR

enum class InsuranceType(val labelRes: StringResource) {
    FEMECV(MR.strings.lending_auth_insurance_federative_femecv),
    FEDERATIVE(MR.strings.lending_auth_insurance_federative_other),
    PRIVATE(MR.strings.lending_auth_insurance_private)
}
