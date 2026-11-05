package dev.brandonvargas.conceptgames.engine.validate

/**
 * What the AI has written so far, counted from the partial JSON while it streams in.
 * Each section uses a key no other section has, so a simple count is accurate even mid-object.
 */
data class PackProgress(
    val concepts: Int = 0,
    val facts: Int = 0,
    val questions: Int = 0,
    val clozes: Int = 0,
    val timelines: Int = 0,
    val categories: Int = 0,
    val oddSets: Int = 0,
    val characters: Int = 0,
) {
    val items: Int get() = concepts + facts + questions + clozes + timelines + categories + oddSets

    companion object {
        private fun key(name: String) = Regex("\"$name\"\\s*:")
        private val definition = key("definition")
        private val statement = key("statement")
        private val question = key("question")
        private val sentence = key("sentence")
        private val steps = key("steps")
        private val members = key("members")
        private val odd = key("odd")

        fun of(partialJson: CharSequence): PackProgress = PackProgress(
            concepts = definition.findAll(partialJson).count(),
            facts = statement.findAll(partialJson).count(),
            questions = question.findAll(partialJson).count(),
            clozes = sentence.findAll(partialJson).count(),
            timelines = steps.findAll(partialJson).count(),
            categories = members.findAll(partialJson).count(),
            oddSets = odd.findAll(partialJson).count(),
            characters = partialJson.length,
        )
    }
}
