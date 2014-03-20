package org.cddcore.carers

import java.util.concurrent.atomic.AtomicInteger
import scala.xml.Elem
import scala.xml.XML
import org.eclipse.jetty.server.Server
import org.junit.runner.RunWith
import org.scalatest.BeforeAndAfterAll
import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.selenium.HtmlUnit
import net.atos.carers.web.endpoint.ClaimHandler
import org.scalatest.junit.JUnitRunner

object SmokeWebtest {
  val port = new AtomicInteger(8090)
}

@RunWith(classOf[JUnitRunner])
class SmokeWebtest extends FlatSpec with ShouldMatchers with HtmlUnit with BeforeAndAfterAll {
  import SmokeWebtest._
  val localPort = port.getAndIncrement()
  val host = s"http://localhost:$localPort/"
  val claimHandler = new ClaimHandler
  val server = new Server(localPort)
  server.setHandler(claimHandler);

  override def beforeAll {
    server.start
  }

  override def afterAll {
    server.stop
  }

  "Our Rubbishy Website" should "Display a form when it recieves a GET" in {
    go to (host + "index.html")
    pageTitle should be("Validate Claim")
  }

  "Our Rubbishy Website" should "be able to set focus to the custxml textarea" in {
    click on name("custxml")
  }
  
  "Our Rubbishy Website" should "be able to set focus to the claimDate textarea" in {
    click on name("claimDate")
  }
  
  "Our Rubbishy Website" should "be able to submit claim XML and then see a timeline" in {
    textArea("custxml").value = getClaimXML
    submit()
    val xml: Elem = XML.loadString(pageSource)
    val timeLineNodes = xml \ "body" \ "form" \ "pre" \ "div" \ "p"
    assert(timeLineNodes.length > 0)
  }

  "Our Rubbishy Website" should "blah if submit without claim xml" in {
    go to (host + "index.html")
    // submit()
  }
  
