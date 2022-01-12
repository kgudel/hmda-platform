package hmda.publisher.query.component

import hmda.model.publication.Msa
import hmda.publisher.helper.PGTableNameLoader
import hmda.publisher.qa.{QAEntity, QARepository, QATableBase}
import hmda.publisher.query.lar._
import hmda.publisher.query.panel.{InstitutionAltEntity, InstitutionEntity}
import hmda.publisher.validation.{LarData, TsData}
import hmda.query.DbConfiguration._
import hmda.query.repository.TableRepository
import hmda.query.ts.TransmittalSheetEntity
import slick.basic.{DatabaseConfig, DatabasePublisher}
import slick.jdbc.{JdbcProfile, ResultSetConcurrency, ResultSetType}

import java.sql.Timestamp
import scala.concurrent.{ExecutionContext, Future}

// $COVERAGE-OFF$
trait PublisherComponent2022 extends PGTableNameLoader {

  import dbConfig.profile.api._

  sealed trait Year2022Period
  object Year2022Period {
    case object Whole extends Year2022Period
    case object Q1    extends Year2022Period
    case object Q2    extends Year2022Period
    case object Q3    extends Year2022Period
  }

  abstract class InstitutionsTableBase[T](tag: Tag, tableName: String) extends Table[T](tag, tableName) {
    def lei             = column[String]("lei", O.PrimaryKey)
    def activityYear    = column[Int]("activity_year")
    def agency          = column[Int]("agency")
    def institutionType = column[Int]("institution_type")
    def id2017          = column[String]("id2017")
    def taxId           = column[String]("tax_id")
    def rssd            = column[Int]("rssd")
    def respondentName  = column[String]("respondent_name")
    def respondentState = column[String]("respondent_state")
    def respondentCity  = column[String]("respondent_city")
    def parentIdRssd    = column[Int]("parent_id_rssd")
    def parentName      = column[String]("parent_name")
    def assets          = column[Long]("assets")
    def otherLenderCode = column[Int]("other_lender_code")
    def topHolderIdRssd = column[Int]("topholder_id_rssd")
    def topHolderName   = column[String]("topholder_name")
    def hmdaFiler       = column[Boolean]("hmda_filer")
  }
  class InstitutionsTable(tag: Tag) extends InstitutionsTableBase[InstitutionEntity](tag, panel2022TableName) {
    override def * =
      (
        lei,
        activityYear,
        agency,
        institutionType,
        id2017,
        taxId,
        rssd,
        respondentName,
        respondentState,
        respondentCity,
        parentIdRssd,
        parentName,
        assets,
        otherLenderCode,
        topHolderIdRssd,
        topHolderName,
        hmdaFiler
      ) <> (InstitutionEntity.tupled, InstitutionEntity.unapply)
  }
  val institutionsTable2022 = TableQuery[InstitutionsTable]

  class QAInstitutionsTable(tag: Tag)
    extends InstitutionsTableBase[QAEntity[InstitutionAltEntity]](tag, panel2022QATableName)
      with QATableBase[InstitutionAltEntity] {
    def emailDomains = column[String]("email_domains")
    def institutionAltEntityProjection =
      (
        lei,
        activityYear,
        agency,
        institutionType,
        id2017,
        taxId,
        rssd,
        respondentName,
        respondentState,
        respondentCity,
        parentIdRssd,
        parentName,
        assets,
        otherLenderCode,
        topHolderIdRssd,
        topHolderName,
        hmdaFiler,
        emailDomains
      ) <> ((InstitutionAltEntity.apply _).tupled, InstitutionAltEntity.unapply)
    def * =
      (institutionAltEntityProjection, fileName,timeStamp) <> ((QAEntity.apply[InstitutionAltEntity] _).tupled, QAEntity
        .unapply[InstitutionAltEntity] _)
  }

  def createQaPanelRepository2022(config: DatabaseConfig[JdbcProfile])(implicit ec: ExecutionContext) =
    new QARepository.Default[InstitutionAltEntity, QAInstitutionsTable](
      config,
      TableQuery(tag => new QAInstitutionsTable(tag))
    )(ec)

  class InstitutionRepository2022(val config: DatabaseConfig[JdbcProfile]) extends TableRepository[InstitutionsTable, String] {

