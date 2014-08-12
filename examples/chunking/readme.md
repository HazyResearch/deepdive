# Example: Text chunking

## [Introduction](http://www.cnts.ua.ac.be/conll2000/chunking/)

Text chunking consists of dividing a text in syntactically correlated parts of words. For example, the sentence He reckons the current account deficit will narrow to only # 1.8 billion in September . can be divided as follows:

  [NP He ] [VP reckons ] [NP the current account deficit ] [VP will narrow ] [PP to ] [NP only # 1.8 billion ] [PP in ] [NP September ] .

Text chunking is an intermediate step towards full parsing. It was the shared task for CoNLL-2000. Training and test data for this task is derived from the Wall Street Journal corpus (WSJ), which includes words, part-of-speech tags, and chunking tags.

In the example, we include three inference rules, corresponding to logistic regression, linear-chain conditional random field, and skip-chain conditional random field. The features we use are extremely simple, including the word, the pos tag of this word, and pos tag of previous word.


## Runing

1. Run run.sh
2. Run result/eval.sh to evaluate the results


## Directory Structure

- `data` contains training and testing data
- `udf` contains extractor for extracting training data and features
- `result` contains evaluation scripts and sample results


## Evaluation Results

Logistic Regression
  processed 47377 tokens with 23852 phrases; found: 23642 phrases; correct: 19156.
  accuracy:  89.56%; precision:  81.03%; recall:  80.31%; FB1:  80.67
               ADJP: precision:  50.40%; recall:  42.92%; FB1:  46.36  373
               ADVP: precision:  69.21%; recall:  71.13%; FB1:  70.16  890
              CONJP: precision:   0.00%; recall:   0.00%; FB1:   0.00  13
               INTJ: precision: 100.00%; recall:  50.00%; FB1:  66.67  1
                LST: precision:   0.00%; recall:   0.00%; FB1:   0.00  0
                 NP: precision:  79.88%; recall:  77.52%; FB1:  78.68  12055
                 PP: precision:  90.51%; recall:  89.59%; FB1:  90.04  4762
                PRT: precision:  66.39%; recall:  76.42%; FB1:  71.05  122
               SBAR: precision:  83.51%; recall:  71.96%; FB1:  77.31  461
                 VP: precision:  79.48%; recall:  84.71%; FB1:  82.01  4965


LR + Linear-Chain CRF
  processed 47377 tokens with 23852 phrases; found: 22996 phrases; correct: 19746.
  accuracy:  91.58%; precision:  85.87%; recall:  82.79%; FB1:  84.30
                   : precision:   0.00%; recall:   0.00%; FB1:   0.00  1
               ADJP: precision:  75.74%; recall:  69.86%; FB1:  72.68  404
               ADVP: precision:  76.47%; recall:  73.56%; FB1:  74.99  833
              CONJP: precision:  25.00%; recall:  22.22%; FB1:  23.53  8
               INTJ: precision:  50.00%; recall:  50.00%; FB1:  50.00  2
                LST: precision:   0.00%; recall:   0.00%; FB1:   0.00  0
                 NP: precision:  82.22%; recall:  77.19%; FB1:  79.63  11662
                 PP: precision:  93.43%; recall:  94.26%; FB1:  93.84  4854
                PRT: precision:  66.67%; recall:  69.81%; FB1:  68.20  111
               SBAR: precision:  84.93%; recall:  74.77%; FB1:  79.52  471
                 VP: precision:  90.37%; recall:  90.21%; FB1:  90.29  4650

LR + Linear-Chain CRF + Skip-Chain CRF
  processed 47377 tokens with 23852 phrases; found: 22950 phrases; correct: 19794.
  accuracy:  91.79%; precision:  86.25%; recall:  82.99%; FB1:  84.59
                   : precision:   0.00%; recall:   0.00%; FB1:   0.00  1
               ADJP: precision:  75.25%; recall:  68.72%; FB1:  71.84  400
               ADVP: precision:  76.29%; recall:  73.56%; FB1:  74.90  835
              CONJP: precision:  30.00%; recall:  33.33%; FB1:  31.58  10
               INTJ: precision: 100.00%; recall:  50.00%; FB1:  66.67  1
                LST: precision:   0.00%; recall:   0.00%; FB1:   0.00  0
                 NP: precision:  82.96%; recall:  77.54%; FB1:  80.16  11611
                 PP: precision:  93.70%; recall:  94.30%; FB1:  94.00  4842
                PRT: precision:  66.67%; recall:  69.81%; FB1:  68.20  111
               SBAR: precision:  83.37%; recall:  74.95%; FB1:  78.94  481
                 VP: precision:  90.34%; recall:  90.34%; FB1:  90.34  4658