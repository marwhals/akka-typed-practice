package actors_part_2

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorSystem, Behavior}

object ActorsIntro {

  /**
   * // behaviour
   * val simpleActorBehaviour: Behavior[String] = Behaviors.receiveMessage{(message: String) =>
   * // do something with the message
   * println(s"[simple actor] I have received: $message")
   *
   * // new behaviour for the next message
   * Behaviors.same
   * }
   * */

  def demoSimpleActor(): Unit = {
    // part 2: Instantiate
    val actorSystem = ActorSystem(SimpleActor(), "FirstActorSystem")

    // part 3: communicate!
    actorSystem ! "I am learning Akka" // asynchronously send a message
    // ! = the "tell" method

    // part 4: gracefully shut down
    Thread.sleep(1000)
    actorSystem.terminate()
  }

  object SimpleActor {
    def apply(): Behavior[String] = Behaviors.receiveMessage { (message: String) =>
      // do something with the message
      println(s"[simple actor] I have recieved: $message")

      // new behaviour for the next message
      Behaviors.same

    }
  }

  object SimpleActor_V2 {
    def apply(): Behavior[String] = Behaviors.receive { (context, message) =>
      // context is a data structure (ActorContext) with access to a variety of APIs
      // simple example: logging
      context.log.info(s"[simple actor] I have received: $message")
      Behaviors.same
    }
  }

  object SimpleActor_V3 {
    def apply(): Behavior[String] = Behaviors.setup { context =>
      // actor "private" data and methods, behaviours etc
      // Your code here

      // behaviour used for the first message
      Behaviors.receiveMessage { message =>
        context.log.info(s"[simple actor] I have recieved: $message")
        Behaviors.same
      }
    }
  }

  def main(args: Array[String]): Unit = {
    demoSimpleActor()
  }

}