    override val table: config.profile.api.TableQuery[InstitutionsTable] =
      institutionsTable2022

    override def getId(row: InstitutionsTable): config.profile.api.Rep[Id] =
      row.lei

    def createSchema() = db.run(table.schema.create)
    def dropSchema()   = db.run(table.schema.drop)

    def insert(institution: InstitutionEntity): Future[Int] =
      db.run(table += institution)

    def findByLei(lei: String): Future[Seq[InstitutionEntity]] =
      db.run(table.filter(_.lei === lei).result)

    //(x => (x.isX && x.name == "xyz"))
    def findActiveFilers(bankIgnoreList: Array[String]): Future[Seq[InstitutionEntity]] =
      db.run(
        table
          .filter(_.hmdaFiler === true)
          .filterNot(_.lei.toUpperCase inSet bankIgnoreList)
          .result
      )

    def getAllInstitutions(): Future[Seq[InstitutionEntity]] =
      db.run(table.result)

    def deleteByLei(lei: String): Future[Int] =
      db.run(table.filter(_.lei === lei).delete)

    def count(): Future[Int] =
      db.run(table.size.result)
  }

  abstract class TransmittalSheetTableBase[T](tag: Tag, tableName: String) extends Table[T](tag, tableName) {

    def lei             = column[String]("lei", O.PrimaryKey)
    def id              = column[Int]("id")
    def institutionName = column[String]("institution_name")
    def year            = column[Int]("year")
    def quarter         = column[Int]("quarter")
    def name            = column[String]("name")
    def phone           = column[String]("phone")
    def email           = column[String]("email")
    def street          = column[String]("street")
    def city            = column[String]("city")
    def state           = column[String]("state")
    def zipCode         = column[String]("zip_code")
    def agency          = column[Int]("agency")
    def totalLines      = column[Int]("total_lines")
    def taxId           = column[String]("tax_id")
    def submissionId    = column[Option[String]]("submission_id")
    def createdAt       = column[Option[Timestamp]]("created_at")
    def isQuarterly     = column[Option[Boolean]]("is_quarterly")
    def signDate        = column[Option[Long]]("sign_date")

    def transmittalSheetEntityProjection =
      (
        lei,
        id,
        institutionName,
        year,
        quarter,
        name,
        phone,
        email,
        street,
        city,
        state,
        zipCode,
        agency,
        totalLines,
        taxId,
        submissionId,
        createdAt,
        isQuarterly,
        signDate
      ) <> ((TransmittalSheetEntity.apply _).tupled, TransmittalSheetEntity.unapply)
  }

  class RealTransmittalSheetTable2022(tag: Tag, tableName: String) extends TransmittalSheetTableBase[TransmittalSheetEntity](tag, tableName) {
    override def * = transmittalSheetEntityProjection
  }

  class QATransmittalSheetTable(tag: Tag, tableName: String)
    extends TransmittalSheetTableBase[QAEntity[TransmittalSheetEntity]](tag, tableName)
      with QATableBase[TransmittalSheetEntity] {
    def * = (transmittalSheetEntityProjection, fileName,timeStamp) <> ((QAEntity.apply[TransmittalSheetEntity] _).tupled, QAEntity.unapply[TransmittalSheetEntity] _)
  }

  def transmittalSheetTableQuery2022(p: Year2022Period): TableQuery[RealTransmittalSheetTable2022] = {
    val tableName = p match {
      case Year2022Period.Whole => ts2022TableName
      case Year2022Period.Q1    => ts2022Q1TableName
      case Year2022Period.Q2    => ts2022Q2TableName
      case Year2022Period.Q3    => ts2022Q3TableName
    }
    TableQuery(tag => new RealTransmittalSheetTable2022(tag, tableName))
  }

  def qaTransmittalSheetTableQuery2022(p: Year2022Period): TableQuery[QATransmittalSheetTable] = {
    val tableName = p match {
      case Year2022Period.Whole => ts2022QATableName
      case Year2022Period.Q1    => ts2022Q1QATableName
      case Year2022Period.Q2    => ts2022Q2QATableName
      case Year2022Period.Q3    => ts2022Q3QATableName
    }
    TableQuery(tag => new QATransmittalSheetTable(tag, tableName))
  }

