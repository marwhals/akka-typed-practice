package actors_part_2

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorSystem, Behavior, SupervisorStrategy, Terminated}

/**
 * Notes on supervisor actors
 *
 * Can decide on what to do with an actor in case of failure
 * --- Stop it (default)
 * --- Resume it: failure ignored, actor state preserved
 * --- restart it: actor state reset
 *
 * Tips
 * - handle multiple exception types be nesting Behaviours.supervise
 * - place the most specific exceptions in the inner handlers and the most general exception in the outer handlers
 *
 */

object Supervision {

  object FussyWordCounter {

    def apply(): Behavior[String] = active()

    def active(total: Int = 0): Behavior[String] = Behaviors.receive { (context, message) =>
      val wordCount = message.split(" ").length
      context.log.info(s"Received piece of text: '$message', counted $wordCount words, total: ${total + wordCount}'")
      // throw some exceptions (maybe unintentionally)
      if (message.startsWith("Q")) throw new RuntimeException("I HATE queues!")
      if (message.startsWith("W")) throw new NullPointerException()
      active(total + wordCount)
    }
  }

  // actor throwing exception get killed
  def demoCrash(): Unit = {
    val guardian: Behavior[Unit] = Behaviors.setup { context =>
      val fussyCounter = context.spawn(FussyWordCounter(), "fussyCounter")

      fussyCounter ! "Starting to understand this Akka business........"
      fussyCounter ! "Quick!, Hide!"
      fussyCounter ! "Are you there?"

      Behaviors.empty
    }

    val system = ActorSystem(guardian, "DemoCrash")
    Thread.sleep(1000)
    system.terminate()
  }

  def demoWithParent(): Unit = {
    val parentBehaviour: Behavior[String] = Behaviors.setup { context =>
      val child = context.spawn(FussyWordCounter(), "fussyChild")
      context.watch(child)

      Behaviors.receiveMessage[String] { message =>
        child ! message
        Behaviors.same
      }.receiveSignal {
        case (context, Terminated(childRef)) =>
          context.log.warn(s"Child failed: ${childRef.path.name}")
          Behaviors.same
      }
    }

    val guardian: Behavior[Unit] = Behaviors.setup { context =>
      val fussyCounter = context.spawn(parentBehaviour, "fussyCounter")

      fussyCounter ! "Starting to understand this Akka business......"
      fussyCounter ! "Quick!, Hide!"
      fussyCounter ! "Are you there?"

      Behaviors.empty
    }

    val system = ActorSystem(guardian, "DemoCrashWithParent")
    Thread.sleep(1000)
    system.terminate()
  }

  def demoSupervisionWithRestart(): Unit = {
    val parentBehaviour: Behavior[String] = Behaviors.setup { context =>
      // supervise the child with a restart "strategy"
      // This is handled from the "inside -> out"
      val childBehaviour = Behaviors.supervise(
        Behaviors.supervise(FussyWordCounter())
        .onFailure[RuntimeException](SupervisorStrategy.restart) //// this is called first
      ).onFailure[NullPointerException](SupervisorStrategy.resume) // see docs

      val child = context.spawn(childBehaviour, "fussyChild")
      context.watch(child)

      Behaviors.receiveMessage[String] { message =>
        child ! message
        Behaviors.same
      }.receiveSignal {
        case (context, Terminated(childRef)) =>
          context.log.warn(s"Child failed: ${childRef.path.name}")
          Behaviors.same
      }
    }

    val guardian: Behavior[Unit] = Behaviors.setup { context =>
      val fussyCounter = context.spawn(parentBehaviour, "fussyCounter")

      fussyCounter ! "Starting to understand this Akka business......"
      fussyCounter ! "Quick!, Hide!"
      fussyCounter ! "Are you there?"
      fussyCounter ! "What are you doing?"
      fussyCounter ! "Are you still there?"

      Behaviors.empty
    }

    val system = ActorSystem(guardian, "DemoCrashWithParent")
    Thread.sleep(1000)
    system.terminate()

  }

  /**
   * Exercise: Consider how we specify different supervisor strategies for different exception types
   *
   */

  val differentStrategies = Behaviors.supervise(
    Behaviors.supervise(FussyWordCounter())
      .onFailure[NullPointerException](SupervisorStrategy.resume)
  ).onFailure[IndexOutOfBoundsException](SupervisorStrategy.restart)


  /**
   * OO Equivalent:
   * try { .. }
   * catch {
   *  case NullPointerException =>
   *  case IndexOutOfBoundsException =>
   * }
   *
   *
   * Take away point: specific expcetions on the inside and most general exceptions on the outside
   *
   *
   */

  def main(args: Array[String]): Unit = {
    //    demoCrash()
    demoWithParent()
  }
}
