package hmda.messages.submission

import hmda.model.filing.submission.SubmissionId

object HmdaRawDataReplies {

  case class LinesAdded(submissionId: SubmissionId)
  case class LinesAddedFailed(submissionId: SubmissionId)

}