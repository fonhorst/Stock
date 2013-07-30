package examples

/**
 * Created with IntelliJ IDEA.
 * User: Николай
 * Date: 30.07.13
 * Time: 10:38
 * To change this template use File | Settings | File Templates.
 */


import akka.actor.{TypedActor, TypedProps, ActorSystem, Props}
import akka.testkit.TestKit
import org.scalatest.WordSpec
import org.scalatest.matchers.MustMatchers
import org.scalatest.BeforeAndAfterAll
import akka.testkit.ImplicitSender
import akka.util.Timeout
import scala.concurrent.duration._
import scala.concurrent.Await


class CoupleActorsExampleIntegrationTest(_system: ActorSystem) extends TestKit(_system) with ImplicitSender
with WordSpec with MustMatchers with BeforeAndAfterAll {

  def this() = this(ActorSystem("MySpec"))

  implicit val dispatcher = system dispatcher
  implicit val timeout = Timeout(10 seconds)
  val calculator:Calculator = TypedActor(system).typedActorOf(TypedProps[CalculatorImpl],"main-facade")

  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }


  "For the first, we " must {

    "check an async-sync method calculate" in {

      val res = (Await result(calculator calculate 10, Duration.Inf))

      assert(res == new Success(0,60))
    }
  }

  "For the second, we " must {
    "check an async-async method asyncCalculate" in {
      val res = (Await result(calculator asyncCalculate 10, Duration.Inf))
      assert(res == new Success(0,60))
    }
  }

  "For the third, we " must {
    "check an sync-sync method immediateCalculate" in {
      val res = calculator immediateCalculate 10
      assert(res == 60)
    }
  }

  "For the forth, we " must {
    "check an sync-async method halfImmediateCalculate" in {
      val res = calculator halfImmediateCalculate 10
      assert(res == 60)
    }
  }
}