  class TSRepository2022Base[TsTable <: RealTransmittalSheetTable2022](val config: DatabaseConfig[JdbcProfile], val table: TableQuery[TsTable])
    extends TableRepository[TsTable, String] {

    override def getId(row: TsTable): config.profile.api.Rep[Id] =
      row.lei

    def createSchema() = db.run(table.schema.create)
    def dropSchema()   = db.run(table.schema.drop)

    def insert(ts: TransmittalSheetEntity): Future[Int] =
      db.run(table += ts)

    def findByLei(lei: String): Future[Seq[TransmittalSheetEntity]] =
      db.run(table.filter(_.lei === lei).result)

    def deleteByLei(lei: String): Future[Int] =
      db.run(table.filter(_.lei === lei).delete)

    def count(): Future[Int] =
      db.run(table.size.result)

    def getAllSheets(bankIgnoreList: Array[String]): Future[Seq[TransmittalSheetEntity]] =
      db.run(table.filterNot(_.lei.toUpperCase inSet bankIgnoreList).result)

  }

  abstract class LarTableBase[T](tag: Tag, tableName: String) extends Table[T](tag, tableName) {

    def id                         = column[Int]("id")
    def lei                        = column[String]("lei")
    def uli                        = column[String]("uli")
    def applicationDate            = column[String]("application_date")
    def loanType                   = column[Int]("loan_type")
    def loanPurpose                = column[Int]("loan_purpose")
    def preapproval                = column[Int]("preapproval")
    def constructionMethod         = column[Int]("construction_method")
    def occupancyType              = column[Int]("occupancy_type")
    def loanAmount                 = column[Double]("loan_amount")
    def actionTakenType            = column[Int]("action_taken_type")
    def actionTakenDate            = column[Int]("action_taken_date")
    def street                     = column[String]("street")
    def city                       = column[String]("city")
    def state                      = column[String]("state")
    def zip                        = column[String]("zip")
    def county                     = column[String]("county")
    def tract                      = column[String]("tract")
    def ethnicityApplicant1        = column[String]("ethnicity_applicant_1")
    def ethnicityApplicant2        = column[String]("ethnicity_applicant_2")
    def ethnicityApplicant3        = column[String]("ethnicity_applicant_3")
    def ethnicityApplicant4        = column[String]("ethnicity_applicant_4")
    def ethnicityApplicant5        = column[String]("ethnicity_applicant_5")
    def otherHispanicApplicant     = column[String]("other_hispanic_applicant")
    def ethnicityCoApplicant1      = column[String]("ethnicity_co_applicant_1")
    def ethnicityCoApplicant2      = column[String]("ethnicity_co_applicant_2")
    def ethnicityCoApplicant3      = column[String]("ethnicity_co_applicant_3")
    def ethnicityCoApplicant4      = column[String]("ethnicity_co_applicant_4")
    def ethnicityCoApplicant5      = column[String]("ethnicity_co_applicant_5")
    def otherHispanicCoApplicant   = column[String]("other_hispanic_co_applicant")
    def ethnicityObservedApplicant = column[Int]("ethnicity_observed_applicant")
    def ethnicityObservedCoApplicant =
      column[Int]("ethnicity_observed_co_applicant")
    def raceApplicant1           = column[String]("race_applicant_1")
    def raceApplicant2           = column[String]("race_applicant_2")
    def raceApplicant3           = column[String]("race_applicant_3")
    def raceApplicant4           = column[String]("race_applicant_4")
    def raceApplicant5           = column[String]("race_applicant_5")
    def otherNativeRaceApplicant = column[String]("other_native_race_applicant")
    def otherAsianRaceApplicant  = column[String]("other_asian_race_applicant")
    def otherPacificRaceApplicant =
      column[String]("other_pacific_race_applicant")
    def raceCoApplicant1 = column[String]("race_co_applicant_1")
    def raceCoApplicant2 = column[String]("race_co_applicant_2")
    def raceCoApplicant3 = column[String]("race_co_applicant_3")
    def raceCoApplicant4 = column[String]("race_co_applicant_4")
    def raceCoApplicant5 = column[String]("race_co_applicant_5")
    def otherNativeRaceCoApplicant =
      column[String]("other_native_race_co_applicant")
    def otherAsianRaceCoApplicant =
      column[String]("other_asian_race_co_applicant")
    def otherPacificRaceCoApplicant =
      column[String]("other_pacific_race_co_applicant")
    def raceObservedApplicant    = column[Int]("race_observed_applicant")
    def raceObservedCoApplicant  = column[Int]("race_observed_co_applicant")
    def sexApplicant             = column[Int]("sex_applicant")
    def sexCoApplicant           = column[Int]("sex_co_applicant")
    def observedSexApplicant     = column[Int]("observed_sex_applicant")
    def observedSexCoApplicant   = column[Int]("observed_sex_co_applicant")
    def ageApplicant             = column[Int]("age_applicant")
    def ageCoApplicant           = column[Int]("age_co_applicant")
    def income                   = column[String]("income")
    def purchaserType            = column[Int]("purchaser_type")
    def rateSpread               = column[String]("rate_spread")
    def hoepaStatus              = column[Int]("hoepa_status")
    def lienStatus               = column[Int]("lien_status")
    def creditScoreApplicant     = column[Int]("credit_score_applicant")
    def creditScoreCoApplicant   = column[Int]("credit_score_co_applicant")
    def creditScoreTypeApplicant = column[Int]("credit_score_type_applicant")
    def creditScoreModelApplicant =
      column[String]("credit_score_model_applicant")
    def creditScoreTypeCoApplicant =
      column[Int]("credit_score_type_co_applicant")
    def creditScoreModelCoApplicant =
      column[String]("credit_score_model_co_applicant")
    def denialReason1           = column[String]("denial_reason1")
    def denialReason2           = column[String]("denial_reason2")
    def denialReason3           = column[String]("denial_reason3")
    def denialReason4           = column[String]("denial_reason4")
    def otherDenialReason       = column[String]("other_denial_reason")
    def totalLoanCosts          = column[String]("total_loan_costs")
    def totalPoints             = column[String]("total_points")
    def originationCharges      = column[String]("origination_charges")
    def discountPoints          = column[String]("discount_points")
    def lenderCredits           = column[String]("lender_credits")
    def interestRate            = column[String]("interest_rate")
    def paymentPenalty          = column[String]("payment_penalty")
    def debtToIncome            = column[String]("debt_to_incode")
    def loanValueRatio          = column[String]("loan_value_ratio")
    def loanTerm                = column[String]("loan_term")
    def rateSpreadIntro         = column[String]("rate_spread_intro")
    def baloonPayment           = column[Int]("baloon_payment")
    def insertOnlyPayment       = column[Int]("insert_only_payment")
    def amortization            = column[Int]("amortization")
    def otherAmortization       = column[Int]("other_amortization")
    def propertyValue           = column[String]("property_value")
    def homeSecurityPolicy      = column[Int]("home_security_policy")
    def landPropertyInterest    = column[Int]("lan_property_interest")
    def totalUnits              = column[Int]("total_uits")
    def mfAffordable            = column[String]("mf_affordable")
    def applicationSubmission   = column[Int]("application_submission")
    def payable                 = column[Int]("payable")
    def nmls                    = column[String]("nmls")
    def aus1                    = column[String]("aus1")
    def aus2                    = column[String]("aus2")
    def aus3                    = column[String]("aus3")
    def aus4                    = column[String]("aus4")
    def aus5                    = column[String]("aus5")
    def otheraus                = column[String]("other_aus")
    def aus1Result              = column[Int]("aus1_result")
    def aus2Result              = column[String]("aus2_result")
    def aus3Result              = column[String]("aus3_result")
    def aus4Result              = column[String]("aus4_result")
    def aus5Result              = column[String]("aus5_result")
    def otherAusResult          = column[String]("other_aus_result")
    def reverseMortgage         = column[Int]("reverse_mortgage")
    def lineOfCredits           = column[Int]("line_of_credits")
    def businessOrCommercial    = column[Int]("business_or_commercial")
    def conformingLoanLimit     = column[String]("conforming_loan_limit")
    def ethnicityCategorization = column[String]("ethnicity_categorization")
    def raceCategorization      = column[String]("race_categorization")
    def sexCategorization       = column[String]("sex_categorization")
    def dwellingCategorization  = column[String]("dwelling_categorization")
    def loanProductTypeCategorization =
      column[String]("loan_product_type_categorization")
    def tractPopulation = column[Int]("tract_population")
    def tractMinorityPopulationPercent =
      column[Double]("tract_minority_population_percent")
    def tractMedianIncome  = column[Int]("ffiec_msa_md_median_family_income")
    def tractOccupiedUnits = column[Int]("tract_owner_occupied_units")
    def tractOneToFourFamilyUnits =
      column[Int]("tract_one_to_four_family_homes")
    def tractMedianAge = column[Int]("tract_median_age_of_housing_units")
    def tractToMsaIncomePercent =
      column[Double]("tract_to_msa_income_percentage")

