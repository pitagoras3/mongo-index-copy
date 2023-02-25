# mongo-index-copy

Kotlin/Java library for copying MongoDB collection indexes programmatically from one collection to another.

## Installation (Gradle)

```kotlin
implementation("io.github.pitagoras3:mongo-index-copy:1.0.0")
```

In case when you already have
[_MongoDB Java Driver_](https://www.mongodb.com/docs/drivers/java/sync/current/#mongodb-java-driver)
on your classpath, you can get _mongo-index-copy_ without transitive
[_MongoDB Java driver_](https://www.mongodb.com/docs/drivers/java/sync/current/#mongodb-java-driver)
dependency by excluding it:

```kotlin
implementation("io.github.pitagoras3:mongo-index-copy:1.0.0") {
  exclude("org.mongodb")
}
```

## How to use it?

You have two options to copy indexes from one MongoDB collection to another with _mongo-index-copy_:

1. You can use `MongoIndexCopy.copyAllIndexes` method to copy **all** indexes from source collection to destination
   collection,
2. You can use `MongoIndexCopy.copyIndexes` method to copy **only specified** (by index name) indexes from source
   collection to destination collection.

## How it works?

_mongo-index-copy_ reads all indexes from source collection using `listIndexes` method from MongoDB Java driver. All
indexes are `Document` objects - here are a few examples:

```
Document{{v=2, key=Document{{x=hashed}}, name=x_hashed}}
Document{{v=2, key=Document{{item=1}}, name=item_1, expireAfterSeconds=100}}
Document{{v=2, key=Document{{item=1}}, name=item_1, partialFilterExpression=Document{{rating=Document{{$gt=5}}}}}}
```

Having list of indexes as `Document` objects, _mongo-index-copy_ maps them to `IndexModel` objects, and creates indexes
on destination collection using `createIndexes` method from MongoDB Java driver.

## How to build it and test it locally?

**Build:**

```
./gradlew build
```

**Run unit tests:**

```
./gradlew test
```

**Run integration tests:**

```
./gradlew integrationTest
```

**Run all tests (unit + integration):**

```
./gradlew check
```

## License

_mongo-index-copy_ is licensed under [Apache License 2.0](./LICENSE).

```
Copyright 2023 Szymon Marcinkiewicz

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
