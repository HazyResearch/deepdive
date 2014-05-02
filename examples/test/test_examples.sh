#! /bin/bash

./../smoke/run.sh 
python test_smoke.py

./../ocr/run.sh 
python test_ocr.py
