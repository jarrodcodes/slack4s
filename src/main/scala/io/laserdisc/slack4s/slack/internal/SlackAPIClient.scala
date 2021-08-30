package io.laserdisc.slack4s.slack.internal

import cats.effect.{ ConcurrentEffect, Resource, Sync }
import cats.implicits._
import com.slack.api.methods.request.chat.ChatPostMessageRequest
import io.laserdisc.slack4s.internal.mkCachedThreadPool
import org.http4s.blaze.client.BlazeClientBuilder
import org.http4s.client.Client
import org.http4s.{ Method, Request, Uri }
import org.typelevel.log4cats.slf4j.Slf4jLogger

object SlackAPIClient {

  private[this] val SlackApiEC = mkCachedThreadPool(prefix = "slack-api-client")

  def resource[F[_]: ConcurrentEffect]: Resource[F, SlackAPIClientImpl[F]] =
    for {
      httpClient <- BlazeClientBuilder[F](SlackApiEC).resource
      client     = SlackAPIClientImpl(httpClient)
    } yield client

}

trait SlackAPIClient[F[_]] {
  def respond(url: String, input: ChatPostMessageRequest): F[Unit]
}

case class SlackResponseAccepted()

case class SlackAPIClientImpl[F[_]: Sync](httpClient: Client[F]) extends SlackAPIClient[F] {

  private[this] val logger = Slf4jLogger.getLogger[F]

  override def respond(url: String, input: ChatPostMessageRequest): F[Unit] =
    for {
      _ <- logger.debug(s"SLACK-RESPOND-REQ url:$url input:$input")
      res <- httpClient.expect[SlackResponseAccepted](
              Request[F](
                Method.POST,
                uri = Uri.unsafeFromString(url)
              ).withEntity(input)
            )
      _ <- logger.debug(s"SLACK-RESPOND-RES $res")
    } yield ()

}
