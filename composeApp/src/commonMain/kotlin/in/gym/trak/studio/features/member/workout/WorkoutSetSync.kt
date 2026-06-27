package `in`.gym.trak.studio.features.member

import `in`.gym.trak.studio.data.model.ActiveExercise
import `in`.gym.trak.studio.data.model.CreateSetRequest
import `in`.gym.trak.studio.data.model.ExerciseType
import `in`.gym.trak.studio.data.model.UpdateSetRequest
import `in`.gym.trak.studio.data.model.WorkoutDetailExerciseDTO
import `in`.gym.trak.studio.data.model.WorkoutSet
import gym.composeapp.generated.resources.Res
import gym.composeapp.generated.resources.ic_workout

fun workoutSetApiWeight(set: WorkoutSet, isTime: Boolean): Double? {
    if (isTime) return null
    return set.kg.toDoubleOrNull() ?: 0.0
}

fun workoutSetApiReps(set: WorkoutSet, isTime: Boolean): Int? {
    if (isTime) return null
    return set.reps.toIntOrNull() ?: 0
}

fun workoutSetApiTime(set: WorkoutSet): Int {
    return set.kg.toIntOrNull()
        ?: set.kg.toDoubleOrNull()?.toInt()
        ?: 0
}

fun buildCreateSetRequest(
    workoutExerciseId: String,
    set: WorkoutSet,
    isTime: Boolean,
): CreateSetRequest =
    if (isTime) {
        CreateSetRequest(
            workoutExerciseId = workoutExerciseId,
            setNumber = set.setNumber,
            time = workoutSetApiTime(set),
        )
    } else {
        CreateSetRequest(
            workoutExerciseId = workoutExerciseId,
            setNumber = set.setNumber,
            reps = workoutSetApiReps(set, isTime = false),
            weight = workoutSetApiWeight(set, isTime = false),
        )
    }

fun buildUpdateSetRequest(
    set: WorkoutSet,
    isTime: Boolean,
    completed: Boolean,
): UpdateSetRequest =
    if (isTime) {
        UpdateSetRequest(
            time = workoutSetApiTime(set),
            completed = completed,
        )
    } else {
        UpdateSetRequest(
            reps = workoutSetApiReps(set, isTime = false),
            weight = workoutSetApiWeight(set, isTime = false),
            completed = completed,
        )
    }

fun mergeWorkoutDetailIntoExercises(
    exercises: List<ActiveExercise>,
    detailExercises: List<WorkoutDetailExerciseDTO>,
) {
    exercises.forEach { localExercise ->
        val serverExercise = detailExercises.find { dto ->
            dto.exercise_id == localExercise.id
        } ?: return@forEach

        localExercise.workoutExerciseId = serverExercise.workout_exercise_id
            .takeIf { it.isNotBlank() }

        localExercise.sets.forEach { localSet ->
            val serverSet = serverExercise.sets.find { it.set_number == localSet.setNumber }
            if (serverSet != null) {
                localSet.serverSetId = serverSet.id.takeIf { it.isNotBlank() }
                localSet.isDone = serverSet.completed
            }
        }
    }
}

fun workoutSetFromDetail(
    setNumber: Int,
    set: `in`.gym.trak.studio.data.model.WorkoutDetailSetDTO,
    isTime: Boolean = false,
): WorkoutSet {
    val durationOrWeight = when {
        isTime && set.time != null -> set.time.toString()
        isTime -> set.weight.toInt().takeIf { it > 0 }?.toString() ?: set.weight.toString()
        set.weight % 1.0 == 0.0 -> set.weight.toInt().toString()
        else -> set.weight.toString()
    }
    return WorkoutSet(
        setNumber = setNumber,
        kg = durationOrWeight,
        reps = set.reps.toString(),
        isDone = set.completed,
        serverSetId = set.id.takeIf { it.isNotBlank() },
    )
}

fun activeExerciseFromDetail(dto: WorkoutDetailExerciseDTO): ActiveExercise {
    val isTime = dto.exercise_type?.contains("DURATION") == true
    return ActiveExercise(
        id = dto.exercise_id,
        name = dto.name,
        icon = Res.drawable.ic_workout,
        assetUrl = dto.asset_url,
        type = if (isTime) ExerciseType.TIME else ExerciseType.REPS,
        workoutExerciseId = dto.workout_exercise_id.takeIf { it.isNotBlank() },
        sets = dto.sets.map { set ->
            workoutSetFromDetail(set.set_number, set, isTime = isTime)
        }.toMutableList(),
    )
}
