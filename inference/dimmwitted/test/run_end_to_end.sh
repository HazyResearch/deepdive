#!/usr/bin/env bash
# run_end_to_end.sh -- Runs dw on a factor graph specified as a set of TSV files
set -euo pipefail

# convert the factor graph from tsv to binary format
for what in variable domain factor weight; do
    for tsv in "$what"s*.tsv; do
        [[ -e "$tsv" ]] || continue
        dw text2bin "$what" "$tsv" graph."${tsv%.tsv}" /dev/stderr $(
            # use extra text2bin args if specified
            ! [[ -e "${tsv%.tsv}".text2bin-args ]] || cat "${tsv%.tsv}".text2bin-args
        )
    done
done

# run sampler
dw gibbs \
    -w <(cat 2>/dev/null graph.weights*) \
    -v <(cat 2>/dev/null graph.variables*) \
    -f <(cat 2>/dev/null graph.factors*) \
    -m graph.meta \
    -o . \
    --domains <(cat 2>/dev/null graph.domains*) \
    --quiet $(cat dw-args)
