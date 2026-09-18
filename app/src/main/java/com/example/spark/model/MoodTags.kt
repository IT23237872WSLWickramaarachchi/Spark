package com.example.spark.model

data class MoodTagInfo(
    val name: String,
    val isCommon: Boolean = false
)

object MoodTags {
    val ALL: List<MoodTagInfo> = listOf(
        MoodTagInfo("Calm", isCommon = true),
        MoodTagInfo("Energetic", isCommon = true),
        MoodTagInfo("Focused", isCommon = true),
        MoodTagInfo("Productive"),
        MoodTagInfo("Motivated"),
        MoodTagInfo("Relaxed"),
        MoodTagInfo("Grateful"),
        MoodTagInfo("Excited"),
        MoodTagInfo("Social"),
        MoodTagInfo("Tired", isCommon = true),
        MoodTagInfo("Stressed", isCommon = true),
        MoodTagInfo("Anxious", isCommon = true),
        MoodTagInfo("Overwhelmed"),
        MoodTagInfo("Distracted"),
        MoodTagInfo("Lonely"),
        MoodTagInfo("Frustrated")
    )

    val COMMON: List<MoodTagInfo> = ALL.filter { it.isCommon }
    val ALL_NAMES: List<String> = ALL.map { it.name }
}
