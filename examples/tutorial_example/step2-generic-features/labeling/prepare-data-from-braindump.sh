# This script copies the latest samples in the report to the task directory
set -e
APP_HOME=`cd $(dirname $0)/../; pwd`

TASK_DIR="$APP_HOME/labeling/spouse_example-precision_with_features"
rm -f $TASK_DIR/tags.json
cp -f $APP_HOME/experiment-reports/latest/inference/has_spouse.csv $TASK_DIR/input.csv
echo "Mindtagger input file copied to $TASK_DIR/input.csv"

TASK_DIR="$APP_HOME/labeling/spouse_example-supervision-positive"
rm -f $TASK_DIR/tags.json
cp -f $APP_HOME/experiment-reports/latest/supervision/has_spouse_true.csv $TASK_DIR/input.csv
echo "Mindtagger input file copied to $TASK_DIR/input.csv"

TASK_DIR="$APP_HOME/labeling/spouse_example-supervision-negative"
rm -f $TASK_DIR/tags.json
cp -f $APP_HOME/experiment-reports/latest/supervision/has_spouse_false.csv $TASK_DIR/input.csv
echo "Mindtagger input file copied to $TASK_DIR/input.csv"