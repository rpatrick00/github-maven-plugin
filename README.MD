# The github-maven-plugin Project
This plugin was created out of the need to interact with GitHub during the project release cycle.  The first goal `create-release` was inspired by and the code adapted from Johannes Utzig's [github-release-plugin](https://github.com/jutzig/github-release-plugin) project.  I have tried to start a structure that will allow us to add other goals that use other GitHub APIs as the need arises.  The plugin uses the [GHitHub API for Java](https://github-api.kohsuke.org/) project to handle the interactions with GitHub.

This plugin requires the use of a GitHub token with the necessary permissions to perform the requested operations.  By default, we leverage the Maven `settings.xml` file's `<server>` stanza to provide the token in the `<passphrase>` element.  Doing this allows us to use Maven's built-in support for encryption so that the encrypted token is stored, as shown below.  For more information about Maven encryption, see the [Maven Encryption Guide](https://maven.apache.org/guides/mini/guide-encryption.html).

```xml
    <server>
      <id>github</id>
      <passphrase>{kRBjmPgdyiHERnNNRp3tWCk2zPWMXBKJQVHxpVcW4T/FCp/L2h29+TRmVlgXh9gAX83hnhS302zAoVSdUTc/FA==}</passphrase>
    </server>
```
If you need to configure a proxy server, we leverage the Maven `settings.xml` file's `<proxies>` stanza to get the `host`, `port`, and `nonProxyHosts` and use those to set the Java System Properties `https.proxyHost`, `https.proxyPort`, and `http.nonProxyHosts`, respectively.  Make sure that the proxy is marked active!

```xml
  <proxies>
    <proxy>
      <active>true</active>
      <id>mycompany</id>
      <protocol>http</protocol>
      <host>proxy.mycompany.com</host>
      <port>80</port>
      <nonProxyHosts>*.us.mycompany.com|*.internal.mycompany.com</nonProxyHosts>
    </proxy>
  </proxies>
```

The other alternative is to set these Java System Properties directly on the command line when running Maven.

To use the plugin, you will need to configure it in your `pom.xml`:

```xml
<plugin>
    <groupId>io.rhpatrick.mojo</groupId>
    <artifactId>github-maven-plugin</artifactId>
    <version>0.6</version>
    <configuration>
        <tag>v${project.version}</tag>
        <name>The name of the release</name>
        <description>The body of the release</description>
        <assets>
            <fileSet>
                <directory>${project.build.directory}</directory>
                <includes>
                    <include>${project.artifactId}*.zip</include>
                    <include>${project.artifactId}*.tar.gz</include>
                </includes>
            </fileSet>
        </assets>
    </configuration>
</plugin>
```

The configuration above depends on several defaults to note:

1. The `settings.xml` file's `<server>` ID is github.  If not, you will need to add the `<serverId>` element and set its value to the ID of the server from `settings.xml`.
2. The `pom.xml` file has an `<scm>` section with the `<connection>` element's value set with an SCM url for Github (only http, https, and SSH formats are supported).  The plugin will extract the repository ID from this URL.  If not, you will need to add the `<repositoryId>` element with an appropriate value for your repository (e.g., the `repositoryId` for this project would be `rpatrick00/github-maven-plugin`).

The plugin has many options that have reasonable default values but can be overridden as needed.  To see the details, use Maven's `help` plugin to see the details:

```bash
mvn help:describe -Dplugin=io.rhpatrick.mojo:github-maven-plugin:0.7 -Dgoal=create-release -Ddetail
```
