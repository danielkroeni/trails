package ch.fhnw.imvs.trails.neo4j

import org.neo4j.graphdb.Direction._
import org.neo4j.tooling.GlobalGraphOperations
import scala.collection.JavaConversions._
import ch.fhnw.imvs.trails._
import org.neo4j.{graphdb => neo4j}

object Neo4jTrails extends TrailsPrimitives {
  import Tr._

  type Env = neo4j.GraphDatabaseService
  type Elem = neo4j.PropertyContainer
  type Edge = neo4j.Relationship
  type Node = neo4j.Node
  type Id = Long

  val Neo4jTypeTag: String = "'TYPE"

  def V[M <: SchemaElement,N <: SchemaNode](sn: N): Tr[Env,State[M],State[N],Node] =
    for {
      env <- getEnv[Env, State[M]]
      allNodes = GlobalGraphOperations.at(env).getAllNodes
      nodes = allNodes.filter(v => v.hasProperty(Neo4jTypeTag) && v.getProperty(Neo4jTypeTag) == sn.name)
      n <- streamToTraverser(nodes.toStream)
      _ <- extendPath(n)
    } yield n

  def outE[E <: SchemaEdge](se: E): Tr[Env,State[E#From],State[E],Edge] =
    for {
      State((head: Node) :: rest, _, _) <- getState[Env,State[E#From]]
      edges = head.getRelationships(neo4j.DynamicRelationshipType.withName(se.name), OUTGOING)
      e <- streamToTraverser(edges.toStream)
      _ <- extendPath(e)
    } yield e

  def inE[E <: SchemaEdge](se: E): Tr[Env,State[E#To],State[E],Edge] =
    for {
      State((head: Node) :: rest, _, _) <- getState[Env,State[E#To]]
      edges = head.getRelationships(neo4j.DynamicRelationshipType.withName(se.name), INCOMING)
      e <- streamToTraverser(edges.toStream)
      _ <- extendPath(e)
    } yield e

  def outV[E <: SchemaEdge]: Tr[Env,State[E],State[E#From],Node] =
    for {
      State((head: Edge) :: rest, _, _) <- getState[Env,State[E]]
      n = head.getStartNode
      _ <- extendPath(n)
    } yield n

  def inV[E <: SchemaEdge]: Tr[Env,State[E],State[E#To],Node] =
    for {
      State((head: Edge) :: rest, _, _) <- getState[Env,State[E]]
      n = head.getEndNode
      _ <- extendPath(n)
    } yield n


  def get[T, E <: SchemaElement](p: E#SchemaProperty[T]): Tr[Env,State[E],State[E],T] =
    Tr(env => i => i match {
      case State(path,_,_) => Stream((i,path.head.getProperty(p.name).asInstanceOf[T]))
    })
}
