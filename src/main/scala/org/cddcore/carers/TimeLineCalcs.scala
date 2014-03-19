package org.cddcore.carers

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
        val newCarers = c.copy(world = c.world.copy(dateProcessingDate = dr.from))
        val result = Carers.engine(newCarers)
        (dr, result)
      }))
    })
  }

}