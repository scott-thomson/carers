package org.cddcore.carers

import org.joda.time.DateTime
import org.joda.time.Weeks

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

  case class SimplifiedTimelineItem(date: DateTime, award: Double, reason: String)

  def simplifyTimeLine(t: TimeLine, endDate: DateTime) = {
    t.flatMap((tli) => {
      val actualEndDate = if (endDate.isAfter(tli.endDate)) tli.endDate else endDate
      val weeks = Weeks.weeksBetween(tli.startDate, actualEndDate).getWeeks
      (0 to weeks - 1).map((week) => {
        val reasons = tli.events.foldLeft(List[KeyAndParams]())((acc, e) => e._2 :: acc)
        SimplifiedTimelineItem(tli.startDate.plusDays(week * 7), tli.wasOk match {
          case false => 0;
          case true => 57.6
        }, reasons.mkString)
      })
    })
  }

  //
  //  def main(args: Array[String]) {
  //    //    println(findTimeLine(Claim.validateClaimWithBreaks(("2010-7-1", "2010-7-10", true))).mkString("\n"))
  //  }

}