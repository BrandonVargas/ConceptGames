plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    api(libs.kotlinx.serialization.json)

    testImplementation(kotlin("test"))
    testImplementation(libs.junit)
}

tasks.test {
    // Tests run every bundled sample pack through the validator and all game builders.
    val samples = rootDir.resolve("app/src/main/assets/sample_packs")
    inputs.dir(samples).withPropertyName("samples").withPathSensitivity(PathSensitivity.RELATIVE)
    systemProperty("samplesDir", samples.path)
}
