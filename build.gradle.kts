plugins {
    java
}

group = "mod.wurmunlimited.npcs.banker"
version = "0.2.3"
java.sourceCompatibility = JavaVersion.VERSION_1_8
val shortName = "banker"
val wurmServerFolder = "E:/Steam/steamapps/common/Wurm Unlimited/WurmServerLauncher/"

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    implementation(project(":BMLBuilder"))
    implementation(project(":PlaceNpc"))
    implementation(project(":CreatureCustomiser"))
    implementation("com.wurmonline:server:1.9")
    implementation("org.gotti.wurmunlimited:server-modlauncher:0.45")

    testImplementation(project(":WurmTestingHelper"))
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.6.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}

tasks {
    jar {
        doLast {
            copy {
                from(jar)
                into(wurmServerFolder + "mods/" + shortName)
            }

            copy {
                from("src/main/resources/$shortName.properties")
                into(wurmServerFolder + "mods/")
            }
        }

        from(configurations.runtimeClasspath.get().filter { it.name.startsWith("BMLBuilder") && it.name.endsWith("jar") }.map { zipTree(it) })
        from(configurations.runtimeClasspath.get().filter { it.name.startsWith("PlaceNpc") && it.name.endsWith("jar") }.map { zipTree(it) })
        from(configurations.runtimeClasspath.get().filter { it.name.startsWith("CreatureCustomiser") && it.name.endsWith("jar") }.map { zipTree(it) })

        includeEmptyDirs = false
        archiveFileName.set("$shortName.jar")

        manifest {
            attributes["Implementation-Version"] = archiveVersion
        }
    }

    register<Zip>("zip") {
        into(shortName) {
            from(jar)
        }

        from("src/main/resources/$shortName.properties")
        archiveFileName.set("$shortName.zip")
    }
}