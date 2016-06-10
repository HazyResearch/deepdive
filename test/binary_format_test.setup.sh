#!/usr/bin/env bash
cd "$(dirname "$0")"
dw text2bin variable biased_coin/variables.tsv biased_coin/graph.variables
dw text2bin factor   biased_coin/factors.tsv   biased_coin/graph.factors   4 1 0 1
dw text2bin weight   biased_coin/weights.tsv   biased_coin/graph.weights
dw text2bin domain   domains/domains.tsv       domains/graph.domains
