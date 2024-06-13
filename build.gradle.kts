plugins {
    java
}

group "me.aleksilassila.islands"
version "5.1.0-BW.V1"

repositories {
    mavenCentral()
    maven { url = uri("https://oss.sonatype.org/content/repositories/snapshots") }
    maven { url = uri("https://hub.spigotmc.org/nexus/content/repositories/snapshots/") }
    maven { url = uri("https://maven.enginehub.org/repo/") }
    maven { url = uri("https://jitpack.io") }
}

dependencies {
    implementation("org.jetbrains:annotations:20.1.0")

    compileOnly("org.spigotmc:spigot-api:1.20.6-R0.1-SNAPSHOT")
    compileOnly("com.sk89q.worldedit:worldedit-bukkit:7.3.0-SNAPSHOT")
    compileOnly("com.github.MilkBowl:VaultAPI:1.7")
    compileOnly("com.github.TechFortress:GriefPrevention:16.18")
    implementation("com.github.stefvanschie.inventoryframework:IF:0.10.14")
}

tasks.register<Copy>("copyJar") {
    from(layout.buildDirectory.file("libs/${project.name}.jar"))
    into("server/plugins")
}

tasks.named("build") {
    dependsOn("copyJar")
}

tasks.jar {
    from(configurations.runtimeClasspath.get().map({ if(it.isDirectory) it else zipTree(it) }))
}
