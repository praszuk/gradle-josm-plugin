package org.openstreetmap.josm.gradle.plugin.i18n.io

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

@ExperimentalUnsignedTypes
class I18nReadWriteTest {

  val emptyTranslations: Map<MsgId, MsgStr> = mapOf(GETTEXT_DEFAULT_HEADER)
  val translations1: Map<MsgId, MsgStr> = mapOf(
    MsgId(MsgStr("")) to MsgStr("Sing\nSing2\n$GETTEXT_CONTENT_TYPE_UTF8\n"),
    MsgId(MsgStr("1", "2")) to MsgStr("Sing"),
    MsgId(MsgStr("1", "2"), "context") to MsgStr("Singular", "Plural"),
    MsgId(MsgStr("Many plurals (253 is maximum of *.lang)", "2")) to MsgStr("1", *(2..253).map { it.toString() }.toTypedArray()),
    MsgId(MsgStr("Emoji \uD83D\uDE0D", "\uD83C\uDDF1\uD83C\uDDFB"), "\uD83D\uDE39") to MsgStr("\uD83E\uDDB8\uD83C\uDFFF\u200D♂️", "\uD83C\uDFF3️\u200D\uD83C\uDF08", ""),
    MsgId(MsgStr("Umlaut äöüÄÖÜ")) to MsgStr("ẞß"),
    MsgId(MsgStr("Special escape chars")) to MsgStr("\u0007\u0008\u000C\n\r\t\u000B\\\"")
  )

  val asciiTranslations = mapOf(
    GETTEXT_DEFAULT_HEADER,
    MsgId(MsgStr("A")) to MsgStr("A+"),
    MsgId(MsgStr("XYZ42")) to MsgStr("XYZ42+"),
    MsgId(MsgStr("context"), "context*") to MsgStr("context+"),
    MsgId(MsgStr("plural", "plural~")) to MsgStr("plural+"),
    MsgId(MsgStr("pluralContext", "pluralContext~"), "pluralContext*") to MsgStr("pluralContext+", "pluralContext2+"),
    MsgId(MsgStr("ABC")) to MsgStr("ABC"),
    MsgId(MsgStr("ABC", "ABC")) to MsgStr("ABC", "ABC"),
    MsgId(MsgStr("ABC", "ABC"), "ABC") to MsgStr("ABC", "ABC", "ABC"),
  )

  val dummyTranslationsEn: Map<String, Map<MsgId, MsgStr>> = LangReaderTest().getDummyTranslations("en")
  val dummyTranslationsRu: Map<String, Map<MsgId, MsgStr>> = LangReaderTest().getDummyTranslations("ru")

  @Test
  fun testMoSerializationPersistence() {
    testMoSerializationPersistence(emptyTranslations, "empty")
    testMoSerializationPersistence(translations1, "translations1")
    testMoSerializationPersistence(asciiTranslations, "ascii")
    dummyTranslationsEn.forEach { (language, translations) ->
      testMoSerializationPersistence(translations.plus(GETTEXT_DEFAULT_HEADER), "dummy-$language")
    }
  }

  private fun testMoSerializationPersistence(translations: Map<MsgId, MsgStr>, name: String) {
    testMoSerializationPersistence(translations, true, name)
    testMoSerializationPersistence(translations, false, name)
  }

  private fun testMoSerializationPersistence(translations: Map<MsgId, MsgStr>, isBigEndian: Boolean, name: String) {
    val writeResult1 = ByteArrayOutputStream()
    MoWriter().writeStream(writeResult1, translations, isBigEndian)

    val readResult1 = MoReader(writeResult1.toByteArray()).readFile()
    val writeResult2 = ByteArrayOutputStream()
    MoWriter().writeStream(writeResult2, readResult1, isBigEndian)
    val readResult2 = MoReader(writeResult2.toByteArray()).readFile()

    assertEquals(translations, readResult1)
    assertEquals(readResult1, readResult2)
    assertEquals(writeResult1.toByteArray().toList(), writeResult2.toByteArray().toList())

    assertEquals(I18nReadWriteTest::class.java.getResource("mo/$name-${if (isBigEndian) "BE" else "LE"}.mo")?.readBytes()?.toList(), writeResult1.toByteArray().toList()) {
      "Contents of mo/$name-${if (isBigEndian) "BE" else "LE"}.mo are not generated as expected!"
    }
  }

  @Test
  fun testPoSerializationPersistence() {
    testPoSerializationPersistence(emptyTranslations, "empty")
    testPoSerializationPersistence(translations1, "translations1")
    testPoSerializationPersistence(asciiTranslations, "ascii")
    dummyTranslationsEn.forEach { (language, translations) ->
      testPoSerializationPersistence(translations.plus(GETTEXT_DEFAULT_HEADER), "dummy-$language")
    }
  }

  private fun testPoSerializationPersistence(translations: Map<MsgId, MsgStr>, name: String) {
    val bytes = PoFormat().encodeToByteArray(translations)
    assertEquals(translations, PoFormat().decodeToTranslations(bytes)) {
      "Contents of po/$name.po are not parsed as expected!"
    }
    assertEquals(I18nReadWriteTest::class.java.getResource("po/$name.po")?.readBytes()?.decodeToString(), bytes.decodeToString()) {
      "Contents of po/$name.po are not generated as expected!"
    }
  }

