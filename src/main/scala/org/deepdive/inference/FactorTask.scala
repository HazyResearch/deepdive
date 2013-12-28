package org.deepdive.inference

import org.deepdive.Task
import org.deepdive.settings.FactorDesc

case class FactorTask(factorDesc: FactorDesc, holdoutFraction: Double)