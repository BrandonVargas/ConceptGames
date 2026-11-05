package dev.brandonvargas.conceptgames.engine

import dev.brandonvargas.conceptgames.engine.model.StudyPack
import dev.brandonvargas.conceptgames.engine.validate.PackValidator
import java.io.File

object Fixtures {
    private val samplesDir = File(System.getProperty("samplesDir") ?: "../app/src/main/assets/sample_packs")

    /** The packs bundled with the app, plus test-only fixtures in non-Latin scripts. */
    val packs: List<StudyPack> by lazy {
        val samples = samplesDir.listFiles { file -> file.extension == "json" }.orEmpty().sortedBy { it.name }
        check(samples.isNotEmpty()) { "No sample packs found in $samplesDir" }
        val fixtures = listOf("solar_system_ru.json", "photosynthesis_zh.json").map { name ->
            Fixtures::class.java.getResource("/fixtures/$name")!!.readText()
        }
        (samples.map { it.readText() } + fixtures).map { PackValidator.validate(PackValidator.parseDocument(it)) }
    }

    fun pack(language: String): StudyPack = packs.first { it.language == language }
}
