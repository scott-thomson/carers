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
    val result = DateRanges.interestingDatesToDateRangesToBeProcessedTogether(dates, c.world.dayToSplitOn)

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

  def foldTimelineOnItemKeys(tl: TimeLine): TimeLine = {
    type accumulator = (List[TimeLineItem], Option[TimeLineItem])
    val initialValue: accumulator = (List[TimeLineItem](), None)
    val foldFn: ((accumulator, TimeLineItem) => accumulator) =
      (acc: accumulator, v: TimeLineItem) => {
        (acc, v) match {
          case ((list, None), v) => (list, Some(v))
          case ((list, Some(TimeLineItem((DateRange(fromM, toM, reasonM), kAndPM) :: Nil))), TimeLineItem((DateRange(from, to, reason), kAndP) :: Nil)) if kAndPM == kAndP => {
            val newTli = TimeLineItem(List((DateRange(fromM, to, reasonM), kAndP)))
            (list, Some(newTli))
          }
          case ((list, Some(mergeV)), v) => ((list :+ mergeV, Some(v)))
        }
      }
    val result = tl.foldLeft[accumulator](initialValue)(foldFn)
    result._2 match {
      case None => result._1
      case Some(tli) => result._1 :+ tli
    }
    //    tl.foldLeft[accumulator](initialValue)(foldFn)

  }

  //
  def main(args: Array[String]) {
    val situation: CarersXmlSituation = CarersXmlSituation(World(), Claim.getXml("CL800119A"))
    val timeLine = findTimeLine(situation)
    println(timeLine.mkString("\n"))
    println
    println(foldTimelineOnItemKeys(timeLine).mkString("\n"))
  }

}