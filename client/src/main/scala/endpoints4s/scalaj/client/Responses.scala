package endpoints4s.scalaj.client

import scalaj.http.HttpResponse
import endpoints4s.{Invalid, InvariantFunctor, Semigroupal, Tupler, Valid, Validated, algebra}
import endpoints4s.algebra.Documentation

/** @group interpreters
  */
trait Responses extends algebra.Responses with StatusCodes {
  this: algebra.Errors =>

  type Response[A] = HttpResponse[String] => Option[ResponseEntity[A]]

  implicit lazy val responseInvariantFunctor: InvariantFunctor[Response] =
    new InvariantFunctor[Response] {
      def xmap[A, B](
          fa: Response[A],
          f: A => B,
          g: B => A
      ): Response[B] =
        resp => fa(resp).map(_.xmap(f)(g))
    }

  type ResponseEntity[A] = String => Either[Throwable, A]

  implicit def responseEntityInvariantFunctor: InvariantFunctor[ResponseEntity] =
    new InvariantFunctor[ResponseEntity] {
      def xmap[A, B](
          fa: ResponseEntity[A],
          f: A => B,
          g: B => A
      ): ResponseEntity[B] =
        s => fa(s).map(f)
    }

  def emptyResponse: ResponseEntity[Unit] =
    _ => Right(())

  def textResponse: ResponseEntity[String] =
    s => Right(s)

  type ResponseHeaders[A] = Map[String, Seq[String]] => Validated[A]

  implicit def responseHeadersSemigroupal: Semigroupal[ResponseHeaders] =
    new Semigroupal[ResponseHeaders] {
      def product[A, B](fa: ResponseHeaders[A], fb: ResponseHeaders[B])(implicit
          tupler: Tupler[A, B]
      ): ResponseHeaders[tupler.Out] =
        headers => fa(headers).zip(fb(headers))
    }

  implicit def responseHeadersInvariantFunctor: InvariantFunctor[ResponseHeaders] =
    new InvariantFunctor[ResponseHeaders] {
      def xmap[A, B](
          fa: ResponseHeaders[A],
          f: A => B,
          g: B => A
      ): ResponseHeaders[B] =
        headers => fa(headers).map(f)
    }

  def emptyResponseHeaders: ResponseHeaders[Unit] = _ => Valid(())

  def responseHeader(
      name: String,
      docs: Documentation = None
  ): ResponseHeaders[String] =
    headers =>
      Validated.fromOption(
        headers.get(name.toLowerCase).map(_.mkString(", "))
      )(s"Missing response header '$name'")

  def optResponseHeader(
      name: String,
      docs: Documentation = None
  ): ResponseHeaders[Option[String]] =
    headers => Valid(headers.get(name.toLowerCase).map(_.mkString(", ")))

  def response[A, B, R](
      statusCode: StatusCode,
      entity: ResponseEntity[A],
      docs: Documentation = None,
      headers: ResponseHeaders[B] = emptyResponseHeaders
  )(implicit
      tupler: Tupler.Aux[A, B, R]
  ): Response[R] =
    response =>
      if (response.code == statusCode) {
        headers(response.headers) match {
          case Valid(b) => Some(s => entity(s).map(tupler(_, b)))
          case Invalid(errors) =>
            Some(_ => Left(new Exception(errors.mkString(". "))))
        }
      } else None

  def choiceResponse[A, B](
      responseA: Response[A],
      responseB: Response[B]
  ): Response[Either[A, B]] =
    resp =>
      responseA(resp)
        .map(entity => (s: String) => entity(s).map(Left(_)))
        .orElse(
          responseB(resp).map(entity => (s: String) => entity(s).map(Right(_)))
        )

  override def addResponseHeaders[A, H](
      response: Response[A],
      headers: ResponseHeaders[H]
  )(implicit tupler: Tupler[A, H]): Response[tupler.Out] =
    resp =>
      response(resp).map(_.andThen(_.flatMap { a =>
        headers(resp.headers) match {
          case Valid(h)        => Right(tupler(a, h))
          case Invalid(errors) => Left(new Exception(errors.mkString(". ")))
        }
      }))
}
