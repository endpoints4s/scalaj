package endpoints.scalaj.client
import endpoints.algebra.Codec

/**
  * Interpreter for [[endpoints.algebra.JsonEntitiesFromCodec]] that encodes JSON requests
  * and decodes JSON responses.
  */
trait JsonEntitiesFromCodec extends Endpoints with endpoints.algebra.JsonEntitiesFromCodec {

  def jsonRequest[A](implicit codec: Codec[String, A]): RequestEntity[A] = (data, request) => {
    request.header("Content-Type", "application/json")
    request.postData(codec.encode(data))
  }

  def jsonResponse[A](implicit codec: Codec[String, A]): Response[A] =
    resp => codec.decode(resp.body)

}
