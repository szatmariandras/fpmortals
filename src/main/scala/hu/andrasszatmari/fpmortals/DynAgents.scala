package hu.andrasszatmari.fpmortals

import scala.concurrent.duration._

import scalaz._, Scalaz._
import simulacrum._

trait DynAgents[F[_]] {
  def initial: F[WorldView]
  def update(old: WorldView): F[WorldView]
  def act(world: WorldView): F[WorldView]
}

final class DynAgentsModule[F[_]: Monad](D: Drone[F], M: Machines[F])
  extends DynAgents[F] {

  def initial: F[WorldView] =
    ^^^^(D.getBacklog, D.getAgents, M.getManaged, M.getAlive, M.getTime) {
      case (db, da, mm, ma, mt) => WorldView(db, da, mm, ma, Map.empty, mt)
  }

  def update(old: WorldView): F[WorldView] = for {
    snap <- initial
    changed = symdiff(old.alive.keySet, snap.alive.keySet)
    pending = (old.pending -- changed).filterNot {
      case (_, started) => (snap.time - started) >= 10.minutes
    }
    update = snap.copy(pending = pending)
  } yield update

  def act(world: WorldView): F[WorldView] = world match {
    case NeedsAgent(node) =>
      for {
        _ <- M.start(node)
        update = world.copy(pending = Map(node -> world.time))
      } yield update
    case Stale(nodes) =>
      for {
        stopped <- nodes.traverse(M.stop)
        updates = stopped.map(_ -> world.time).toList.toMap
        update = world.copy(pending = world.pending ++ updates)
      } yield update
    case _ => world.pure[F]
  }

  private def symdiff[T](a: Set[T], b: Set[T]): Set[T] =
    (a union b) -- (a intersect b)
}

private object NeedsAgent {
  def unapply(world: WorldView): Option[MachineNode] = world match {
    case WorldView(backlog, 0, managed, alive, pending, _)
      if backlog > 0 && alive.isEmpty && pending.isEmpty
        => Option(managed.head)
    case _ => None
  }
}

private object Stale {
  def unapply(world: WorldView): Option[NonEmptyList[MachineNode]] = world match {
    case WorldView(backlog, _, _, alive, pending, time) if alive.nonEmpty =>
      (alive -- pending.keys).collect {
        case (n, started) if backlog == 0 && (time - started).toMinutes % 60 >= 58 => n
        case (n, started) if (time - started) >= 5.hours => n
      }.toList.toNel
    case _ => None
  }
}
