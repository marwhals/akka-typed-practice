package actors_part_2

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorSystem, Behavior, PostStop}

/**
 * Notes
 * - Stopping an actor : use "Behaviour.stopped
 * --> Optionally include a handler for a "PostStop signal" ---- like finally block?
 * - Stopping a child: use the actor context
 * ----- "context.stop(childRef) <---- this only works CHILD actors
 */

object SensitiveActor {

  object SensitiveActor {
    def apply(): Behavior[String] = Behaviors.receive[String] { (context, message) =>
      context.log.info(s"Received: $message")
      if (message == "you're ugly") {
        //        Behaviors.stopped(() => context.log.info("I'm stopped now, not receiving other message")) // optionally pass a () => Unit --- to clear up resources after the actor is stopped
        Behaviors.stopped
      } else {
        Behaviors.same
      }
    }.receiveSignal {
      case (context, PostStop) =>
        // clean up resources that this actor might use
        context.log.info("I'm stopping now.")
        Behaviors.same // not used anymore in case of stopping
    }
  }

  def main(args: Array[String]): Unit = {
    val userGuardian = Behaviors.setup[Unit] { context =>
      val sensitiveActor = context.spawn(SensitiveActor(), "sensitiveActor")

      sensitiveActor ! "Hi"
      sensitiveActor ! "How are you"
      sensitiveActor ! "You're ugly"
      sensitiveActor ! "Sorry about that"

      Behaviors.empty
    }

    val system = ActorSystem(userGuardian, "DemoStoppingActor")
    Thread.sleep(1000)
    system.terminate()
  }
}
