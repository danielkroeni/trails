trails schema [![Build Status](https://secure.travis-ci.org/danielkroeni/trails.png?branch=schema)](http://travis-ci.org/danielkroeni/trails)
======

Purely functional graph traversal combinators written in Scala.

trails is applying the idea of [parser combinators](http://en.wikipedia.org/wiki/Parser_combinator) to graph traversals.
The following combinators are supported:
```scala
    t1 ~ t2     // Sequence
    t1 | t2     // Choice
    t.?         // Optionality
    t.*         // Repetition 0..n
    t.+         // Repetition 1..n
```

Find a more comprehensive description in our [paper](https://dl.acm.org/authorize?6839137). [Slides](ScalaDays2013_Presentation.pdf) from the talk are available as well.

## Features

* Purely functional: No mutable state, no surprises.
* Lazy: Compute only as much information as required.
* Cycle aware: Avoid spinning in a circle.
* Labeling: Name path snippets.
* Supports different graph APIs ([blueprints](https://github.com/tinkerpop/blueprints/wiki) and [neo4j](http://www.neo4j.org/) are already included)
* Schema aware: Guided graph traversals.

## Examples
```scala
  object AliceSchema extends Schema {
    object Person extends SchemaNode {
      object Name extends SchemaProperty[String]
      def properties = Seq(Name)
    }

    object Pet extends SchemaNode {
      object Name extends SchemaProperty[String]
      def properties = Seq(Name)
    }

    object Loves extends SchemaEdge {
      type From = Person.type; type To = Person.type
    }

    object Likes extends SchemaEdge {
      type From = Person.type; type To = Person.type
    }

    object Owns extends SchemaEdge {
      type From = Person.type; type To = Pet.type
    }

    def nodes = Seq(Pet, Person)
    def edges = Seq(Loves, Likes, Owns)
  }

  val alicesWorld = /* Create an instance of your favorite graph database implementation */

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
```

## License
trails is licensed under the [MIT License](http://www.opensource.org/licenses/mit-license.php).


## Credits
trails adapted many ideas (especially method names) from [gremlin](https://github.com/tinkerpop/gremlin/wiki).
