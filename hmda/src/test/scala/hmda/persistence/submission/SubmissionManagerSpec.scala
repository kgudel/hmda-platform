package hmda.persistence.submission

import akka.actor
import akka.actor.testkit.typed.scaladsl.TestProbe
import akka.actor.typed.scaladsl.adapter._
import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import akka.cluster.typed.{Cluster, Join}
import hmda.messages.submission.SubmissionCommands.{
  CreateSubmission,
  GetSubmission
}
import hmda.messages.submission.SubmissionEvents.{
  SubmissionCreated,
  SubmissionEvent
}
import hmda.messages.submission.SubmissionManagerCommands._
import hmda.model.filing.submission._
import hmda.model.submission.SubmissionGenerator.submissionGen
import hmda.persistence.AkkaCassandraPersistenceSpec
import hmda.persistence.filing.FilingPersistence
import org.scalatest.{BeforeAndAfterAll, MustMatchers, WordSpec}

import scala.concurrent.duration._

class SubmissionManagerSpec
    extends AkkaCassandraPersistenceSpec
    with MustMatchers
    with BeforeAndAfterAll {
  implicit val system = actor.ActorSystem()
  implicit val typedSystem = system.toTyped

  val sharding = ClusterSharding(typedSystem)

  Cluster(typedSystem).manager ! Join(Cluster(typedSystem).selfMember.address)

  val submissionId = SubmissionId("12345", "2018", 1)

  val submissionProbe = TestProbe[SubmissionEvent]("submission-probe")
  val maybeSubmissionProbe =
    TestProbe[Option[Submission]]("submission-get-probe")

  val sampleSubmission = submissionGen
    .suchThat(s => !s.id.isEmpty)
    .suchThat(s => s.id.lei != "" && s.id.lei != "AA")
    .suchThat(s => s.status == Created)
    .sample
    .getOrElse(Submission(submissionId))

  val uploaded = sampleSubmission.copy(status = Uploaded)

  override def beforeAll(): Unit = {
    super.beforeAll()
    SubmissionManager.startShardRegion(sharding)
    SubmissionPersistence.startShardRegion(ClusterSharding(system.toTyped))
    FilingPersistence.startShardRegion(ClusterSharding(system.toTyped))
  }

  override def afterAll(): Unit = {
    super.afterAll()
    system.terminate()
  }

  "Submission Manager" must {
    "Update submission and filing" in {
      val submissionManager = sharding.entityRefFor(
        SubmissionManager.typeKey,
        s"${SubmissionManager.name}-${sampleSubmission.id.toString}")

      val submissionPersistence =
        sharding.entityRefFor(
          SubmissionPersistence.typeKey,
          s"${SubmissionPersistence.name}-${sampleSubmission.id.toString}")

      submissionPersistence ! CreateSubmission(sampleSubmission.id,
                                               submissionProbe.ref)
      submissionProbe.expectMessageType[SubmissionCreated]

      submissionPersistence ! GetSubmission(maybeSubmissionProbe.ref)
      maybeSubmissionProbe.expectMessageType[Option[Submission]]

      maybeSubmissionProbe.within(0.seconds, 5.seconds) {
        msg: Option[Submission] =>
          msg match {
            case Some(s) => s mustBe sampleSubmission
            case None    => fail
          }
      }
      submissionManager ! UpdateSubmissionStatus(uploaded)
      submissionPersistence ! GetSubmission(maybeSubmissionProbe.ref)
      maybeSubmissionProbe.expectMessageType[Option[Submission]]

      maybeSubmissionProbe.within(0.seconds, 5.seconds) {
        msg: Option[Submission] =>
          msg match {
            case Some(s) => s mustBe uploaded
            case None    => fail
          }
      }
    }
  }
}
