/*
 * GitHubUtils.java - This file contains helper methods related to GitHub.
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

import java.io.IOException;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.Settings;
import org.codehaus.plexus.PlexusContainer;
import org.kohsuke.github.GHRelease;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.PagedIterable;

import static io.rhpatrick.mojo.github.MavenUtils.getMojoExecutionException;
import static io.rhpatrick.mojo.github.MavenUtils.logDebug;
import static io.rhpatrick.mojo.github.MavenUtils.logInfo;

/**
 * This class provides helper methods for interacting with GitHub.
 */
public final class GitHubUtils {
    private static final String AUTH_TOKEN_PROPERTY_NAME = "github.auth.token";

    private GitHubUtils() { /* Hide constructor for utility class */ }

    /**
     * Connect to GitHub.
     *
     * @param container the Maven Plexus container
     * @param settings  the Maven settings object
     * @param serverId  the server ID from settings.xml to use for authentication
     * @param logger    the Maven logger to use
     * @return the GitHub API object
     * @throws MojoExecutionException if a non-GitHub API error is detected
     * @throws IOException            if a GitHub API method encounters an error
     */
    public static GitHub connect(final PlexusContainer container, final Settings settings,
                                 final String serverId, Log logger)
            throws MojoExecutionException, IOException {

        String authToken = System.getProperty(AUTH_TOKEN_PROPERTY_NAME);
        if (StringUtils.isNotEmpty(authToken)) {
            logDebug(logger,"Auth token found in system property {0}", AUTH_TOKEN_PROPERTY_NAME);
        } else {

            // If the auth token was not provided via the system property, go get it from the Maven settings.
            // We are hijacking the server's <passphrase> element to store the auth token so that we can
            // benefit from Maven's built-in support for settings.xml encryption.
            //
            // The settings.xml snippet should look like this is the serverId is set to "github":
            //
            // <servers>
            //   <server>
            //     <id>github</id>
            //     <passphrase>06fd8bb5770345a9294x5a0189b36cpac97d8bcc</passphrase>
            //   </server>
            // </servers>
            //
            if (settings == null) {
                throw new MojoExecutionException("connect() requires the settings not to be null");
            }
            if (StringUtils.isEmpty(serverId)) {
                throw new MojoExecutionException("connect() requires the server ID not to be null");
            }
            Server server = MavenUtils.getServerFromSettings(container, settings, serverId, logger);

            authToken = server.getPassphrase();
            if (StringUtils.isEmpty(authToken)) {
                throw getMojoExecutionException("Server {0} passphrase element was empty", serverId);
            }
            logDebug(logger, "Auth token found in settings.xml for server {0}", serverId);
        }
        return GitHub.connectUsingOAuth(authToken);
    }

    /**
     * Find a GitHub Release by its name.
     *
     * @param repository  the GitHub API repository object
     * @param releaseName the name of the release to use
     * @param logger      the Maven logger
     * @return the GitHub API release object, or null of the release was not found
     * @throws MojoExecutionException if a non-GitHub API error condition is detected
     * @throws IOException            if a GitHub API encounters an error condition
     */
    public static GHRelease findReleaseByName(GHRepository repository, String releaseName, Log logger)
            throws MojoExecutionException, IOException {
        if (StringUtils.isEmpty(releaseName)) {
            throw getMojoExecutionException("findByReleaseName() requires the release name to not be empty");
        }

        GHRelease result = null;
        PagedIterable<GHRelease> releases = repository.listReleases();
        int count = 0;
        for (GHRelease release : releases) {
            count++;
            if (releaseName.equals(release.getName())) {
                logDebug(logger, "Found release with the name {0} in repository {1}",
                        releaseName, repository.getName());
                result = release;
                break;
            } else {
                logDebug(logger, "Release {0} in repository {1} does not match specified name {2}",
                        release.getName(), repository.getName(), releaseName);
            }
        }
        if (count == 0) {
            logInfo(logger, "Repository {0} has no releases", repository.getName());
        }
        return result;
    }
}
