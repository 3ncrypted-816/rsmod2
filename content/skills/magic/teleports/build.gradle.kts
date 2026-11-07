plugins {
    id("base-conventions")
    id("integration-test-suite")
}

dependencies {
    integrationImplementation(projects.api.spellsRunes)
    implementation(projects.api.pluginCommons)
    implementation(projects.api.spells)
    integrationImplementation(projects.api.invPlugin)
    integrationImplementation(projects.api.player)
    integrationImplementation(projects.api.teleblock)
}
