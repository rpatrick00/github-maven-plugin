/*
 * CreateReleaseMojo.java - This file contains the implementation for the create-release goal.
 *
 * Copyright 2021, 2022, Robert Patrick <rhpatrick@gmail.com>
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
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.model.FileSet;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.settings.Settings;
import org.codehaus.plexus.PlexusConstants;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.context.Context;
import org.codehaus.plexus.context.ContextException;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Contextualizable;
import org.codehaus.plexus.util.FileUtils;
import org.kohsuke.github.GHAsset;
import org.kohsuke.github.GHRelease;
import org.kohsuke.github.GHReleaseBuilder;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;

import static io.rhpatrick.mojo.github.MavenUtils.getMojoExecutionException;
import static io.rhpatrick.mojo.github.MavenUtils.logDebug;
import static io.rhpatrick.mojo.github.MavenUtils.logError;
import static io.rhpatrick.mojo.github.MavenUtils.logInfo;
import static io.rhpatrick.mojo.github.MavenUtils.logWarn;

/**
 * This class implements the github:create-release goal.  It supports creating a GitHub repository release
 * and uploading artifacts to it.
 */
@Mojo(name = "create-release", defaultPhase = LifecyclePhase.DEPLOY)
public class CreateReleaseMojo extends AbstractMojo implements Contextualizable {
    private static final Map<String, String> DEFAULT_MIME_TYPE_MAPPINGS = new LinkedHashMap<>();
    static {
        DEFAULT_MIME_TYPE_MAPPINGS.put("zip", "application/zip");
        // Since tar.gz files content type is application/gzip, don't bother looking for the leading tar extension
        DEFAULT_MIME_TYPE_MAPPINGS.put("tgz", "application/gzip");
        DEFAULT_MIME_TYPE_MAPPINGS.put("gz", "application/gzip");
        DEFAULT_MIME_TYPE_MAPPINGS.put("jar", "application/java-archive");
    }

    @Requirement
    private PlexusContainer container;

    @Parameter(defaultValue = "${settings}", readonly = true, required = true)
    private Settings settings;

    /**
     * The server ID from settings.xml to use to find the GitHub token for authenticating with GitHub.
     * This token must be stored in the server's passphrase element (either in clear text or using standard
     * Maven password encryption support for settings.xml).
     */
    @Parameter(defaultValue = "github", required = true)
    private String serverId;

    /**
     * The repository name of the GitHub repository (e.g., rpatrick00/github-maven-plugin).  By default, the plugin
     * parses the scm section's connection element to extract the repository name.
     */
    @Parameter(property = "github.release.repositoryId", defaultValue = "${project.scm.connection}", required = true)
    private String repositoryId;

    /**
     * The name of the GitHub repository release to create.
     */
    @Parameter(property = "github.release.name", required = true)
    private String name;

    /**
     * The description (aka. body) of the GitHub repository release.
     */
    @Parameter(property = "github.release.description")
    private String description;

    /**
     * The text file containing the description (aka. body) of theGitHub repository release.
     */
    @Parameter(property = "github.release.descriptionFile")
    private File descriptionFile;

    /**
     * The git tag name on which to base the release.  This tag name must already exist in the GitHub repository.
     */
    @Parameter(defaultValue = "${project.version}", required = true)
    private String tag;

    /**
     * Whether or not the new release should be marked as a pre-release.  By default, the plugin analyzes the tag
     * looking at the version number and matching it against popular patterns for pre-release versions in Maven.
     */
    @Parameter
    private Boolean preRelease;

    /**
     * The git commitish on which to base the new release.  See the
     * <a href="https://docs.github.com/en/rest/reference/repos#create-a-release">GitHub Rest API Reference</a> for
     * more information on the semantics of this parameter.
     */
    @Parameter(property = "github.release.commitish")
    private String commmitish;

    /**
     * Whether or not the release should be created in draft form.
     */
    @Parameter(property = "github.release.draft", defaultValue = "false")
    private boolean draft;

    /**
     * The optional list of assets to upload to the release.
     */
    @Parameter
    private List<FileSet> assets;

    /**
     * The MIME type map to use to associate file extensions with their MIME type used for uploading the assets.
     * The default map covers the zip, tgz/tar.gz, and jar file extensions.
     */
    @Parameter
    private Map<String, String> mimeTypeMap;

    /**
     * Whether or not to overwrite an existing asset with the same name.
     */
    @Parameter(property = "github.release.overwriteAssets", defaultValue = "false")
    private boolean overwriteExistingAssets;

    /**
     * Whether or not to skip execution for pre-releases.
     */
    @Parameter(property = "github.release.excludePreReleases", defaultValue = "true")
    private boolean excludePreReleases;

    /**
     * Whether or not the execution should fail if the release already exists.
     */
    @Parameter(property = "github.release.failIfReleaseExists", defaultValue = "false")
    private boolean failIfReleaseExists;

