plugins {
    id("base-conventions")
    id("integration-test-suite")
}

dependencies {
    implementation(projects.api.pluginCommons)
    implementation(projects.api.scriptAdvanced)
    integrationImplementation(projects.api.invPlugin)
    integrationImplementation(projects.api.player)
}
