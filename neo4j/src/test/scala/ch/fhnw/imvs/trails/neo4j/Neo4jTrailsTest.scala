package ch.fhnw.imvs.trails.neo4j

import org.scalatest.FunSuite
import ch.fhnw.imvs.trails.neo4j.Neo4jTrails._
import org.neo4j.test.TestGraphDatabaseFactory
import org.neo4j.graphdb.{GraphDatabaseService, DynamicRelationshipType}


class Neo4jTrailsTest extends FunSuite {
  import DynamicRelationshipType.withName

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

  test("Simple pattern") {
    withNeo4jGraph { graph =>
      val v0 = graph.createNode()
      val v1 = graph.createNode()
      val v2 = graph.createNode()

      val e0 = v0.createRelationshipTo(v1, withName("e"))
      val e1 = v1.createRelationshipTo(v2, withName("e"))

      val f0 = v1.createRelationshipTo(v1, withName("f"))
      val g0 = v1.createRelationshipTo(v1, withName("g"))


      val expr0 = V ~ out("e") ~ out("f") ~ out("g") ~ out("e")

      val paths = Traverser.run(expr0, graph).take(1).map(_._1)

      assert(paths.size === 1)
      assert(paths.head === List(v0, e0, v1, f0, v1, g0, v1, e1, v2))
    }
  }

  test("Cycles") {
    withNeo4jGraph { graph =>
      val v0 = graph.createNode()

      val eType = withName("e")
      val fType = withName("f")

      val e0 = v0.createRelationshipTo(v0, eType)
      val f0 = v0.createRelationshipTo(v0, fType)
      val f1 = v0.createRelationshipTo(v0, fType)

      val expr0 = V(v0.getId) ~ (out("e").+ ~ out("f")).+

      val paths = Traverser.run(expr0, graph).take(4).map(_._1)

      assert(paths.size === 4)

      assert(paths.contains(List(v0, e0, v0, f1, v0)))
      assert(paths.contains(List(v0, e0, v0, f1, v0, e0, v0, f0, v0)))
      assert(paths.contains(List(v0, e0, v0, f0, v0)))
      assert(paths.contains(List(v0, e0, v0, f0, v0, e0, v0, f1, v0)))
    }
  }
}