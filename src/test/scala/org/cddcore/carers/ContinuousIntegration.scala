package org.cddcore.carers

import org.cddcore.engine._
import org.cddcore.engine.tests._
import org.junit.runner.RunWith

/**
 * This class will be swept up by JUnit. It should access all the engines that you want to check
 *  It would be nice if it could be just done by reflection but there are issues with it: objects don't get checked by JUnit, Some engines are created in places with funny constructors...
 */
@RunWith(classOf[CddContinuousIntegrationRunner])
class CarersContinuousIntegration extends CddContinuousIntegrationTest {
  val engines = List(Carers.engine)
}