# Publishing to GitHub Packages

`thinreports-java` (the core module) is published to
**GitHub Packages** as a Maven artifact:

```
com.github.naofum:thinreports-java:1.0.0
```

The `examples` module is not published — it only demonstrates usage.

## Automatic publish (recommended)

Publishing is wired to `.github/workflows/publish.yml`:

- Push a version tag → the workflow deploys the artifact.
- Or trigger it manually from the Actions tab (workflow_dispatch).

```bash
git tag v1.0.0
git push origin v1.0.0
```

The workflow authenticates with the built-in `GITHUB_TOKEN` (which has
`packages: write` permission granted in the workflow), so no extra secrets are
required as long as the package is published under the same repository owner.

## Manual publish from a local machine

### 1. Create a Personal Access Token (classic)

Create a PAT with the `write:packages` (and `read:packages`) scopes at
GitHub → Settings → Developer settings → Personal access tokens.

### 2. Configure `~/.m2/settings.xml`

The `<id>` must match the `distributionManagement` repository id in `pom.xml`
(`github`):

```xml
<settings>
  <servers>
    <server>
      <id>github</id>
      <username>YOUR_GITHUB_USERNAME</username>
      <password>YOUR_PERSONAL_ACCESS_TOKEN</password>
    </server>
  </servers>
</settings>
```

> Do not commit your token. Keep it only in `~/.m2/settings.xml` or a secret store.

### 3. Deploy

```bash
mvn -DskipTests deploy
```

This uploads the main jar plus the attached `-sources.jar` and `-javadoc.jar`
to `https://maven.pkg.github.com/naofum/thinreports-java`.

## Consuming the published artifact

Consumers also need a `github` server entry (with a `read:packages` token) in
their `settings.xml`, plus this repository in their `pom.xml`:

```xml
<repositories>
  <repository>
    <id>github</id>
    <url>https://maven.pkg.github.com/naofum/thinreports-java</url>
  </repository>
</repositories>

<dependency>
  <groupId>com.github.naofum</groupId>
  <artifactId>thinreports-java</artifactId>
  <version>1.0.0</version>
</dependency>
```

## Notes

- GitHub Packages does not allow overwriting an existing release version.
  Bump the `<version>` in `pom.xml` before re-deploying.
- SNAPSHOT versions can be re-deployed and use the same `snapshotRepository`.
