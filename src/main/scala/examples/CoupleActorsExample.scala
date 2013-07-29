package examples

import akka.actor._
import scala.concurrent.duration._
import scala.concurrent.Future
import akka.pattern.ask
import scala.concurrent.Await
import examples.Success
import examples.Fail
import examples.CalculateTask
import examples.InitializeB
import examples.Calculate
import scala.Some
import examples.CalculateResult
import akka.util.Timeout
import scala.util.Try
import akka.dispatch.Futures
import scala.collection.JavaConversions._
import com.typesafe.config.ConfigFactory

/**
 * Created with IntelliJ IDEA.
 * User: Николай
 * Date: 26.07.13
 * Time: 17:37
 * To change this template use File | Settings | File Templates.
 */
object CoupleActorsExample {
  def main(argv:Array[String])={

    implicit val timeout = Timeout(10 seconds)

//    val system = ActorSystem("fonhorst-custom",ConfigFactory parseString
//      """akka{
//        loglevel = "DEBUG"
//        actor.debug.lifecycle = on
//         }""")

    val system = ActorSystem("fonhorst-custom")

    implicit val dispatcher = system dispatcher

    val calculator:Calculator = TypedActor(system).typedActorOf(TypedProps[CalculatorImpl],"main-facade")

    def shutdown={
      println("System are going to shutdown")
      system.shutdown()
    }

    val proccess_results = (arg:Try[Any]) => {println(Thread currentThread); arg match {
        case x if x isFailure => {
          println("Error occured! =( Info:" + (x get))
        }
        case x if x isSuccess =>{
          x get match {
            case Success(opId,n) => println("Success! Result: " + n)
            case Fail(opId) => println("Fail! =(")
          }
        }
        case _ => {
          throw new UnexpectedErrorException
        }
      }
    }

//    calculator calculate 10 onComplete proccess_results
//    calculator asyncCalculate 11 onComplete proccess_results
//    println("Result: " + (calculator immediateCalculate 12))

    val future1 = calculator calculate 10
    future1 onComplete proccess_results
    val future2 = calculator asyncCalculate 11
    future2 onComplete proccess_results
    println("Result immediate: " + (calculator immediateCalculate 12))
    println("Result halfImmediate: " + (calculator immediateCalculate 13))


    Futures.sequence[CalculateResult](asJavaIterable(
                                              List[Future[CalculateResult]](future1,future2).toIterable),
                                              dispatcher
                                      ).onComplete( (_) =>shutdown)


  }
}

/**
 * interface of a typed actor.
 * It represents gateway from a non-akka part of application to an akka part
 */

trait Calculator {
  def calculate(n:Int):Future[CalculateResult]
  def asyncCalculate(n:Int):Future[CalculateResult]
  def immediateCalculate(n:Int):Int
  def halfImmediateCalculate(n:Int):Int
}

class CalculatorImpl extends Calculator{

  implicit val timeout = Timeout(10 seconds)

  val actor = {
    val ctx = TypedActor.context
    ctx.actorOf(Props[ActorA], "a-calc")
  }

  private[this] var initializator:()=>Unit = () => {
    actor ! new InitializeB("ActorB")
    initializator = () => {}
  }

  private[this] def sendRequest(n:Int,async:Boolean) = (actor ? new CalculateTask(0,n,async)).mapTo[CalculateResult]

  private[this] def sendAndWaitRequest(n:Int,async:Boolean):Int =
    Await.result[CalculateResult](sendRequest(n,async),Duration.Inf) match {
      case Success(_,n) => return n
      case Fail(_) => throw new UnexpectedErrorException
    }

  def calculate(n: Int): Future[CalculateResult] = {
    initializator()
    sendRequest(n,false)
  }

  def asyncCalculate(n: Int): Future[CalculateResult] = {
    initializator()
    sendRequest(n,true)
  }

  def immediateCalculate(n: Int): Int = {
    initializator()
    sendAndWaitRequest(n,false)
  }

  def halfImmediateCalculate(n: Int): Int = {
    initializator()
    sendAndWaitRequest(n,true)
  }
}

/**
 * messages are received by ActorA and ActorB
 */
case class InitializeB(name:String)
case object GetChildPath
case class CalculateTask(id:Int,n:Int,async:Boolean)
case class Calculate(val n:Int)
class CalculateResult(id:Int)
case class Success(opId:Int,n:Int) extends CalculateResult(opId)
case class Fail(opId:Int) extends CalculateResult(opId)

/**
 * dummy class for exception.
 * It will be better to replace this exception with something more useful
 */
class UnexpectedErrorException extends RuntimeException

/**
 * this actor gets CalculateTask messages,
 * updates its with shiftFactor then sends last part of work to its child.
 * For the first use it must be initialized with InitializeB message,
 * in other cases it will drop any message except GetChildPath
 * In uninitialized state Receiving of GetChildPath message results to UnexpectedErrorException
 */
class ActorA extends Actor{

  import context.dispatcher

  implicit val timeout = Timeout(10 seconds)

  val shiftFactor:Int = 2
  var actorB:Option[ActorRef] = None

  def calculate(n:Int) = new Calculate(n + shiftFactor)

  def receive: Actor.Receive = {

    case InitializeB(name) if actorB == None => actorB = Some[ActorRef](context.actorOf(Props[ActorB],name))

    case CalculateTask(id,n,false) if actorB != None => {
      val futurier = actorB.get ? calculate(n)
      val result  = (Await result(futurier, Duration.Inf)).asInstanceOf[Calculate]
      sender ! new Success(id,result n)
    }

    case CalculateTask(id,n,true) if actorB != None => {
      val client = sender
      actorB.get ? calculate(n) onComplete{
        case x if x isFailure => client ! new Fail(id)
        case x if x isSuccess => {
          val res:Calculate = (x get).asInstanceOf[Calculate]
          client ! new Success(id,res n)
        }
        case _ => throw new UnexpectedErrorException
      }

    }

    case GetChildPath if actorB == None => throw new UnexpectedErrorException

    case GetChildPath if actorB != None => sender ! actorB.get.path

    case _ => ()
  }
}

/**
 * this actor scalizes Calculate entities with scaleFactor
 * It responds only on Calculate messages,
 * in other cases the message will be dropped
 */
class ActorB extends Actor{
  val scaleFactor:Int = 5

  def scalize(n:Int) = n*scaleFactor

  def receive: Actor.Receive = {
    case Calculate(n) => sender ! new Calculate(scalize(n))
    case _ => ()
  }
}
