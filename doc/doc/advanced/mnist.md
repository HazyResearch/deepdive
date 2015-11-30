---
layout: default
title: "Tutorial: Walk-through with an Example"
---

# Example Application: MNIST

This document describes how to build MNIST application using the fusion language.
This document assumes you are familiar with basic concepts in DeepDive, DDlog, and Caffe.



## Overview of Fusion Language

Fusion fuses neural networks and factor graphs.
It allows you to jointly train a neural network and a factor graph, and get inference results from the fused network.

In terms of implementation, fusion bridges Caffe and DeepDive.
The fusion language simply extends DeepDive/DDlog with the ability to handle images, and CNN inference rules.

We will now walk through the MNIST application.


## Preparation

First, switch to our MNIST example folder

```bash
cd $DEEPDIVE_HOME/examples/mnist
```

Our application directory contains the following files and directories:

* `app.ddlog`
* `deepdive.conf`
* `db.url`
* `udf/`
* `data/`

For our application, we will create an image table that contains image filenames, labels, whether it's training or testing data.
We will have the following schema declaration in `app.ddlog`

```
images(file text, label int, is_training boolean).
```

Now, we need to declare the variable relation in `app.ddlog`:

```
variables? (
@image_path("data")
file text,
is_training boolean)
Categorical(10).
```

This says the variable relation has two keys, `file` and `is_training`.
The `file` column is declared as paths to images through the annotation `@image_path`, relative to the `data` folder.
The variables are multinomial with 10 classes.

Now, DeepDive will initialize the database for us if we run the following command:

```bash
deepdive initdb
```

Now, we're all set to start filling in more detail to this new application.

## Data Preprocessing

In our application, we use jpg files as input.
So the first step is to convert MNIST data into jpg format and load into our database.

We have prepared scripts for data preprocessing.

```bash
./get_mnist.sh
./prepare_data.py
```

Note `prepare_data.py` uses numpy, scipy, and Image.

These commands will download the MNIST data, transform the data into jpg format, and store the files in `data/` folder.
A file `images.tsv` will also be generated, which contains the image filenames, the labels, and whether the image is training data.
This will be the input for the `images` table.
In MNIST, we have 60000 training images and 10000 testing images.

Next, we should load these input data into the database.

```bash
deepdive sql "COPY images FROM STDIN" < ./images.tsv
```

## Writing scoping rules and inference rules
Now, we need to scope/supervise our variables, using the same scoping rule as in normal DDlog

```
@label(l)
variables(file, t) :- images(file, l, t).
```

Then, we will specify the graph structure DeepDive perform probabilistic learning and inference.
In fusion, normal DeepDive inference rules can still be used.
Fusion adds another type of inference rule: CNN rule, which expresses variables in a CNN.
For our application, we want to predict the label for images in the `variables` table.
We will only need one CNN rule for this.

```
@cnn("solver.prototxt", "train_test.prototxt")
variables(file, t) :- images(file, l, t).
```

In the above rule, we have an annotation `@cnn("solver.prototxt", "train_test.prototxt")` that says we will use the two protobuf files for Caffe.
We will talk about the details in the next section.

We also need to specify the held-out set for testing.
This is done by the holdout query in `deepdive.conf`

```
deepdive.calibration.holdout_query: "INSERT INTO dd_graph_variables_holdout(variable_id) SELECT id FROM variables WHERE is_training = false"
```

## Defining Caffe network and solver

In the previous section, we use `solver.prototxt` and `train_test.prototxt` to specify Caffe.
This section explains how to use the protobuf in fusion.
These protobuf files are basically the same as the solver and network protobuf files in Caffe, with some slight modifications.

In `solver.prototxt`, use the following to specify the net
```
net: TRAIN_TEXT_PROTOTXT
```

In `train_test.prototxt`, use `TRAIN_LMDB` or `TEST_LMDB` for the source of data layer
```
source: TRAIN_LMDB
```
If we want to subtract the image mean from each image, use `IMAGE_MEAN` for image mean file
```
mean_file: IMAGE_MEAN
```
Set `num_output` of the last inner product layer to `NUM_OUTPUT`
```
num_output: NUM_OUTPUT
```

DeepDive uses image id to communicate with Caffe. We need to add image id to data layers, loss layers and accuracy layers.
Add a third top blob to data layers for image id
```
top: "imgid"
```
Add a thrid bottom blob to loss layers and accuracy layers
```
bottom: "imgid"
```

Currently, fusion only supports `SoftmaxWithLoss` layer for training and `Accuracy` layer for testing.
So make sure in `SoftmaxWithLoss` layer, we have
```
include { phase: TRAIN }
```
and in `Accuracy` layer, we have
```
include { phase: TEST }
```

## Running the application

We're all set to run the application. Let's run the `endtoend` pipeline

```bash
deepdive run endtoend
```

DeepDive will run learning and inference on the given network.
Caffe outputs will also be printed on the screen.
After the run, we can check the results in the same way as a usual DeepDive application.


## Using a pretrained Caffe model

To use a pretrained Caffe model, use the following annotation for the inference rule

```
@cnn("solver.prototxt", "train_test.prototxt", "iter_5000.caffemodel")
```

where the first prototxt should contain specification for `test_iter`, and make sure the combination of test batch size and test iterations cover all examples.
The third argument is a pretrained caffe model.


## DeepDive.conf

Fusion can also be written in `deepdive.conf`.
Check `run/LATEST/deepdive.conf` for the compiled `deepdive.conf` to see how to write a fusion program.


