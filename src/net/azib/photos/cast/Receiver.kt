package net.azib.photos.cast

class Receiver(
  val name: String,
  val url: String,
  val path: String,
  val token: String
) {
  val fullUrl get() = url + path
}
