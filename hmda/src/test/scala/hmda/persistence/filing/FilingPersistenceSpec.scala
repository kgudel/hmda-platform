package hmda.persistence.filing

import akka.actor
import akka.actor.testkit.typed.scaladsl.TestProbe
import akka.actor.typed.scaladsl.adapter._
import hmda.messages.filing.FilingCommands._
import hmda.messages.filing.FilingEvents.FilingCreated
import hmda.model.filing.{Filing, FilingDetails}
import hmda.persistence.AkkaCassandraPersistenceSpec
import hmda.model.filing.FilingGenerator._
import hmda.model.filing.submission.{Submission, SubmissionId}
import hmda.model.submission.SubmissionGenerator._

class FilingPersistenceSpec extends AkkaCassandraPersistenceSpec {
  override implicit val system = actor.ActorSystem()
  override implicit val typedSystem = system.toTyped

  val filingCreatedProbe = TestProbe[FilingCreated]("filing-created-probe")
  val maybeFilingProbe = TestProbe[Option[Filing]]("maybe-filing-probe")
  val filingDetailsProbe = TestProbe[FilingDetails]("filing-details-probe")
  val maybeSubmissionProbe =
    TestProbe[Option[Submission]]("maybe-submission-probe")
  val submissionProbe = TestProbe[Submission]("submission-probe")
  val submissionsProbe = TestProbe[List[Submission]](name = "submissions-probe")

  val sampleFiling = filingGen
    .suchThat(_.lei != "")
    .suchThat(_.period != "")
    .sample
    .getOrElse(Filing())

  val sampleSubmission = submissionGen
    .suchThat(s => !s.id.isEmpty)
    .suchThat(s => s.id.lei != "" && s.id.lei != "AA")
    .sample
    .getOrElse(Submission(SubmissionId("12345", "2018", 1)))

  "Filings" must {
    "be created and read back" in {
      val filingPersistence = system.spawn(
        FilingPersistence.behavior(sampleFiling.lei, sampleFiling.period),
        actorName)

      filingPersistence ! GetFiling(maybeFilingProbe.ref)
      maybeFilingProbe.expectMessage(None)

      filingPersistence ! GetFilingDetails(filingDetailsProbe.ref)
      filingDetailsProbe.expectMessage(FilingDetails())

      filingPersistence ! CreateFiling(sampleFiling, filingCreatedProbe.ref)
      filingCreatedProbe.expectMessage(FilingCreated(sampleFiling))

      filingPersistence ! GetFiling(maybeFilingProbe.ref)
      maybeFilingProbe.expectMessage(Some(sampleFiling))
    }
    "create submissions and read them back" in {
      val filingPersistence = system.spawn(
        FilingPersistence.behavior(sampleFiling.lei, sampleFiling.period),
        actorName)

      filingPersistence ! CreateFiling(sampleFiling, filingCreatedProbe.ref)
      filingCreatedProbe.expectMessage(FilingCreated(sampleFiling))

      filingPersistence ! GetLatestSubmission(maybeSubmissionProbe.ref)
      maybeSubmissionProbe.expectMessage(None)

      filingPersistence ! AddSubmission(sampleSubmission, submissionProbe.ref)
      submissionProbe.expectMessage(sampleSubmission)

      filingPersistence ! GetLatestSubmission(maybeSubmissionProbe.ref)
      maybeSubmissionProbe.expectMessage(Some(sampleSubmission))

      val sampleSubmission2 =
        sampleSubmission.copy(id = sampleSubmission.id.copy(lei = "AAA"))
      filingPersistence ! AddSubmission(sampleSubmission2, submissionProbe.ref)
      submissionProbe.expectMessage(sampleSubmission2)

      filingPersistence ! GetSubmissions(submissionsProbe.ref)
      submissionsProbe.expectMessage(List(sampleSubmission2, sampleSubmission))

      filingPersistence ! GetLatestSubmission(maybeSubmissionProbe.ref)
      maybeSubmissionProbe.expectMessage(Some(sampleSubmission2))

      filingPersistence ! GetFilingDetails(filingDetailsProbe.ref)
      filingDetailsProbe.expectMessage(
        FilingDetails(sampleFiling, List(sampleSubmission2, sampleSubmission)))
    }
  }
}
