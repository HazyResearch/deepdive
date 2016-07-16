# Census Income Model

Given demographic features, predict if income is above $50K.
- https://archive.ics.uci.edu/ml/datasets/Census+Income
- https://www.tensorflow.org/versions/r0.9/tutorials/wide/index.html

Loigt regression with
- 8 categorical features
- 5 continuous features
- 1 bucketized feature (age_bucket)
- 2 composite features (education_x_occupation, age_buckets_x_race_x_occupation)

Note that we set the l2 regularization parameter to 100 (`--reg_param 100`),
to get a holdout (25%) accuracy of about 80% and a beautiful calibration plot.
If we use the default parameter, the holdout accuracy is only around 65% and
there are lots of double-digit weights. Regularization is important especially
for continuous features that are not normalized (e.g., capital gain/loss values).

Top weights (we should support rule naming...):
```
                        description                        |    weight
-----------------------------------------------------------+--------------
 inf13_istrue_rich--1                                      |     -2.66023
 inf4_istrue_rich--Own-child                               |      -2.2631
 inf2_istrue_rich--Never-married                           |     -2.14101
 inf6_istrue_rich--Female                                  |     -1.74616
 inf14_istrue_rich--HS-grad-Never-married                  |     -1.44536
 inf4_istrue_rich--Wife                                    |      1.35081
 inf1_istrue_rich--HS-grad                                 |      -1.3017
 inf14_istrue_rich--HS-grad-Married-civ-spouse             |       1.1145
 inf2_istrue_rich--Married-civ-spouse                      |      1.06154
 inf3_istrue_rich--Other-service                           |     -1.04069
 inf14_istrue_rich--Some-college-Never-married             |    -0.955602
 inf13_istrue_rich--2                                      |    -0.893953
 inf13_istrue_rich--6                                      |      0.88745
 inf4_istrue_rich--Unmarried                               |    -0.851389
 inf_istrue_rich--Self-emp-not-inc                         |    -0.823674
 inf1_istrue_rich--Prof-school                             |     0.750805
 inf1_istrue_rich--Doctorate                               |     0.740388
 inf3_istrue_rich--Farming-fishing                         |    -0.715585
 inf13_istrue_rich--5                                      |     0.697805
 inf5_istrue_rich--Black                                   |    -0.689167
 inf3_istrue_rich--Exec-managerial                         |     0.653698
 inf3_istrue_rich--Handlers-cleaners                       |    -0.652161
 inf6_istrue_rich--Male                                    |    -0.637331
```
