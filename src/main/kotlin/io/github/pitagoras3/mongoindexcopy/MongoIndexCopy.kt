package io.github.pitagoras3.mongoindexcopy

import com.mongodb.client.MongoCollection
import com.mongodb.client.model.Collation
import com.mongodb.client.model.CollationAlternate
import com.mongodb.client.model.CollationCaseFirst
import com.mongodb.client.model.CollationMaxVariable
import com.mongodb.client.model.CollationStrength
import com.mongodb.client.model.CreateIndexOptions
import com.mongodb.client.model.IndexModel
import com.mongodb.client.model.IndexOptions
import org.bson.Document
import org.bson.conversions.Bson
import java.util.concurrent.TimeUnit

object MongoIndexCopy {

  /**
   * Copy specified indexes from one Mongo collection to another Mongo collection.
   *
   * @param indexNamesToCopy Set of index names which should be copied from source collection to destination collection
   * @param source MongoCollection which is a source of indexes to copy
   * @param destination MongoCollection where copied indexes should be created
   * @param createIndexOptions CreateIndexOptions which should be applied when creating copied indexes on destination collection.
   * @return List of successfully copied index names
   * @throws io.github.pitagoras3.mongoindexcopy.MongoIndexCopyException If something wrong happens when copying indexes from source collection to destination collection.
   */
  @JvmStatic
  @JvmOverloads
  fun copyIndexes(
    indexNamesToCopy: Set<String>,
    source: MongoCollection<*>,
    destination: MongoCollection<*>,
    createIndexOptions: CreateIndexOptions? = null,
  ): List<String> =
    performIndexCopy(destination, createIndexOptions) { filteredSourceIndexes(source, indexNamesToCopy) }

  /**
   * Copy all indexes from one Mongo collection to another Mongo collection.
   *
   * @param source MongoCollection which is a source of indexes to copy
   * @param destination MongoCollection where copied indexes should be created
   * @param createIndexOptions CreateIndexOptions which should be applied when creating copied indexes on destination collection.
   * @return List of successfully copied index names
   * @throws io.github.pitagoras3.mongoindexcopy.MongoIndexCopyException If something wrong happens when copying indexes from source collection to destination collection.
   */
  @JvmStatic
  @JvmOverloads
  fun copyAllIndexes(
    source: MongoCollection<*>,
    destination: MongoCollection<*>,
    createIndexOptions: CreateIndexOptions? = null,
  ): List<String> = performIndexCopy(destination, createIndexOptions) { allSourceIndexes(source) }

  private fun performIndexCopy(
    destination: MongoCollection<*>,
    createIndexOptions: CreateIndexOptions?,
    indexesToCopyProvider: () -> List<Document>,
  ): List<String> = runCatching {
    val indexModels = indexesToCopyProvider.invoke().map {
      IndexModel(it.toBsonDocument().getDocument("key"), copyOfIndexOptions(it))
    }
    if (indexModels.isNotEmpty()) {
      createIndexOptions?.let { destination.createIndexes(indexModels, it) } ?: destination.createIndexes(indexModels)
    } else emptyList()
  }.fold({ it }, { cause ->
    throw MongoIndexCopyException(
      "Could not copy MongoDB indexes from source collection to destination collection", cause
    )
  })

  @Suppress("DEPRECATION") // bucketSize is deprecated
  private fun copyOfIndexOptions(source: Document) = IndexOptions().apply {
    if (source.containsKey("background")) this.background(source.getBoolean("background"))
    if (source.containsKey("unique")) this.background(source.getBoolean("unique"))
    if (source.containsKey("name")) this.name(source.getString("name"))
    if (source.containsKey("sparse")) this.sparse(source.getBoolean("sparse"))
    if (source.containsKey("expireAfterSeconds")) this.expireAfter(
      source.getInteger("expireAfterSeconds").toLong(),
      TimeUnit.SECONDS
    )
    if (source.containsKey("v")) this.version(source.getInteger("v"))
    if (source.containsKey("weights")) this.weights(source.get("weights", Bson::class.java))
    if (source.containsKey("default_language")) this.defaultLanguage(source.getString("default_language"))
    if (source.containsKey("language_override")) this.languageOverride(source.getString("language_override"))
    if (source.containsKey("textIndexVersion")) this.textVersion(source.getInteger("textIndexVersion"))
    if (source.containsKey("2dsphereIndexVersion")) this.sphereVersion(source.getInteger("2dsphereIndexVersion"))
    if (source.containsKey("bits")) this.bits(source.getInteger("bits"))
    if (source.containsKey("min")) this.min(source.getDouble("min"))
    if (source.containsKey("max")) this.max(source.getDouble("max"))
    if (source.containsKey("bucketSize")) this.bucketSize(source.getDouble("bucketSize"))
    if (source.containsKey("storageEngine")) this.storageEngine(source.get("storageEngine", Bson::class.java))
    if (source.containsKey("partialFilterExpression")) this.partialFilterExpression(
      source.get(
        "partialFilterExpression",
        Bson::class.java
      )
    )
    if (source.containsKey("collation")) this.collation(copyOfCollation(source))
    if (source.containsKey("wildcardProjection")) this.wildcardProjection(
      source.get(
        "wildcardProjection",
        Bson::class.java
      )
    )
    if (source.containsKey("hidden")) this.hidden(source.getBoolean("hidden"))
  }

  private fun copyOfCollation(source: Document): Collation =
    source.toBsonDocument().getDocument("collation").let { collationRawBson ->
      with(Collation.builder()) {
        if (collationRawBson.containsKey("locale")) locale(collationRawBson.getString("locale").value)
        if (collationRawBson.containsKey("caseLevel")) caseLevel(collationRawBson.getBoolean("caseLevel").value)
        if (collationRawBson.containsKey("caseFirst")) collationCaseFirst(
          CollationCaseFirst.fromString(
            collationRawBson.getString("caseFirst").value
          )
        )
        if (collationRawBson.containsKey("strength")) collationStrength(
          CollationStrength.fromInt(
            collationRawBson.getInt32("strength").value
          )
        )
        if (collationRawBson.containsKey("numericOrdering")) caseLevel(collationRawBson.getBoolean("numericOrdering").value)
        if (collationRawBson.containsKey("alternate")) collationAlternate(
          CollationAlternate.fromString(
            collationRawBson.getString("alternate").value
          )
        )
        if (collationRawBson.containsKey("maxVariable")) collationMaxVariable(
          CollationMaxVariable.fromString(
            collationRawBson.getString("maxVariable").value
          )
        )
        if (collationRawBson.containsKey("normalization")) normalization(collationRawBson.getBoolean("normalization").value)
        if (collationRawBson.containsKey("backwards")) backwards(collationRawBson.getBoolean("backwards").value)
        build()
      }
    }

  private fun allSourceIndexes(source: MongoCollection<*>): List<Document> = source.listIndexes().toList()

  private fun filteredSourceIndexes(source: MongoCollection<*>, indexNames: Set<String>): List<Document> {
    val sourceIndexes = allSourceIndexes(source).filter { indexNames.contains(it.getString("name")) }
    if (indexNames.size != sourceIndexes.size) {
      val missingIndexes = (indexNames - sourceIndexes.map { it.getString("name") }.toSet()).joinToString()
      throw IllegalArgumentException("Not all indexes passed as 'indexNames' param are available in source MongoDB collection. Missing indexes: [$missingIndexes]")
    }
    return sourceIndexes
  }
}
