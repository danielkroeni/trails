trails WIP [![Build Status](https://secure.travis-ci.org/danielkroeni/trails.png?branch=WIP)](http://travis-ci.org/danielkroeni/trails)
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

## Features

* Purely functional: No mutable state, no surprises.
* Lazy: Compute only as much information as required.
* Cycle aware: Avoid spinning in a circle.
* Labeling: Name path snippets.
* Supports different graph APIs ([blueprints](https://github.com/tinkerpop/blueprints/wiki) and [neo4j](http://www.neo4j.org/) are already included)

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

Experimental in-memory sql support:
```scala
  test("sql table properties") {
    val graph = new TinkerGraph()
    val v0 = graph.addVertex("v0")
    v0.setProperty("name", "Name0")

    val v1 = graph.addVertex("v1")
    v1.setProperty("name", "Name1")

    val v2 = graph.addVertex("v2")
    v2.setProperty("name", "Name2")

    val v3 = graph.addVertex("v3")
    v3.setProperty("name", "Name3")

    val v4 = graph.addVertex("v4")
    v4.setProperty("name", "Name4")

    graph.addEdge("e0", v0, v1, "e").setProperty("weight", 0)
    graph.addEdge("e1", v0, v2, "e").setProperty("weight", 1)
    graph.addEdge("e2", v1, v3, "e").setProperty("weight", 2)
    graph.addEdge("e3", v2, v3, "e").setProperty("weight", 3)

    val sqlQuery = from (
      (V("v0") ~ outE("e").^[Int]("weight") ~ inV().^[String]("name") ~ out("e")).asTable("t1"),
      (V("v1") ~ (inE("e") | outE("e")).^[Int]("weight")).asTable("t2")
    ).extract (
      """
      select lower(t1.name) as lowerName, t2.weight
        from t1, t2
       where t1.weight <> t2.weight
       order by t1.weight asc
      """
    ) ( printResultSet )

    sqlQuery(graph) // execute at the end
  }
```

Output:
```
LOWERNAME | WEIGHT
name1 | 2
name2 | 0
name2 | 2
```


Experimental scala collection table support:
```scala
  test("Collection table") {
    val graph = new TinkerGraph()
    val v0 = graph.addVertex("v0")
    val v1 = graph.addVertex("v1")
    val v2 = graph.addVertex("v2")
    val v3 = graph.addVertex("v3")
    val v4 = graph.addVertex("v4")

    graph.addEdge("e0", v0, v1, "e")
    graph.addEdge("e1", v1, v3, "e")
    graph.addEdge("e2", v0, v2, "e")
    graph.addEdge("e3", v2, v3, "e")
    graph.addEdge("e4", v2, v4, "e")

    val expr0 = V("v0") ~ out("e").as("out(e)").+ ~ outE("e").as("outE(e)").?
    val traces = expr0.run(graph)

    val table = ScalaTable(traces)
    println(table.pretty)
  }
```
Output:
```
|----------------------------------------------------------------|---------------------------|
|out(e)                                                          |outE(e)                    |
|----------------------------------------------------------------|---------------------------|
|List(List(v[v2], e[e2][v0-e->v2]))                              |List()                     |
|List(List(v[v2], e[e2][v0-e->v2]))                              |List(List(e[e3][v2-e->v3]))|
|List(List(v[v2], e[e2][v0-e->v2]))                              |List(List(e[e4][v2-e->v4]))|
|List(List(v[v3], e[e3][v2-e->v3]), List(v[v2], e[e2][v0-e->v2]))|List()                     |
|List(List(v[v4], e[e4][v2-e->v4]), List(v[v2], e[e2][v0-e->v2]))|List()                     |
|List(List(v[v1], e[e0][v0-e->v1]))                              |List()                     |
|List(List(v[v1], e[e0][v0-e->v1]))                              |List(List(e[e1][v1-e->v3]))|
|List(List(v[v3], e[e1][v1-e->v3]), List(v[v1], e[e0][v0-e->v1]))|List()                     |
|----------------------------------------------------------------|---------------------------|
```


## License
trails is licensed under the [MIT License](http://www.opensource.org/licenses/mit-license.php).


## Credits
trails adapted many ideas (especially method names) from [gremlin](https://github.com/tinkerpop/gremlin/wiki).
