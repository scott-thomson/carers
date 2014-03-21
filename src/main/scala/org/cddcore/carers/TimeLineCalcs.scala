package org.cddcore.carers

import org.joda.time.DateTime

case class TimeLineItem(events: List[(DateRange, KeyAndParams)]) {
  val startDate = events.head._1.from
  val endDate = events.last._1.to
  val daysInWhichIWasOk = events.foldLeft[Int](0)((acc, tuple) => tuple match {
    case (dr, keyAndParams) if keyAndParams.key == "ENT" => acc + dr.days
    case _ => acc
  })
  val wasOk = daysInWhichIWasOk >= 2
  override def toString = s"TimeLineItem($startDate, $endDate, days=$daysInWhichIWasOk, wasOK=$wasOk, dateRange=\n  ${events.mkString("\n  ")})"
  def eventToJsonString(event: (DateRange, KeyAndParams)) =
    event match { case (_, KeyAndParams(key, _)) => s"'$key'" }
  def jsonToString = {
    val renderedStartDate = Claim.toString(startDate)
    val renderedEndDate = Claim.toString(endDate)
    val renderedEvents = events.map(eventToJsonString(_)).mkString("[", ",", "]")
    val result = s"{'startDate': '$renderedStartDate','endDate': '$renderedEndDate', 'wasOk':$wasOk, 'events':$renderedEvents}"
    result
  }
}

object TimeLineCalcs {
  def toJson(list: TimeLine): String =
    list.map(_.jsonToString).mkString("[", ",\n", "]").replaceAll("\'", "\"")

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
  }

  def main(args: Array[String]) {
    println(findTimeLine(CarersXmlSituation(World(), Claim.getXml("CL800119A"))).mkString("\n"))
  }

}