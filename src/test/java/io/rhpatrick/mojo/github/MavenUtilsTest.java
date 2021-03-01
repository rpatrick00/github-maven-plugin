/*
 * MavenUtilsTest.java - This file contains unit tests for Maven-related helper methods.
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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MavenUtilsTest {

    @DisplayName("Test MavenUtils.getRepositoryIdFromScmConnection(scmConnection)")
    @ParameterizedTest
    @ValueSource(strings = {
            "scm:git:http://github.com/rpatrick00/github-maven-plugin.git",
            "scm:git:https://github.com/rpatrick00/github-maven-plugin.git",
            "scm:git:git@github.com:rpatrick00/github-maven-plugin.git"
    })
    public void getRepositoryIdFromScmConnection_WithValidScmString_ReturnsExpectedResult(String scmConnection)
            throws Exception {
        String expected = "rpatrick00/github-maven-plugin";

        String actual = MavenUtils.getRepositoryIdFromScmConnection(scmConnection);

        assertEquals(expected, actual);
    }

    @DisplayName("Test MavenUtils.getRepositoryIdFromScmConnection(repositoryId)")
    @ParameterizedTest
    @ValueSource(strings = {
            "rpatrick00/github-maven-plugin",
            "oracle/weblogic-deploy-tooling"
    })
    public void getRepositoryIdFromScmConnection_WithRepositoryId_ReturnsRepositoryId(String repositoryId)
            throws Exception {
        String actual = MavenUtils.getRepositoryIdFromScmConnection(repositoryId);

        assertEquals(repositoryId, actual);
    }

    @DisplayName("Test MavenUtils.isPreReleaseVersion(preReleaseVersion)")
    @ParameterizedTest
    @ValueSource(strings = {
            "1.0-SNAPSHOT",
            "1.0-alpha1",
            "1.0-beta3",
            "1.0-RC2",
            "1.0.RC3",
            "3.0.0-M1",
            "3.0.0.M1"
    })
    public void isPreReleaseVersion_WithPreReleaseVersions_ReturnsTrue(String version) {
        boolean actual = MavenUtils.isPreReleaseVersion(version);

        assertTrue(actual, "Version " + version + " should be a pre-release version");
    }
}
