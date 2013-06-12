package ch.fhnw.imvs.trails.blueprint

import org.scalatest.FunSuite
import BlueprintTrails._

import com.tinkerpop.blueprints.impls.tg.TinkerGraph

class BlueprintTrailsTest extends FunSuite {

  test("seq") {
    val graph = new TinkerGraph()
    val v0 = graph.addVertex("v0")
    val v1 = graph.addVertex("v1")

    val e0 = graph.addEdge("e0", v0, v1, "e")

    val t = V("v0") ~ outE("e") ~ inV

    val res = Traverser.run(t, graph)

    assert(res.size === 1)

    val (path, rv0 ~ re0 ~ rv1) = res.head

    assert(path === List(v0, e0, v1))
    assert(rv0 === v0)
    assert(re0 === e0)
    assert(rv1 === v1)
  }

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

  test("Simple pattern") {
    val graph = new TinkerGraph()
    val v0 = graph.addVertex("v0")
    val v1 = graph.addVertex("v1")
    val v2 = graph.addVertex("v2")

    val e0 = graph.addEdge("e0", v0, v1, "e")
    val e1 = graph.addEdge("e1", v1, v2, "e")

    val f0 = graph.addEdge("f0", v1, v1, "f")
    val g0 = graph.addEdge("g0", v1, v1, "g")


    val expr0 = V ~ out("e") ~ out("f") ~ out("g") ~ out("e")

    val paths = Traverser.run(expr0, graph).map(_._1)

    assert(paths.size === 1)
    assert(paths.head === List(v0, e0, v1, f0, v1, g0, v1, e1, v2))
  }

  test("context sensitive"){
    val graph = new TinkerGraph()
    val v0 = graph.addVertex("v0")
    v0.setProperty("nr", 2)
    val v1 = graph.addVertex("v1")
    val v2 = graph.addVertex("v2")
    val v3 = graph.addVertex("v3")

    val e0 = graph.addEdge("e0", v0, v1, "e")
    val e1 = graph.addEdge("e1", v1, v2, "e")
    graph.addEdge("e2", v2, v3, "e")

    def times[E<:Elem,A](n: Int, tr: Tr[Env,State[E],State[E],A]): Tr[Env,State[E],State[E],Stream[A]] = {
      if(n == 0) success(Stream())
      else for { a <- tr; as <- times(n-1, tr)} yield a #:: as
    }

    val expr = for {
      nr <- V ^^ get[Int]("nr")
      _  <- times(nr, out("e"))
    } yield nr

    val result = Traverser.run(expr, graph)
    assert(result.size === 4)
    assert(result.toSet === Set(
      (List(v0, e0, v1, e1, v2), 2),
      (List(v1), 0),
      (List(v2), 0),
      (List(v3), 0)
    ))
  }

  def has[T, E <: Elem](name: String, value: T): Tr[Env, State[E], State[E], T] =
    for {
      State(p,_,_) <- getState[Env, State[E]]
      n = get[T](name)(p.head) if n == value
      _ = setState(n)
    } yield n

  test("property") {
    val graph = new TinkerGraph()
    val v0 = graph.addVertex("v0")
    v0.setProperty("type", "a")
    val v1 = graph.addVertex("v1")
    v1.setProperty("type", "b")
    val v2 = graph.addVertex("v2")
    v2.setProperty("type", "b")
    val v3 = graph.addVertex("v3")
    v3.setProperty("type", "a")

    val expr = V ~> has("type", "a")

    val result = Traverser.run(expr, graph)

    assert(result.size === 2)
    assert(result.toSet === Set(
      (List(v0), "a"),
      (List(v3), "a")
    ))
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
}