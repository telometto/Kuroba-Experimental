package com.github.k1rakishou.chan.core.cache.downloader

import com.github.k1rakishou.fsaf.file.RawFile
import java.io.File
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

internal open class FileDownloadRequest(
  val url: String,
  // How many bytes were downloaded across all chunks
  val downloaded: AtomicLong,
  // How many bytes a file we download takes in total
  val total: AtomicLong,
  // A handle to cancel the current download
  val cancelableDownload: CancelableDownload,
  val extraInfo: DownloadRequestExtraInfo,
  // Chunks to delete from the disk upon download success or error
  val chunks: MutableSet<Chunk> = mutableSetOf()
) {
  private var output: RawFile? = null

  private var chunksCount = AtomicInteger(-1)

  @Synchronized
  fun chunksCount(count: Int) {
    chunksCount.set(count)
  }

  @Synchronized
  fun setOutputFile(outputFile: RawFile) {
    if (output != null) {
      throw IllegalStateException("Output file is already set!")
    }

    this.output = outputFile
  }

  @Synchronized
  fun getOutputFile(): RawFile? {
    return output
  }

  override fun toString(): String {
    val outputFileName = synchronized(this) {
      if (output == null) {
        "<null>"
      } else {
        File(output!!.getFullPath()).name
      }
    }

    return "[FileDownloadRequest: url=$url, outputFileName = $outputFileName]"
  }
}

class DownloadRequestExtraInfo(
  val fileSize: Long = -1L,
  val fileHash: String? = null
)