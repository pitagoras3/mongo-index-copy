package io.github.pitagoras3.mongoindexcopy

import com.mongodb.MongoException

class MongoIndexCopyException @JvmOverloads constructor(
  override val message: String,
  override val cause: Throwable? = null
) : MongoException(message, cause) {
  companion object {
    private const val serialVersionUID: Long = 1
  }
}
