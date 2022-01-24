package org.openstreetmap.josm.gradle.plugin.demo

import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.TestInfo
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.openstreetmap.josm.gradle.plugin.testutils.GradleProjectUtil
import java.io.File
import java.security.MessageDigest

class DemoTest {

  /**
   * Gradle >= 7.1 is required in order to use [JavaPluginExtension.getSourceSets], because [Project.getConvention]
   * is now deprecated in favour of [Project.getExtensions].
   */
  @Suppress("unused")
  enum class GradleVersion(val expectingSuccess: Boolean) {
    GRADLE_7_0_2(false),
    GRADLE_7_1_1(true),
    GRADLE_7_2(true),
    GRADLE_7_3_3(true);

    val version = name.substring(name.indexOf('_') + 1).replace('_', '.')
  }

  @ParameterizedTest
  @EnumSource(GradleVersion::class)
  fun testDemo(gradleVersion: GradleVersion, testInfo: TestInfo) {
    println("Building demo project with Gradle ${gradleVersion.version}.")
    println("Expecting to ${if (gradleVersion.expectingSuccess) "succeed" else "fail"}!")

    val tmpDir = GradleProjectUtil.createTempSubDir(testInfo, true)
    println("build dir: ${tmpDir.absolutePath}")

    File(DemoTest::class.java.getResource("/demo")!!.toURI()).copyRecursively(tmpDir, overwrite = false)

    val buildResult = GradleRunner.create().withGradleVersion(gradleVersion.version)
      .withProjectDir(tmpDir)
      .withArguments(
        "--stacktrace",
        "build",
        "compileJava_minJosm",
        "compileJava_testedJosm",
        "compileJava_latestJosm",
        "generatePot",
        "localDist",
        "shortenPoFiles",
        "listJosmVersions"
      )
      .forwardOutput()
      .withPluginClasspath()
      .run {
        if (gradleVersion.expectingSuccess) build() else buildAndFail()
      }

    // Basic tests if the files in the build directory at least exist where we would expect them
    if (gradleVersion.expectingSuccess) {
      assertTrue {
        buildResult.output.contains("""
          Resolving required JOSM plugins…
          \* apache-commons \([^)]+\)
          \* apache-http \([^)]+\)
            \* apache-commons \(see above for dependencies\)
            \* jna \([^)]+\)
           → 3 JOSM plugins are required: apache-commons, apache-http, jna
          """.trimIndent().toRegex(RegexOption.MULTILINE)
        )
      }
      assertTrue {
        buildResult.output.contains("""
          locales: de (+ en)
          2 strings for base language en
          ▒█████████████████████████▒ 100.00 % (2 of 2 strings) for de
          """.trimIndent()
        )
      }
      // Files generated by `compilePo`
      assertFileHasSha256Sum(
        "8aac0d93f71979bcabae95b5584f85be70d385e66e8990a4d04613a5e43330ab",
        tmpDir.resolve("build/i18n/main/po/data/de.lang")
      )
      assertFileHasSha256Sum(
        "725baf2700bd187d419ca3cfadaa91ab08caa77bbf054e5ac0877fea6022e5cf",
        tmpDir.resolve("build/i18n/main/po/data/en.lang")
      )
      // Files generated by `generatePot`
      assertTrue {
        tmpDir.resolve("build/i18n/pot/josm-plugin_MyAwesomePlugin.pot").exists()
      }
      assertFileHasSha256Sum(
        "421a1206ab31f1e19736e588c67712f153bf10ed824aac8ef5509fd6341f8aa0",
        tmpDir.resolve("build/i18n/srcFileList/josm-plugin_MyAwesomePlugin.txt")
      )
      // Files generated by `dist`
      assertTrue {
        File(tmpDir, "build/dist/MyAwesomePlugin.jar").exists()
      }
      // Files generated by `localDist`
      assertTrue {
        File(tmpDir, "build/localDist/list").exists() &&
        File(tmpDir, "build/localDist/MyAwesomePlugin-dev.jar").exists()
      }
    }
  }

  fun assertFileHasSha256Sum(sha256sum: String, file: File) {
    assertTrue(file.canRead(), "Can't read ${file.absolutePath} !")
    assertEquals(
      sha256sum,
      MessageDigest.getInstance("SHA-256").digest(file.readBytes()).joinToString("") {
        it.toUByte().toString(16).padStart(2, '0')
      },
      "Wrong checksum for file ${file.absolutePath}"
    )
  }
}
