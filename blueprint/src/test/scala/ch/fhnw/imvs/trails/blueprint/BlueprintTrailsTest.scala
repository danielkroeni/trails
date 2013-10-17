package ch.fhnw.imvs.trails.blueprint

import org.scalatest.FunSuite
import BlueprintTrails._
import ch.fhnw.imvs.trails._
import ch.fhnw.imvs.trails.Tr.success
import com.tinkerpop.blueprints.impls.tg.TinkerGraph
import BlueprintTestUtil._

class BlueprintTrailsTest extends FunSuite {

  object TestSchema extends Schema("TBD") {
    object Node extends SchemaNode {
      object Name extends SchemaProperty[String]
      object Nr extends SchemaProperty[Int]

      def properties = Seq(Name,Nr)
      def idProperties = Seq(Name)
    }

    object E extends SchemaEdge {
      type From = Node.type; type To = Node.type
      def from = Node; def to = Node
      def properties = Seq()
    }

    object F extends SchemaEdge {
      type From = Node.type; type To = Node.type
      def from = Node; def to = Node
      def properties = Seq()
    }

    object G extends SchemaEdge {
      type From = Node.type; type To = Node.type
      def from = Node; def to = Node
      def properties = Seq()
    }

    def nodes = Seq(Node)
    def edges = Seq(E,F,G)
  }

  import TestSchema._


  test("seq") {
    val graph = new TinkerGraph()
    val v0 = createNode(graph, Node, Node.Name, "v0")
    val v1 = createNode(graph, Node, Node.Name, "v1")

    val e0 = graph.addEdge("e0", v0, v1, E.name)

    val t = V(Node) ~ get(Node.Name).filter(_ == "v0") ~ outE(E) ~ inV

    val res = Traverser.run(t, graph)

    assert(res.size === 1)

    val (path, rv0 ~ name ~ re0 ~ rv1) = res.head

    assert(path === List(v0, e0, v1))
    assert(rv0 === v0)
    assert(re0 === e0)
    assert(rv1 === v1)
  }

  test("Example") {
    val graph = new TinkerGraph()
    val v0 = createNode(graph, Node, Node.Name, "v0")
    val v1 = createNode(graph, Node, Node.Name, "v1")
    val v2 = createNode(graph, Node, Node.Name, "v2")

    val e0 = createEdge(E, v0, v1)
    val f0 = createEdge(F, v0, v2)

    val t = V(Node) ~ get(Node.Name).filter(_ == "v0") ~> (out(E)|out(F))
    val res = Traverser.run(t, graph)

    assert(res.size === 2)

    val (List(`v0`, `e0`, `v1`), <|(`v1`)) = res.head
    val (List(`v0`, `f0`, `v2`), |>(`v2`)) = res.tail.head
  }

  test("Simple pattern") {
    val graph = new TinkerGraph()
    val v0 = createNode(graph, Node, Node.Name, "v0")
    val v1 = createNode(graph, Node, Node.Name, "v1")
    val v2 = createNode(graph, Node, Node.Name, "v2")

    val e0 =  createEdge(E, v0, v1)
    val e1 =  createEdge(E, v1, v2)

    val f0 =  createEdge(F, v1, v1)
    val g0 =  createEdge(G, v1, v1)


    val expr0 = V(Node) ~ out(E) ~ out(F) ~ out(G) ~ out(E)

    val paths = Traverser.run(expr0, graph).map(_._1)

    assert(paths.size === 1)
    assert(paths.head === List(v0, e0, v1, f0, v1, g0, v1, e1, v2))
  }

  test("context sensitive"){
    val graph = new TinkerGraph()
    val v0 = createNode(graph, Node, Node.Name, "v0", (Node.Nr, 2))
    val v1 = createNode(graph, Node, Node.Name, "v1")
    val v2 = createNode(graph, Node, Node.Name, "v2")
    val v3 = createNode(graph, Node, Node.Name, "v3")

    val e0 = createEdge(E, v0, v1)
    val e1 =createEdge(E, v1, v2)
    createEdge(E, v2, v3)

    def times[E<:SchemaElement,A](n: Int, tr: Tr[Env,State[E],State[E],A]): Tr[Env,State[E],State[E],Stream[A]] = {
      if(n == 0) success(Stream())
      else for { a <- tr; as <- times(n-1, tr)} yield a #:: as
    }

    val expr = for {
      nr <- V(Node) ~> get(Node.Nr)
      _  <- times(nr, out(E))
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


  test("Cycles") {
    val graph = new TinkerGraph()
    val v0 = createNode(graph, Node, Node.Name, "v0")

    val e0 = createEdge(E, v0, v0)
    val f0 = createEdge(F, v0, v0)
    val f1 = createEdge(F, v0, v0)

    val expr0 = V(Node) ~ get(Node.Name).filter(_ == "v0") ~ (out(E).+ ~ out(F)).+

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