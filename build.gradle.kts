plugins {
    java
}

group "me.aleksilassila.islands"
version "5.1.0-BW.V1"

repositories {
    mavenCentral()
    maven { url = uri("https://repo.papermc.io/repository/maven-public/") }
    maven { url = uri("https://oss.sonatype.org/content/repositories/snapshots") }
    maven { url = uri("https://maven.enginehub.org/repo/") }
    maven { url = uri("https://jitpack.io") }
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21-R0.1-SNAPSHOT")
    compileOnly("com.sk89q.worldedit:worldedit-bukkit:7.3.0-SNAPSHOT")
    compileOnly("com.github.MilkBowl:VaultAPI:1.7.1")
    compileOnly("com.github.GriefPrevention:GriefPrevention:16.18.3")

    implementation("org.jetbrains:annotations:20.1.0")
    implementation("com.github.stefvanschie.inventoryframework:IF:0.10.14")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

tasks.register<Copy>("copyJar") {
    dependsOn("jar") // Ensure the jar task is run before this one
    from(layout.buildDirectory.file("libs/${project.name}.jar"))
    into("server/plugins")
}

tasks.named("build") {
    dependsOn("copyJar")
}

tasks.jar {
    from(configurations.runtimeClasspath.get().map({ if(it.isDirectory) it else zipTree(it) }))
}
