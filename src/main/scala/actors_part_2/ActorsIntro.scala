package actors_part_2

import actors_part_2.ActorsIntro.BetterActor.{IntMessage, StringMessage}
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorSystem, Behavior}

/**
 * Actors
 * - Actors are defined in terms of their behaviour
 *
 * Ways of building behaviours:
 * - receiveMessage
 * - receive
 * - setup
 *
 * Tips:
 * - build behaviours in factory methods of objects, e.g. apply()
 * - Never use `Behaviour[Any]` because you will lose type safety
 */

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

  /**
   * Exercises
   * 1) Define two "person" actor behaviours, which receive Strings:
   * - "happy", which logs your message, e.g. "I've received ____. That's great!"
   * - "sad", ... "That sucks."
   * Test both.
   *
   * 2) Change the actor behaviour:
   * - the happy behaviour will turn to sad() if it receives "Akka is bad."
   * - the sad behaviour will turn to happy() if it receives "Akka is awesome!"
   *
   * 3) Inspect the code and try and make it better
   */

  object Person {
    def happy(): Behavior[String] = Behaviors.receive { (context, message) =>
      message match {
        case "Akka is bad." =>
          context.log.info("Don't you say anything bad about Akka!!!!")
          sad()
        case _ =>
          context.log.info(s"I've received '$message'. That's great!'")
          Behaviors.same
      }
    }

    def sad(): Behavior[String] = Behaviors.receive { (context, message) =>
      message match {
        case "Akka is awesome!" =>
          context.log.info("Happy now!")
          happy()
        case _ =>
          context.log.info(s"I've received '$message'. That sucks!'")
          Behaviors.same
      }
    }

    def apply(): Behavior[String] = happy()
  }

  def testPerson(): Unit = {
    val person = ActorSystem(Person(), "PersonTest")

    person ! "I love the color blue"
    person ! "Akka is bad"
    person ! "I also love the color red"
    person ! "Akka is awesome!"
    person ! "Akka is great"

    Thread.sleep(1000)
    person.terminate()
  }

  object WeridActor {
    // wants to receive message of type Int AND String
    def apply(): Behavior[Any] = Behaviors.receive { (context, message) => // this is bad. Create type hierarchy
      message match {
        case number: Int =>
          context.log.info(s"I've received an int: $number")
          Behaviors.same
        case string: String =>
          context.log.info(s"I've received a String: $string")
          Behaviors.same
      }
    }
  }

  // solution: add wrapper types and type hierarchy via case classes and objects
  object BetterActor {
    trait Message
    case class IntMessage(number: Int) extends Message
    case class StringMessage(string: String) extends Message

    def apply(): Behavior[Message] = Behaviors.receive { (context, message) =>
      message match {
        case IntMessage(number) =>
          context.log.info(s"I've received an int: $number")
          Behaviors.same
        case StringMessage(string) =>
          context.log.info(s"I've received a String: $string")
          Behaviors.same
      }
    }
  }


  def demoWeridActor(): Unit = {
//    val weridActor = ActorSystem(WeridActor(), "WeridActorDemo")
    val weridActor = ActorSystem(BetterActor(), "WeridActorDemo") //---- cause type mismatch (this is the intention)
//    weridActor ! 43 // ok
//    weridActor ! "Akka" // ok
    weridActor ! IntMessage(23)
    weridActor ! StringMessage("Akka")
//    weridActor ! '\t' // not ok

    Thread.sleep(1000)
    weridActor.terminate()
  }

  def main(args: Array[String]): Unit = {
    demoWeridActor()
  }

}