# Census Income Model

Given demographic features, predict if income is above $50K.
- https://archive.ics.uci.edu/ml/datasets/Census+Income
- https://www.tensorflow.org/versions/r0.9/tutorials/wide/index.html

Loigt regression with
- 8 categorical features
- 5 continuous features
- 1 bucketized feature (age_bucket)
- 2 composite features (education_x_occupation, age_buckets_x_race_x_occupation)

Some top weights:
```
                              description                               |   weight
------------------------------------------------------------------------+-------------
 education--Doctorate                                                   |     12.0777
 education--Prof-school                                                 |      10.494
 education_x_occupation--Prof-school-Married-civ-spouse                 |      10.203
 education_x_occupation--Doctorate-Married-civ-spouse                   |     9.46119
 education--7th-8th                                                     |    -9.00476
 education_x_occupation--7th-8th-Married-civ-spouse                     |    -8.86731
 race_x_occupation_x_age_bucket--White-Exec-managerial-6                |     8.28021
 education--9th                                                         |    -8.16179
 education_x_occupation--Masters-Married-civ-spouse                     |     7.59935
 relationship--Other-relative                                           |    -7.39554
 relationship--Wife                                                     |     7.13358
 race_x_occupation_x_age_bucket--White-Exec-managerial-5                |     6.79246
 age_bucket--8                                                          |    -6.73011
 education_x_occupation--HS-grad-Never-married                          |    -6.59893
 occupation--Farming-fishing                                            |    -6.43566
 marital_status--Widowed                                                |    -6.40761
 education_x_occupation--9th-Married-civ-spouse                         |    -6.29423
 race_x_occupation_x_age_bucket--White-Sales-1                          |    -6.27174
 education_x_occupation--HS-grad-Widowed                                |     -6.2578
 race_x_occupation_x_age_bucket--White-Prof-specialty-5                 |     5.83243
 education_x_occupation--Assoc-acdm-Married-civ-spouse                  |     5.50173
 education--10th                                                        |    -5.45628
 race_x_occupation_x_age_bucket--White-Craft-repair-1                   |    -5.34799
 education_x_occupation--HS-grad-Divorced                               |     -5.3002
 race--Amer-Indian-Eskimo                                               |    -5.27035
 age_bucket--1                                                          |    -5.26784
 race_x_occupation_x_age_bucket--White-Machine-op-inspct-2              |    -5.22079
 relationship--Own-child                                                |    -5.11061
```
