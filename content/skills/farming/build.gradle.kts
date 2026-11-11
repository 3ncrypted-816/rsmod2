plugins {
    id("base-conventions")
    id("integration-test-suite")
}

dependencies {
    implementation(projects.api.pluginCommons)
    // Farmed trees are chopped with the regular woodcutting axes and success rates.
    implementation(projects.content.skills.woodcutting)
    integrationImplementation(projects.api.player)
}
