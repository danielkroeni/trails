package ch.fhnw.imvs.trails.neo4j

import org.neo4j.graphdb.Direction._
import org.neo4j.tooling.GlobalGraphOperations
import ch.fhnw.imvs.trails.{TrailsPrimitives, Trails}
import scala.collection.JavaConversions.iterableAsScalaIterable
import org.neo4j.{graphdb => neo4j}

object Neo4jTrails extends TrailsPrimitives with Trails {
  type Env = neo4j.GraphDatabaseService
  type Elem = neo4j.PropertyContainer
  type Edge = neo4j.Relationship
  type Node = neo4j.Node
  type Id = Long

  def V: Tr[Env,State[Nothing],State[Node],Node] =
    for {
      env <- getEnv[Env, State[Nothing]]
      nodes = GlobalGraphOperations.at(env).getAllNodes
      n <- streamToTraverser(nodes.toStream)
      _ <- extendPath(n)
    } yield n

  def V(id: Id): Tr[Env,State[Nothing],State[Node],Node] =
    for {
      env  <- getEnv[Env, State[Nothing]]
      n = env.getNodeById(id)
      _    <- extendPath(n)
    } yield n

  def E: Tr[Env,State[Nothing],State[Edge],Edge] =
    for {
      env <- getEnv[Env, State[Nothing]]
      edges = GlobalGraphOperations.at(env).getAllRelationships
      e <- streamToTraverser(edges.toStream)
      _ <- extendPath(e)
    } yield e

  def E(id: Id): Tr[Env,State[Nothing],State[Edge],Edge] =
    for {
      env  <- getEnv[Env, State[Nothing]]
      e = env.getRelationshipById(id)
      _    <- extendPath(e)
    } yield e

  def outE(edgeName: String): Tr[Env,State[Node],State[Edge],Edge] =
    ontoE(edgeName, OUTGOING)

  def inE(edgeName: String): Tr[Env,State[Node],State[Edge],Edge] =
    ontoE(edgeName, INCOMING)

  private def ontoE(edgeName: String, dir: neo4j.Direction): Tr[Env,State[Node],State[Edge],Edge] =
    for {
      State((head: Node) :: rest) <- getState[Env,State[Node]]
      edges = head.getRelationships(neo4j.DynamicRelationshipType.withName(edgeName), dir)
      e <- streamToTraverser(edges.toStream)
      _ <- extendPath(e)
    } yield e

  def outV: Tr[Env,State[Edge],State[Node],Node] =
    ontoV(OUTGOING)

  def inV: Tr[Env,State[Edge],State[Node],Node] =
    ontoV(INCOMING)

  private def ontoV(dir: neo4j.Direction): Tr[Env,State[Edge],State[Node],Node] =
    for {
      State((head: Edge) :: rest) <- getState[Env,State[Edge]]
      n = if(dir == OUTGOING) head.getStartNode else head.getEndNode
      _ <- extendPath(n)
    } yield n

  def get[A](name: String)(e: Elem): A = e.getProperty(name).asInstanceOf[A]
}
