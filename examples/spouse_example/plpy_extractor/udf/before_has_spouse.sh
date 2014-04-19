#! /usr/bin/env bash

psql -c "TRUNCATE has_spouse CASCADE;" deepdive_spouse
