plugins {
    id("base-conventions")
    id("integration-test-suite")
}

dependencies {
    integrationImplementation(projects.api.spellsRunes)
    implementation(projects.api.pluginCommons)
    implementation(projects.api.spells)
    implementation(projects.content.skills.smithing)
    integrationImplementation(projects.api.invPlugin)
    integrationImplementation(projects.api.player)
    integrationImplementation(projects.api.spells)
    integrationImplementation(projects.content.skills.smithing)
}
