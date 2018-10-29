package hu.andrasszatmari.fpmortals

import scalaz._, Scalaz._

import org.scalatest.FlatSpec
import org.scalatest.Matchers._

object Data {
  val node1 = MachineNode("1243d1af-828f-4ba3-9fc0-a19d86852b5a")
  val node2 = MachineNode("550c4943-229e-47b0-b6be-3d686c5f013f")

  val managed = NonEmptyList(node1, node2)

  val time1: Epoch = epoch"2017-03-03T18:07:00Z"
  val time2: Epoch = epoch"2017-03-03T18:59:00Z" // +52 mins
  val time3: Epoch = epoch"2017-03-03T19:06:00Z" // +59 mins
  val time4: Epoch = epoch"2017-03-03T23:07:00Z" // +5 hours

  val needsAgents = WorldView(5, 0, managed, Map.empty, Map.empty, time1)
}

import Data._

class Mutable(state: WorldView) {
  var started, stopped: Int = 0

  private val D: Drone[Id] = new Drone[Id] {
    def getBacklog: Int = state.backlog
    def getAgents: Int = state.agents
  }

  private val M: Machines[Id] = new Machines[Id] {
    def getAlive: Map[MachineNode, Epoch] = state.alive
    def getManaged: NonEmptyList[MachineNode] = state.managed
    def getTime: Epoch = state.time
    def start(node: MachineNode): Unit = { started += 1 }
    def stop(node: MachineNode): Unit = { stopped -= 1 }
  }

  val program = new DynAgentsModule[Id](D, M)
}

object ConstImpl {
  type F[a] = Const[String, a]

  private val D = new Drone[F] {
    def getBacklog: F[Int] = Const("backlog")
    def getAgents: F[Int] = Const("agents")
  }

  private val M = new Machines[F] {
    def getTime: F[Epoch] = Const("time")
    def getManaged: F[NonEmptyList[MachineNode]] = Const("managed")
    def getAlive: F[Map[MachineNode, Epoch]] = Const("alive")
    def start(node: MachineNode): F[Unit] = Const("start")
    def stop(node: MachineNode): F[Unit] = Const("stop")
  }

  val program = new DynAgentsModule[F](D, M)
}

object ConstCounterImpl {
  type F[a] = Const[String ==>> Int, a]

  private val D = new Drone[F] {
    def getBacklog: F[Int] = Const(IMap("backlog" -> 1))
    def getAgents: F[Int] = Const(IMap("agents" -> 1))
  }

  private val M = new Machines[F] {
    def getTime: F[Epoch] = Const(IMap("time" -> 1))
    def getManaged: F[NonEmptyList[MachineNode]] = Const(IMap("managed" -> 1))
    def getAlive: F[Map[MachineNode, Epoch]] = Const(IMap("alive" -> 1))
    def start(node: MachineNode): F[Unit] = Const(IMap("start" -> 1))
    def stop(node: MachineNode): F[Unit] = Const(IMap("stop" -> 1))
  }

  val program = new DynAgentsModule[F](D, M)
}

class BusinessLogicTest extends FlatSpec {

  "Business Logic" should "generate an initial world view" in {
    val mutable = new Mutable(needsAgents)
    import mutable._

    program.initial shouldBe needsAgents
  }

  it should "remove changed nodes from pending" in {
    val world = WorldView(0, 0, managed, Map(node1 -> time3), Map.empty, time3)
    val mutable = new Mutable(world)
    import mutable._

    val old = world.copy(
      alive = Map.empty,
      pending = Map(node1 -> time2),
      time = time2
    )

    program.update(old) shouldBe world
  }

  it should "request agents when needed" in {
    val mutable = new Mutable(needsAgents)
    import mutable._

    val expected = needsAgents.copy(
      pending = Map(node1 -> time1)
    )
    program.act(needsAgents) shouldBe expected

    mutable.stopped shouldBe 0
    mutable.started shouldBe 1
  }

  it should "not request agents when pending" in {
    val world = WorldView(5, 0, managed, Map.empty, Map(node1 -> time1, node2 -> time1), time1)
    val mutable = new Mutable(world)
    import mutable._

    val world2 = program.act(world)

    world2 shouldBe world
  }

  it should "call the expected methods" in {
    import ConstImpl._

    val alive = Map(node1 -> time1, node2 -> time1)
    val world = WorldView(1, 1, managed, alive, Map.empty, time4)

    program.act(world).getConst shouldBe "stopstop"
  }

  it should "call the expected methods expected time" in {
    import ConstCounterImpl._

    val alive = Map(node1 -> time1, node2 -> time1)
    val world = WorldView(1, 1, managed, alive, Map.empty, time4)

    program.act(world).getConst shouldBe IMap("stop" -> 2)
  }
}
