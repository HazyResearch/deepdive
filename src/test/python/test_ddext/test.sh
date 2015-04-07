# Testing ddext (plpy extractor) functionality
# Specifically, testing if the translated script has proper indent.

DEEPDIVE_HOME=`cd $(dirname $0)/../../../../; pwd`

python $DEEPDIVE_HOME/util/ddext.py input-1.py test-1.py func_ext_has_spouse_candidates
python $DEEPDIVE_HOME/util/ddext.py input-2.py test-2.py func_ext_has_spouse_candidates
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
rm -f test-1.py
rm -f test-2.py
