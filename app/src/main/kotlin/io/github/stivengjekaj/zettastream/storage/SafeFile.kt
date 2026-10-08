package io.github.stivengjekaj.zettastream.storage

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Writes a file through a temporary file, so that a stop during a write keeps
 * the old file. One write runs at a time, so two writes do not mix their bytes.
 */
class SafeFile(private val file: File) {
    private val lock = Mutex()

    suspend fun write(text: String) = lock.withLock {
        withContext(Dispatchers.IO) {
            val temp = File(file.parentFile, file.name + ".tmp")
            temp.writeText(text)
            temp.renameTo(file)
        }
    }
}
