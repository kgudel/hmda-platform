package hmda.persistence.submission

import akka.actor.typed.scaladsl.Behaviors
import akka.NotUsed
import akka.actor.typed.{ActorRef, Behavior}
import akka.util.ByteString
import scala.concurrent.Future
import akka.cluster.sharding.typed.ShardingEnvelope
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, EntityRef}
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.EventSourcedBehavior.CommandHandler
import hmda.messages.submission.HmdaRawDataCommands.{AddLines, HmdaRawDataCommand, StopRawData}
import hmda.messages.submission.HmdaRawDataReplies.LinesAdded
import hmda.model.filing.submission.SubmissionId
import hmda.actor.HmdaTypedActor
import com.typesafe.config.ConfigFactory
import akka.stream.alpakka.s3.ApiVersion.ListBucketVersion2
import akka.stream.alpakka.s3.scaladsl.S3
import akka.stream.alpakka.s3.{MemoryBufferType, MultipartUploadResult, S3Attributes, S3Settings}
import akka.stream.scaladsl.{Sink, Source}
import software.amazon.awssdk.auth.credentials.{AwsBasicCredentials, StaticCredentialsProvider}
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.regions.providers.AwsRegionProvider
import akka.stream.Materializer
import scala.util.Failure
import scala.util.Success

object HmdaRawData extends HmdaTypedActor[HmdaRawDataCommand]{

  override val name: String = "HmdaRawData"

  override def behavior(entityId: String): Behavior[HmdaRawDataCommand] =
    Behaviors.setup { ctx =>
      val log                   = ctx.log
      implicit val ec           = ctx.system.executionContext
      implicit val system       = ctx.system
      implicit val materializer = Materializer(ctx)
      val config = ConfigFactory.load()
      val accessKeyId  = config.getString("aws.access-key-id")
      val secretAccess = config.getString("aws.secret-access-key ")
      val region       = config.getString("aws.region")
      val bucket       = config.getString("aws.public-bucket")
      val environment  = config.getString("aws.environment")

      val awsCredentialsProvider = StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKeyId, secretAccess))

      val awsRegionProvider: AwsRegionProvider = () => Region.of(region)

      val s3Settings = S3Settings()
          .withBufferType(MemoryBufferType)
          .withCredentialsProvider(awsCredentialsProvider)
          .withS3RegionProvider(awsRegionProvider)
          .withListBucketApiVersion(ListBucketVersion2)

    Behaviors.receiveMessage {
      case AddLines(submissionId, timestamp, data, maybeReplyTo) => {
        val s3Sink: Sink[ByteString, Future[MultipartUploadResult]] =
            S3.multipartUpload(bucket, s"$environment/raw/submissions/${submissionId.period}/${submissionId.lei}/${submissionId.lei}-${submissionId.sequenceNumber}.txt")
              .withAttributes(S3Attributes.settings(s3Settings))
        val byteData = data.map(s => ByteString.fromString(s))
        val dataSource: Source[ByteString, NotUsed] = Source(byteData.toList)
        val result = dataSource.runWith(s3Sink)
        result.onComplete {
          case Failure(e) =>
            log.error(s"S3 Upload failed submissionId", e)
            maybeReplyTo match {
              case Some(replyTo) => replyTo ! LinesAdded(submissionId)
              case None => //Do Nothing
            }
          case Success(_) => {
            log.info(s"S3 Upload complete for $submissionId")
            maybeReplyTo match {
              case Some(replyTo) => replyTo ! LinesAdded(submissionId)
              case None => //Do Nothing
            }
          }
        }
        Behaviors.same
      }
      case StopRawData =>
        Behaviors.stopped

    }

  }

  def startShardRegion(sharding: ClusterSharding): ActorRef[ShardingEnvelope[HmdaRawDataCommand]] =
    super.startShardRegion(sharding, StopRawData)

  def selectHmdaRawData(sharding: ClusterSharding, submissionId: SubmissionId): EntityRef[HmdaRawDataCommand] =
    sharding.entityRefFor(HmdaRawData.typeKey, s"${HmdaRawData.name}-${submissionId.toString}")

}
