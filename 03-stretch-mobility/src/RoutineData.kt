package com.unkink.stretch

/** One stretch in a routine. [seconds] is how long to hold it. */
data class StretchStep(
    val name: String,
    val instruction: String,
    val seconds: Int,
    val side: String? = null   // e.g. "Left", "Right", or null if symmetric
)

data class Routine(
    val id: String,
    val title: String,
    val subtitle: String,
    val steps: List<StretchStep>
) {
    val totalSeconds: Int get() = steps.sumOf { it.seconds }
    val minutes: Int get() = (totalSeconds + 59) / 60
}

/** Built-in, asset-free routines shipped with v1. */
object Routines {
    val deskReset = Routine(
        id = "desk_reset",
        title = "Desk Reset",
        subtitle = "Quick relief for neck & shoulders",
        steps = listOf(
            StretchStep("Neck tilt", "Drop your right ear toward your shoulder. Breathe.", 30, "Right"),
            StretchStep("Neck tilt", "Drop your left ear toward your shoulder. Breathe.", 30, "Left"),
            StretchStep("Shoulder rolls", "Roll shoulders slowly backward, big circles.", 30),
            StretchStep("Chest opener", "Clasp hands behind back, lift and open the chest.", 40),
            StretchStep("Seated twist", "Twist gently to the right, hold the chair.", 30, "Right"),
            StretchStep("Seated twist", "Twist gently to the left, hold the chair.", 30, "Left")
        )
    )

    val lowerBack = Routine(
        id = "lower_back",
        title = "Lower Back",
        subtitle = "Ease stiffness after sitting",
        steps = listOf(
            StretchStep("Cat–cow", "On all fours, arch and round your spine slowly.", 45),
            StretchStep("Child's pose", "Sit hips back to heels, arms long, forehead down.", 45),
            StretchStep("Knee to chest", "Lie down, hug right knee to chest.", 40, "Right"),
            StretchStep("Knee to chest", "Hug left knee to chest.", 40, "Left"),
            StretchStep("Supine twist", "Drop both knees right, look left.", 40, "Right"),
            StretchStep("Supine twist", "Drop both knees left, look right.", 40, "Left")
        )
    )

    val morning = Routine(
        id = "morning",
        title = "Morning Wake-Up",
        subtitle = "Loosen up to start the day",
        steps = listOf(
            StretchStep("Reach & yawn", "Stand tall, reach overhead, lengthen the whole body.", 30),
            StretchStep("Side bend", "Reach up and lean to the right.", 25, "Right"),
            StretchStep("Side bend", "Reach up and lean to the left.", 25, "Left"),
            StretchStep("Forward fold", "Hinge at hips, let your head hang.", 40),
            StretchStep("Hip flexor", "Right foot forward in a low lunge.", 35, "Right"),
            StretchStep("Hip flexor", "Left foot forward in a low lunge.", 35, "Left"),
            StretchStep("Calf stretch", "Press right heel down, straight leg.", 30, "Right"),
            StretchStep("Calf stretch", "Press left heel down, straight leg.", 30, "Left")
        )
    )

    val all = listOf(deskReset, lowerBack, morning)
    fun byId(id: String): Routine = all.first { it.id == id }
}
