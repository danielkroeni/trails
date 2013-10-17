package ch.fhnw.imvs.trails.blueprint

import scala.collection.JavaConversions._
import ch.fhnw.imvs.trails._
import com.tinkerpop.blueprints

object BlueprintTrails extends TrailsPrimitives {
  import Tr._

  type Env = blueprints.Graph
  type Elem = blueprints.Element
  type Edge = blueprints.Edge
  type Node = blueprints.Vertex
  type Id = Any

  val BlueprintTypeTag = "_TYPE"

  def V[M <: SchemaElement,N <: SchemaNode](sn: N): Tr[Env,State[M],State[N],Node] =
    for {
      env <- getEnv[Env,State[M]]
      v   <- streamToTraverser[M,Node](env.getVertices.filter(v => v.getProperty[String](BlueprintTypeTag) == sn.name).toStream)
      _   <- extendPath[M,N](v)
    } yield v

  def outE[E <: SchemaEdge](se: E): Tr[Env,State[E#From],State[E],Edge] =
    for {
      State((head: Node) :: rest,_,_) <- getState[Env,State[E#From]]
      e <- streamToTraverser(head.getEdges(blueprints.Direction.OUT, se.name).toStream)
      _ <- extendPath(e)
    } yield e

  def inE[E <: SchemaEdge](se: E): Tr[Env,State[E#To],State[E],Edge] =
    for {
      State((head: Node) :: rest,_,_) <- getState[Env,State[E#To]]
      e <- streamToTraverser(head.getEdges(blueprints.Direction.IN, se.name).toStream)
      _ <- extendPath(e)
    } yield e

  def outV[E <: SchemaEdge]: Tr[Env,State[E],State[E#From],Node] =
    for {
      State((head: Edge) :: rest,_,_) <- getState[Env,State[E]]
      v = head.getVertex(blueprints.Direction.OUT)
      _ <- extendPath(v)
    } yield v

  def inV[E <: SchemaEdge]: Tr[Env,State[E],State[E#To],Node] =
    for {
      State((head: Edge) :: rest,_,_) <- getState[Env,State[E]]
      v = head.getVertex(blueprints.Direction.IN)
      _ <- extendPath(v)
    } yield v


  def get[T, E <: SchemaElement](p: E#SchemaProperty[T]): Tr[Env,State[E],State[E],T] =
    Tr(env => i => i match {
      case State(path,_,_) => Stream((i,path.head.getProperty[T](p.name)))
    })

}