package hu.andrasszatmari.fpmortals

import scalaz.NonEmptyList

final case class MachineNode(id: String)
trait Machines[F[_]] {
  def getTime: F[Epoch]
  def getManaged: F[NonEmptyList[MachineNode]]
  def getAlive: F[Map[MachineNode, Epoch]]
  def start(node: MachineNode): F[Unit]
  def stop(node: MachineNode): F[Unit]
}
