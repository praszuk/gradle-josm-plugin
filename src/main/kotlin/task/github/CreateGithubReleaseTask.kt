package org.openstreetmap.josm.gradle.plugin.task.github

import org.gradle.api.tasks.TaskAction
import org.openstreetmap.josm.gradle.plugin.github.GithubReleaseException
import org.openstreetmap.josm.gradle.plugin.github.ReleasesSpec
import org.openstreetmap.josm.gradle.plugin.josm

/**
 * Task to create a github release using the github API.
 */
open class CreateGithubReleaseTask : BaseGithubReleaseTask() {

  @TaskAction
  fun createGithubRelease() {
    val releaseConfigFile = project.extensions.josm.github.releasesConfig
    val releaseLabel = configuredReleaseLabel
    val releaseConfig = ReleasesSpec.load(releaseConfigFile)

    val notFound = GithubReleaseException(
      """The releases config file '$releaseConfigFile' doesn't include a
            |release with release label '$releaseLabel yet. Add and configure
            |a release with this label in '$releaseConfigFile' and rerun."""
        .trimMargin("|")
    )
    val release = releaseConfig[releaseLabel] ?: throw notFound

    val client = githubReleaseClient()

    client.getReleases().find {it["tag_name"] == releaseLabel}?.let {
      throw GithubReleaseException(
        "Release with release label '$releaseLabel' already exists "
          + "on the GitHub server."
      )
    }

    try {
      client.createRelease(
        tagName = releaseLabel,
        targetCommitish = configuredTargetCommitish,
        name = release.name,
        body = release.description)
      logger.lifecycle("New release '{}' created in GitHub repository",
        releaseLabel)
    } catch(e: Throwable) {
      throw GithubReleaseException(e)
    }
  }
}
