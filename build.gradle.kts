plugins {
    java
    `maven-publish`

    // Nothing special about this, just keep it up to date
    id("com.github.johnrengelman.shadow") version "8.1.1" apply false

    // In general, keep this version in sync with upstream. Sometimes a newer version than upstream might work, but an older version is extremely likely to break.
    id("io.papermc.paperweight.patcher") version "1.7.1"
}

val paperMavenPublicUrl = "https://repo.papermc.io/repository/maven-public/"

buildscript {
    repositories {
        mavenCentral()
    }
    dependencies {
        classpath("com.guardsquare:proguard-gradle:7.4.2")
    }
}



repositories {
    mavenCentral()
    maven(paperMavenPublicUrl) {
        content { onlyForConfigurations(configurations.paperclip.name) }
    }
}

dependencies {
    remapper("net.fabricmc:tiny-remapper:0.10.2:fat") // Must be kept in sync with upstream
    decompiler("org.vineflower:vineflower:1.10.1") // Must be kept in sync with upstream
    paperclip("io.papermc:paperclip:3.0.3") // You probably want this to be kept in sync with upstream
}

allprojects {
    apply(plugin = "java")
    apply(plugin = "maven-publish")

    java {
        toolchain {
            languageVersion = JavaLanguageVersion.of(21)
        }
    }
}

subprojects {
    tasks.withType<JavaCompile> {
        options.encoding = Charsets.UTF_8.name()
        options.release = 21
    }
    tasks.withType<Javadoc> {
        options.encoding = Charsets.UTF_8.name()
    }
    tasks.withType<ProcessResources> {
        filteringCharset = Charsets.UTF_8.name()
    }

    repositories {
        mavenCentral()
        maven(paperMavenPublicUrl)
    }
}

paperweight {
    serverProject = project(":nylon-server")

    remapRepo = paperMavenPublicUrl
    decompileRepo = paperMavenPublicUrl

    usePaperUpstream(providers.gradleProperty("paperRef")) {
        withPaperPatcher {
            apiPatchDir = layout.projectDirectory.dir("patches/api")
            apiOutputDir = layout.projectDirectory.dir("nylon-api")

            serverPatchDir = layout.projectDirectory.dir("patches/server")
            serverOutputDir = layout.projectDirectory.dir("nylon-server")

        }
        patchTasks.register("generatedApi") {
            isBareDirectory = true
            upstreamDirPath = "paper-api-generator/generated"
            patchDir = layout.projectDirectory.dir("patches/generatedApi")
            outputDir = layout.projectDirectory.dir("paper-api-generator/generated")
        }
    }
}

//
// Everything below here is optional if you don't care about publishing API or dev bundles to your repository
//

tasks.generateDevelopmentBundle {
    apiCoordinates = "com.starsrealm.nylon:nylon-api"
    libraryRepositories = listOf(
        "https://repo.maven.apache.org/maven2/",
        paperMavenPublicUrl,
        // "https://my.repo/", // This should be a repo hosting your API (in this example, 'com.example.paperfork:nylon-api')
    )
}

allprojects {
    publishing {
        repositories {
            maven {
                name = "AliYun-Release"
                url = uri("https://packages.aliyun.com/maven/repository/2421751-release-ZmwRAc/")
                credentials {
                    username = project.findProperty("aliyun.package.user") as String? ?: System.getenv("ALY_USER")
                    password = project.findProperty("aliyun.package.password") as String? ?: System.getenv("ALY_PASSWORD")
                }
            }
            maven {
                name = "AliYun-Snapshot"
                url = uri("https://packages.aliyun.com/maven/repository/2421751-snapshot-i7Aufp/")
                credentials {
                    username = project.findProperty("aliyun.package.user") as String? ?: System.getenv("ALY_USER")
                    password = project.findProperty("aliyun.package.password") as String? ?: System.getenv("ALY_PASSWORD")
                }
            }
        }
        publications {
            create("gpr", MavenPublication::class.java) {
                from(components.getByName("java"))
            }
        }
    }
    repositories{
        maven {
            name = "AliYun-Release"
            url = uri("https://packages.aliyun.com/maven/repository/2421751-release-ZmwRAc/")
            credentials {
                username = project.findProperty("aliyun.package.user") as String? ?: System.getenv("ALY_USER")
                password = project.findProperty("aliyun.package.password") as String? ?: System.getenv("ALY_PASSWORD")
            }
        }
        maven {
            name = "AliYun-Snapshot"
            url = uri("https://packages.aliyun.com/maven/repository/2421751-snapshot-i7Aufp/")
            credentials {
                username = project.findProperty("aliyun.package.user") as String? ?: System.getenv("ALY_USER")
                password = project.findProperty("aliyun.package.password") as String? ?: System.getenv("ALY_PASSWORD")
            }
        }
    }
}

publishing {
    publications {
        publications.create<MavenPublication>("devBundle") {
            artifact(tasks.generateDevelopmentBundle) {
                artifactId = "dev-bundle"
            }
        }
    }
}