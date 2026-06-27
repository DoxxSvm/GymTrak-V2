package `in`.gym.trak.studio.viewmodel.member

import `in`.gym.trak.studio.data.model.MemberOnboardingDraft
import `in`.gym.trak.studio.data.model.MemberOnboardingRequest

object MemberOnboardingValidation {

    const val MIN_AGE = 13
    const val MAX_AGE = 100
    const val MIN_HEIGHT_CM = 50
    const val MAX_HEIGHT_CM = 280
    const val MIN_WEIGHT_KG = 25
    const val MAX_WEIGHT_KG = 350

    fun genderToApi(label: String): String = when (label) {
        "Male" -> "MALE"
        "Female" -> "FEMALE"
        else -> "OTHER"
    }

    fun activityToApi(label: String): String = when (label) {
        "High" -> "HIGH"
        "Moderate" -> "MODERATE"
        "Low" -> "LOW"
        else -> "LOW"
    }

    /** Maps stored/API activity (e.g. HIGH) to UI card label. */
    fun activityFromApi(api: String?): String {
        if (api.isNullOrBlank()) return "Moderate"
        return when (api.uppercase()) {
            "HIGH" -> "High"
            "MODERATE" -> "Moderate"
            "LOW" -> "Low"
            else -> "Moderate"
        }
    }

    /** Maps stored/API fitness goal (e.g. LOSE_WEIGHT) to UI card label. */
    fun fitnessGoalFromApi(api: String?): String {
        if (api.isNullOrBlank()) return "Stay Fit"
        return when (api.uppercase()) {
            "LOSE_WEIGHT" -> "Lose Weight"
            "BUILD_MUSCLE" -> "Build Muscle"
            "STAY_FIT" -> "Stay Fit"
            "IMPROVE_ENDURANCE" -> "Improve Endurance"
            else -> "Stay Fit"
        }
    }

    fun fitnessGoalToApi(label: String): String = when (label) {
        "Lose Weight" -> "LOSE_WEIGHT"
        "Build Muscle" -> "BUILD_MUSCLE"
        "Stay Fit" -> "STAY_FIT"
        "Improve Endurance" -> "IMPROVE_ENDURANCE"
        else -> "STAY_FIT"
    }

    /**
     * @param heightRaw numeric text in [heightUnit] ("Cm" or "In")
     * @param weightRaw numeric text in [weightUnit] ("Kg" or "Lbs")
     */
    fun parsePhysicalMetrics(
        heightRaw: String,
        heightUnit: String,
        weightRaw: String,
        weightUnit: String
    ): Pair<Int?, Int?> {
        val h = heightRaw.trim().toDoubleOrNull() ?: return null to null
        val w = weightRaw.trim().toDoubleOrNull() ?: return null to null
        val heightCm = when (heightUnit) {
            "In" -> (h * 2.54).toInt().coerceAtLeast(1)
            else -> h.toInt().coerceAtLeast(1)
        }
        val weightKg = when (weightUnit) {
            "Lbs" -> (w * 0.45359237).toInt().coerceAtLeast(1)
            else -> w.toInt().coerceAtLeast(1)
        }
        return heightCm to weightKg
    }

    data class FieldErrors(
        val fullName: String? = null,
        val age: String? = null,
        val gender: String? = null,
        val height: String? = null,
        val weight: String? = null,
    ) {
        val hasErrors: Boolean
            get() = listOfNotNull(fullName, age, gender, height, weight).isNotEmpty()
    }

    fun validateBasics(
        fullName: String,
        ageText: String,
        genderLabel: String,
        heightRaw: String,
        heightUnit: String,
        weightRaw: String,
        weightUnit: String
    ): FieldErrors {
        var nameErr: String? = null
        var ageErr: String? = null
        var genderErr: String? = null
        var heightErr: String? = null
        var weightErr: String? = null

        val trimmedName = fullName.trim()
        if (trimmedName.length < 2) {
            nameErr = "Enter your full name (at least 2 characters)."
        }

        val age = ageText.trim().toIntOrNull()
        if (age == null) {
            ageErr = "Enter a valid age."
        } else if (age < MIN_AGE || age > MAX_AGE) {
            ageErr = "Age must be between $MIN_AGE and $MAX_AGE."
        }

        if (genderLabel.isBlank()) {
            genderErr = "Select a gender."
        }

        val (heightCm, weightKg) = parsePhysicalMetrics(heightRaw, heightUnit, weightRaw, weightUnit)
        if (heightCm == null || heightRaw.isBlank()) {
            heightErr = "Enter a valid height."
        } else if (heightCm !in MIN_HEIGHT_CM..MAX_HEIGHT_CM) {
            heightErr = "Height looks invalid. Check the value and unit."
        }

        if (weightKg == null || weightRaw.isBlank()) {
            weightErr = "Enter a valid weight."
        } else if (weightKg !in MIN_WEIGHT_KG..MAX_WEIGHT_KG) {
            weightErr = "Weight looks invalid. Check the value and unit."
        }

        return FieldErrors(nameErr, ageErr, genderErr, heightErr, weightErr)
    }

    /** Height/weight only (e.g. physical metrics screen). */
    fun validateHeightWeightOnly(
        heightRaw: String,
        heightUnit: String,
        weightRaw: String,
        weightUnit: String,
    ): Pair<String?, String?> {
        var heightErr: String? = null
        var weightErr: String? = null
        val (heightCm, weightKg) = parsePhysicalMetrics(heightRaw, heightUnit, weightRaw, weightUnit)
        if (heightCm == null || heightRaw.isBlank()) {
            heightErr = "Enter a valid height."
        } else if (heightCm !in MIN_HEIGHT_CM..MAX_HEIGHT_CM) {
            heightErr = "Height looks invalid. Check the value and unit."
        }
        if (weightKg == null || weightRaw.isBlank()) {
            weightErr = "Enter a valid weight."
        } else if (weightKg !in MIN_WEIGHT_KG..MAX_WEIGHT_KG) {
            weightErr = "Weight looks invalid. Check the value and unit."
        }
        return heightErr to weightErr
    }

    fun toRequest(draft: MemberOnboardingDraft): MemberOnboardingRequest? {
        val age = draft.ageYears ?: return null
        val h = draft.heightCm ?: return null
        val w = draft.weightKg ?: return null
        val activity = draft.activityLevel ?: return null
        val goal = draft.fitnessGoal ?: return null
        if (draft.fullName.trim().length < 2) return null
        return MemberOnboardingRequest(
            fullName = draft.fullName.trim(),
            heightCm = h,
            weightKg = w,
            ageYears = age,
            gender = genderToApi(draft.genderLabel),
            activityLevel = activity,
            fitnessGoal = goal,
        )
    }
}
