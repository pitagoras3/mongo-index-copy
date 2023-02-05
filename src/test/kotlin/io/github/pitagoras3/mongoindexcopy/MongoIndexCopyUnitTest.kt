package io.github.pitagoras3.mongoindexcopy

import com.mongodb.MongoException
import com.mongodb.client.ListIndexesIterable
import com.mongodb.client.MongoCollection
import com.mongodb.client.MongoCursor
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeTypeOf
import io.mockk.every
import io.mockk.mockk
import org.bson.Document

internal class MongoIndexCopyUnitTest : StringSpec({
  "given no indexes to copy when copying all indexes then should copy no indexes" {
    val sourceCollection = mockk<MongoCollection<*>>(relaxed = true)
    val destinationCollection = mockk<MongoCollection<*>>(relaxed = true)

    val result = MongoIndexCopy.copyAllIndexes(sourceCollection, destinationCollection)
    result.shouldBe(emptyList())
  }

  "given exception on sourceCollection.listIndexes when executing copyAllIndexes then should result with failure" {
    val sourceCollection = mockk<MongoCollection<*>>(relaxed = true)
    val destinationCollection = mockk<MongoCollection<*>>(relaxed = true)

    every { sourceCollection.listIndexes() } throws MongoException("Mongo exception")

    val result = shouldThrow<MongoIndexCopyException> {
      MongoIndexCopy.copyAllIndexes(sourceCollection, destinationCollection)
    }
    result.cause.shouldBeTypeOf<MongoException>()
  }

  "given non-existing index name when executing copyIndexes then should result with failure" {
    val sourceCollection = mockk<MongoCollection<*>>()
    val destinationCollection = mockk<MongoCollection<*>>()
    val listIndexesIterable = mockk<ListIndexesIterable<Document>>()
    val mongoCursor = mockk<MongoCursor<Document>>()

    every { sourceCollection.listIndexes() } returns listIndexesIterable
    every { listIndexesIterable.iterator() } returns mongoCursor
    every { mongoCursor.hasNext() } returnsMany listOf(true, false) // Return only one document from mongoCursor
    every { mongoCursor.next() } returns Document()

    val result = shouldThrow<MongoIndexCopyException> {
      MongoIndexCopy.copyIndexes(setOf("non_existing_index"), sourceCollection, destinationCollection)
    }

    result.cause.shouldBeTypeOf<IllegalArgumentException>()
  }
})