    // TODO: This is not actually used in the projection so creating a schema does not actually pick this field up
    // so don't use create Schema
    def isQuarterly = column[Option[Boolean]]("is_quarterly")

    def larEntityImpl2022Projection =
      (
        larPartOneProjection,
        larPartTwoProjection,
        larPartThreeProjection,
        larPartFourProjection,
        larPartFiveProjection,
        larPartSixProjection,
        larPartSevenProjection
      ) <> ((LarEntityImpl2022.apply _).tupled, LarEntityImpl2022.unapply)

    def larPartOneProjection =
      (
        id,
        lei,
        uli,
        applicationDate,
        loanType,
        loanPurpose,
        preapproval,
        constructionMethod,
        occupancyType,
        loanAmount,
        actionTakenType,
        actionTakenDate,
        street,
        city,
        state,
        zip,
        county,
        tract
      ) <> ((LarPartOne2022.apply _).tupled, LarPartOne2022.unapply)

    def larPartTwoProjection =
      (
        ethnicityApplicant1,
        ethnicityApplicant2,
        ethnicityApplicant3,
        ethnicityApplicant4,
        ethnicityApplicant5,
        otherHispanicApplicant,
        ethnicityCoApplicant1,
        ethnicityCoApplicant2,
        ethnicityCoApplicant3,
        ethnicityCoApplicant4,
        ethnicityCoApplicant5,
        otherHispanicCoApplicant,
        ethnicityObservedApplicant,
        ethnicityObservedCoApplicant,
        raceApplicant1,
        raceApplicant2,
        raceApplicant3,
        raceApplicant4,
        raceApplicant5
      ) <> ((LarPartTwo2022.apply _).tupled, LarPartTwo2022.unapply)

