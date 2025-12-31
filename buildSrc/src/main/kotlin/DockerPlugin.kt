import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.Exec

/**
 * Custom Docker plugin for multi-platform image builds
 * Based on Intorqa's DockerPlugin pattern
 */
class DockerPlugin : Plugin<Project> {

    private val dockerExecutable = if (System.getProperty("os.name").contains("Mac", ignoreCase = true)) {
        "/usr/local/bin/docker"
    } else {
        "docker"
    }

    override fun apply(project: Project) {
        // Register setupQemu task for multi-platform builds
        val setupQemu = project.tasks.register("setupQemu", Exec::class.java) {
            commandLine(dockerExecutable, "run", "--privileged", "--rm",
                "tonistiigi/binfmt", "--install", "all")
            doFirst {
                println("Setting up QEMU for multi-platform builds...")
            }
        }

        // Register inspectBuildxBuilder task
        val inspectBuildxBuilder = project.tasks.register("inspectBuildxBuilder", Exec::class.java) {
            commandLine(dockerExecutable, "buildx", "inspect")
            isIgnoreExitValue = true
        }

        // Register buildDockerImage task
        val buildDockerImage = project.tasks.register("buildDockerImage", Exec::class.java) {
            dependsOn(setupQemu, inspectBuildxBuilder)

            doFirst {
                println("====================================")
                println("Building Docker Image")
                println("====================================")
                println("Project: ${project.name}")
                println("Version: ${project.version}")
                println("Working directory: ${project.projectDir}")

                // Create buildx builder if it doesn't exist
                if (!doesBuildxBuilderExist(project, "actionsBuilder")) {
                    println("Creating Docker buildx builder 'actionsBuilder'...")
                    project.exec {
                        commandLine(dockerExecutable, "buildx", "create",
                            "--name", "actionsBuilder",
                            "--driver", "docker-container",
                            "--use")
                    }
                    project.exec {
                        commandLine(dockerExecutable, "buildx", "inspect", "--bootstrap")
                    }
                }
            }

            // Get the actual build directory (handles custom build dirs like .build/)
            val buildDir = project.layout.buildDirectory.get().asFile.name
            val jarFile = "${buildDir}/libs/${project.name}-${project.version}.jar"

            val buildCmd = listOf(
                dockerExecutable, "buildx", "build",
                "--platform", "linux/amd64,linux/arm64",
                "--build-arg", "JAR_FILE=${jarFile}",
                "--tag", "shinobislayer/${project.name}:${project.version}",
                "--tag", "shinobislayer/${project.name}:latest",
                "--file", "Dockerfile",
                ".",
                "--push"
            )

            println("Build command: ${buildCmd.joinToString(" ")}")
            commandLine(buildCmd)
        }

        // Make buildDockerImage depend on the build task
        project.tasks.named("buildDockerImage") {
            dependsOn("build")
        }

        // Register docker task as alias
        project.tasks.register("docker") {
            group = "docker"
            description = "Build and push Docker image to Docker Hub"
            dependsOn(buildDockerImage)
        }
    }

    private fun doesBuildxBuilderExist(project: Project, builderName: String): Boolean {
        val result = project.exec {
            commandLine(dockerExecutable, "buildx", "inspect", builderName)
            isIgnoreExitValue = true
        }
        return result.exitValue == 0
    }
}
