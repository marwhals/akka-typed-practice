package actors_part_2

import akka.actor.TypedActor.context
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.actor.typed.scaladsl.Behaviors

import scala.collection.mutable.{Map => MutableMap}

/**
 * A common source of bugs:
 * Very Important! ---- Never pass immutable state to other actors
 * Very Important! ---- Never pass the context reference to other actors
 * This is the same with Futures
 *
 * This is breaking actor encapsulation
 */

object BreakingActorEncapsulation {

  // naive bank account
  trait AccountCommand

  case class Deposit(cardId: String, amount: Double) extends AccountCommand

  case class Withdraw(cardId: String, amount: Double) extends AccountCommand

  case class CreateCreditCard(cardId: String) extends AccountCommand

  case object CheckCardStatuses extends AccountCommand

  trait CreditCardCommand

  case class AttachToAccount(
                              balances: MutableMap[String, Double],
                              cards: MutableMap[String, ActorRef[CreditCardCommand]])
    extends CreditCardCommand

  case object CheckStatus extends CreditCardCommand

  object NaiveBankAccount {
    def apply(): Behavior[AccountCommand] = Behaviors.setup { context =>
      val accountBalances: MutableMap[String, Double] = MutableMap()
      val cardMap: MutableMap[String, ActorRef[CreditCardCommand]] = MutableMap()

      Behaviors.receiveMessage { message =>
        message match {
          case CreateCreditCard(cardId) =>
            context.log.info(s"Creating card $cardId")
            // create a CreditCard child
            val creditCardRef = context.spawn(CreditCard(cardId), cardId)
            /** bug: say you want to give customers a referral bonus*/
            accountBalances += cardId -> 10
            // send an AttachToAccount message to the child
            creditCardRef ! AttachToAccount(accountBalances, cardMap)
            // change behaviour
            Behaviors.same
          case Deposit(cardId, amount) =>
            val oldBalance: Double = accountBalances.getOrElse(cardId, 0)
            context.log.info(s"Depositing $amount via card $cardId, balance on card: ${oldBalance + amount}")
            accountBalances += cardId -> (oldBalance + amount)
            Behaviors.same
          case Withdraw(cardId, amount) =>
            val oldBalance: Double = accountBalances.getOrElse(cardId, 0)
            if (oldBalance < amount) {
              context.log.warn(s"Attempted withdrawal of $amount via card $cardId: insufficient funds")
              Behaviors.same
            } else {
              context.log.info(s"Withdrawing $amount via car $cardId, balance on card: ${oldBalance - amount}")
              accountBalances += cardId -> (oldBalance - amount)
              Behaviors.same
            }
          case CheckCardStatuses =>
            context.log.info(s"Checking all card statuses")
            cardMap.values.foreach(cardRef => cardRef ! CheckStatus)
            Behaviors.same
        }
      }
    }
  }

  object CreditCard {
    def apply(cardId: String): Behavior[CreditCardCommand] = Behaviors.receive { (context, message) =>
      message match {
        case AttachToAccount(balances, cards) =>
          context.log.info(s"[$cardId] Attaching to bank account")
          balances += cardId -> 0
          cards += cardId -> context.self
          Behaviors.same
        case CheckStatus =>
          context.log.info(s"[$cardId] All things green.")
          Behaviors.same
      }
    }
  }

  def main(args: Array[String]): Unit = {
    val userGuardian: Behavior[Unit] = Behaviors.setup { context =>
      val bankAccount = context.spawn(NaiveBankAccount(), "bankAccount")

      bankAccount ! CreateCreditCard("gold")
      bankAccount ! CreateCreditCard("premium")
      bankAccount ! Deposit("gold", 1000)
      bankAccount ! CheckCardStatuses

      Behaviors.empty
    }

    val system = ActorSystem(userGuardian, "DemoNaiveBankAccount")
    Thread.sleep(1000)
    system.terminate()
  }

}
