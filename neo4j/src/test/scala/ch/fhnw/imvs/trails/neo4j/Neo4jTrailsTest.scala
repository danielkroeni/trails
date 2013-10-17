package ch.fhnw.imvs.trails.neo4j

import org.scalatest.FunSuite
import org.neo4j.test.TestGraphDatabaseFactory
import org.neo4j.graphdb.GraphDatabaseService
import Neo4jTrails._
import ch.fhnw.imvs.trails.TestSchema

class Neo4jTrailsTest extends FunSuite {

  def tx[T](block: => T)(implicit graph: GraphDatabaseService): T = {
    val tx = graph.beginTx()
    try {
      val result = block
      tx.success()
      result
    } catch {
      case e: Exception => tx.failure(); throw e
    } finally {
      tx.finish()
    }
  }

  def withNeo4jGraph[T](block: GraphDatabaseService => T): T = {
    implicit val graph = new TestGraphDatabaseFactory().newImpermanentDatabaseBuilder().newGraphDatabase()
    try {
      tx[T] { block(graph) }
    } finally {
      graph.shutdown()
    }
  }

  import Neo4jTestUtil._
  import TestSchema._

  test("Simple pattern") {
    withNeo4jGraph { graph =>
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
  }

  test("Cycles") {
    withNeo4jGraph { graph =>
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
}