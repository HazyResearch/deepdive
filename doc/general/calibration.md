---
layout: default
---

# Interpreting results

One of the most important aspects of feature engineering and probabilistic inference is being able to act on the results. 

### Generating calibration plots

DeepDive automatically generates calibration plots for all variables you define in the [schema](/doc/schema.html). The destination file of the calibration plots is part of the system output. DeepDive also prints the command to generate calibration plots in its output, so you can generate the plots yourself. 

### Interpreting calibration plots

A typical calibration plot looks as follows:

![]({{site.baseurl}}/images/calibration_example.png)

**Note that plots (a) and (b) can only be generated if you specify a [holdout fraction](/doc/calibration.html)! **

**The accuracy plot (a)** shows how many correcy positive predictions were made for each probability bucket. Ideally, the red line should follow the blue line. That is, you want to make no correct positive predictions for a pobability of 0, and many correct positive predictions for a probability of > 0.9.

**Plots (b) and (c)** shows the number of total prediction on the test and the training set, respectively. Ideally these plots should follow a U-curve. That is, you want many predictions with probability 0 (event that are likely to be false), and many predictions with probability > 0.9 (events that are likely to be true). Predictions in the range of 0.4 - 0.6 mean that the system is not sure, often indicating that you may need features to make more accurate predictions.


### Acting on calibration data

There are many reasons why your results may be off. Common reasons include:

- **Not enough features:** This is particularly common when you have a lot of probability mass in the middle buckets (0.4 - 0.6). The system may be unable to make predictions about events because you do no have specific-enough features for them. Take a look at variables that were assigned a probability in the 0.4 to 0.6 range, inspect them, and come up with specific features that would push these variables towards a positive or negative probability.

- **Not enough positive evidence:** If you do not provide enough positive evidence the system will be unable to learn weights that push variables towards a high probability (or low probability if your variables are negated). Having little probability mass on the right side of the graph is often an indicator for not having enough positive evidence, or not using features that uses the positive evidence effectively.

- **Not enough negative evidence:** When you do not have enough negative evidence, or negative evidence that is biased, then the system will not be able to distinguish true events from false events. That is, you will obtain many *false positives*. In the graph this is often indicated by having little probability mass on the left (no U-shape) in plots b) and c), or/and by having a low accuracy for high probabilities in plot a). [Gerating negative evidence the can be somewhat of an art (TODO: Link here)]()

- **Weight learning does not converge:** When DeepDive is unable to learn weights for the inference rules the predicated data will be invalid. Check the DeepDive log file, and if the gradient value at the end of the learning phrase is very large (1000 or more), then it is possible that weight learning was not successful. In this case, you can try to [decrease the learning rate or increase the number of learning iterations](/doc/performance.html) for the sampler. 

