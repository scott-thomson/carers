package org.cddcore.carers

import org.joda.time.DateTime

case class TimeLineItem(events: List[(DateRange, KeyAndParams)]) {
  val startDate = events.head._1.from
  val endDate = events.last._1.to
  val daysInWhichIWasOk = events.foldLeft[Int](0)((acc, tuple) => tuple match {
    case (dr, keyAndParams) if keyAndParams.key == "ENT" => dr.days
    case _ => 0
  })
  val wasOk = daysInWhichIWasOk > 2
  override def toString = s"TimeLineItem($startDate, $endDate, days=$daysInWhichIWasOk, wasOK=$wasOk, dateRange=\n  ${events.mkString("\n  ")})"
}

object TimeLineCalcs {

  type TimeLine = List[TimeLineItem]
  /** Returns a DatesToBeProcessedTogether and the days that the claim is valid for */
  def findTimeLine(c: CarersXmlSituation): TimeLine = {
    val dates = InterestingDates.interestingDates(c)
    val dayToSplit = DateRanges.sunday
    val result = DateRanges.interestingDatesToDateRangesToBeProcessedTogether(dates, dayToSplit)

    result.map((dateRangeToBeProcessedTogether: DateRangesToBeProcessedTogether) => {
      TimeLineItem(dateRangeToBeProcessedTogether.dateRanges.map((dr) => {
        val result = Carers.engine(dr.from, c)
        (dr, result)
      }))
    })
  }

  def main(args: Array[String]) {
    println(findTimeLine(Claim.validateClaimWithBreaks(("2010-7-1", "2010-7-10", true))).mkString("\n"))
  }

}