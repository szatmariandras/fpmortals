package hu.andrasszatmari.fpmortals

trait Drone[F[_]] {
  def getBacklog: F[Int]
  def getAgents: F[Int]
}
