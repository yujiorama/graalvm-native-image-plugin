/*
 * Copyright 2019 Shinya Mochida
 *
 * Licensed under the Apache License,Version2.0(the"License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,software
 * Distributed under the License is distributed on an"AS IS"BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.mikeneck.graalvm;

import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.Assert.*;

public class GraalVmHomeTest {

    @Test
    public void on_nixLikeOs() throws IOException {
        Path graalVmHome = Files.createTempDirectory("test-on_nixLikeOs");
        Path binDirectory = Files.createDirectory(graalVmHome.resolve("bin"));
        Files.createFile(binDirectory.resolve("native-image"));
        Files.createFile(binDirectory.resolve("gu"));

        GraalVmHome home = new GraalVmHome(graalVmHome);

        Optional<Path> nativeImage = home.nativeImage();

        assertTrue(nativeImage.isPresent());

        assertTrue(home.gu().toFile().exists());
    }

    @Test
    public void onWindows() throws IOException {
        Path graalVmHome = Files.createTempDirectory("test-onWindows");
        Path binDirectory = Files.createDirectory(graalVmHome.resolve("bin"));
        Files.createFile(binDirectory.resolve("native-image.cmd"));
        Files.createFile(binDirectory.resolve("gu.cmd"));

        GraalVmHome home = new GraalVmHome(graalVmHome);

        Optional<Path> nativeImage = home.nativeImage();

        assertTrue(nativeImage.isPresent());

        assertTrue(home.gu().toFile().exists());
    }

    @Test
    public void invalidHome() throws IOException {
        Path graalVmHome = Files.createTempDirectory("test-onWindows");
        Files.createDirectory(graalVmHome.resolve("bin"));

        GraalVmHome home = new GraalVmHome(graalVmHome);

        Optional<Path> nativeImage = home.nativeImage();

        assertFalse(nativeImage.isPresent());

        try {
            home.gu();
            fail("never fall here");
        } catch (IllegalArgumentException ignore) {
        }
    }
}