    /**
     * Whether or not to delete any existing release that is found with the same name.
     */
    @Parameter(property = "github.release.deleteExistingRelease", defaultValue = "false")
    private boolean deleteExistingRelease;

    @Override
    public void contextualize(Context context) throws ContextException {
        container = (PlexusContainer) context.get(PlexusConstants.PLEXUS_KEY);
    }

    @Override
    public void execute() throws MojoExecutionException {
        if (StringUtils.isEmpty(tag)) {
            throw getMojoExecutionException("tag parameter must not be empty");
        }

        if (preRelease == null) {
            preRelease = MavenUtils.isPreReleaseVersion(tag);
        }
        if (preRelease && excludePreReleases) {
            logInfo(getLog(), "Release '{0}' with tag '{1}' was marked as a pre-release and " +
                    "pre-releases are being excluded", name, tag);
            return;
        }
        if (mimeTypeMap == null) {
            mimeTypeMap = DEFAULT_MIME_TYPE_MAPPINGS;
        }

        GHRelease release = getRelease();
        uploadAssets(release);
    }

    private GHRelease getRelease() throws MojoExecutionException {
        GitHub github;
        try {
            github = GitHubUtils.connect(container, settings, serverId, getLog());
        } catch (IOException ex) {
            throw getMojoExecutionException(ex, "Failed to connect to GitHub: {0}", ex.getMessage());
        }

        repositoryId = MavenUtils.getRepositoryIdFromScmConnection(repositoryId);
        GHRepository repository;
        try {
            logDebug(getLog(), "getting repository {0}", repositoryId);
            repository = github.getRepository(repositoryId);
        } catch (IOException ex) {
            throw getMojoExecutionException(ex, "Failed to get repository {0} from GitHub: {1}",
                    repositoryId, ex.getMessage());
        }

        GHRelease release;
        try {
            release = GitHubUtils.findReleaseByName(repository, name, getLog());
            if (release == null) {
                logInfo(getLog(), "Repository {0} did not contain release {1}", repository.getName(), name);
            }
        } catch (IOException ex) {
            throw getMojoExecutionException(ex, "Failed to find release {0}: {1}", name, ex.getMessage());
        }

        if (release != null) {
            if (failIfReleaseExists) {
                throw getMojoExecutionException("Release {0} already exists and failIfReleaseExists is true",
                        release.getName());
            } else if (deleteExistingRelease) {
                logInfo(getLog(), "Deleting existing release {0} before creating new release",
                        release.getName());
                try {
                    release.delete();
                } catch (IOException ex) {
                    throw getMojoExecutionException(ex, "Failed to delete release {0}: {1}",
                            release.getName(), ex.getMessage());
                }
                logInfo(getLog(), "Existing release {0} successfully deleted", release.getName());
                release = null;
            } else {
                logInfo(getLog(), "Release {0} already exists so using the existing release", release.getName());
            }
        }
        if (release == null) {
            logInfo(getLog(), "Creating new release {0} for repository {1}", name, repository.getName());
            logDebug(getLog(), "Creating GHReleaseBuilder [ tag: {0}, name: {1}, prerelease: {2}, draft: {3}]",
                    tag, name, preRelease, draft);
            GHReleaseBuilder releaseBuilder = repository.createRelease(tag)
                    .name(name)
                    .prerelease(preRelease)
                    .draft(draft);
            if (StringUtils.isNotEmpty(description)) {
                logDebug(getLog(), "Adding body to GHReleaseBuilder: {0}", description);
                releaseBuilder.body(description);
            } else if (descriptionFile != null) {
                String descriptionFileContents = getFileContents(descriptionFile);
                if (StringUtils.isNotEmpty(descriptionFileContents))
                logDebug(getLog(), "Add body from file {0} to GHReleaseBuilder: {1}",
                         descriptionFile.getAbsolutePath(), descriptionFileContents);
                releaseBuilder.body(descriptionFileContents);
            }

            if (StringUtils.isNotEmpty(commmitish)) {
                logInfo(getLog(), "Adding commitish to GHReleaseBuilder: {0}", commmitish);
                releaseBuilder.commitish(commmitish);
            }

            try {
                logInfo(getLog(), "Calling GHReleaseBuilder.create()...");
                release = releaseBuilder.create();
                logInfo(getLog(), "GHReleaseBuilder.create() returned successfully");
            } catch (IOException ex) {
                logError(getLog(), ex, "Failed to create release: {0}", ex.getMessage());
                throw getMojoExecutionException(ex, "Failed to create new release {0}: {1}", name, ex.getMessage());
            }
        }
        return release;
    }

