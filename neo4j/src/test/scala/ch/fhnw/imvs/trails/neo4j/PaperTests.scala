package ch.fhnw.imvs.trails.neo4j

import org.scalatest.{BeforeAndAfterAll, FunSuite}
import ch.fhnw.imvs.trails.neo4j.Neo4jTrails._
import org.neo4j.test.TestGraphDatabaseFactory
import org.neo4j.graphdb.DynamicRelationshipType

/*
test-only ch.fhnw.imvs.trails.neo4j.PaperTests
*/
//class PaperTests extends FunSuite with BeforeAndAfterAll {
//
//
//  val alicesWorld = new TestGraphDatabaseFactory().newImpermanentDatabase()
//  val tx = alicesWorld.beginTx()
//
//  val loves = DynamicRelationshipType.withName("loves")
//  val likes = DynamicRelationshipType.withName("likes")
//  val pet = DynamicRelationshipType.withName("pet")
//
//  val alice = alicesWorld.createNode()
//  val bob = alicesWorld.createNode()
//  val carol = alicesWorld.createNode()
//  val dave = alicesWorld.createNode()
//  val murphy = alicesWorld.createNode(); murphy.setProperty("name", "Murphy")
//  val fluffy = alicesWorld.createNode(); fluffy.setProperty("name", "Fluffy")
//  val ab = alice.createRelationshipTo(bob, loves)
//  val ac = alice.createRelationshipTo(carol, likes)
//  val ba = bob.createRelationshipTo(alice, loves)
//  val bm = bob.createRelationshipTo(murphy, pet)
//  val cb = carol.createRelationshipTo(bob, loves)
//  val cd = carol.createRelationshipTo(dave, likes)
//  val df = dave.createRelationshipTo(fluffy, pet)
//
//  tx.success()
//  tx.finish()
//
//  override def afterAll(configMap: Map[String, Any]) {
//   alicesWorld.shutdown()
//  }
//
//
//  test("Pet names of friends of friends") {
//    val friends = V(carol.getId) ~ (out("loves") | out("likes")).+
//    val petNames = friends ~> out("pet") ^^ get[String]("name")
//    val answer = Traverser.run(petNames,alicesWorld)
//
//    assert(answer.size === 4)
//
//    assert(answer.toSet === Set(
//      (List(carol, cd, dave, df, fluffy), "Fluffy"),
//      (List(carol, cb, bob, bm, murphy), "Murphy"),
//      (List(carol, cb, bob, ba, alice, ac, carol, cd, dave, df, fluffy), "Fluffy"),
//      (List(carol, cb, bob, ba, alice, ab, bob, bm, murphy), "Murphy")
//    ))
//  }
//
//  test("unhappy lovers") {
//    val unhappyLovers = for {
//      beloved <- V.as("lvr") ~ out("loves") ~> out("loves")
//      lover <- label("lvr") if !lover.contains(beloved)
//    } yield lover
//
//    val answer = Traverser.run(unhappyLovers, alicesWorld)
//    assert(answer.size === 1)
//    val (path, value) = answer.head
//    assert(value.size === 1)
//    assert(value.head === carol)
//  }
//
//  test("happy pet owner") {
//    val happyPetOwners = for {
//      petOwner <- V
//      pets  <- sub(out("pet")) if pets.nonEmpty
//      lover <- in("loves")
//    } yield (petOwner, lover)
//
//    val answer = Traverser.run(happyPetOwners, alicesWorld)
//    assert(answer.size === 2)
//    assert(answer.contains((List(bob, ab, alice), (bob,alice))))
//    assert(answer.contains((List(bob, cb, carol), (bob,carol))))
//  }
//}
