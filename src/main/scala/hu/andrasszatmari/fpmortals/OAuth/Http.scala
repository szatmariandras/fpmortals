package hu.andrasszatmari.fpmortals.OAuth

import eu.timepit.refined.api.{ Refined, Validate }
import java.util.regex.Pattern
import java.net.{ URI, URLEncoder }
import scalaz.IList
import scalaz._
import Scalaz._
import eu.timepit.refined.string.Url
import simulacrum._

sealed abstract class UrlEncoded
object UrlEncoded {
  private[this] val valid: Pattern =
    Pattern.compile("\\A(\\P{Alnum}++|[-.*_+=&]++%\\p{XDigit}{2})*\\z")

  implicit def urlValidate: Validate.Plain[String, UrlEncoded] =
    Validate.fromPredicate(
      s => valid.matcher(s).find(),
      identity,
      new UrlEncoded {}
    )
}

// URL query key=value pairs, in un-encoded form.
final case class UrlQuery(params: List[(String, String)])

@typeclass trait UrlQueryWriter[A] {
  def toUrlQuery(a: A): UrlQuery
}

@typeclass trait UrlEncodedWriter[A] {
  def toUrlEncoded(a: A): String Refined UrlEncoded
}

object UrlEncodedWriter {
  import ops._

  implicit val encoded: UrlEncodedWriter[String Refined UrlEncoded] = instance(
    identity
  )

  implicit val url: UrlEncodedWriter[String Refined Url] = instance(
    s => Refined.unsafeApply(s.value)
  )

  // WORKAROUND: no SAM here https://github.com/scala/bug/issues/10814
  def instance[T](f: T => String Refined UrlEncoded): UrlEncodedWriter[T] =
    new UrlEncodedWriter[T] {
      override def toUrlEncoded(t: T): String Refined UrlEncoded = f(t)
    }

  implicit val string: UrlEncodedWriter[String] = instance(
    s => Refined.unsafeApply(URLEncoder.encode(s, "UTF-8"))
  )

  implicit val long: UrlEncodedWriter[Long] = instance(
    s => Refined.unsafeApply(s.toString)
  )

  implicit def ilist[K: UrlEncodedWriter, V: UrlEncodedWriter]
  : UrlEncodedWriter[IList[(K, V)]] = instance({ m =>
    val raw = m.map {
      case (k, v) => k.toUrlEncoded.value + "=" + v.toUrlEncoded.value
    }.intercalate("&")
    Refined.unsafeApply(raw) // by deduction
  })
}

object UrlQuery {
  object ops {
    implicit class UrlOps(private val encoded: String Refined Url) {
      def withQuery(query: UrlQuery): String Refined Url = {
        val uri = new URI(encoded.value)
        val update = new URI(
          uri.getScheme,
          uri.getUserInfo,
          uri.getHost,
          uri.getPort,
          uri.getPath,
          // not a mistake: URI takes the decoded versions
          query.params.map { case (k, v) => k + "=" + v }.intercalate("&"),
          uri.getFragment
        )
        Refined.unsafeApply(update.toASCIIString)
      }
    }
  }
}