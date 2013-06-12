package ch.fhnw.imvs.trails.blueprint

import org.scalatest.FunSuite
import com.tinkerpop.blueprints.impls.tg.TinkerGraph
import BlueprintTrails._
import ch.fhnw.imvs.trails.{Tr, SchemaEdge, SchemaNode, Schema}

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



  val alice = alicesWorld.addVertex("Alice"); alice.setProperty(Person.Name.name, "Alice"); alice.setProperty(BlueprintTypeTag, Person.name)
  val bob = alicesWorld.addVertex("Bob"); bob.setProperty(Person.Name.name, "Bob"); bob.setProperty(BlueprintTypeTag, Person.name)
  val carol = alicesWorld.addVertex("Carol"); carol.setProperty(Person.Name.name, "Carol"); carol.setProperty(BlueprintTypeTag, Person.name)
  val dave = alicesWorld.addVertex("Dave"); dave.setProperty(Person.Name.name, "Dave"); dave.setProperty(BlueprintTypeTag, Person.name)
  val murphy = alicesWorld.addVertex("Murphy"); murphy.setProperty(Pet.Name.name, "Murphy"); murphy.setProperty(BlueprintTypeTag, Pet.name)
  val fluffy = alicesWorld.addVertex("Fluffy"); fluffy.setProperty(Pet.Name.name, "Fluffy"); fluffy.setProperty(BlueprintTypeTag, Pet.name)
  val ab = alice.addEdge(Loves.name, bob)
  val ac = alice.addEdge(Likes.name, carol)
  val ba = bob.addEdge(Loves.name, alice)
  val bm = bob.addEdge(Owns.name, murphy)
  val cb = carol.addEdge(Loves.name, bob)
  val cd = carol.addEdge(Likes.name, dave)
  val df = dave.addEdge(Owns.name, fluffy)


  test("Pet names of friends of friends") {

    val friends: Tr[Env,State[Nothing],State[Person.type],Stream[Person.type]] = V(Person) ~ get(Person.Name).filter(_ == "Alice") ~> (out(Loves) | out(Likes)).+
    val petNames = friends ~> out(Owns) ~ get(Pet.Name)
    val answer = friends(alicesWorld)(State(List(), Set(), Map()))



    println(answer.map(_._2))


    assert(answer.size === 4)

    assert(answer.toSet === Set(
      (List(carol, cd, dave, df, fluffy), "Fluffy"),
      (List(carol, cb, bob, bm, murphy), "Murphy"),
      (List(carol, cb, bob, ba, alice, ac, carol, cd, dave, df, fluffy), "Fluffy"),
      (List(carol, cb, bob, ba, alice, ab, bob, bm, murphy), "Murphy")
    ))
  }

//  test("unhappy lovers") {
//    val unhappyLovers = for {
//      beloved <- V.as("lvr") ~ out("loves") ~> out("loves")
//      lover <- label("lvr") if !lover.contains(beloved)
//    } yield lover
//
//    val answer = Traverser.run(unhappyLovers , alicesWorld)
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
}
