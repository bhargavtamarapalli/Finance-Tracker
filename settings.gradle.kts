pluginManagement {
  repositories {
    google()
    mavenCentral()
    gradlePluginPortal()
  }
}

plugins { id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0" }

dependencyResolutionManagement {
  repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
  repositories {
    google()
    mavenCentral()
  }
}

rootProject.name = "My Application"

include(":app", ":FinanceManager-tests:app")
project(":FinanceManager-tests").projectDir = file("../FinanceManager-tests")
project(":FinanceManager-tests:app").projectDir = file("../FinanceManager-tests/app")
