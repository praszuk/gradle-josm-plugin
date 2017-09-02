package org.openstreetmap.josm.gradle.plugin

import org.gradle.api.Project
import org.gradle.api.plugins.PluginInstantiationException

/**
 * The info that will be written into the manifest file of the plugin *.jar
 */
public class Manifest {
  protected final Project project;
  /**
   * The author of the plugin.
   *
   * <p><strong>Default:</strong> the value of property <code>plugin.author</code> or <code>null</code> if that property is not set.</p>
   */
  def String author = project.findProperty('plugin.author')
  /**
   * Determines if the plugin needs a restart after installation. <code>true</code> if no restart is required, <code>false</code> otherwise.
   *
   * <p><strong>Default:</strong> the value of property <code>plugin.canloadatruntime</code> or <code>null</code> if that property is not set.</p>
   */
  def boolean canLoadAtRuntime = Boolean.valueOf(project.findProperty('plugin.canloadatruntime'))
  /**
   * The description of what the plugin does.
   *
   * <p><strong>Default:</strong> the value of property <code>plugin.description</code> or <code>null</code> if that property is not set.</p>
   */
  def String description = project.findProperty('plugin.description')
  /**
   * Path to the logo of the plugin. Relative to the root of the released jar-file, so that it can be loaded via <code>getClass.getResource()</code>.
   *
   * <p><strong>Default:</strong> the value of property <code>plugin.icon</code> or <code>null</code> if that property is not set.</p>
   */
  def String iconPath = project.findProperty('plugin.icon')
  /**
   * This can be set to <code>true</code>, when the plugin should load before the GUI classes of JOSM.
   *
   * <p><strong>Default:</strong> The value of property <code>plugin.early</code> or <code>false</code> if that property is not set.</p>
   */
  def boolean loadEarly = Boolean.valueOf(project.findProperty('plugin.early'))
  /**
   * A number indicating the order in which the plugins should be loaded. Lower numbers first, higher numbers later, then the plugins with this field set to <code>null</code>.
   *
   * <p><strong>Default:</strong> The value of property <code>plugin.stage</code> or <code>null</code> if that property is not set.</p>
   */
  def Integer loadPriority = project.hasProperty('plugin.stage') ? Integer.valueOf(project.findProperty('plugin.stage')) : null
  /**
   * The main class of the plugin
   *
   * <p><strong>Default:</strong> the value of property <code>plugin.class</code> or <code>null</code> if that property is not set.</p>
   */
  def String mainClass = project.findProperty('plugin.class')
  /**
   * The minimum JOSM version with which the plugin is compatible.<br>
   * This field is required!
   *
   * <p><strong>Default:</strong> the value of property <code>plugin.main.version</code> or <code>null</code> if that property is not set.</p>
   */
  def String minJosmVersion = project.findProperty('plugin.main.version')
  /**
   * A collection of the names of all JOSM plugins that must be installed for this JOSM plugin to work
   *
   * <p><strong>Default:</strong> the value of property <code>plugin.requires</code> split at every semicolon (do not rely on the order, as it is not necessarily maintained) or <code>null</code> if that property is not set.</p>
   */
  final def Set<String> pluginDependencies = new HashSet<>()
  /**
   * A URL pointing to a web resource describing the plugin.
   *
   * <p><strong>Default:</strong> The value of property <code>plugin.link</code> as URL (might error out on malformed URLs), or <code>null</code> if that property is not set.</p>
   */
  def URL website = project.hasProperty('plugin.link') ? new URL(project.findProperty('plugin.link')) : null
  /**
   * For compatibility with older JOSM versions, that are not supported by the current version of the plugin,
   * this field contains URLs where versions of the plugin can be downloaded, which are compatible with older JOSM versions.
   * The URL value points to a location where the plugin can be downloaded from and the integer key denotes the minimum JOSM version that the plugin at that location is compatible with.
   */
  final def Map<Integer, URL> oldVersionDownloadURLs = [:]

  /**
   * Initialize the manifest for the given project
   * @param project the {@link Project} for which the manifest should be initialized
   */
  public Manifest(final Project project) {
    this.project = project

    project.gradle.projectsEvaluated {
      boolean missesRequiredFields =
        isRequiredFieldMissing(minJosmVersion == null, "the minimum JOSM version your plugin is compatible with", "josm.manifest.minJosmVersion = ‹a JOSM version›") |
        isRequiredFieldMissing(project.version == Project.DEFAULT_VERSION, "the version of your plugin", "version = ‹a version number›") |
        isRequiredFieldMissing(mainClass == null, "the main class of your plugin", "josm.manifest.mainClass = ‹full name of main class›") |
        isRequiredFieldMissing(description == null, "the description of your plugin", "josm.manifest.description = ‹a textual description›")

      if (missesRequiredFields) {
        throw new PluginInstantiationException("The JOSM plugin misses required configuration options. See above for which options are missing.")
      }
    }

    // Fill the map containing the plugin dependencies
    final def requirements = project.findProperty('plugin.requires')
    if (requirements != null) {
      String[] dependencyArray = requirements.split(';')
      for (String dependency : dependencyArray) {
        dependency = dependency.trim()
        if (dependency.length() >= 1) {
          pluginDependencies.add(dependency)
        }
      }
    }
  }

  /**
   *
   * @param checkResult
   * @param fieldDescription a textual description of the field (e.g. "the version of your plugin")
   * @param requiredValue the property which needs to be set in order to correct for this error (e.g. "josm.manifest.requiredValue = ‹some value›")
   * @return <code>true</code> if the required field is <code>null</code>, <code>false</code> otherwise
   */
  private boolean isRequiredFieldMissing(boolean checkResult, String fieldDescription, String requiredValue) {
    if (checkResult) {
      System.err.printf("You haven't configured %s. Please add %s to your build.gradle file.\n", fieldDescription, requiredValue)
    }
    return checkResult
  }

  public Map<String,String> createJosmPluginJarManifest() {
    // Required attributes
    def manifestAtts = [
      "Created-By": System.getProperty("java.version") + " (" + System.getProperty("java.vendor") + ")",
      "Gradle-Version": project.gradle.getGradleVersion(),
      "Plugin-Class": mainClass,
      "Plugin-Date": String.format("%1\$tY-%1\$tm-%1\$tdT%1\$tH:%1\$tM:%1\$tS%1\$tz", new GregorianCalendar()),
      "Plugin-Description": description,
      "Plugin-Mainversion": minJosmVersion,
      "Plugin-Version": project.version,
      "Plugin-Early": loadEarly,
      "Plugin-Canloadatruntime": canLoadAtRuntime
    ]
    oldVersionDownloadURLs.each { key, value ->
      manifestAtts << [ (key+"_Plugin-Url") : value.toString()]
    }

    // Optional attributes
    if (author != null) {
      manifestAtts["Author"] = author
    }
    if (iconPath != null) {
      manifestAtts["Plugin-Icon"] = iconPath
    }
    if (website != null) {
      manifestAtts["Plugin-Link"] = website
    }
    if (pluginDependencies.size() >= 1) {
      manifestAtts["Plugin-Requires"] = pluginDependencies.join(';')
    }
    if (loadPriority != null) {
      manifestAtts["Plugin-Stage"] = loadPriority
    }

    project.logger.info "The following lines will be added to the manifest of the plugin *.jar file:"
    manifestAtts.sort().each { e ->
      project.logger.info "  "+e.key+": "+e.value
    }
    return manifestAtts
  }
}
