/*
 * MavenUtils.java - This file contains helper methods related to Maven.
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

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.Settings;
import org.apache.maven.settings.crypto.DefaultSettingsDecryptionRequest;
import org.apache.maven.settings.crypto.SettingsDecrypter;
import org.apache.maven.settings.crypto.SettingsDecryptionRequest;
import org.apache.maven.settings.crypto.SettingsDecryptionResult;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;

import java.text.MessageFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class provides helper methods for interacting with Maven.
 */
public final class MavenUtils {
    /**
     * @see <a href="https://maven.apache.org/scm/scm-url-format.html">SCM URL Format</a>
     */
    private static final Pattern SCM_CONNECTION_PATTERN = Pattern.compile(
            "^(scm:git[:|])?" +								// Maven prefix for git SCM
                    "(https?://github\\.com/|git@github\\.com:)" +	// GitHub prefix for HTTP/HTTPS/SSH/Subversion scheme
                    "([^/]+/[^/\\.]+)" +							// Repository ID
                    "(\\.git)?" +									// Optional suffix ".git"
                    "(/.*)?$"										// Optional child project path
            , Pattern.CASE_INSENSITIVE);

    private MavenUtils() { /* Hide constructor for utility class */ }

    /**
     * Helper method to get a MojoExecutionException with a formatted error message.
     *
     * @param ex       the exception that caused the error
     * @param template the new exception's error message template, based on java.text.MessageFormat.format()
     * @param args     the arguments to use to populate the error message
     * @return         a new MojoExecutionException with the custom error message
     */
    public static MojoExecutionException getMojoExecutionException(Exception ex, String template, Object...args) {
        return new MojoExecutionException(MessageFormat.format(template, args), ex);
    }

    /**
     * Helper method to get a MojoExecutionException with a formatted error message.
     *
     * @param template the new exception's error message template, based on java.text.MessageFormat.format()
     * @param args     the arguments to use to populate the error message
     * @return         a new MojoExecutionException with the custom error message
     */
    public static MojoExecutionException getMojoExecutionException(String template, Object... args) {
        return new MojoExecutionException(MessageFormat.format(template, args));
    }

    /**
     * A helper method for creating and logging custom messages at the debug level.  The custom message
     * is constructed only if the log level is enabled.
     *
     * @param logger   the Maven logger to use
     * @param ex       the exception to log
     * @param template the log message template, based on java.text.MessageFormat.format()
     * @param args     the arguments to use to populate the log message
     */
    public static void logDebug(Log logger, Exception ex, String template, Object... args) {
        if (logger.isDebugEnabled()) {
            logger.debug(MessageFormat.format(template, args), ex);
        }
    }

    /**
     * A helper method for creating and logging custom messages at the debug level.  The custom message
     * is constructed only if the log level is enabled.
     *
     * @param logger   the Maven logger to use
     * @param template the log message template, based on java.text.MessageFormat.format()
     * @param args     the arguments to use to populate the log message
     */
    public static void logDebug(Log logger, String template, Object... args) {
        if (logger.isDebugEnabled()) {
            logger.debug(MessageFormat.format(template, args));
        }
    }

    /**
     * A helper method for creating and logging custom messages at the info level.  The custom message
     * is constructed only if the log level is enabled.
     *
     * @param logger   the Maven logger to use
     * @param ex       the exception to log
     * @param template the log message template, based on java.text.MessageFormat.format()
     * @param args     the arguments to use to populate the log message
     */
    public static void logInfo(Log logger, Exception ex, String template, Object... args) {
        if (logger.isInfoEnabled()) {
            logger.info(MessageFormat.format(template, args), ex);
        }
    }

    /**
     * A helper method for creating and logging custom messages at the info level.  The custom message
     * is constructed only if the log level is enabled.
     *
     * @param logger   the Maven logger to use
     * @param template the log message template, based on java.text.MessageFormat.format()
     * @param args     the arguments to use to populate the log message
     */
    public static void logInfo(Log logger, String template, Object... args) {
        if (logger.isInfoEnabled()) {
            logger.info(MessageFormat.format(template, args));
        }
    }

    /**
     * A helper method for creating and logging custom messages at the warn level.  The custom message
     * is constructed only if the log level is enabled.
     *
     * @param logger   the Maven logger to use
     * @param ex       the exception to log
     * @param template the log message template, based on java.text.MessageFormat.format()
     * @param args     the arguments to use to populate the log message
     */
    public static void logWarn(Log logger, Exception ex, String template, Object... args) {
        if (logger.isWarnEnabled()) {
            logger.warn(MessageFormat.format(template, args), ex);
        }
    }

