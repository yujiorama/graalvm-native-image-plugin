/*
 * Copyright 2019 Shinya Mochida
 *
 * Licensed under the Apache License,Version2.0(the"License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,software Distributed under the License
 * is distributed on an"AS IS"BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,either express or
 * implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package org.mikeneck.graalvm;

import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.internal.provider.DefaultProvider;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@SuppressWarnings("WeakerAccess")
public class NativeImageExtension {

    final Property<String> graalVmHome;
    final Property<Boolean> findGraalVmHome;
    final Property<Boolean> installNativeImage;
    final Property<Task> jarTask;
    final Property<String> mainClass;
    final Property<String> executableName;
    final Property<Configuration> runtimeClasspath;
    final ListProperty<String> additionalArguments;

    NativeImageExtension(Project project) {
        ObjectFactory objects = project.getObjects();
        this.graalVmHome = objects.property(String.class);
        this.findGraalVmHome = objects.property(Boolean.class);
        this.installNativeImage = objects.property(Boolean.class);
        this.jarTask = objects.property(Task.class);
        this.mainClass = objects.property(String.class);
        this.executableName = objects.property(String.class);
        this.runtimeClasspath = objects.property(Configuration.class);
        this.additionalArguments = objects.listProperty(String.class);

        this.graalVmHome.set((System.getProperty("java.home")));
        this.findGraalVmHome.set(false);
        this.installNativeImage.set(false);
        configureDefaultJarTask(project);
        configureDefaultRuntimeClasspath(project);
    }

    @NotNull
    File jarFile() {
        return this.jarTask.get().getOutputs().getFiles().getSingleFile();
    }

    @NotNull
    String executableName() {
        return this.executableName.get();
    }

    private void configureDefaultJarTask(Project project) {
        this.jarTask.set(new DefaultProvider<>(() -> project.getTasks().getByName("jar")));
    }

    private void configureDefaultRuntimeClasspath(Project project) {
        this.runtimeClasspath.set(new DefaultProvider<>(() -> project.getConfigurations().getByName("runtimeClasspath")));
    }

    static Optional<GraalVmHome> findGraalVmHomeFromEnvironmentVariables(VariablesHelper variables) {

        return Stream.concat(
                Stream.of(
                        variables.getEnv("PATH").orElse("")
                                .split(File.pathSeparator)),
                Stream.concat(
                        Stream.of(
                                variables.getEnv("JAVA_HOME").map(File::new),
                                variables.getProperty("java.home").map(File::new)
                        ),
                        variables.getEnv("SDKMAN_CANDIDATES_DIR")
                                .map(candidates -> new File(candidates, "java"))
                                .filter(File::isDirectory)
                                .stream()
                                .flatMap(file -> Stream.of(file.listFiles()).map(Optional::of))
                )
                        .flatMap(Optional::stream)
                        .filter(File::isDirectory)
                        .map(javaHome -> new File(javaHome, "bin"))
                        .filter(File::isDirectory)
                        .map(File::getAbsolutePath)
        )
                .map(Paths::get)
                .flatMap(path -> Stream.of("gu", "gu.cmd").map(path::resolve))
                .filter(Files::exists)
                .findFirst()
                .map(path -> path.getParent().getParent())
                .map(GraalVmHome::new);
    }

    GraalVmHome graalVmHome() {
        if (findGraalVmHome()) {
            Optional<GraalVmHome> resolvedHome = findGraalVmHomeFromEnvironmentVariables(new VariablesHelperImpl());
            if (resolvedHome.isPresent()) {
                return resolvedHome.get();
            }
        }

        String pathString = this.graalVmHome.get();
        Path path = Paths.get(pathString);
        return new GraalVmHome(path);
    }

    boolean findGraalVmHome() {
        return this.findGraalVmHome.get();
    }

    boolean installNativeImage() {
        return this.installNativeImage.get();
    }

    public void setGraalVmHome(String graalVmHome) {
        this.graalVmHome.set(graalVmHome);
    }

    public void setFindGraalVmHome(boolean findGraalVmHome) {
        this.findGraalVmHome.set(findGraalVmHome);
    }

    public void setInstallNativeImage(boolean installNativeImage) {
        this.installNativeImage.set(installNativeImage);
    }

    public void setJarTask(Task jarTask) {
        this.jarTask.set(jarTask);
    }

    public void setMainClass(String mainClass) {
        this.mainClass.set(mainClass);
    }

    public void setExecutableName(String name) {
        this.executableName.set(name);
    }

    public void setRuntimeClasspath(Configuration configuration) {
        this.runtimeClasspath.set(configuration);
    }

    public void arguments(String... arguments) {
        List<String> list = Arrays.stream(arguments)
                .filter(Objects::nonNull)
                .filter(it -> !it.isEmpty())
                .collect(Collectors.toList());
        this.additionalArguments.addAll(list);
    }

    @SuppressWarnings("StringBufferReplaceableByString")
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("GraalExtension{");
        sb.append("graalVmHome=").append(graalVmHome);
        sb.append(", findGraalVmHome=").append(findGraalVmHome);
        sb.append(", installNativeImage=").append(installNativeImage);
        sb.append(", jarTask=").append(jarTask);
        sb.append(", mainClass=").append(mainClass);
        sb.append(", executableName=").append(executableName);
        sb.append(", runtimeClasspath=").append(runtimeClasspath);
        sb.append('}');
        return sb.toString();
    }

    interface VariablesHelper {
        Optional<String> getProperty(String key);
        Optional<String> getEnv(String key);
    }

    static class VariablesHelperImpl implements VariablesHelper {

        @Override
        public Optional<String> getProperty(String key) {
            return Optional.ofNullable(System.getProperty(key));
        }

        @Override
        public Optional<String> getEnv(String key) {
            return Optional.ofNullable(System.getenv(key));
        }
    }
}
