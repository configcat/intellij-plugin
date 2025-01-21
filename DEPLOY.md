# Steps to deploy

## Preparation

1. Run tests 
2. Run `verifyPlugin` gradle task
3. Increase the version in the `gradle.properties`. Update the `pluginVersion` property.
4. Update change log
   1. Add changes to the `CHANGELOG.md` under `Unreleased`
   2. Run `patchChangelog` gradle task
5. Commit & Push

## Publish

Use the **same version** for the git tag as in the properties file.

- Via Github release

  Create a new [Github release](https://github.com/configcat/intellij-plugin/releases) with a new version tag and release
  notes.