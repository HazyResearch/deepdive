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

- Create a *deepdive_ocr* database (`createdb deepdive_ocr`)
- Change the application.conf `db.default.user` entry to yours.
- If necessary, add database connection details to `run.sh`
- Execute `run.sh`

Results
----

- Feature analysis and system calibration result is in `output/`.
- For details, enter your database specified in `application.conf`,
  and examine the result relations.
