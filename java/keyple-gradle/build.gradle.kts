import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.net.URL
import java.net.HttpURLConnection
import java.io.IOException

plugins {
    maven
    kotlin("jvm") version "1.3.61"
}

buildscript {
    repositories {
        mavenCentral()
    }
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.3.61")
        classpath("com.sun.istack:istack-commons-runtime:3.+")
    }
}

dependencies {
    implementation(gradleApi())
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.3.61")
    testImplementation("org.junit.jupiter:junit-jupiter:5.6.0")
    testImplementation("com.sun.istack:istack-commons-runtime:3.+")
    testImplementation("org.assertj:assertj-core:3.15.0")
}

repositories {
    mavenLocal()
    mavenCentral()
}

val compileKotlin: KotlinCompile by tasks
val compileTestKotlin: KotlinCompile by tasks
val releaseRepo: String by project
val snapshotRepo: String by project
val ossrhUsername: String by project
val ossrhPassword: String by project

compileKotlin.kotlinOptions {
    jvmTarget = "1.8"
}

compileTestKotlin.kotlinOptions {
    jvmTarget = "1.8"
}

fun canBeUploaded(project: Project): Boolean {
    val jarGroup = (project.group as String).replace('.', '/')
    val jarName = "${project.name}-${project.version}.jar"
    val repositoryPath = "$jarGroup/${project.name}/${project.version}/$jarName"
    val repositoryUrl = releaseRepo + repositoryPath

    val canBeUploaded = !urlExists(repositoryUrl)
    if (!canBeUploaded) {
        println("Artifacts already exists on repository, no need to upload it again.")
    }
    return canBeUploaded
}

fun urlExists(repositoryUrl: String): Boolean {
    return try {
        val connection = URL(repositoryUrl).openConnection() as HttpURLConnection
        connection.connectTimeout = 10_000
        connection.readTimeout = 10_000
        connection.requestMethod = "HEAD"
        connection.responseCode == 200
    } catch (ignored: IOException) {
        false
    }
}

tasks {
    test {
        useJUnitPlatform()
        testLogging {
            events("passed", "skipped", "failed")
        }
    }
    "install" {
        group = "publishing"
        description = "Install Keyple Plugin in the local maven repository."
    }
    "uploadArchives"(Upload::class) {
        onlyIf{
            canBeUploaded(project)
        }
        dependsOn("install")
        group = "publishing"
        description = "Upload Keyple Plugin to sonatype."
        repositories {
            withConvention(MavenRepositoryHandlerConvention::class) {
                mavenDeployer {
                    withGroovyBuilder {
                        "repository"("url" to releaseRepo) {
                            "authentication"("userName" to ossrhUsername, "password" to ossrhPassword)
                        }
                        "snapshotRepository"("url" to snapshotRepo) {
                            "authentication"("userName" to ossrhUsername, "password" to ossrhPassword)
                        }
                    }
                    pom.project {
                        withGroovyBuilder {
                            "name"(project.description)
                            "description"(project.description)
                            "url"("https://projects.eclipse.org/projects/iot.keyple")
                            "organization" {
                                "name"("Eclipse Keyple")
                                "url"("https://projects.eclipse.org/projects/iot.keyple")
                            }
                            "scm" {
                                "connection"("scm:git:git://github.com/eclipse/keyple-ops.git")
                                "developerConnection"("scm:git:https://github.com/eclipse/keyple-ops.git")
                                "url"("http://github.com/eclipse/keyple-ops/tree/master")
                            }
                            "licenses" {
                                "license" {
                                    "name"("Eclipse Public License - v 2.0")
                                    "url"("https://www.eclipse.org/legal/epl-2.0/")
                                    "distribution"("repo")
                                }
                            }
                            "developers" {
                                "developer" {
                                    "name"("Olivier Delcroix")
                                    "email"("odelcroi@gmail.com")
                                }
                                "developer" {
                                    "name"("Brice Ruppen")
                                    "email"("brice.ruppen@armotic.fr")
                                }
                            }

                        }
                    }
                }
            }
        }
    }
}