    /**
     * A helper method for creating and logging custom messages at the warn level.  The custom message
     * is constructed only if the log level is enabled.
     *
     * @param logger   the Maven logger to use
     * @param template the log message template, based on java.text.MessageFormat.format()
     * @param args     the arguments to use to populate the log message
     */
    public static void logWarn(Log logger, String template, Object... args) {
        if (logger.isWarnEnabled()) {
            logger.warn(MessageFormat.format(template, args));
        }
    }

    /**
     * A helper method for creating and logging custom messages at the error level.  The custom message
     * is constructed only if the log level is enabled.
     *
     * @param logger   the Maven logger to use
     * @param ex       the exception to log
     * @param template the log message template, based on java.text.MessageFormat.format()
     * @param args     the arguments to use to populate the log message
     */
    public static void logError(Log logger, Exception ex, String template, Object... args) {
        if (logger.isErrorEnabled()) {
            logger.error(MessageFormat.format(template, args), ex);
        }
    }

    /**
     * A helper method for creating and logging custom messages at the error level.  The custom message
     * is constructed only if the log level is enabled.
     *
     * @param logger   the Maven logger to use
     * @param template the log message template, based on java.text.MessageFormat.format()
     * @param args     the arguments to use to populate the log message
     */
    public static void logError(Log logger, String template, Object... args) {
        if (logger.isErrorEnabled()) {
            logger.error(MessageFormat.format(template, args));
        }
    }

    /**
     * Get the git repository ID (aka. name) from the SCM connection string.  Id the string is not in the expected
     * SCM format, the supplied string is returned.
     *
     * @param scmConnectionString the SCM connection string
     * @return the repository ID or the original string if the string does not match the expected format
     * @throws MojoExecutionException if the supplied string is empty or null
     */
    public static String getRepositoryIdFromScmConnection(String scmConnectionString) throws MojoExecutionException {
        if (StringUtils.isEmpty(scmConnectionString)) {
            throw new MojoExecutionException("Repository ID must not be empty");
        }

        String result = scmConnectionString;
        Matcher matcher = SCM_CONNECTION_PATTERN.matcher(scmConnectionString);
        if (matcher.matches()) {
            result = matcher.group(3);
        }
        return result;
    }

    /**
     * Whether or not the supplied version match one of the well-known patterns for pre-release versions.
     *
     * @param version the string containing the version
     * @return true if the provided string matches one of the known pre-release version number formats, false otherwise
     */
    public static boolean isPreReleaseVersion(final String version) {
        return version.endsWith("-SNAPSHOT")
                || StringUtils.containsIgnoreCase(version, "-alpha")
                || StringUtils.containsIgnoreCase(version, "-beta")
                || StringUtils.containsIgnoreCase(version, "-RC")
                || StringUtils.containsIgnoreCase(version, ".RC")
                || StringUtils.containsIgnoreCase(version, "-M")
                || StringUtils.containsIgnoreCase(version, ".M");
    }

    /**
     * Get the unencrypted server entry from settings.xml.
     *
     * @param container the Maven Plexus container to use
     * @param settings  the Maven settings object to use
     * @param serverId  the server ID of the server to get
     * @param logger    the Maven logger to use
     * @return the Maven server object
     * @throws MojoExecutionException if the arguments were invalid, the server was not found, or decryption failed
     */
    public static Server getServerFromSettings(final PlexusContainer container, final Settings settings,
                                               String serverId, Log logger) throws MojoExecutionException {
        if (container == null) {
            throw new MojoExecutionException("getServerFromSettings() requires that the Plexus container not be null");
        }
        if (settings == null) {
            throw new MojoExecutionException("getServerFromSettings() requires that the Maven settings not be null");
        }
        if (StringUtils.isEmpty(serverId)) {
            throw new MojoExecutionException("getServerFromSettings() requires that the server ID not be empty");
        }

        Server server = settings.getServer(serverId);
        if (server == null) {
            throw getMojoExecutionException("Server {0} not found in Maven settings.xml", serverId);
        }
        logDebug(logger, "Found server {0} in Maven settings.xml", serverId);

        try {
            SettingsDecrypter settingsDecrypter = container.lookup(SettingsDecrypter.class);
            SettingsDecryptionRequest request = new DefaultSettingsDecryptionRequest(server);
            SettingsDecryptionResult result = settingsDecrypter.decrypt(request);
            server = result.getServer();
        } catch (ComponentLookupException ex) {
            throw getMojoExecutionException(ex, "Decrypting server entry {0} failed: {1}", serverId, ex.getMessage());
        }
        return server;
    }
}
