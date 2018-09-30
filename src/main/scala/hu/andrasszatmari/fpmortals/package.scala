package hu.andrasszatmari

import contextual.{ Prefix, Verifier }
import java.time.Instant

import scala.util.control.NonFatal

package object fpmortals {

  object EpochInterpolator extends Verifier[Epoch] {
    override def check(string: String): Either[(Int, String), Epoch] =
      try Right(Epoch(Instant.parse(string).toEpochMilli))
    catch { case NonFatal(_) => Left((0, "not in ISO-8601 format")) }
  }
  implicit class EpochMillisStringContext(sc: StringContext) {
    val epoch = Prefix(EpochInterpolator, sc)
  }

}
