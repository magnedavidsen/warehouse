package nbrno

import unfiltered.filter.Plan
import unfiltered.request._
import unfiltered.response._
import nbrno.domain.{Vote, User}
import org.json4s.DefaultFormats
import org.json4s.native.Serialization.{read, write}
import unfiltered.response.ResponseString
import org.slf4j.{LoggerFactory, Logger}
import unfiltered.Cookie

object StatsPlan extends Plan {
  implicit val formats = DefaultFormats
  val logger : Logger = LoggerFactory.getLogger("nbrno.StatsPlan")
  val dbHandler = ComponentRegistry.databaseHandler

  def intent = {
    case GET(_) & Path("/api/stats") => {
      JsonContent ~> ResponseString(write(dbHandler.getStats))
    }
  }
}

object RappersPlan extends Plan {
  implicit val formats = DefaultFormats
  val logger : Logger = LoggerFactory.getLogger("nbrno.RappersPlan")
  val dbHandler = ComponentRegistry.databaseHandler
  val sessionStore = ComponentRegistry.sessionStore

  def intent = {
    case GET(_) & Cookies(cookies) & Path("/api/rappers")  => {
      val user = sessionStore.getUserFromCookie(cookies("SESSION_ID"))
      val rappersString = write(dbHandler.getRappersWithTotalScore)
      var votesString = ""
      user match {
        case Some(value) => votesString = write(dbHandler.getVotes(user.get.username))
        case None => votesString = "[]"
      }
      ResponseString("{\"rappers\": " ++ rappersString ++ ", \"votes\": " ++ votesString ++ "}")
    }

    case req@Path(Seg("api" ::"rappers" :: rapperId :: "vote" :: Nil)) =>
      val body : String = Body.string(req)
      logger.info("RequestBody: " ++ body)
      req match {
        case POST(_) & Cookies(cookies) => req match {
          case RequestContentType("application/json;charset=UTF-8") => req match {
            case Accepts.Json(_) =>
              Ok ~> JsonContent ~> {
                val vote : Vote = read[Vote](body)
                val user : Option[User] = sessionStore.getUser(cookies("SESSION_ID").get.value)
                //TODO: Ensure rapperId is int
                if(user.isDefined){
                  dbHandler.vote(user.get.id.get, rapperId.toInt, vote.voteUp)
                  ResponseString("Vote registered")
                }
                else Unauthorized
              }
            case _ => NotAcceptable
          }
          case _ => UnsupportedMediaType
        }
        case _ => MethodNotAllowed
      }
  }
}

object UserPlan extends Plan {
  implicit val formats = DefaultFormats
  val logger : Logger = LoggerFactory.getLogger("nbrno.UserPlan")
  val dbHandler = ComponentRegistry.databaseHandler
  val sessionStore = ComponentRegistry.sessionStore

  val oneYear: Int = 31536000

  def intent = {

    case req@Path("/api/user/signup") =>
      val body : String = Body.string(req)
      logger.info("RequestBody: " ++ body)
      req match {
        case POST(_) => req match {
          case RequestContentType("application/json;charset=UTF-8") => req match {
            case Accepts.Json(_) =>
              Ok ~> JsonContent ~> {
                val user: User = read[User](body)
                if (dbHandler.availableUsername(user.username)) {
                  val newUser : User = dbHandler.createUser(user, req.remoteAddr)
                  val sessionId : String = sessionStore.addUser(newUser)
                  Ok ~> SetCookies(Cookie("SESSION_ID", sessionId, None, Some("/"), Some(oneYear)))
                }
                else BadRequest ~> ResponseString("Username not available")
              }
            case _ => NotAcceptable
          }
          case _ => UnsupportedMediaType
        }
        case _ => MethodNotAllowed
      }

    case req@Path("/api/user/login") =>
      val body : String = Body.string(req)
      logger.info("RequestBody: " ++ body)
      req match {
        case POST(_) => req match {
          case RequestContentType("application/json;charset=UTF-8") => req match {
            case Accepts.Json(_) =>
              Ok ~> JsonContent ~> {
                val user: User = read[User](body)
                val validatedUser = dbHandler.validateUser(user.username, user.password.get)
                if (validatedUser.isDefined) {
                  val sessionId : String = sessionStore.addUser(validatedUser.get)
                  Ok ~> SetCookies(Cookie("SESSION_ID", sessionId, None, Some("/"), Some(oneYear)))
                }
                else BadRequest ~> ResponseString("Wrong username or password")
              }
            case _ => NotAcceptable
          }
          case _ => UnsupportedMediaType
        }
        case _ => MethodNotAllowed
      }

    case req@Path("/api/user/login/cookie") =>
      val body : String = Body.string(req)
      logger.info("RequestBody: " ++ body)
      req match {
        case POST(_) => req match {
          case RequestContentType("application/json;charset=UTF-8") => req match {
            case Accepts.Json(_) =>
              Ok ~> JsonContent ~> {
                val cookie = scala.util.parsing.json.JSON.parseFull(body)
                cookie match {
                  case Some(map : Map[String, String]) => {
                    val user = sessionStore.getUser(map.get("SESSION_ID").get)
                    user match {
                      case Some(user) =>{
                        val noPassHashUser = user.copy(passhash=None, createdFromIp = None)
                        Ok ~> ResponseString(write(noPassHashUser))
                      }
                      case None => BadRequest ~> ResponseString("No user found")
                    }
                  }
                  case _ => BadRequest ~> ResponseString("No SESSION_ID")
                }
              }
            case _ => NotAcceptable
          }
          case _ => UnsupportedMediaType
        }
        case _ => MethodNotAllowed
      }

    case req@Path("/api/user/logout") =>
      val body : String = Body.string(req)
      logger.info("RequestBody: " ++ body)
      req match {
        case POST(_) & Cookies(cookies) => req match {
          case RequestContentType("application/json;charset=UTF-8") => req match {
            case Accepts.Json(_) =>
              Ok ~> JsonContent ~> {
                val sessionId = cookies("SESSION_ID")
                if(sessionId.isDefined) sessionStore.removeUser(sessionId.get.value)
                Ok ~> SetCookies(Cookie("SESSION_ID", "", None, Some("/")))
              }
            case _ => NotAcceptable
          }
          case _ => UnsupportedMediaType
        }
        case _ => MethodNotAllowed
      }

    case req@Path("/api/user/login/forgot") =>
      val body : String = Body.string(req)
      logger.info("RequestBody: " ++ body)
      req match {
        case POST(_) => req match {
          case RequestContentType("application/json;charset=UTF-8") => req match {
            case Accepts.Json(_) =>
              Ok ~> JsonContent ~> {
                val json = scala.util.parsing.json.JSON.parseFull(body)
                json match {
                  case Some(map : Map[String, String]) => {
                    val email = map.get("email").get
                    val passwordReset : Boolean = dbHandler.resetPassword(email, "password")
                    passwordReset match {
                      case true =>{
                        Ok ~> ResponseString("")
                      }
                      case false => BadRequest ~> ResponseString("No user found")
                    }
                  }
                  case _ => BadRequest ~> ResponseString("No SESSION_ID")
                }
              }
            case _ => NotAcceptable
          }
          case _ => UnsupportedMediaType
        }
        case _ => MethodNotAllowed
      }
  }
}