    def larPartThreeProjection =
      (
        otherNativeRaceApplicant,
        otherAsianRaceApplicant,
        otherPacificRaceApplicant,
        raceCoApplicant1,
        raceCoApplicant2,
        raceCoApplicant3,
        raceCoApplicant4,
        raceCoApplicant5,
        otherNativeRaceCoApplicant,
        otherAsianRaceCoApplicant,
        otherPacificRaceCoApplicant,
        raceObservedApplicant,
        raceObservedCoApplicant,
        sexApplicant,
        sexCoApplicant,
        observedSexApplicant,
        observedSexCoApplicant,
        ageApplicant,
        ageCoApplicant,
        income
      ) <> ((LarPartThree2022.apply _).tupled, LarPartThree2022.unapply)

    def larPartFourProjection =
      (
        purchaserType,
        rateSpread,
        hoepaStatus,
        lienStatus,
        creditScoreApplicant,
        creditScoreCoApplicant,
        creditScoreTypeApplicant,
        creditScoreModelApplicant,
        creditScoreTypeCoApplicant,
        creditScoreModelCoApplicant,
        denialReason1,
        denialReason2,
        denialReason3,
        denialReason4,
        otherDenialReason,
        totalLoanCosts,
        totalPoints,
        originationCharges
      ) <> ((LarPartFour2022.apply _).tupled, LarPartFour2022.unapply)

    def larPartFiveProjection =
      (
        discountPoints,
        lenderCredits,
        interestRate,
        paymentPenalty,
        debtToIncome,
        loanValueRatio,
        loanTerm,
        rateSpreadIntro,
        baloonPayment,
        insertOnlyPayment,
        amortization,
        otherAmortization,
        propertyValue,
        homeSecurityPolicy,
        landPropertyInterest,
        totalUnits,
        mfAffordable,
        applicationSubmission
      ) <> ((LarPartFive2022.apply _).tupled, LarPartFive2022.unapply)

