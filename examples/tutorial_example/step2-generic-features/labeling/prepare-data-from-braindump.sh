# This script copies the latest samples in the report to the task directory
set -e
APP_HOME=`cd $(dirname $0)/../; pwd`
TASK_DIR="$APP_HOME/labeling/spouse_example-precision_with_features"
cp -f $APP_HOME/experiment-reports/latest/inference/has_spouse.csv $TASK_DIR/input.csv
rm -f $TASK_DIR/tags.json
echo "Mindtagger input file copied to $TASK_DIR/input.csv"
