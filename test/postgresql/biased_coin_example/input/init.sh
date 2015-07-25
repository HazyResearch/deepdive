#!/usr/bin/env bash
set -eu

deepdive sql "
insert into coin(is_correct) values
        (true), (true), (true), (true),
        (true), (true), (true), (true),
        (false),
        (NULL), (NULL), (NULL), (NULL),
        (NULL), (NULL), (NULL), (NULL),
        (NULL);
"
