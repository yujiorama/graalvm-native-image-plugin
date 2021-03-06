/*
 * This Java source file was generated by the Gradle 'init' task.
 */
package org.mikeneck.graalvm;

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.BuildTask;
import org.gradle.testkit.runner.GradleRunner;
import org.gradle.testkit.runner.TaskOutcome;
import org.junit.Test;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * A simple functional test for the 'org.mikeneck.graalvm.greeting' plugin.
 */
public class GraalvmNativeImagePluginFunctionalTest {
    @Test public void runTaskOnJavaProject() throws IOException {
        // Setup the test build
        File projectDir = createProjectRoot("build/functionalTest/java");
        copyFile("java-project/build-gradle.txt", projectDir.toPath().resolve("build.gradle"));
        Path dir = projectDir.toPath().resolve("src/main/java/com/example");
        Files.createDirectories(dir);
        Path appJava = dir.resolve("App.java");
        copyFile("java-project/com_example_App_java.txt", appJava);

        // Run the build
        GradleRunner runner = GradleRunner.create();
        runner.forwardOutput();
        runner.withPluginClasspath();
        runner.withArguments("nativeImage");
        runner.withProjectDir(projectDir);
        BuildResult result = runner.build();

        List<String> succeededTasks = result.tasks(TaskOutcome.SUCCESS).stream()
                .map(BuildTask::getPath)
                .collect(Collectors.toList());
        assertThat(succeededTasks, hasItems(":compileJava", ":classes", ":jar", ":nativeImage"));
        assertTrue(Files.exists(projectDir.toPath().resolve("build/native-image/test-app")));
    }

    @Test public void runTaskOnKotlinProject() throws IOException {
        // Setup the test build
        File projectDir = createProjectRoot("build/functionalTest/kotlin");
        copyFile("kotlin-project/build-gradle-kts.txt", projectDir.toPath().resolve("build.gradle.kts"));
        Path dir = projectDir.toPath().resolve("src/main/kotlin/com/example");
        Files.createDirectories(dir);
        Path appJava = dir.resolve("App.kt");
        copyFile("kotlin-project/com_example_App_kt.txt", appJava);

        // Run the build
        GradleRunner runner = GradleRunner.create();
        runner.forwardOutput();
        runner.withPluginClasspath();
        runner.withArguments("nativeImage");
        runner.withProjectDir(projectDir);
        BuildResult result = runner.build();

        List<String> succeededTasks = result.tasks(TaskOutcome.SUCCESS).stream()
                .map(BuildTask::getPath)
                .collect(Collectors.toList());
        assertThat(succeededTasks, hasItems(":compileKotlin", ":inspectClassesForKotlinIC", ":jar", ":nativeImage"));
        assertTrue(Files.exists(projectDir.toPath().resolve("build/native-image/test-app")));
    }

    private File createProjectRoot(String s) throws IOException {
        File projectDir = new File(s);
        Files.createDirectories(projectDir.toPath());
        writeString(new File(projectDir, "settings.gradle"), "");
        return projectDir;
    }

    private void copyFile(String resourceName, Path file) throws IOException {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        URL url = loader.getResource(resourceName);
        if (url == null) {
            throw new FileNotFoundException(resourceName);
        }
        try (final InputStream inputStream = loader.getResourceAsStream(resourceName)) {
            Files.copy(Objects.requireNonNull(inputStream), file);
        }
    }

    private void writeString(File file, String string) throws IOException {
        try (Writer writer = new FileWriter(file)) {
            writer.write(string);
        }
    }
}
