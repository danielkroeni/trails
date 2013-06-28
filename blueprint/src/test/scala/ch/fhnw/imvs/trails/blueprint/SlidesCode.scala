package ch.fhnw.imvs.trails.blueprint


import org.scalatest.FunSuite
import com.tinkerpop.blueprints.impls.tg.TinkerGraph
import BlueprintTrails._

/*
test-only ch.fhnw.imvs.trails.blueprint.SlidesTests
*/
class SlidesTests extends FunSuite {

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

