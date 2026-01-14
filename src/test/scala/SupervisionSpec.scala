import akka.actor.SupervisorStrategy.{Escalate, Restart, Resume, Stop}
import akka.actor._
import akka.testkit.{EventFilter, ImplicitSender, TestKit}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

class SupervisionSpec
  extends TestKit(ActorSystem("SuperVisionSpec"))
    with ImplicitSender
    with AnyWordSpecLike
    with Matchers
    with BeforeAndAfterAll {

  override def afterAll(): Unit =
    TestKit.shutdownActorSystem(system)

  import SupervisionSpec._

  "A supervisor" should {

    "resume its child in case of minor fault" in {
      val supervisor = system.actorOf(Props[Supervisor])
      supervisor ! Props[FussyWordCounter]
      val child = expectMsgType[ActorRef]

      child ! "I love Akka"
      child ! Report
      expectMsg(3)

      // too long -> RuntimeException -> Resume -> state should stay 3
      child ! "Akka is awesome because I am learning to think in a whole new way"
      child ! Report
      expectMsg(3)
    }

    "restart its child in case of an empty sentence" in {
      val supervisor = system.actorOf(Props[Supervisor])
      supervisor ! Props[FussyWordCounter]
      val child = expectMsgType[ActorRef]

      child ! "I love Akka"
      child ! Report
      expectMsg(3)

      // empty -> NullPointerException -> Restart -> state resets
      EventFilter[NullPointerException]() intercept {
        child ! ""
      }

      child ! Report
      expectMsg(0)
    }

    "terminate its child in case of a major error" in {
      val supervisor = system.actorOf(Props[Supervisor])
      supervisor ! Props[FussyWordCounter]
      val child = expectMsgType[ActorRef]

      watch(child)
      // starts with lowercase -> IllegalArgumentException -> Stop
      EventFilter[IllegalArgumentException]() intercept {
        child ! "akka is nice"
      }

      val terminated = expectMsgType[Terminated]
      terminated.actor shouldBe child
    }

    "escalate an error when it doesn't know what to do" in {
      val supervisor = system.actorOf(Props[Supervisor])
      supervisor ! Props[FussyWordCounter]
      val child = expectMsgType[ActorRef]

      watch(child)
      // non-string -> Exception -> Escalate (default: parent may restart/stop child depending on hierarchy)
      // easiest observable outcome in this simple setup: child terminates
      EventFilter[Exception]() intercept {
        child ! 43
      }

      val terminated = expectMsgType[Terminated]
      terminated.actor shouldBe child
    }
  }

  "A kinder supervisor" should {
    "not kill children in case it's restarted or escalates failures" in {
      val supervisor = system.actorOf(Props[NoDeathOnRestartSupervisor])
      supervisor ! Props[FussyWordCounter]
      val child = expectMsgType[ActorRef]

      child ! "Akka is cool"
      child ! Report
      expectMsg(3)

      // non-string -> Exception -> Escalate; with this supervisor we override preRestart to not kill children
      EventFilter[Exception]() intercept {
        child ! 45
      }

      // Depending on exact escalation handling, the actor might be restarted and state reset
      child ! Report
      expectMsg(0)
    }
  }

  "An all-for-one supervisor" should {
    "apply the all-for-one strategy" in {
      val supervisor = system.actorOf(Props[AllForOneSupervisor])
      supervisor ! Props[FussyWordCounter]
      val child = expectMsgType[ActorRef]

      supervisor ! Props[FussyWordCounter]
      val secondChild = expectMsgType[ActorRef]

      secondChild ! "Testing supervision"
      secondChild ! Report
      expectMsg(2)

      // empty -> NPE -> Restart in all-for-one -> restarts all siblings
      EventFilter[NullPointerException]() intercept {
        child ! ""
      }

      // After restart, second child state should reset to 0
      secondChild ! Report
      expectMsg(0)
    }
  }
}

object SupervisionSpec {

  case object Report

  class NoDeathOnRestartSupervisor extends Supervisor {
    override def preRestart(reason: Throwable, message: Option[Any]): Unit = {
      // don't stop children
    }
  }

  class AllForOneSupervisor extends Supervisor {
    override val supervisorStrategy: SupervisorStrategy =
      AllForOneStrategy() {
        case _: NullPointerException => Restart
        case _: IllegalArgumentException => Stop
        case _: RuntimeException => Resume
        case _: Exception => Escalate
      }
  }

  class Supervisor extends Actor {

    // IMPORTANT: must be named exactly supervisorStrategy
    override val supervisorStrategy: SupervisorStrategy =
      OneForOneStrategy() {
        case _: NullPointerException => Restart
        case _: IllegalArgumentException => Stop
        case _: RuntimeException => Resume
        case _: Exception => Escalate
      }

    override def receive: Receive = {
      case props: Props =>
        val childRef = context.actorOf(props)
        sender() ! childRef
    }
  }

  class FussyWordCounter extends Actor {
    private var words = 0

    override def receive: Receive = {
      case Report => sender() ! words
      case "" => throw new NullPointerException("sentence is empty")
      case sentence: String =>
        if (sentence.length > 20) throw new RuntimeException("sentence is too big")
        else if (!Character.isUpperCase(sentence(0))) throw new IllegalArgumentException("sentence must start with uppercase")
        else words = sentence.split(" ").length
      case _ => throw new Exception("can only receive string")
    }
  }
}