  def getClaimXML: String = {
    val xml: Elem =
      <ValidateClaim xsi:schemaLocation="http://www.autotdd.com/ca Conversation%20v2_1%202010-07-16.xsd" xmlns="http://www.autotdd.com/ca" xmlns:n1="http://www.autotdd.com/ca" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
        <piid>String</piid>
        <newClaimantData>true</newClaimantData>
        <ClaimantData>
          <ClaimantNINO>CL100100A</ClaimantNINO>
          <ClaimantNameDetails>
            <PersonNameTitle>MR</PersonNameTitle>
            <PersonGivenName>ADAM</PersonGivenName>
            <PersonFamilyName>APPLE</PersonFamilyName>
            <PersonNameSuffix>BSC</PersonNameSuffix>
            <PersonRequestedName> APPLE</PersonRequestedName>
          </ClaimantNameDetails>
          <ClaimantBirthDate>
            <PersonBirthDate>1970-08-13</PersonBirthDate>
            <VerificationLevel>Level 1</VerificationLevel>
          </ClaimantBirthDate>
          <ClaimantDeathDate>
            <PersonDeathDate>3000-01-01</PersonDeathDate>
            <VerificationLevel>Level 0</VerificationLevel>
          </ClaimantDeathDate>
          <ClaimantMaritalStatus>
            <MaritalStatus>Married</MaritalStatus>
            <VerificationLevel>Level 1</VerificationLevel>
          </ClaimantMaritalStatus>
          <ClaimantGenderAtRegistration>1 = Male</ClaimantGenderAtRegistration>
          <ClaimantGenderCurrent>1 = Male</ClaimantGenderCurrent>
          <ClaimantNationality>GB</ClaimantNationality>
          <ClaimantContactDetails>
            <PreferredLanguages>en</PreferredLanguages>
            <Telephone n1:TelMobile="no" n1:TelUse="home" n1:TelPreferred="yes">
              <TelNationalNumber>01937843786</TelNationalNumber>
              <TelExtensionNumber>0</TelExtensionNumber>
              <TelCountryCode>44</TelCountryCode>
            </Telephone>
          </ClaimantContactDetails>
          <ClaimantAddress>
            <Line1>12 FREESTYLE MEADOWS</Line1>
            <Line2>ASHTON UNDER LYNE</Line2>
            <PostCode>AL5 9IK</PostCode>
            <AddressStartDate>2003-11-05</AddressStartDate>
          </ClaimantAddress>
        </ClaimantData>
        <newClaimData>true</newClaimData>
        <ClaimData>
          <ClaimStartDate>2010-05-10</ClaimStartDate>
          <ClaimNinoKnown>yes</ClaimNinoKnown>
          <ClaimPrevious>no</ClaimPrevious>
          <ClaimPreviousPaid>no</ClaimPreviousPaid>
          <ClaimOverseas>no</ClaimOverseas>
          <ClaimAlwaysUK>yes</ClaimAlwaysUK>
          <ClaimCurrentResidentUK>yes</ClaimCurrentResidentUK>
          <ClaimPartnerExists>no</ClaimPartnerExists>
          <ClaimRelationToCarer>Son</ClaimRelationToCarer>
          <ClaimPaidCare>no</ClaimPaidCare>
          <ClaimRivalCarer>no</ClaimRivalCarer>
          <Claim35Hours>yes</Claim35Hours>
          <ClaimBreakInCare>no</ClaimBreakInCare>
          <ClaimPrior35Hours>no</ClaimPrior35Hours>
          <ClaimEducationFullTime>no</ClaimEducationFullTime>
          <ClaimEmployment>no</ClaimEmployment>
          <ClaimRentalIncome>no</ClaimRentalIncome>
          <ClaimPaidClass2NIC>no</ClaimPaidClass2NIC>
          <ClaimSelfEmployed>no</ClaimSelfEmployed>
        </ClaimData>
        <newPartnerData>true</newPartnerData>
        <newDependantData>true</newDependantData>
        <DependantData>
          <DependantNINO>DP100100A</DependantNINO>
          <DependantNameDetails>
            <PersonNameTitle>MRS</PersonNameTitle>
            <PersonGivenName>MAUREEN</PersonGivenName>
            <PersonFamilyName>HEPWORTH</PersonFamilyName>
            <PersonNameSuffix>
            </PersonNameSuffix>
            <PersonRequestedName>
            </PersonRequestedName>
          </DependantNameDetails>
          <DependantBirthDate>
            <PersonBirthDate>1920-05-01</PersonBirthDate>
            <VerificationLevel>Level 1</VerificationLevel>
          </DependantBirthDate>
          <DependantMaritalStatus>
            <MaritalStatus>Widowed</MaritalStatus>
            <VerificationLevel>Level 1</VerificationLevel>
          </DependantMaritalStatus>
          <DependantGenderAtRegistration>
            2 = Female
          </DependantGenderAtRegistration>
          <DependantGenderCurrent>2 = Female</DependantGenderCurrent>
          <DependantNationality>GB</DependantNationality>
          <DependantAddress>
            <Line1>9 HALL MEWS</Line1>
            <Line2>BOSTON SPA</Line2>
            <PostCode>LS23 6QB</PostCode>
          </DependantAddress>
        </DependantData>
        <newPaidCareData>true</newPaidCareData>
        <newRivalCarerData>true</newRivalCarerData>
        <newStatementData>true</newStatementData>
        <StatementData>
          <StatementType>SignedBySelf</StatementType>
          <StatementRole>Self</StatementRole>
          <StatementSignature/>
          <StatementDate>2010-05-10</StatementDate>
          <StatementHoursConfirmed>yes</StatementHoursConfirmed>
          <StatementHoursUnconfirmed>no</StatementHoursUnconfirmed>
          <StatementReason/>
        </StatementData>
        <newResidenceData>true</newResidenceData>
        <ResidenceData>
          <ResidenceNormallyGB>yes</ResidenceNormallyGB>
          <ResidenceNormalCountry/>
          <ResidenceCurrentlyGB>yes</ResidenceCurrentlyGB>
          <Residence4WeeksOutsideGB>no</Residence4WeeksOutsideGB>
          <ResidencePrior12Months>no</ResidencePrior12Months>
        </ResidenceData>
        <newEducationData>false</newEducationData>
        <newEmploymentData>false</newEmploymentData>
        <newExpensesData>false</newExpensesData>
        <newSelfEmpData>false</newSelfEmpData>
        <newOtherMoneyData>false</newOtherMoneyData>
        <newPaymentData>false</newPaymentData>
        <newConsentData>true</newConsentData>
        <ConsentData>
          <ConsentAgreeEmployer>yes</ConsentAgreeEmployer>
          <ConsentAgreeOthers>yes</ConsentAgreeOthers>
          <ConsentSignature></ConsentSignature>
          <ConsentDate>2010-03-15</ConsentDate>
        </ConsentData>
      </ValidateClaim>
      xml.toString
  }
 }
