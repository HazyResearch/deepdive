curl https://archive.ics.uci.edu/ml/machine-learning-databases/adult/adult.data |
    sed '/^$/d;s/, /,/g' |  # remove last empty line
    awk '{printf "%d,%s\n", NR, $0}'  # prepend record ID
