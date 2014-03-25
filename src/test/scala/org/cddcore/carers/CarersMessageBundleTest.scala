package org.cddcore.carers

import org.junit.Test
import org.junit._


class CarersMessageBundleTest {

  //test that default bundle returns message for a valid key and correct number of inserts
  @Test
  def testDefaultBundleValidKey() {
    val carersMessageBundle = CarersMessageBundle
    
    Assert.assertEquals(carersMessageBundle.getMessage("502", "2010-01-01", "2010-01-02", "Mr Unwell"), "You are not entitled from 2010-01-01 to 2010-01-02. This is because you were not caring for Mr Unwell for at least 35 hours a week.");
  }
  
  //test that default bundle return nothing for an invalid key
  
  //test that default bundle returns message when passed an insert
  
  //test that default bundle returns message when passed multiple inserts
  
  //test that default bundle errors when given the wrong number of inserts
  
  //test that welsh bundle returns message for a valid key
  
  //test that welsh bundle returns nothing for an invalid key
  
  //test that welsh bundle returns message when passed an insert
  
  //test that welsh bundle returns message when passed multiple inserts
  
  //test that welsh bundle errors when given the wrong number of inserts 
}