  @Test
  fun testLangSerializationPersistence() {
    testLangSerializationPersistence(emptyTranslations, "empty")
    testLangSerializationPersistence(translations1, "translations1")
    testLangSerializationPersistence(asciiTranslations, "ascii")

    val originalMsgIdsEn = requireNotNull(dummyTranslationsEn["en"]?.map { it.key })

    val dummyEnLangBytes = dummyTranslationsEn.map { (language, translations) ->
      val stream = ByteArrayOutputStream()
      LangWriter().writeLangStream(stream, originalMsgIdsEn, translations, language == "en")
      assertEquals(I18nReadWriteTest::class.java.getResource("lang/dummy-baseEn-$language.lang")?.readBytes()?.toList(), stream.toByteArray().toList())

      language to ByteArrayInputStream(stream.toByteArray())
    }.toMap()
    assertEquals(dummyTranslationsEn, LangReader().readLangStreams("en", dummyEnLangBytes["en"]!!, dummyEnLangBytes.minus("en")))


    val originalMsgIdsRu = requireNotNull(dummyTranslationsRu["ru"]?.map { it.key })
    val dummyRuLangBytes = dummyTranslationsRu.map { (language, translations) ->
      val stream = ByteArrayOutputStream()
      LangWriter().writeLangStream(stream, originalMsgIdsRu, translations, language == "ru")
      assertEquals(I18nReadWriteTest::class.java.getResource("lang/dummy-baseRu-$language.lang")?.readBytes()?.toList(), stream.toByteArray().toList())

      language to ByteArrayInputStream(stream.toByteArray())
    }.toMap()
    assertEquals(dummyTranslationsRu, LangReader().readLangStreams("ru", dummyRuLangBytes["ru"]!!, dummyRuLangBytes.minus("ru")))
  }

  fun testLangSerializationPersistence(translations: Map<MsgId, MsgStr>, name: String) {
    val langBaseStream = ByteArrayOutputStream()
    val langStream = ByteArrayOutputStream()
    LangWriter().writeLangStream(langBaseStream, translations.map { it.key }.filter { it.id.strings.first().isNotEmpty() }, translations.mapValues { it.key.id }, true)
    LangWriter().writeLangStream(langStream, translations.entries.map { it.key }.filter { it.id.strings.first().isNotEmpty() }, translations, false)
    val langBaseBytes = langBaseStream.toByteArray()
    val langBytes = langStream.toByteArray()

    assertEquals(
      translations.filter { it.key.id.strings.any { it.isNotEmpty() } },
      LangReader().readLangStreams("en", ByteArrayInputStream(langBaseBytes), mapOf("xy" to ByteArrayInputStream(langBytes)))["xy"]
    )

    assertEquals(I18nReadWriteTest::class.java.getResource("lang/$name-en.lang")?.readBytes()?.toList(), langBaseBytes.toList())
    assertEquals(I18nReadWriteTest::class.java.getResource("lang/$name-xy.lang")?.readBytes()?.toList(), langBytes.toList())
  }

  @Test
  fun testMoToLangAndBack() {
    testMoToLangAndBack(emptyTranslations)
    testMoToLangAndBack(translations1)
    testMoToLangAndBack(asciiTranslations)
  }

  fun testMoToLangAndBack(translations: Map<MsgId, MsgStr>) {
    val moWriteResult1 = ByteArrayOutputStream()
    MoWriter().writeStream(moWriteResult1, translations, true)

    val moReadResult1 = MoReader(moWriteResult1.toByteArray()).readFile()
    // The empty string is not envisaged in the *.lang file format, so it is extracted here and put back into the result later
    val emptyElement = moReadResult1.keys.firstOrNull { it.toByteArray().isEmpty() }
    assertEquals(translations, moReadResult1)

    val langStreamOrig = ByteArrayOutputStream()
    LangWriter().writeLangStream(langStreamOrig, moReadResult1.minus(listOfNotNull(emptyElement).toTypedArray()).keys.toList(), mapOf(), true)
    val langStreamTrans = ByteArrayOutputStream()
    LangWriter().writeLangStream(langStreamTrans, moReadResult1.minus(listOfNotNull(emptyElement).toTypedArray()).keys.toList(), moReadResult1)

    val langReadResult = LangReader().readLangStreams("en", ByteArrayInputStream(langStreamOrig.toByteArray()), mapOf("es" to ByteArrayInputStream(langStreamTrans.toByteArray()))).get("es")
      // putting the empty element back into the result (if present)
      ?.plus(listOfNotNull(emptyElement).map { it to moReadResult1.get(it) })

    assertEquals(moReadResult1, langReadResult)
    assertNotNull(langReadResult)
    requireNotNull(langReadResult)

    val moWriteResult2 = ByteArrayOutputStream()
    MoWriter().writeStream(moWriteResult2, langReadResult.mapNotNull{ val value = it.value; if (value != null) it.key to value else null }.toMap(), false)

    val moReadResult2 = MoReader(moWriteResult2.toByteArray()).readFile()
    assertEquals(langReadResult, moReadResult2)
  }
}
