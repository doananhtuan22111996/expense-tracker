package dev.tuandoan.expensetracker.data.backup

import dev.tuandoan.expensetracker.data.backup.model.BackupDocumentV1
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackupSerializer
    @Inject
    constructor() {
        private val json =
            Json {
                ignoreUnknownKeys = true
                prettyPrint = true
                encodeDefaults = true
            }

        private val compactJson =
            Json {
                ignoreUnknownKeys = true
                prettyPrint = false
                encodeDefaults = true
            }

        fun encode(document: BackupDocumentV1): String = json.encodeToString(BackupDocumentV1.serializer(), document)

        fun decode(string: String): BackupDocumentV1? =
            try {
                json.decodeFromString(BackupDocumentV1.serializer(), string)
            } catch (_: SerializationException) {
                null
            } catch (_: IllegalArgumentException) {
                null
            }

        fun encodeToStream(
            document: BackupDocumentV1,
            outputStream: OutputStream,
        ) {
            compactJson.encodeToStream(BackupDocumentV1.serializer(), document, outputStream)
        }

        fun decodeFromStream(inputStream: InputStream): BackupDocumentV1? =
            try {
                compactJson.decodeFromStream(BackupDocumentV1.serializer(), inputStream)
            } catch (_: SerializationException) {
                null
            } catch (_: IllegalArgumentException) {
                null
            } catch (_: IOException) {
                null
            }
    }
