/**
  * Created by jack on 16-6-22.
  */

package com.guanghe

import java.util.concurrent.TimeUnit

import org.json4s.native.JsonMethods._
import org.mongodb.scala._
import org.mongodb.scala.bson.conversions.Bson

import scala.concurrent.Await
import scala.concurrent.duration.Duration

class MongoDB(connStr: String, database: String = "") {
  val client: MongoClient = MongoClient(s"mongodb://$connStr")

//  val client: MongoClient = MongoClient(s"mongodb://$host:$port")
  val db = client.getDatabase(database)
  var collection: MongoCollection[Document] = null

  def selectCollection(col: String): MongoCollection[Document] = {
    collection = db.getCollection(col)
    collection
  }

  def batchInsert(json: String) = {
    import Helpers._

    val ch = parse(json).children
    val docs: IndexedSeq[Document] = ch.map(d => Document(compact(render(d)))).toIndexedSeq
    collection.insertMany(docs).results()
  }

  def findPrint(filter: Bson) = {
    import Helpers._

    collection.find(filter).printResults()
  }
}

object MongoDB {
  var instance: MongoDB = null
  def apply(connStr: String, database: String) = {
    if (instance == null) {
      instance = new MongoDB(connStr, database)
    }
    instance
  }
}

object Helpers {
  implicit class DocumentObservable[C](val observable: Observable[Document]) extends ImplicitObservable[Document] {
    override val converter: (Document) => String = (doc) => doc.toJson()
  }

  implicit  class GenericObservable[C](val observable: Observable[C]) extends ImplicitObservable[C] {
    override val converter: (C) => String = (doc) => doc.toString()
  }

  trait ImplicitObservable[C] {
    val observable: Observable[C]
    val converter: (C) => String

    def results(): Seq[C] = {
      try {
        Await.result(observable.toFuture(), Duration(10, TimeUnit.SECONDS))
      }
      catch {
        case e: Exception => {
          e.printStackTrace()
          throw e
        }
      }
    }
    def headResult() = {
      try {
        Await.result(observable.head(), Duration(10, TimeUnit.SECONDS))
      }
      catch {
        case e: Exception => {
          e.printStackTrace()
          throw e
        }
      }
    }

    def printResults() = results().foreach(res => println(converter(res)))
  }
}
