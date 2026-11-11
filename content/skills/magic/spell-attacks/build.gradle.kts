plugins {
    id("base-conventions")
    id("integration-test-suite")
}

dependencies {
    integrationImplementation(libs.guice)
    integrationImplementation(projects.api.spellsRunes)
    implementation(projects.api.combat.combatManager)
    implementation(projects.api.pluginCommons)
    implementation(projects.api.spells)
    implementation(projects.api.toxins)
    integrationImplementation(projects.api.combat.combatCommons)
    integrationImplementation(projects.api.combat.combatWeapon)
    integrationImplementation(projects.api.player)
    integrationImplementation(projects.api.spells)
    integrationImplementation(projects.api.teleblock)
    integrationImplementation(projects.api.freeze)
    integrationImplementation(projects.api.toxins)
}
