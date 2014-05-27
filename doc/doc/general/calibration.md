---
layout: default
---

# Interpreting results

One of the most important aspects of DeepDive is its iterative workflow. After running a DeepDive application and performing [probabilistic inference](probabilistic_inference.html) it is crucial to evaluate the results and act on feedback that the system provides. DeepDive produces *calibration plots* to help you with this task.

### Generating calibration plots

DeepDive automatically generates calibration plots for all variables you define in the [schema]({{site.baseurl}}/doc/schema.html). The destination file of the calibration plots is part of the system output. DeepDive also prints the command to generate calibration plots in its output, so you can generate the plots yourself. 

### Interpreting calibration plots

A typical calibration plot looks as follows:

![]({{site.baseurl}}/images/calibration_example.png)

**Note that plots (a) and (b) can only be generated if you specify a [holdout fraction]({{site.baseurl}}/doc/calibration.html) in the configuration! **

**The accuracy plot (a)** shows how the ratio of correct positive predictions for each probability bucket. Ideally, the red line should follow the blue line. That is, you want to make no correct positive predictions for a probability of 0, and 100% correct positive predictions for a probability of 1.0.

**Plots (b) and (c)** shows the number of total prediction on the test and the training set, respectively. Ideally these plots should follow a U-curve. That is, you want many predictions with probability 0 (event that are likely to be false), and many predictions with probability > 0.9 (events that are likely to be true). Predictions in the range of 0.4 - 0.6 mean that the system is not sure, which may indicate that need more features to make predictions for such events.

### Acting on calibration data

There are many reasons why results could be suboptimal. Common ones are:

- **Not enough features:** This is particularly common when you have a lot of probability mass in the middle buckets (0.4 - 0.6). The system may be unable to make predictions about events because you do no have specific-enough features for them. Take a look at variables that were assigned a probability in the 0.4 to 0.6 range, inspect them, and come up with specific features that would push these variables towards a positive or negative probability.

- **Not enough positive evidence:** If you do not provide enough positive evidence the system will be unable to learn weights that push variables towards a high probability (or low probability if your variables are negated). Having little probability mass on the right side of the graph is often an indicator for not having enough positive evidence, or not using features that uses the positive evidence effectively.

- **Not enough negative evidence:** When you do not have enough negative evidence, or negative evidence that is biased, then the system will not be able to distinguish true events from false events. That is, you will obtain many *false positives*. In the graph this is often indicated by having little probability mass on the left (no U-shape) in plots b) and c), or/and by having a low accuracy for high probabilities in plot a). Generating negative evidence can be somewhat of an art.

- **Weight learning does not converge:** When DeepDive is unable to learn weights for the inference rules the predicated data will be invalid. Check the DeepDive log file for the gradient value at the end of the learning phrase. If the value is very large (1000 or more), then it is possible that weight learning was not successful. In this case, you can try to increase the number of learning iterations, decrease learning rate, or using faster decay. On the other hand, if results converge too fast to a local optimum, you can try increasing learning rate, or using a slower decay. [See more in the performance tuning guide]({{site.baseurl}}/doc/performance.html). 


### Recall Errors

Recall is the fraction of relevant events that are extracted. In information extraction applications there are generally two sources of recall errors:

- **Events candidates are not recognized in the text.** In this case, no variables are created are recall errors and these events do not show up in the calibration plots. For example, you may fail to identify "micro soft" as a company name if it is lowercase and misspelled. Such errors are difficult to debug, unless you have a complete database that you can test  against, or you make a [closed-world assumption](http://en.wikipedia.org/wiki/Closed_world_assumption) on your test set.
- **Events fall below a confidence cutoff**. Assuming your limit to final output of your system to events that have a high probability, then events in the mid-range of the calibration plots can be seen as recall errors. For example, if you only output company names that you are > 90% confident are correct, then the company names in the buckets below 0.9 are recall errors. You can then improve your recall by using some of the suggestions above.




