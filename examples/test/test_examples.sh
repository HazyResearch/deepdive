#! /bin/bash

. "../spouse_example/default_extractor/env.sh"
./../spouse_example/default_extractor/run.sh
python test_spouse.py

. "../smoke/env.sh"
./../smoke/run.sh
python test_smoke.py


. "../ocr/env.sh"
./../ocr/run.sh
python test_ocr.py

