package dev.tuandoan.expensetracker.data.backup

import dev.tuandoan.expensetracker.data.backup.model.BackupDocumentV1
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
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

        fun encode(document: BackupDocumentV1): String = json.encodeToString(BackupDocumentV1.serializer(), document)

        fun decode(string: String): BackupDocumentV1? =
            try {
                json.decodeFromString(BackupDocumentV1.serializer(), string)
            } catch (_: SerializationException) {
                null
            } catch (_: IllegalArgumentException) {
                null
            }
    }
