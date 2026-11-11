plugins {
    id("base-conventions")
    id("integration-test-suite")
}

dependencies {
    implementation(projects.api.pluginCommons)
    implementation(projects.api.spellsAutocast)
    integrationImplementation(projects.api.player)
    integrationImplementation(projects.api.combat.combatCommons)
}
