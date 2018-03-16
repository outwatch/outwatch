package outwatch.dom

import cats.effect.Effect
import monix.eval.Task
import monix.execution.Scheduler

object task {
  implicit def dsl(implicit ec: Scheduler): OutwatchDsl[Task] = new OutwatchDsl[Task] {
    implicit val effectF: Effect[Task] = Task.catsEffect(ec)
  }
}
