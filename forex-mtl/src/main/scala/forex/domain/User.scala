package forex.domain

sealed trait User
final case class RegularUser(key: String) extends User
