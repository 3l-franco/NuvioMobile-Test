package com.nuvio.app.features.player

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class OpenTypeNameReaderTest {
    @Test
    fun readsLegacyFamilyNameAheadOfTypographicFamilyName() {
        val bytes = fontFixture(
            names = listOf(
                16 to "Modern Display",
                1 to "Nuvio Family",
            ),
        )

        assertEquals("Nuvio Family", OpenTypeNameReader.familyName(bytes))
    }

    @Test
    fun rejectsMalformedFontData() {
        assertNull(OpenTypeNameReader.familyName(byteArrayOf(0, 1, 2, 3)))
        assertNull(OpenTypeNameReader.familyName(ByteArray(32)))
    }

    @Test
    fun identifiesTrueTypeAndOpenTypeFileExtensions() {
        assertEquals("ttf", OpenTypeNameReader.fileExtension(fontFixture()))
        assertEquals("otf", OpenTypeNameReader.fileExtension(fontFixture(signature = 0x4F54544F)))
        assertEquals("ttc", OpenTypeNameReader.fileExtension(collectionFixture(faceSignature = 0x00010000)))
        assertEquals("otc", OpenTypeNameReader.fileExtension(collectionFixture(faceSignature = 0x4F54544F)))
    }

    private fun collectionFixture(faceSignature: Int): ByteArray {
        val bytes = ByteArray(20)

        fun writeU32(offset: Int, value: Int) {
            bytes[offset] = (value ushr 24).toByte()
            bytes[offset + 1] = (value ushr 16).toByte()
            bytes[offset + 2] = (value ushr 8).toByte()
            bytes[offset + 3] = value.toByte()
        }

        writeU32(0, 0x74746366) // TTC collection signature
        writeU32(4, 0x00010000) // collection version
        writeU32(8, 1) // one face
        writeU32(12, 16) // first face offset
        writeU32(16, faceSignature)
        return bytes
    }

    private fun fontFixture(
        names: List<Pair<Int, String>> = listOf(1 to "Nuvio Family"),
        signature: Int = 0x00010000,
    ): ByteArray {
        val nameTableOffset = 64
        val stringStorageOffset = 6 + names.size * 12
        val encodedNames = names.map { (_, name) ->
            name.map { character ->
                val code = character.code
                byteArrayOf((code shr 8).toByte(), code.toByte())
            }.fold(ByteArray(0)) { result, part -> result + part }
        }
        val stringBytesSize = encodedNames.sumOf { it.size }
        val bytes = ByteArray(nameTableOffset + stringStorageOffset + stringBytesSize)

        fun writeU16(offset: Int, value: Int) {
            bytes[offset] = (value shr 8).toByte()
            bytes[offset + 1] = value.toByte()
        }

        fun writeU32(offset: Int, value: Int) {
            writeU16(offset, value ushr 16)
            writeU16(offset + 2, value and 0xFFFF)
        }

        writeU32(0, signature)
        writeU16(4, 1)
        writeU32(12, 0x6E616D65) // name table tag
        writeU32(20, nameTableOffset)
        writeU32(24, stringStorageOffset + stringBytesSize)

        writeU16(nameTableOffset, 0) // name table format
        writeU16(nameTableOffset + 2, names.size)
        writeU16(nameTableOffset + 4, stringStorageOffset)

        var stringOffset = 0
        names.forEachIndexed { index, (nameId, name) ->
            val recordOffset = nameTableOffset + 6 + index * 12
            val encoded = encodedNames[index]
            writeU16(recordOffset, 3) // Windows platform
            writeU16(recordOffset + 2, 1) // Unicode BMP encoding
            writeU16(recordOffset + 4, 0x0409) // English (United States)
            writeU16(recordOffset + 6, nameId)
            writeU16(recordOffset + 8, encoded.size)
            writeU16(recordOffset + 10, stringOffset)
            encoded.copyInto(bytes, nameTableOffset + stringStorageOffset + stringOffset)
            stringOffset += encoded.size
        }

        return bytes
    }
}
