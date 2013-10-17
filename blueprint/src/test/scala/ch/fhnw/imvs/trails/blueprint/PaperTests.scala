package ch.fhnw.imvs.trails.blueprint

import org.scalatest.FunSuite
import com.tinkerpop.blueprints.impls.tg.TinkerGraph
import BlueprintTrails._
import ch.fhnw.imvs.trails.{SchemaEdge, SchemaNode, Schema}
import ch.fhnw.imvs.trails.Tr.sub
import BlueprintTestUtil._

/*
test-only ch.fhnw.imvs.trails.neo4j.PaperTests
*/
class PaperTests extends FunSuite {

  object AliceSchema extends Schema("Structural description of Alice's World") {
    object Person extends SchemaNode {
      object Name extends SchemaProperty[String]
      def properties = Seq(Name)
      def idProperties = Seq(Name)
    }

    object Pet extends SchemaNode {
      object Name extends SchemaProperty[String]
      def properties = Seq(Name)
      def idProperties = Seq(Name)
    }

    object Loves extends SchemaEdge {
      type From = Person.type; type To = Person.type
      def from = Person; def to = Person
      def properties = Seq()
    }

    object Likes extends SchemaEdge {
      type From = Person.type; type To = Person.type
      def from = Person; def to = Person
      def properties = Seq()
    }

    object Owns extends SchemaEdge {
      type From = Person.type; type To = Pet.type
      def from = Person; def to = Pet
      def properties = Seq()
    }

    def nodes = Seq(Pet, Person)
    def edges = Seq(Loves, Likes, Owns)

  }

  val alicesWorld = new TinkerGraph()

  import AliceSchema._

  val alice = createNode(alicesWorld, Person, Person.Name, "Alice")
  val bob = createNode(alicesWorld, Person, Person.Name, "Bob")
  val carol = createNode(alicesWorld, Person, Person.Name, "Carol")
  val dave = createNode(alicesWorld, Person, Person.Name, "Dave")
  val murphy = createNode(alicesWorld, Person, Person.Name, "Murphy")
  val fluffy = createNode(alicesWorld, Person, Person.Name, "Fluffy")
  val ab = createEdge(Loves, alice, bob)
  val ac = createEdge(Likes, alice, carol)
  val ba = createEdge(Loves, bob, alice)
  val bm = createEdge(Owns, bob, murphy)
  val cb = createEdge(Loves, carol, bob)
  val cd = createEdge(Likes, carol, dave)
  val df = createEdge(Owns, dave, fluffy)


  test("Pet names of friends of friends") {
    val friends = V(Person) ~ get(Person.Name).filter(_ == "Carol") ~> (out(Loves) | out(Likes)).+
    val petNames = friends ~> out(Owns) ~> get(Pet.Name)
    val answer = Traverser.run(petNames, alicesWorld)

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
      beloved <- V(Person).as("lvr") ~ out(Loves) ~> out(Loves)
      lover <- label("lvr") if !lover.contains(beloved)
    } yield lover

    val answer = Traverser.run(unhappyLovers , alicesWorld)

    assert(answer.size === 1)
    val (path, value) = answer.head
    assert(value.size === 1)
    assert(value.head === carol)
  }

  test("happy pet owner") {
    val happyPetOwners = for {
      petOwner <- V(Person)
      pets  <- sub(out(Owns)) if pets.nonEmpty
      lover <- in(Loves)
    } yield (petOwner, lover)

    val answer = Traverser.run(happyPetOwners, alicesWorld)
    assert(answer.size === 2)
    assert(answer.contains((List(bob, ab, alice), (bob,alice))))
    assert(answer.contains((List(bob, cb, carol), (bob,carol))))
  }
}
