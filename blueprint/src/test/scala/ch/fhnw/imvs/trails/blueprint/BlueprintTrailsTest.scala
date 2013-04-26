package ch.fhnw.imvs.trails.blueprint

import ch.fhnw.imvs.trails.blueprint.BlueprintTrails._
import com.tinkerpop.blueprints.impls.tg.TinkerGraph
import org.scalatest.FunSuite
import java.sql.ResultSet

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

  test("subquery") {
    val graph = new TinkerGraph()
    val v0 = graph.addVertex("v0")
    v0.setProperty("a", 2)
    v0.setProperty("b", 2)
    val v1 = graph.addVertex("v1")
    v1.setProperty("a", 3)
    v1.setProperty("b", 2)

    def hasT[T, I <: Elem, O <: Elem, S <: Elem](name: String, t: Tr[Env, State[I], State[O], T]): Tr[Env, State[S], State[S], T] =
      for{
        v <- sub[S,I,O,T](t)
        a <- has(name, v)
      } yield a

    val expr = for {
      _ <- V
      n <- sub(V("v0") ^^ get[Int]("a"))
      _ <- has("b", n)
    } yield n

    val expr1 = V ~> hasT("b", V("v0") ^^ get[Int]("a"))

    val res0 = Traverser.run(expr, graph)
    val res1 = Traverser.run(expr1, graph)

    assert(res0.size === 2)
    assert(res0.toSet === res1.toSet)

    assert(res0.toSet === Set(
      (List(v0), 2),
      (List(v1), 2)
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

  test("matches") {
    val graph = new TinkerGraph()
    val v0 = graph.addVertex("v0")
    val v1 = graph.addVertex("v1")
    val v2 = graph.addVertex("v2")

    val e0 = graph.addEdge("e0", v0, v1, "e")
    val e1 = graph.addEdge("e1", v1, v2, "e")

    val f0 = graph.addEdge("f0", v1, v1, "f")
    val g0 = graph.addEdge("g0", v1, v1, "g")


    val expr0 = V ~ matches(out("e") ~ out("f") ~ out("g") ~ out("e"))
    val traces = Traverser.run(expr0, graph)
    val paths = traces.map(t => t._1)

    assert(paths.size === 1)
    assert(paths.head === List(v0))
  }

  test("names") {
    val graph = new TinkerGraph()
    val v0 = graph.addVertex("v0")
    val v1 = graph.addVertex("v1")
    val v2 = graph.addVertex("v2")
    val v3 = graph.addVertex("v3")

    val e0 = graph.addEdge("e0", v0, v1, "e")
    val e1 = graph.addEdge("e1", v1, v3, "e")
    val e2 = graph.addEdge("e2", v0, v2, "e")
    val e3 = graph.addEdge("e3", v2, v3, "e")

    val f0 = graph.addEdge("f0", v1, v1, "f")

    val expr0 = V("v0") ~> outE("e").as("es") ~ inV.as("vs") <~ out("e")
    val result = Traverser.run(expr0, graph)

    assert(result.size === 2)
    assert(result.toSet === Set(
      (List(v0,e0,v1,e1,v3),new ~(e0,v1)),
      (List(v0,e2,v2,e3,v3),new ~(e2,v2))
    ))
  }

  test("table") {
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

    val expr0 = V("v0") ~ out("e").as("out(e)").+ ~ outE("e").as("outE(e)")
    val result = Traverser.runAll(expr0, graph).map(_._1)

    val table = ScalaTable(result)
    println(table.pretty)
  }

//  test("sql table") {
//    val graph = new TinkerGraph()
//    val v0 = graph.addVertex("v0")
//    val v1 = graph.addVertex("v1")
//    val v2 = graph.addVertex("v2")
//    val v3 = graph.addVertex("v3")
//    val v4 = graph.addVertex("v4")
//
//    graph.addEdge("e0", v0, v1, "e")
//    graph.addEdge("e1", v1, v3, "e")
//    graph.addEdge("e2", v0, v2, "e")
//    graph.addEdge("e3", v2, v3, "e")
//    graph.addEdge("e4", v2, v4, "e")
//
//    val dbAction = from {
//      V("v0") ~ out("e").as("col1").+ ~ outE("e").as("col2").asTable("yeah")
//    }.extract[Unit] (" select col1, col2 from yeah order by col2 ") { (rs: ResultSet) => printResultSet(rs) }
//
//    dbAction(graph)
//  }

//  test("sql table properties") {
//    val graph = new TinkerGraph()
//    val v0 = graph.addVertex("v0")
//    v0.setProperty("name", "Name0")
//
//    val v1 = graph.addVertex("v1")
//    v1.setProperty("name", "Name1")
//
//    val v2 = graph.addVertex("v2")
//    v2.setProperty("name", "Name2")
//
//    val v3 = graph.addVertex("v3")
//    v3.setProperty("name", "Name3")
//
//    val v4 = graph.addVertex("v4")
//    v4.setProperty("name", "Name4")
//
//    graph.addEdge("e0", v0, v1, "e").setProperty("weight", 0)
//    graph.addEdge("e1", v0, v2, "e").setProperty("weight", 1)
//    graph.addEdge("e2", v1, v3, "e").setProperty("weight", 2)
//    graph.addEdge("e3", v2, v3, "e").setProperty("weight", 3)
//
//    val sqlQuery = from (
//      (V("v0") ~ outE("e").^[Int]("weight") ~ inV.^[String]("name") ~ out("e")).asTable("t1"),
//      (V("v1") ~ (inE("e") | outE("e")).^[Int]("weight")).asTable("t2")
//    ).extract (
//      """
//      select lower(t1.name) as lowerName, t2.weight
//        from t1, t2
//       where t1.weight <> t2.weight
//       order by t1.weight asc
//      """
//    ) ( printResultSet )
//
//    sqlQuery(graph) // execute at the end
//  }

    /*
      select lower(t1.name) as lowerName, t1.weight, t2.ageA
        from t1, t2
       where t1.age != t2.age
       order by t1.age
 println("UNFOLD TABLE")

    Vector(// table
      Vector(List(a,b), List(c,d)) //row
    )
    =>

    Vector(
      Vector(List(a,b), c)
      Vector(List(a,b), d)
    )

    =>

    Vector(
      Vector(a,c)
      Vector(b,c)

      Vector(a,d)
      Vector(b,d)
    )
    */
}