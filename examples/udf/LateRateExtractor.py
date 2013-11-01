class LateRateExtractor(FeatureExtractor):
  def udf(input_relation): # ??
    for row in input_relation:
      lateLevel = ''
      if row.lateRate == 0:
        lateLevel = 'never'
      elif row.lateRate < 0.3:
        lateLevel = 'low'
      elif row.lateRate < 0.5:
        lateLevel = 'medium'
      else:
        lateLevel = 'high'
      # Not sure whether this discretization is needed.

      AddFactor('AirlineLateF', row.airlineId, lateLevel) # ???
      AddFactor('FromCityLateF', row.fromCity, lateLevel)
      AddFactor('ToCityLateF', row.toCity, lateLevel)
      # We should define AddFactor for users..
