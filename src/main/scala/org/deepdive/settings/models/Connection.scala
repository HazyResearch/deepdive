package org.deepdive.settings

/* Database connection specifie in the settings */
case class Connection(url: String, user: String, password: String)