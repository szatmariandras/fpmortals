package hu.andrasszatmari.fpmortals

import scalaz._, Scalaz._
import simulacrum._

trait DynAgents[F[_]] {
  def initial: F[WorldView]
  def update(old: WorldView): F[WorldView]
  def act(world: WorldView): F[WorldView]
}

final class DynAgentsModule[F[_]: Monad](D: Drone[F], M: Machines[F])
  extends DynAgents[F] {

  def initial: F[WorldView] = for {
    db <- D.getBacklog
    da <- D.getAgents
    mm <- M.getManaged
    ma <- M.getAlive
    mt <- M.getTime
  } yield {
    WorldView(db, da, mm, ma, Map.empty, mt)
  }

  def update(old: WorldView): F[WorldView] = ???

  def act(world: WorldView): F[WorldView] = ???
}
