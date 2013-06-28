package ch.fhnw.imvs.trails.blueprint

import org.scalatest.FunSuite
import com.tinkerpop.blueprints.impls.tg.TinkerGraph
import BlueprintTrails._

/*
~test-only ch.fhnw.imvs.trails.blueprint.SlideTests
*/
class SlideTests extends FunSuite {

  val greekWorld = new TinkerGraph()

  val α = "α"; val β = "β"; val γ = "γ"; val δ = "δ"; val ε = "ε"

  val A = greekWorld.addVertex("A")
  val B = greekWorld.addVertex("B")
  val Γ = greekWorld.addVertex("Γ")
  val Δ = greekWorld.addVertex("Δ")
  val Ω = greekWorld.addVertex("Ω")
  val α1 = A.addEdge(α, B)
  val β1 = B.addEdge(β, Γ)
  val γ1 = Γ.addEdge(γ, Ω)
  val δ1 = A.addEdge(δ, Ω)
  val α2 = A.addEdge(α, Δ)
  val ε1 = Δ.addEdge(ε, Δ)
  val α3 = Δ.addEdge(α, Ω)


  test("result") {
    val question = V("A") ~ out(α) ~ out(ε).* ~> out(α)
    val answer = Traverser.run(question, greekWorld)

    assert(answer.size === 2)

    assert(answer.toSet === Set(
      (List(A, α2, Δ, α3, Ω), Ω),
      (List(A, α2, Δ, ε1, Δ, α3, Ω), Ω)
    ))
  }

  val travelMap = new TinkerGraph()

  val train = "train"
  val plane = "plane"
  val taxi = "taxi"
  val foot = "foot"

  val fhnw = travelMap.addVertex("FHNW")
  val zrh = travelMap.addVertex("ZRH")
  val mrs = travelMap.addVertex("MRS")
  val scala13 = travelMap.addVertex("Scala'13")
  val lys = travelMap.addVertex("LYS")
  val fz = fhnw.addEdge(train, zrh)
  val zm = zrh.addEdge(plane, mrs)
  val ms = mrs.addEdge(taxi, scala13)
  val fl = fhnw.addEdge(train, lys)
  val ll = lys.addEdge(foot, lys)
  val ls = lys.addEdge(train, scala13)

  test("Example journey") {
    val journey = V("FHNW") ~ out("train") ~ out("foot").* ~> out("train")
    val answer = Traverser.run(journey,travelMap)

    assert(answer.size === 2)

    assert(answer.toSet === Set(
      (List(fhnw, fl, lys, ls, scala13),scala13),
      (List(fhnw, fl, lys, ll, lys, ls, scala13), scala13)
    ))
  }
}