    def larPartSixProjection =
      (
        payable,
        nmls,
        aus1,
        aus2,
        aus3,
        aus4,
        aus5,
        otheraus,
        aus1Result,
        aus2Result,
        aus3Result,
        aus4Result,
        aus5Result,
        otherAusResult,
        reverseMortgage,
        lineOfCredits,
        businessOrCommercial
      ) <> ((LarPartSix2022.apply _).tupled, LarPartSix2022.unapply)

    def larPartSevenProjection =
      (
        conformingLoanLimit,
        ethnicityCategorization,
        raceCategorization,
        sexCategorization,
        dwellingCategorization,
        loanProductTypeCategorization,
        tractPopulation,
        tractMinorityPopulationPercent,
        tractMedianIncome,
        tractOccupiedUnits,
        tractOneToFourFamilyUnits,
        tractMedianAge,
        tractToMsaIncomePercent
      ) <> ((LarPartSeven2022.apply _).tupled, LarPartSeven2022.unapply)

  }

  def createTransmittalSheetRepository2022(config: DatabaseConfig[JdbcProfile], p: Year2022Period) =
    new TSRepository2022Base(config, transmittalSheetTableQuery2022(p))

  def createQaTransmittalSheetRepository2022(config: DatabaseConfig[JdbcProfile], p: Year2022Period)(implicit ec: ExecutionContext) =
    new QARepository.Default[TransmittalSheetEntity, QATransmittalSheetTable](config, qaTransmittalSheetTableQuery2022(p))(ec)


  class RealLarTable2022(tag: Tag, tableName: String) extends LarTableBase[LarEntityImpl2022](tag, tableName) {
    def * = larEntityImpl2022Projection
  }

  class QALarTableBase2022(tag: Tag, tableName: String)
    extends LarTableBase[QAEntity[LarEntityImpl2022]](tag, tableName)
      with QATableBase[LarEntityImpl2022] {
    def * = (larEntityImpl2022Projection, fileName,timeStamp) <> ((QAEntity.apply[LarEntityImpl2022] _).tupled, QAEntity.unapply[LarEntityImpl2022] _)
  }

  def larTableQuery2022(p: Year2022Period) = {
    val tableName = p match {
      case Year2022Period.Whole => lar2022TableName
      case Year2022Period.Q1    => lar2022Q1TableName
      case Year2022Period.Q2    => lar2022Q2TableName
      case Year2022Period.Q3    => lar2022Q3TableName
    }
    TableQuery(tag => new RealLarTable2022(tag, tableName))
  }

  def qaLarTableQuery2022(p: Year2022Period) = {
    val tableName = p match {
      case Year2022Period.Whole => lar2022QATableName
      case Year2022Period.Q1    => lar2022Q1QATableName
      case Year2022Period.Q2    => lar2022Q2QATableName
      case Year2022Period.Q3    => lar2022Q3QATableName
    }
    TableQuery(tag => new QALarTableBase2022(tag, tableName))
  }

  class LarRepository2022Base[LarTable <: RealLarTable2022](val config: DatabaseConfig[JdbcProfile], val table: TableQuery[LarTable])
    extends TableRepository[LarTable, String] {

    override def getId(row: LarTable): config.profile.api.Rep[Id] =
      row.lei

    // TODO: Unless is_quarterly is part of the projection, this excludes the column causing breakage so don't use
    // $COVERAGE-OFF$
    def createSchema() = db.run(table.schema.create)
    def dropSchema()   = db.run(table.schema.drop)
    // $COVERAGE-ON$

    def insert(ts: LarEntityImpl2022): Future[Int] =
      db.run(table += ts)

    def findByLei(lei: String): Future[Seq[LarEntityImpl2022]] =
      db.run(table.filter(_.lei === lei).result)

    def deleteByLei(lei: String): Future[Int] =
      db.run(table.filter(_.lei === lei).delete)
    def count(): Future[Int] =
      db.run(table.size.result)

    def getAllLARsCount(bankIgnoreList: Array[String]): Future[Int] =
      db.run(getAllLARsQuery(bankIgnoreList).size.result)

    def getAllLARs(bankIgnoreList: Array[String]): DatabasePublisher[LarEntityImpl2022] =
      db.stream(
        getAllLARsQuery(bankIgnoreList).result
          .withStatementParameters(
            rsType = ResultSetType.ForwardOnly,
            rsConcurrency = ResultSetConcurrency.ReadOnly,
            fetchSize = 1000
          )
          .transactionally
      )
    protected def getAllLARsQuery(bankIgnoreList: Array[String]): Query[LarTable, LarEntityImpl2022, Seq] =
      table.filterNot(_.lei.toUpperCase inSet bankIgnoreList)
  }

