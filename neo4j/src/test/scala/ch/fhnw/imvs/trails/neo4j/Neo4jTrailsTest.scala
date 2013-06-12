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
}