    private List<GHAsset> uploadAssets(GHRelease release) throws MojoExecutionException {
        List<GHAsset> results = new ArrayList<>();
        if (assets != null) {
            int counter = 1;
            for (FileSet fileSet : assets) {

                List<File> files;
                try {
                    files = FileUtils.getFiles(
                        new File(fileSet.getDirectory()).getAbsoluteFile(),
                        StringUtils.join(fileSet.getIncludes(), ','),
                        StringUtils.join(fileSet.getExcludes(), ',')
                    );
                } catch (IOException ex) {
                    throw getMojoExecutionException(ex, "Failed to get files for fileSet at position {0}: {1}",
                            counter, ex.getMessage());
                }

                for (File file : files) {
                    if (file == null ) {
                        throw getMojoExecutionException("File in fileSet at position {0} is null", counter);
                    } else if (!file.exists()) {
                        throw getMojoExecutionException("File {0} in fileSet at position {1} does not exist",
                                file.getName(), counter);
                    }
                    GHAsset uploadedAsset = uploadAsset(release, file);
                    if (uploadedAsset != null) {
                        results.add(uploadedAsset);
                    } else {
                        logWarn(getLog(),"uploadAsset() for release {0} and file {1} did not upload the asset",
                                release.getName(), file.getName());
                    }
                }
                counter++;
            }
        }
        return results;
    }

    private GHAsset uploadAsset(GHRelease release, File file) throws MojoExecutionException {
        getLog().info("Processing asset " + file.getAbsolutePath());

        List<GHAsset> existingAssets;
        try {
            existingAssets = release.listAssets()
                    .toList()
                    .stream()
                    .filter(a -> a.getName().equals(file.getName()))
                    .collect(Collectors.toList());
        } catch (IOException ex) {
            throw getMojoExecutionException(ex, "Failed to list existing assets for release {0}: {1}",
                    release.getName(), ex.getMessage());
        }

        if (!existingAssets.isEmpty()) {
            if (overwriteExistingAssets) {
                // There should only ever be one but...
                for (GHAsset existingAsset : existingAssets) {
                    logInfo(getLog(), "    Deleting existing asset {0}", existingAsset.getName());
                    try {
                        existingAsset.delete();
                    } catch (IOException ex) {
                        throw getMojoExecutionException(ex, "Failed to delete existing asset {0}",
                                existingAsset.getName());
                    }
                }
            } else {
                logWarn(getLog(), "Asset {0} already exists...skipping upload", existingAssets.get(0).getName());
                return null;
            }
        }

        String mimeType = getMimeTypeFromFileExtension(file.getName());
        logInfo(getLog(), "    Uploading asset {0} as type {1}", file.getName(), mimeType);
        GHAsset uploadedAsset;
        try {
            uploadedAsset = release.uploadAsset(file, mimeType);
        } catch (IOException ex) {
            throw getMojoExecutionException(ex, "Failed to upload asset {0}: {1}", file.getName(), ex.getMessage());
        }
        return uploadedAsset;
    }

    private String getMimeTypeFromFileExtension(String fileName) throws MojoExecutionException {
        if (StringUtils.isEmpty(fileName)) {
            throw getMojoExecutionException("Asset file name was empty");
        } else if (fileName.lastIndexOf('.') == -1) {
            throw getMojoExecutionException("File {0} has no extension so it cannot be mapped to a MIME type",
                                            fileName);
        }

        String[] fileNameComponents = fileName.split("\\.");
        String extension = fileNameComponents[fileNameComponents.length - 1];
        if (fileNameComponents.length == 1) {
            throw getMojoExecutionException("Unable to determine content type for asset file {0} due to " +
                    "missing file extension", fileName);
        } else if (StringUtils.isEmpty(extension)) {
            throw getMojoExecutionException("Unable to determine content type for asset file {0} due to " +
                    "an empty file extension", fileName);
        }

        String mimeType;
        if (mimeTypeMap.containsKey(extension)) {
            mimeType = mimeTypeMap.get(extension);
            if (StringUtils.isEmpty(mimeType)) {
                throw getMojoExecutionException("MIME type map mapped extension {0} mapped to an empty type",
                        extension);
            }
        } else {
            throw getMojoExecutionException("Missing MIME type mapping for file extension {0}", extension);
        }
        return mimeType;
    }

    protected static String getFileContents(File file) throws MojoExecutionException {
        if (file == null) {
            throw getMojoExecutionException("getFileContents() requires a file that is not null");
        } else if (!file.exists()) {
            throw getMojoExecutionException("getFileContents() requires an existing file");
        } else if (file.isDirectory()) {
            throw getMojoExecutionException("getFileContents() requires a file that is not a directory");
        }

        Path filePath = file.toPath();
        StringBuilder contents = new StringBuilder();
        try {
            Files.lines(filePath).forEachOrdered(line -> {
                contents.append(line);
                contents.append('\n');
            });
        } catch (IOException ex) {
            throw getMojoExecutionException(ex, "Reading the contents of file {0} failed: {1}",
                                            file.getAbsolutePath(), ex.getMessage());
        }
        return contents.toString();
    }
}
