package examples

import akka.testkit.{ImplicitSender, TestActorRef, TestKit}
import akka.actor.{ActorRef, ActorSystem}
import org.scalatest.{WordSpec, BeforeAndAfterAll}
import org.scalatest.matchers.ShouldMatchers

/**
 * Created with IntelliJ IDEA.
 * User: Николай
 * Date: 29.07.13
 * Time: 16:08
 * To change this template use File | Settings | File Templates.
 */
class CoupleActorsExampleTest extends  TestKit(ActorSystem("examples-unit-testing"))
                              with ImplicitSender
                              with WordSpec
                              with ShouldMatchers
                              with BeforeAndAfterAll
{

  val actorBRef = TestActorRef[ActorB]
  val actorARef = TestActorRef[ActorA]


  override def afterAll {
    shutdown(system)
  }

  "An actorB " should {
    "scalize argument" in {
       actorBRef ! new Calculate(5)
       expectMsg(new Calculate(25))
    }
  }

  "An actorB" should {
    "multiplicate argument with function scalize" in {
      assert(actorBRef.underlyingActor.scalize(5) == 25)
    }
  }

  "An actorA" should {
    "throw exception while trying to get the child's name in the uninitialized state" in {

      intercept[UnexpectedErrorException]{ actorARef receive GetChildPath }
    }
  }

  "An actorA" should {
    "initialize a child actorB" in {
      val name = "child-actor"
      actorARef ! new InitializeB(name)

      val child:Option[ActorRef] = actorARef.underlyingActor.actorB
      assert(child != None)
      assert(child.get.path.toString.endsWith(name))
    }
  }

  "An actorA" should {
    "synchroniously proccess a task" in {
      actorARef ! new CalculateTask(0, 5, false)
      expectMsg(new Success(0,35))
    }
  }

  "An actorA" should {
    "asynchroniously proccess a task" in {
      actorARef ! new CalculateTask(0, 5, true)
      expectMsg(new Success(0,35))
    }
  }
}