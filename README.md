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
  test("Example") {
    val graph = new TinkerGraph()
    val v0 = graph.addVertex("v0")
    val v1 = graph.addVertex("v1")
    val v2 = graph.addVertex("v2")

    val e0 = graph.addEdge("e0", v0, v1, "e")
    val e1 = graph.addEdge("e1", v0, v2, "f")

    val t = V("v0") ~ (outE("e")|outE("f")) ~ inV
    val res = Traverser.run(t, graph)

    assert(res.size === 2)
    val (List(`v0`, `e0`, `v1`), `v0` ~ `e0` ~ `v1`) = res.head
    val (List(`v0`, `e1`, `v2`), `v0` ~ `e1` ~ `v2`) = res.tail.head
  }

  test("Cycles") {
    val graph = new TinkerGraph()
    val v0 = graph.addVertex("v0")

    val e0 = graph.addEdge("e0", v0, v0, "e")
    val f0 = graph.addEdge("f0", v0, v0, "f")
    val f1 = graph.addEdge("f1", v0, v0, "f")

    val expr0 = V("v0") ~ (out("e").+ ~ out("f")).+

    val paths = Traverser.run(expr0, graph).map(_._1)

    assert(paths.size === 4)

    assert(paths.toSet === Set(
      List(v0, e0, v0, f1, v0),
      List(v0, e0, v0, f1, v0, e0, v0, f0, v0),
      List(v0, e0, v0, f0, v0),
      List(v0, e0, v0, f0, v0, e0, v0, f1, v0)
    ))
  }
```

## License
trails is licensed under the [MIT License](http://www.opensource.org/licenses/mit-license.php).


## Credits
trails adapted many ideas (especially method names) from [gremlin](https://github.com/tinkerpop/gremlin/wiki).
