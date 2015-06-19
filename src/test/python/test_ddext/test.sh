# Testing ddext (plpy extractor) functionality
# Specifically, testing if the translated script has proper indent.

DEEPDIVE_HOME=`cd $(dirname $0)/../../../../; pwd`

# Get into the testing directory
cd $(dirname $0)

python $DEEPDIVE_HOME/util/ddext.py input-1.py test-1.py func_ext_has_spouse_candidates
python $DEEPDIVE_HOME/util/ddext.py input-2.py test-2.py func_ext_has_spouse_candidates
python $DEEPDIVE_HOME/util/ddext.py input-3.py test-3.py func_test
if ! cmp test-1.py output-1.py
then
        echo "Test failed!"
        exit 1
fi
if ! cmp test-2.py output-2.py
then
        echo "Test failed!"
        exit 2
fi
if ! cmp test-3.py output-3.py
then
  echo "Test failed!"
  exit 3
fi
rm -f test-1.py
rm -f test-2.py
rm -f test-3.py
