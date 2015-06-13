# Spouse Example with Incremental Development Support

See the [full documentation](http://deepdive.stanford.edu/doc/advanced/incremental.html) for more details.

## Incremental Workflow Synopsis

1. `export DBNAME=deepdive_spouse_inc`
2. `./0-setup.sh                 spouse_example.f1.ddl       inc-base.out`
3. `./1-materialization_phase.sh spouse_example.f1.ddl       inc-base.out  spouse_example.f1.active.vars spouse_example.f1.active.rules`
4. `./2-incremental_phase.sh     spouse_example.f2.ddl       inc-base.out  inc-f1+f2.out`
7. `./4-merge.sh                 spouse_example.f2.ddl                     inc-f1+f2.out`
6. `./2-incremental_phase.sh     spouse_example.symmetry.ddl inc-base.out  inc-f1+f2+symmetry.out`
5. `./3-cleanup.sh               spouse_example.symmetry.ddl               inc-f1+f2+symmetry.out`

## Non-incremental Workflow

One full run:

1. `export DBNAME=deepdive_spouse_noninc`
2. `./0-setup.sh              spouse_example.f1+f2.ddl       noninc-f1+f2.out`
3. `./9-nonincremental_run.sh spouse_example.f1+f2.ddl       noninc-f1+f2.out`

Another full run:

1. `export DBNAME=deepdive_spouse_noninc2`
2. `./0-setup.sh              spouse_example.f1+f2+symmetry.ddl noninc-f1+f2+symmetry.out`
3. `./9-nonincremental_run.sh spouse_example.f1+f2+symmetry.ddl noninc-f1+f2+symmetry.out`
