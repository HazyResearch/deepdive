Example: Logistic Regression on OCR results
====

Optical Character Recognition (OCR) Systems process scanned text into
text usable by computers. We observe that different OCRs make
independent mistakes. This example uses a simple Logistic Regression
encoded in our system, to select between OCR outputs when they differ.

This example uses outputs from two open-source OCRs for a dataset of 620
words, whose features are already extracted. The dataset is hand-
labeled.

Requirements
----

- PostreSQL
- Python
- Matplotlib (`pip install matplotlib`)

How to run the system
----

- If necessary, modify `db.url` to fill in your database connection details.
- Execute `deepdive do init/app weights`.

Results
----

- Execute `./feature-analysis.sh`.
- Feature analysis and system calibration result are in `output/` and `run/LATEST/calibration/`, respectively.
- For details, run `deepdive sql` to examine the result relations.
