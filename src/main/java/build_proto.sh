#! /usr/bin/env bash

protoc --java_out=. FactorGraph.proto
protoc --java_out=. InferenceResult.proto