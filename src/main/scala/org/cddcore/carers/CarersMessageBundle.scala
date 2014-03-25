package org.cddcore.carers

import java.util.Locale
import java.util.ResourceBundle
import java.text.MessageFormat

class CarersMessageBundle

object CarersMessageBundle {
  private val messageFile: String = "bundles.messages"
  
  val welshLocale: Locale = new Locale("cy_GB", "Welsh")

  def getMessage(msgId: String, locale: Locale, msgInserts: String*): String = {
    val messageBundle : ResourceBundle = ResourceBundle.getBundle(messageFile, locale) 
    
    MessageFormat.format(messageBundle.getString(msgId), msgInserts:_ *);
  }
  
  def getMessage(msgId: String, msgInserts: String*): String = {
    println("Getting bundle")
    
    val messageBundle : ResourceBundle = ResourceBundle.getBundle(messageFile, Locale.ENGLISH)   
    
    MessageFormat.format(messageBundle.getString(msgId), msgInserts:_ *);
  }
}
