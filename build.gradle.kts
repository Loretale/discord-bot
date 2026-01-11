plugins {
    id("java")
}

group = "net.loretale"
version = "1.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation("net.dv8tion:JDA:6.2.0") {
        exclude(module="opus-java")
        exclude(module="tink")
    }
    implementation("org.postgresql:postgresql:42.7.8")
    implementation("ch.qos.logback:logback-classic:1.5.6")
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}

tasks.register("fatJar", Jar::class) {
    group = "build"
    description = "Assembles a fat JAR with all dependencies"

    archiveBaseName.set("${project.name}-all")
    archiveVersion.set("${project.version}")
    manifest {
        attributes["Main-Class"] = "net.loretale.discordbot.Main"
    }

    from(sourceSets.main.get().output)

    dependsOn(configurations.runtimeClasspath)
    from({
        configurations.runtimeClasspath.get()
            .filter { it.name.endsWith("jar") }
            .map { zipTree(it) }
    })

    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}