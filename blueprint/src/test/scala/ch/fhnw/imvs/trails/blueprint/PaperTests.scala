package ch.fhnw.imvs.trails.blueprint

import org.scalatest.FunSuite
import com.tinkerpop.blueprints.impls.tg.TinkerGraph
import BlueprintTrails._

/*
test-only ch.fhnw.imvs.trails.blueprint.PaperTests
*/
class PaperTests extends FunSuite {

  val alicesWorld = new TinkerGraph()

  val loves = "loves"
  val likes = "likes"
  val pet = "pet"

  val alice = alicesWorld.addVertex("Alice")
  val bob = alicesWorld.addVertex("Bob")
  val carol = alicesWorld.addVertex("Carol")
  val dave = alicesWorld.addVertex("Dave")
  val murphy = alicesWorld.addVertex("Murphy"); murphy.setProperty("name", "Murphy")
  val fluffy = alicesWorld.addVertex("Fluffy"); fluffy.setProperty("name", "Fluffy")
  val ab = alice.addEdge(loves, bob)
  val ac = alice.addEdge(likes, carol)
  val ba = bob.addEdge(loves, alice)
  val bm = bob.addEdge(pet, murphy)
  val cb = carol.addEdge(loves, bob)
  val cd = carol.addEdge(likes, dave)
  val df = dave.addEdge(pet, fluffy)


  test("Pet names of friends of friends") {
    val friends = V("Carol") ~ (out("loves") | out("likes")).+
    val petNames = friends ~> out("pet") ^^ get[String]("name")
    val answer = Traverser.run(petNames,alicesWorld)

    assert(answer.size === 4)

    assert(answer.toSet === Set(
      (List(carol, cd, dave, df, fluffy), "Fluffy"),
      (List(carol, cb, bob, bm, murphy), "Murphy"),
      (List(carol, cb, bob, ba, alice, ac, carol, cd, dave, df, fluffy), "Fluffy"),
      (List(carol, cb, bob, ba, alice, ab, bob, bm, murphy), "Murphy")
    ))
  }

  test("unhappy lovers") {
    val unhappyLovers = for {
      l <- V.as("lover") ~ out("loves") ~> out("loves")
      uhl <- label("lover") ^^ (_.head) if !uhl.contains(l)
    } yield uhl

    val answer = Traverser.run(unhappyLovers, alicesWorld)
    assert(answer.size === 1)
    val (path, value) = answer.head
    assert(value.size === 1)
    assert(value.head === carol)
  }
}
