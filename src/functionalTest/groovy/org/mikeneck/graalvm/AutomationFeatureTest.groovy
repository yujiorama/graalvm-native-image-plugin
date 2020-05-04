package org.mikeneck.graalvm

import groovy.text.SimpleTemplateEngine
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Test
import org.junit.experimental.runners.Enclosed
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

import java.nio.file.Files

import static org.hamcrest.CoreMatchers.hasItems
import static org.hamcrest.MatcherAssert.assertThat

@RunWith(Enclosed)
class AutomationFeatureTest {

    @RunWith(Parameterized)
    static class JavaProjectTest {

        @Parameterized.Parameters(name = 'JavaProjectTest({index}): name=[{0}] findGraalVmHome=[{1}] installNativeImage=[{2}')
        static parameters() {
            [
                    ['default', false, false,],
                    ['find GraalVmHome', true, false,],
                    ['install native-image', false, true,],
                    ['find GraalVmHome, install native-image', true, true,],
            ]*.toArray()
        }

        private String name
        private boolean findGraalVmHome
        private boolean installNativeImage

        JavaProjectTest(String name, boolean findGraalVmHome, boolean installNativeImage) {
            this.name = name
            this.findGraalVmHome = findGraalVmHome
            this.installNativeImage = installNativeImage
        }

        @Test
        void runTask() {
            def projectRoot = new File("build/${name}/java")
            projectRoot.mkdirs()
            new File(projectRoot, 'settings.gradle').withPrintWriter { writer ->
                writer.write('')
            }
            new File(projectRoot, 'build.gradle').withPrintWriter { writer ->
                writer.write(buildGradleSource(findGraalVmHome, installNativeImage))
            }
            def appJava = new File(projectRoot, 'src/main/java/com/example/App.java')
            appJava.getParentFile().mkdirs()
            appJava.withPrintWriter { writer ->
                writer.write('''package com.example;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

public class App {
  static final Logger logger = LoggerFactory.getLogger(App.class);
  public static void main(String... args) {
    logger.info("Hello");
  }
}
''')
            }

            // Run the build
            BuildResult result = GradleRunner.create()
                    .forwardOutput()
                    .withPluginClasspath()
                    .withArguments('nativeImage', '--stacktrace')
                    .withProjectDir(projectRoot)
                    .build()

            List<String> succeededTasks = result.tasks(TaskOutcome.SUCCESS).collect { it.path }
            assertThat(succeededTasks, hasItems(':compileJava', ':classes', ':jar', ':nativeImage'))
            assert Files.exists(projectRoot.toPath().resolve('build/native-image/test-app'))
        }

        static String buildGradleSource(boolean findGraalVmHome, boolean installNativeImage) {
            def text = '''plugins {
  id 'java'
  id 'org.mikeneck.graalvm-native-image'
}

repositories {
  mavenCentral()
}

dependencies {
  implementation 'org.slf4j:slf4j-simple:1.7.28'
}

nativeImage {
  graalVmHome = System.getProperty('java.home')
  findGraalVmHome = ${findGraalVmHome}
  installNativeImage = ${installNativeImage}
  mainClass = 'com.example.App'
  executableName = 'test-app'
  arguments('--no-fallback')
}
'''
            return new SimpleTemplateEngine().createTemplate(text).make([
                    findGraalVmHome   : findGraalVmHome,
                    installNativeImage: installNativeImage,
            ])
        }
    }

    @RunWith(Parameterized)
    static class KotlinProjectTest {

        @Parameterized.Parameters(name = 'KotlinProjectTest({index}): name=[{0}] findGraalVmHome=[{1}] installNativeImage=[{2}')
        static parameters() {
            [
                    ['default', false, false,],
                    ['find GraalVmHome', true, false,],
                    ['install native-image', false, true,],
                    ['find GraalVmHome, install native-image', true, true,],
            ]*.toArray()
        }

        private String name
        private boolean findGraalVmHome
        private boolean installNativeImage

        KotlinProjectTest(String name, boolean findGraalVmHome, boolean installNativeImage) {
            this.name = name
            this.findGraalVmHome = findGraalVmHome
            this.installNativeImage = installNativeImage
        }

        @Test
        void runTask() {
            def projectRoot = new File("build/${name}/kotlin")
            projectRoot.mkdirs()
            new File(projectRoot, 'settings.gradle').withPrintWriter { writer ->
                writer.write('')
            }
            new File(projectRoot, 'build.gradle.kts').withPrintWriter { writer ->
                writer.write(buildGradleSource(findGraalVmHome, installNativeImage))
            }
            def appKotlin = new File(projectRoot, 'src/main/kotlin/com/example/App.kt')
            appKotlin.getParentFile().mkdirs()
            appKotlin.withPrintWriter { writer ->
                writer.write('''package com.example

fun main() {
  println("hello")
}

''')
            }

            // Run the build
            BuildResult result = GradleRunner.create()
                    .forwardOutput()
                    .withPluginClasspath()
                    .withArguments('nativeImage', '--stacktrace')
                    .withProjectDir(projectRoot)
                    .build()

            List<String> succeededTasks = result.tasks(TaskOutcome.SUCCESS).collect { it.path }
            assertThat(succeededTasks, hasItems(':compileKotlin', ':inspectClassesForKotlinIC', ':jar', ':nativeImage'))
            assert Files.exists(projectRoot.toPath().resolve('build/native-image/test-app'))
        }

        static String buildGradleSource(boolean findGraalVmHome, boolean installNativeImage) {
            def text = '''plugins {
    id("org.jetbrains.kotlin.jvm") version "1.3.41"
    id("org.mikeneck.graalvm-native-image")
    application
}

repositories {
    jcenter()
}

dependencies {
    implementation(platform("org.jetbrains.kotlin:kotlin-bom"))
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

    testImplementation("org.jetbrains.kotlin:kotlin-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit")
}

application {
    mainClassName = "com.example.AppKt"
}

nativeImage {
  setGraalVmHome(System.getProperty("java.home"))
  setFindGraalVmHome(${findGraalVmHome})
  setInstallNativeImage(${installNativeImage})
  setMainClass("com.example.AppKt")
  setExecutableName("test-app")
  arguments("--no-fallback")
}
'''
            return new SimpleTemplateEngine().createTemplate(text).make([
                    findGraalVmHome   : findGraalVmHome,
                    installNativeImage: installNativeImage,
            ])
        }
    }
}
