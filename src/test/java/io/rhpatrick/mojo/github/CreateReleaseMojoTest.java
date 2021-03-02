/*
 * CreateReleaseMojoTest.java - This file contains unit tests for helper methods in CreateReleaseMojo.
 *
 * Copyright 2021 Robert Patrick <rhpatrick@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.rhpatrick.mojo.github;

import java.io.File;

import org.apache.maven.plugin.MojoExecutionException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class CreateReleaseMojoTest {
    private static final String TEST_MD_EXPECTED =
        "### Issues addressed\n"
        + "1. Issue 1\n"
        + "2. Issue 2\n"
        + "3. Issue 3\n"
        + "\n"
        + "### Enhancements added\n"
        + "1. `Enhancement 1` - This is an enhancement.\n"
        + "2. `Enhancement 2` - This is an enhancement.\n"
        + "3. `Enhancement 3` - This is an enhancement.\n"
        + "\n"
        + "```xml\n"
        + "    <server>\n"
        + "      <id>github</id>\n"
        + "      <passphrase>{kRBjmPgdyiHERnNNRp3tWCk2zPWMXBKJQVHxpVcW4T/FCp/L2h29+TRmVlgXh9gAX83hnhS302zAoVSdUTc/FA==}</passphrase>\n"
        + "    </server>\n"
        + "```\n";

    @DisplayName("Test CreateReleaseMojo.getFileContents(file)")
    @Test
    public void getFileContents_FromFile_ReturnsExpectedResult() throws Exception {

        String actual = CreateReleaseMojo.getFileContents(new File("src/test/resources/Test.md"));

        assertEquals(TEST_MD_EXPECTED, actual);
    }

    @DisplayName("Test CreateReleaseMojo.getFileContents(<empty file>)")
    @Test
    public void getFileContents_FromEmptyFile_ReturnsExpectedResult() throws Exception {

        String actual = CreateReleaseMojo.getFileContents(new File("src/test/resources/Empty.md"));

        assertEquals("", actual);
    }

    @DisplayName("Test CreateReleaseMojo.getFileContents(null)")
    @Test
    public void getFileContents_FromNull_ThrowsMojoExecutionException() throws Exception {

        Exception ex = assertThrows(MojoExecutionException.class, () -> CreateReleaseMojo.getFileContents(null));

        assertEquals("getFileContents() requires a file that is not null", ex.getMessage());
    }

    @DisplayName("Test CreateReleaseMojo.getFileContents(<nonexistent-file>)")
    @Test
    public void getFileContents_FromNonExistentFile_ThrowsMojoExecutionException() throws Exception {

        Exception ex = assertThrows(MojoExecutionException.class, () ->
            CreateReleaseMojo.getFileContents(new File("src/test/resources/NonExistentFile.md"))
        );

        assertEquals("getFileContents() requires an existing file", ex.getMessage());
    }

    @DisplayName("Test CreateReleaseMojo.getFileContents(<directory>)")
    @Test
    public void getFileContents_FromDirectory_ThrowsMojoExecutionException() throws Exception {

        Exception ex = assertThrows(MojoExecutionException.class, () ->
            CreateReleaseMojo.getFileContents(new File("src/test/resources"))
        );

        assertEquals("getFileContents() requires a file that is not a directory", ex.getMessage());
    }
}
