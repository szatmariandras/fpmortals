package hu.andrasszatmari.fpmortals.OAuth

import eu.timepit.refined.api.Refined
import eu.timepit.refined.string.Url
import hu.andrasszatmari.fpmortals.Epoch
import jsonformat.JsDecoder
import scalaz._, Scalaz._

import scala.concurrent.duration._

trait JsonClient[F[_]] {
  def get[A: JsDecoder](
    uri: String Refined Url,
    headers: IList[(String, String)]
  ): F[A]

  def post[P: UrlEncodedWriter, A: JsDecoder](
    url: String Refined Url,
    payload: P,
    headers: IList[(String, String)] = IList.empty
  ): F[A]
}

final case class CodeToken(token: String, redirect_uri: String Refined Url)

trait UserInteraction[F[_]] {
  def start: F[String Refined Url]
  def open(uri: String Refined Url): F[Unit]
  def stop: F[CodeToken]
}

trait LocalClock[F[_]] {
  def now: F[Epoch]
}

final case class ServerConfig(
  auth: String Refined Url,
  access: String Refined Url,
  refresh: String Refined Url,
  scope: String,
  clientId: String,
  clientSecret: String
)

final case class RefreshToken(token: String)
final case class BearerToken(token: String, expires: Epoch)

import UrlQueryWriter.ops._
import UrlEncodedWriter.ops._
import UrlQuery.ops._

class OAuth2Client[F[_]: Monad](
  config: ServerConfig
)(
  user: UserInteraction[F],
  client: JsonClient[F],
  clock: LocalClock[F]
) {
  def authenticate: F[CodeToken] =
    for {
      callback <- user.start
      params   = AuthRequest(callback, config.scope, config.clientId)
      _        <- user.open(config.auth.withQuery(params.toUrlQuery))
      code     <- user.stop
    } yield code

  def access(code: CodeToken): F[(RefreshToken, BearerToken)] =
    for {
      request <- AccessRequest(
          code.token,
          code.redirect_uri,
          config.clientId,
          config.clientSecret
        ).pure[F]
      msg     <- client.post[AccessRequest, AccessResponse](
          config.access, request
        )
      time    <- clock.now
      expires = time + msg.expires_in.seconds
      refresh = RefreshToken(msg.refresh_token)
      bearer  = BearerToken(msg.access_token, expires)
    } yield (refresh, bearer)

  def bearer(refresh: RefreshToken): F[BearerToken] =
    for {
      request <- RefreshRequest(
        config.clientSecret,
        refresh.token,
        config.clientId).pure[F]
      msg     <- client.post[RefreshRequest, RefreshResponse](
          config.refresh, request)
      time    <- clock.now
      expires = time + msg.expires_in.seconds
      bearer  = BearerToken(msg.access_token, expires)
    } yield bearer
}

