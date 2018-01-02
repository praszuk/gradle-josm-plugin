package org.openstreetmap.josm.gradle.plugin

import org.gradle.api.Action
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.artifacts.repositories.IvyPatternRepositoryLayout
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test


class ProjectExtensionsTest {

  private fun createGradleJosmProjectWithoutRepos(): Project {
    val pro = ProjectBuilder.builder().build()
    JosmPlugin().apply(pro)
    pro.repositories.removeIf { true }
    return pro
  }

  private fun createRequiredPluginsTestRepo(): Project {
    val pro = createGradleJosmProjectWithoutRepos()
    pro.repositories.add(pro.repositories.ivy {
      it.url = ProjectExtensionsTest::class.java.getResource("/josmPluginRepo").toURI()
      it.layout("pattern", Action<IvyPatternRepositoryLayout> {
        it.artifact("[artifact].jar")
      })
    })
    return pro
  }

  @Test
  fun testRequiredPlugins() {
    val result = createRequiredPluginsTestRepo().getAllRequiredJosmPlugins(setOf("A"))
    assertEquals(6, result.size)
    assertEquals(1, result.count{ it.name == "A" })
    assertEquals(1, result.count{ it.name == "B" })
    assertEquals(1, result.count{ it.name == "C" })
    assertEquals(1, result.count{ it.name == "D" })
    assertEquals(1, result.count{ it.name == "E" })
    assertEquals(1, result.count{ it.name == "F" })
  }

  private fun createNextJosmTestRepo(): Project {
    val pro = createGradleJosmProjectWithoutRepos()
    pro.repositories.add(pro.repositories.ivy {
      it.url = ProjectExtensionsTest::class.java.getResource("/josmRepo").toURI()
      it.layout("pattern", Action<IvyPatternRepositoryLayout> {
        it.artifact("[artifact]-[revision]")
      })
    })
    return pro
  }

  @Test
  fun testNextJosm() {
    assertEquals("100", createNextJosmTestRepo().getNextJosmVersion("51").version)
  }

  @Test
  fun testNextJosmFail() {
    assertThrows(GradleException::class.java, {
      createNextJosmTestRepo().getNextJosmVersion("50")
    })
  }

  @Test
  fun testNextJosmSame() {
    assertEquals("100", createNextJosmTestRepo().getNextJosmVersion("100").version)
  }

  @Test
  fun testNextJosmNull() {
    assertThrows(GradleException::class.java, {
      createNextJosmTestRepo().getNextJosmVersion(null)
    })
  }

  @Test
  fun testNextJosmString() {
    assertEquals("XYZ", createNextJosmTestRepo().getNextJosmVersion("XYZ").version)
  }

  @Test
  fun testNextJosmStringFail() {
    assertThrows(GradleException::class.java, {
      createNextJosmTestRepo().getNextJosmVersion("ABC")
    })
  }

  /**
   * @throws UnknownDomainObjectException
   */
  @Test
  fun testJosmExtension() {
    createGradleJosmProjectWithoutRepos().getJosmExtension()
  }
}
