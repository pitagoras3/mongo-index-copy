package io.github.pitagoras3.mongoindexcopy

import com.mongodb.client.MongoClients
import com.mongodb.client.MongoCollection
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.Collation
import com.mongodb.client.model.CollationAlternate
import com.mongodb.client.model.CollationCaseFirst
import com.mongodb.client.model.CollationMaxVariable
import com.mongodb.client.model.CollationStrength
import com.mongodb.client.model.IndexOptions
import com.mongodb.client.model.Indexes
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.equality.shouldBeEqualToComparingFields
import org.bson.Document
import org.bson.conversions.Bson
import org.junit.jupiter.api.assertThrows
import java.util.UUID
import java.util.concurrent.TimeUnit

private const val TEST_DB_NAME = "testDb"

internal class MongoIndexCopyIntegrationTest : BaseIntegrationTest({

  val mongoDb36 = MongoClients.create(mongoDb36Container.connectionString).getDatabase(TEST_DB_NAME)
  val mongoDb60 = MongoClients.create(mongoDb60Container.connectionString).getDatabase(TEST_DB_NAME)
  val simpleIndexKey = Document().append("item", 1)

  "given multiple indexes in source collection when using MongoIndexCopy.copyAllIndexes then should copy all indexes" {
    val (sourceCollection, destinationCollection) = createSourceAndDestinationTestCollections(mongoDb60, mongoDb60)

    sourceCollection.createIndex(simpleIndexKey, IndexOptions().name("item"))
    // "_id_" index is created by default when new collection is created - https://www.mongodb.com/docs/manual/indexes/#default-_id-index
    sourceCollection.listIndexes().shouldHaveSize(2)

    val result = MongoIndexCopy.copyAllIndexes(sourceCollection, destinationCollection)

    destinationCollection.listIndexes().shouldHaveSize(2)
    result shouldContainExactly listOf("_id_", "item")
  }

  "given multiple indexes in source collection when using MongoIndexCopy.copyIndexes then should copy specified indexes" {
    val (sourceCollection, destinationCollection) = createSourceAndDestinationTestCollections(mongoDb60, mongoDb60)

    sourceCollection.createIndex(Document().append("index1", 1), IndexOptions().name("index1"))
    sourceCollection.createIndex(Document().append("index2", 1), IndexOptions().name("index2"))
    sourceCollection.listIndexes().filterNot { isIdIndex(it) }.shouldHaveSize(2)

    val result = MongoIndexCopy.copyIndexes(setOf("index2"), sourceCollection, destinationCollection)

    destinationCollection.listIndexes().filterNot { isIdIndex(it) }.shouldHaveSize(1)
    result shouldContainExactly listOf("index2")
  }

  @Suppress("DEPRECATION")
  "given deprecated index in old mongo when copying indexes from old mongo to a new mongo then should result with failure" {
    val (sourceCollection, destinationCollection) = createSourceAndDestinationTestCollections(mongoDb36, mongoDb60)

    sourceCollection.createIndex(
      Indexes.geoHaystack("contact.location", Indexes.ascending("stars")),
      IndexOptions().bucketSize(1.0)
    )
    sourceCollection.listIndexes().filterNot { isIdIndex(it) }.shouldHaveSize(1)

    assertThrows<MongoIndexCopyException> {
      MongoIndexCopy.copyAllIndexes(sourceCollection, destinationCollection)
    }
  }

  data class TestCaseData(
    val testCaseName: String,
    val indexKeys: Bson,
    val indexOptions: IndexOptions = IndexOptions(),
    val mongoDatabase: MongoDatabase = mongoDb60,
    val shouldCompareNsField: Boolean = true
  )

  @Suppress("DEPRECATION")
  val indexTypesTestCases = listOf(
    TestCaseData("ascending index", Indexes.ascending("x", "u")),
    TestCaseData("descending index", Indexes.descending("x", "y")),
    TestCaseData("geo2dsphere index", Indexes.geo2dsphere("x", "y")),
    TestCaseData("geo2d index", Indexes.geo2d("x")),
    TestCaseData("text index", Indexes.text("title")),
    TestCaseData("default text index", Indexes.text()),
    TestCaseData("hashed index", Indexes.hashed("x")),
    TestCaseData("compound index", Indexes.compoundIndex(Indexes.text("x"), Indexes.text("y"))),
    TestCaseData(
      "geoHaystack index",
      Indexes.geoHaystack("contact.location", Indexes.ascending("stars")),
      IndexOptions().bucketSize(1.0),
      mongoDb36, // MongoDB 5.0 removed the deprecated geoHaystack index - https://www.mongodb.com/docs/manual/core/geohaystack/
      false
    ),
  )

  @Suppress("DEPRECATION")
  val indexOptionsTestCases = listOf(
    TestCaseData("index with background option", simpleIndexKey, IndexOptions().background(false)),
    TestCaseData("index with unique option", simpleIndexKey, IndexOptions().unique(false)),
    TestCaseData("index with name option", simpleIndexKey, IndexOptions().name("test_name")),
    TestCaseData("index with sparse option", simpleIndexKey, IndexOptions().sparse(true)),
    TestCaseData(
      "index with expireAfter option",
      simpleIndexKey,
      IndexOptions().expireAfter(100, TimeUnit.SECONDS)
    ),
    TestCaseData("index with version option", simpleIndexKey, IndexOptions().version(1)),
    TestCaseData(
      "index with weights option",
      Document().append("item", "text").append("item2", "text"),
      IndexOptions().weights(Document().append("item", 99).append("item2", 1)),
    ),
    TestCaseData("index with defaultLanguage option", simpleIndexKey, IndexOptions().defaultLanguage("spanish")),
    TestCaseData("index with languageOverride option", simpleIndexKey, IndexOptions().languageOverride("idioma")),
    TestCaseData("index with textVersion option", simpleIndexKey, IndexOptions().textVersion(1)),
    TestCaseData("index with sphereVersion option", simpleIndexKey, IndexOptions().sphereVersion(2)),
    TestCaseData("index with bits option", simpleIndexKey, IndexOptions().bits(25)),
    TestCaseData("index with min option", simpleIndexKey, IndexOptions().min(-170.0)),
    TestCaseData("index with max option", simpleIndexKey, IndexOptions().max(170.0)),
    TestCaseData(
      "index with storageEngine option", simpleIndexKey, IndexOptions().storageEngine(
        Document().append("wiredTiger", Document().append("configString", "block_compressor=snappy"))
      )
    ),
    TestCaseData(
      "index with partialFilterExpression option",
      simpleIndexKey,
      IndexOptions().partialFilterExpression(Document().append("rating", Document().append("\$gt", 5)))
    ),
    TestCaseData(
      "index with collation option",
      simpleIndexKey,
      IndexOptions().collation(Collation.builder()
        .locale("fr")
        .caseLevel(false)
        .collationCaseFirst(CollationCaseFirst.OFF)
        .collationStrength(CollationStrength.QUATERNARY)
        .numericOrdering(true)
        .collationAlternate(CollationAlternate.SHIFTED)
        .collationMaxVariable(CollationMaxVariable.SPACE)
        .normalization(true)
        .backwards(true)
        .build()
      )
    ),
    TestCaseData(
      "index with wildcardProjection option",
      Document().append("$**", 1),
      IndexOptions().wildcardProjection(Document().append("fieldA", 0).append("fieldB.fieldC", 0))
    ),
    TestCaseData("index with hidden option", simpleIndexKey, IndexOptions().hidden(true)),
    TestCaseData(
      "index with bucketSize option",
      Indexes.geoHaystack("contact.location", Indexes.ascending("stars")),
      IndexOptions().bucketSize(1.0),
      mongoDb36, // MongoDB 5.0 removed the deprecated geoHaystack index - https://www.mongodb.com/docs/manual/core/geohaystack/
      false
    ),
  )

  (indexTypesTestCases + indexOptionsTestCases).forEach { (testCaseName, indexKeys, indexOptions, mongo, shouldCompareNsField) ->
    "given $testCaseName in source collection when using MongoIndexCopy then should copy $testCaseName " {
      val (sourceCollection, destinationCollection) = createSourceAndDestinationTestCollections(mongo, mongo)

      sourceCollection.createIndex(indexKeys, indexOptions)

      sourceCollection.listIndexes().filterNot { isIdIndex(it) }.shouldHaveSize(1)

      MongoIndexCopy.copyAllIndexes(sourceCollection, destinationCollection)

      with(destinationCollection.listIndexes().filterNot { isIdIndex(it) }) {
        this shouldHaveSize (1)
        this.first().apply { if (!shouldCompareNsField) remove("ns") }
          .shouldBeEqualToComparingFields(sourceCollection.listIndexes().filterNot { isIdIndex(it) }.first())
      }
    }
  }
})

private fun createSourceAndDestinationTestCollections(
  sourceMongo: MongoDatabase,
  destinationMongo: MongoDatabase
): Pair<MongoCollection<Document>, MongoCollection<Document>> {
  val sourceCollectionName = UUID.randomUUID().toString()
  val destinationCollectionName = UUID.randomUUID().toString()

  sourceMongo.createCollection(sourceCollectionName)
  destinationMongo.createCollection(destinationCollectionName)

  val sourceCollection = sourceMongo.getCollection(sourceCollectionName)
  val destinationCollection = destinationMongo.getCollection(destinationCollectionName)

  return sourceCollection to destinationCollection
}

private fun isIdIndex(it: Document): Boolean = it.get("key", Document::class.java) == Document().append("_id", 1)
