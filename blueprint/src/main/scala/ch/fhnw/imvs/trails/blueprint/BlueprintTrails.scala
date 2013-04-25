package ch.fhnw.imvs.trails.blueprint

import scala.collection.JavaConversions._
import ch.fhnw.imvs.trails.{TrailsPrimitives, Trails}
import com.tinkerpop.blueprints

object BlueprintTrails extends TrailsPrimitives with Trails {
  type Env = blueprints.Graph
  type Elem = blueprints.Element
  type Edge = blueprints.Edge
  type Node = blueprints.Vertex
  type Id = Any

  def V: Tr[Env,State[Nothing],State[Node],Node] =
    for {
      env <- getEnv[Env,State[Nothing]]
      v   <- streamToTraverser(env.getVertices.toStream)
      _   <- extendPath(v)
    } yield v

  def V(id: Id): Tr[Env,State[Nothing],State[Node],Node] =
    for {
      env <- getEnv[Env,State[Nothing]]
      v = env.getVertex(id)
      _   <- extendPath(v)
    } yield v

  def E: Tr[Env,State[Nothing],State[Edge],Edge] =
    for {
      env <- getEnv[Env,State[Nothing]]
      e   <- streamToTraverser(env.getEdges.toStream)
      _   <- extendPath(e)
    } yield e


  def E(id: Id): Tr[Env,State[Nothing],State[Edge],Edge] =
    for {
      env <- getEnv[Env,State[Nothing]]
      e   = env.getEdge(id)
      _   <- extendPath(e)
    } yield e

  def outE(edgeName: String): Tr[Env,State[Node],State[Edge],Edge] =
    ontoE(edgeName, blueprints.Direction.OUT)

  def inE(edgeName: String): Tr[Env,State[Node],State[Edge],Edge] =
    ontoE(edgeName, blueprints.Direction.IN)

  private def ontoE(edgeName: String, dir: blueprints.Direction): Tr[Env,State[Node],State[Edge],Edge] =
    for {
      State((head: Node) :: rest) <- getState[Env,State[Node]]
      e <- streamToTraverser(head.getEdges(dir, edgeName).toStream)
      _ <- extendPath(e)
    } yield e

  def outV: Tr[Env,State[Edge],State[Node],Node] =
    ontoV(blueprints.Direction.OUT)

  def inV: Tr[Env,State[Edge],State[Node],Node] =
    ontoV(blueprints.Direction.IN)

  private def ontoV(dir: blueprints.Direction): Tr[Env,State[Edge],State[Node],Node] =
    for {
      State((head: Edge) :: rest) <- getState[Env,State[Edge]]
      v = head.getVertex(dir)
      _ <- extendPath(v)
    } yield v

  def get[T](name: String)(e: Elem): T = e.getProperty[T](name)
}