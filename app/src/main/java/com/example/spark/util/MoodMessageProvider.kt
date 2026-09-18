package com.example.spark.util

object MoodMessageProvider {

    /**
     * Immediate in-app supportive feedback message shown right after saving a mood entry.
     */
    fun getFeedbackMessage(level: Int, tags: List<String>): String {
        // Tag-specific warmth takes highest precedence if relevant
        val lowerTags = tags.map { it.lowercase() }
        when {
            lowerTags.any { it in listOf("stressed", "anxious", "overwhelmed") } -> {
                return "Take a slow breath. Be gentle with yourself today — you are doing your best."
            }
            lowerTags.any { it in listOf("tired") } -> {
                return "Rest is productive too. Honor your body and take it easy."
            }
            lowerTags.any { it in listOf("focused", "productive", "motivated") } -> {
                return "Fantastic focus! Keep riding this wave of steady momentum."
            }
            lowerTags.any { it in listOf("grateful", "excited") } -> {
                return "Wonderful warmth! Small moments of gratitude brighten the whole journey."
            }
            lowerTags.any { it in listOf("calm", "relaxed") } -> {
                return "Peaceful and grounded. Cherish this serene headspace."
            }
        }

        // Level-specific fallback
        return when (level) {
            1 -> "Thank you for checking in. Even on tough days, you are strong and worthy."
            2 -> "Some days feel heavier than others. Remember: one tiny step is more than enough."
            3 -> "Balanced and steady. Keep moving forward at your own comfortable pace."
            4 -> "Feeling good! Celebrate the small wins and keep your inner spark shining."
            5 -> "Radiant energy! What a wonderful day — keep spreading that warmth."
            else -> "Mood recorded! Keep honoring each step of your daily journey."
        }
    }

    /**
     * Reminder notification copy customized according to recent mood context.
     * Returns Pair(title, message).
     */
    fun getReminderNotificationContent(lastMoodLevel: Int?): Pair<String, String> {
        return when (lastMoodLevel) {
            1, 2 -> Pair(
                "A gentle check-in for today",
                "Be kind to yourself today. Take a mindful breath and check in."
            )
            4, 5 -> Pair(
                "Keep your spark going!",
                "Carrying positive energy forward? Take a quick moment to log today's mood."
            )
            else -> Pair(
                "Daily Mood Check-in",
                "How are you feeling today? Take a gentle moment to reflect."
            )
        }
    }
}
