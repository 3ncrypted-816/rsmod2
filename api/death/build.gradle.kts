plugins {
    id("base-conventions")
}

kotlin {
    explicitApi()
}

dependencies {
    implementation(libs.guice)
    implementation(projects.api.areaChecker)
    implementation(projects.api.config)
    implementation(projects.api.dropTable)
    implementation(projects.api.hunt)
    implementation(projects.api.random)
    implementation(projects.api.npc)
    implementation(projects.api.player)
    implementation(projects.api.playerOutput)
    implementation(projects.api.repo)
    implementation(projects.api.route)
    implementation(projects.api.stats.levelmod)
    implementation(projects.api.type.typeReferences)
    implementation(projects.engine.game)
    implementation(projects.engine.map)
}
