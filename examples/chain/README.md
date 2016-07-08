This model consists of a sequence of 1000 binary categorical vars repeating this pattern: `HT????`, where `H` = heads, `T` = tails, and `?` are unknown vars. There is a binary factor for each pair of consecutive vars, parameterized on the 4 label outcomes on the pair.

Internally, the cid of `H` is 0 and `T` is 1.
Before the sampler fix in https://github.com/HazyResearch/sampler/pull/41 ,
the unknown vars keep the default value of `H` during SGD steps, and we
repeatedly see {HH, HT} (sample first component) and {HT, TH} (sample second component).
As a result, we learn these skewed weights:
```
select * from dd_inference_result_weights_mapping;
 wid | isfixed | initvalue |      description       |  weight
-----+---------+-----------+------------------------+-----------
   2 | f       |         0 | inf_and_tags_tags--H-T |   16.4677
   0 | f       |         0 | inf_and_tags_tags--T-H |  -12.9592
   1 | f       |         0 | inf_and_tags_tags--T-T |   -3.7851
   3 | f       |         0 | inf_and_tags_tags--H-H | -0.405992
```


After the fix, the patterns become {?H, HT} and {HT, T?}, and we get
```
select * from dd_inference_result_weights_mapping;
 wid | isfixed | initvalue |      description       |  weight
-----+---------+-----------+------------------------+----------
   2 | f       |         0 | inf_and_tags_tags--H-T |  3.30405
   0 | f       |         0 | inf_and_tags_tags--T-H |  0.20926
   1 | f       |         0 | inf_and_tags_tags--T-T | -1.59762
   3 | f       |         0 | inf_and_tags_tags--H-H | -1.92788
```

In terms of frequency, `P(HT) = 0.5`, but
```
e ^ 3.30405 / (e ^ 3.30405 + e ^ 0.20926 + e ^ -1.59762 + e ^ -1.92788) = 0.945
```

That's likely due to the *evil of the linear semantics*. To compare:


If we change the chain size from 1000 to 100:
```
select * from dd_inference_result_weights_mapping;
 wid | isfixed | initvalue |      description       |  weight
-----+---------+-----------+------------------------+-----------
   2 | f       |         0 | inf_and_tags_tags--H-T |   1.56867
   0 | f       |         0 | inf_and_tags_tags--T-H |  0.160969
   1 | f       |         0 | inf_and_tags_tags--T-T | -0.989042
   3 | f       |         0 | inf_and_tags_tags--H-H | -0.772551

e ^ 1.56867 / (e ^ 1.56867 + e ^ 0.160969 + e ^ -0.989042 + e ^ -0.772551) = 0.705
```

And if we change chain size to 30:
```
select * from dd_inference_result_weights_mapping;
 wid | isfixed | initvalue |      description       |   weight
-----+---------+-----------+------------------------+-------------
   2 | f       |         0 | inf_and_tags_tags--H-T |    0.899476
   0 | f       |         0 | inf_and_tags_tags--T-H | 0.000851062
   1 | f       |         0 | inf_and_tags_tags--T-T |   -0.420835
   3 | f       |         0 | inf_and_tags_tags--H-H |   -0.491091

e ^ 0.899476 / (e ^ 0.899476 + e ^ 0.000851062 + e ^ -0.420835 + e ^ -0.491091) = 0.520
```
