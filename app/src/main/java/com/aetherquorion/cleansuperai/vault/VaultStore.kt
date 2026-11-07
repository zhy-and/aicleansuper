package com.aetherquorion.cleansuperai.vault

import com.tencent.mmkv.MMKV

object VaultStore {
    private const val MMKV_ID = "private_vault_index"
    private const val KEY_ITEMS = "items"
    private const val SEP = "\u001f"

    private fun kv(): MMKV = MMKV.mmkvWithID(MMKV_ID)

    fun list(): List<VaultMediaItem> {
        return try {
            val raw = kv().decodeString(KEY_ITEMS, null) ?: return emptyList()
            if (raw.isBlank()) return emptyList()
            raw.split('\n')
                .mapNotNull { parseLine(it) }
                .sortedByDescending { it.importedAt }
        } catch (_: Throwable) {
            emptyList()
        }
    }

    fun add(item: VaultMediaItem) {
        try {
            val items = list().toMutableList()
            items.removeAll { it.id == item.id }
            items.add(0, item)
            save(items)
        } catch (_: Throwable) {
        }
    }

    fun remove(ids: Collection<String>) {
        try {
            if (ids.isEmpty()) return
            val idSet = ids.toSet()
            save(list().filterNot { it.id in idSet })
        } catch (_: Throwable) {
        }
    }

    fun clear() {
        try {
            kv().encode(KEY_ITEMS, "")
        } catch (_: Throwable) {
        }
    }

    private fun save(items: List<VaultMediaItem>) {
        kv().encode(KEY_ITEMS, items.joinToString("\n") { serialize(it) })
    }

    private fun serialize(item: VaultMediaItem): String {
        return listOf(
            item.id,
            item.vaultRelativePath,
            item.displayName.replace(SEP, "_").replace("\n", " "),
            item.mimeType,
            item.sizeBytes.toString(),
            item.importedAt.toString(),
            if (item.isVideo) "1" else "0"
        ).joinToString(SEP)
    }

    private fun parseLine(line: String): VaultMediaItem? {
        if (line.isBlank()) return null
        val parts = line.split(SEP)
        if (parts.size < 7) return null
        return VaultMediaItem(
            id = parts[0],
            vaultRelativePath = parts[1],
            displayName = parts[2],
            mimeType = parts[3],
            sizeBytes = parts[4].toLongOrNull() ?: 0L,
            importedAt = parts[5].toLongOrNull() ?: 0L,
            isVideo = parts[6] == "1"
        )
    }
}