  def createLarRepository2022(config: DatabaseConfig[JdbcProfile], p: Year2022Period) =
    new LarRepository2022Base(config, larTableQuery2022(p))

  def createQaLarRepository2022(config: DatabaseConfig[JdbcProfile], p: Year2022Period)(implicit ec: ExecutionContext) =
    new QARepository.Default[LarEntityImpl2022, QALarTableBase2022](config, qaLarTableQuery2022(p))(ec)

  def validationLarData2022(p: Year2022Period): LarData = LarData[LarEntityImpl2022, RealLarTable2022](larTableQuery2022(p))(_.lei)

  def validationTSData2022(p: Year2022Period): TsData =
    TsData[TransmittalSheetEntity, RealTransmittalSheetTable2022](transmittalSheetTableQuery2022(p))(_.lei, _.totalLines, _.submissionId)
  class QALarTableLoanLimit(tag: Tag)
    extends LarTableBase[QAEntity[LarEntityImpl2022WithMsa]](tag, lar2022QALoanLimitTableName)
      with QATableBase[LarEntityImpl2022WithMsa] {

    object MsaColumns {
      def msaID                   = column[String]("msa_id")
      def name                 = column[String]("name")
      def totalLars            = column[Int]("total_lars")
      def totalAmount          = column[BigDecimal]("total_amount")
      def conv                 = column[Int]("conv")
      def FHA                  = column[Int]("fha")
      def VA                   = column[Int]("va")
      def FSA                  = column[Int]("fsa")
      def siteBuilt            = column[Int]("site_built")
      def manufactured         = column[Int]("manufactured")
      def oneToFour            = column[Int]("one_to_four")
      def fivePlus             = column[Int]("five_plus")
      def homePurchase         = column[Int]("home_purchase")
      def homeImprovement      = column[Int]("home_improvement")
      def refinancing          = column[Int]("refinancing")
      def cashOutRefinancing   = column[Int]("cash_out_refinancing")
      def otherPurpose         = column[Int]("other_purpose")
      def notApplicablePurpose = column[Int]("not_applicable_purpose")
    }

    def msaProjection =
      (
        MsaColumns.msaID,
        MsaColumns.name,
        MsaColumns.totalLars,
        MsaColumns.totalAmount,
        MsaColumns.conv,
        MsaColumns.FHA,
        MsaColumns.VA,
        MsaColumns.FSA,
        MsaColumns.siteBuilt,
        MsaColumns.manufactured,
        MsaColumns.oneToFour,
        MsaColumns.fivePlus,
        MsaColumns.homePurchase,
        MsaColumns.homeImprovement,
        MsaColumns.refinancing,
        MsaColumns.cashOutRefinancing,
        MsaColumns.otherPurpose,
        MsaColumns.notApplicablePurpose
      ) <> ((Msa.apply _).tupled, Msa.unapply)

    def larEntityImpl2022WithMsaProjection = (larEntityImpl2022Projection, msaProjection) <> ((LarEntityImpl2022WithMsa.apply _).tupled, LarEntityImpl2022WithMsa.unapply)

    override def * = (larEntityImpl2022WithMsaProjection, fileName,timeStamp) <> ((QAEntity.apply[LarEntityImpl2022WithMsa] _).tupled, QAEntity.unapply[LarEntityImpl2022WithMsa] _)
  }
  val qaLarTable2022LoanLimit = TableQuery[QALarTableLoanLimit]

  class QALarRepository2022LoanLimit(config: DatabaseConfig[JdbcProfile])(implicit ec: ExecutionContext)
    extends QARepository.Default[LarEntityImpl2022WithMsa, QALarTableLoanLimit](config, qaLarTable2022LoanLimit)(ec)


}
// $COVERAGE-ON$