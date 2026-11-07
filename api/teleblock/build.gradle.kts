plugins {
    id("base-conventions")
    id("integration-test-suite")
}

kotlin {
    explicitApi()
}

dependencies {
    implementation(libs.guice)
    implementation(projects.api.areaChecker)
    implementation(projects.api.config)
    implementation(projects.api.player)
    implementation(projects.api.playerOutput)
    implementation(projects.api.script)
    implementation(projects.api.type.typeReferences)
    implementation(projects.engine.events)
    implementation(projects.engine.game)
    implementation(projects.engine.map)
    implementation(projects.engine.plugin)
    integrationImplementation(projects.api.pluginCommons)
}
