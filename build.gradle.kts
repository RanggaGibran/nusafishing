plugins {
    `java-library`
}

group = "id.nusafishing"
version = "1.1+dev"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
    maven("https://jitpack.io")
    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
    maven("https://repo.nexomc.com/releases")
    maven("https://repo.oraxen.com/releases")
}

val java25SourceSet = sourceSets.create("java25") {
    java {
        srcDir("src/main/java25")
    }
}

dependencies {
    // Java 21 baseline compilation (Minecraft 1.21.x+)
    compileOnly("io.papermc.paper:paper-api:1.21.4-R0.1-SNAPSHOT")
    compileOnly("com.github.MilkBowl:VaultAPI:1.7") {
        isTransitive = false
    }
    compileOnly("me.clip:placeholderapi:2.11.6")
    compileOnly("com.nexomc:nexo:1.29.1") {
        isTransitive = false
    }
    compileOnly("io.th0rgal:oraxen:1.220.1") {
        isTransitive = false
    }
    compileOnly("beer.devs:itemsadder-api:4.0.17") {
        isTransitive = false
    }

    // Java 25 source set dependencies (Paper 26.2+ baseline)
    "java25CompileOnly"("io.papermc.paper:paper-api:26.2.build.134-stable")
    "java25CompileOnly"(sourceSets["main"].output)

    testImplementation("junit:junit:4.13.2")
    testImplementation("com.github.seeseemelk:MockBukkit-v1.21:3.133.2")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

tasks.named<JavaCompile>("compileJava") {
    options.encoding = "UTF-8"
    options.release.set(21)
}

tasks.named<JavaCompile>("compileJava25Java") {
    options.encoding = "UTF-8"
    options.release.set(25)
}

tasks.jar {
    manifest {
        attributes(
            "Multi-Release" to "true",
            "Specification-Title" to "NusaFishing",
            "Specification-Version" to project.version,
            "Implementation-Title" to "NusaFishing",
            "Implementation-Version" to project.version
        )
    }
    from(sourceSets["java25"].output) {
        into("META-INF/versions/25")
    }
}

tasks.test {
    jvmArgs(
        "--add-opens", "java.base/java.lang=ALL-UNNAMED",
        "--add-opens", "java.base/java.lang.invoke=ALL-UNNAMED",
        "--add-opens", "java.base/java.lang.reflect=ALL-UNNAMED"
    )
}

tasks.processResources {
    filteringCharset = "UTF-8"
    filesMatching("plugin.yml") {
        expand(
            "version" to project.version,
            "name" to project.name
        )
    }
}
