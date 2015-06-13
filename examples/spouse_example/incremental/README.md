# Spouse Example with Incremental Development Support

See the [full documentation](http://deepdive.stanford.edu/doc/advanced/incremental.html) for more details.

## Incremental Workflow Synopsis

1. `export DBNAME=deepdive_spouse_inc`
1. `./0-setup.sh spouse_example.ddl`
2. `./1-materialization_phase.sh spouse_example.ddl base.out  spouse_example.active.vars spouse_example.active.rules`
3. `./2-incremental_phase.sh spouse_example.f2.ddl inc.out base.out`
4. `./3-cleanup.sh spouse_example.ddl`
3. `./2-incremental_phase.sh spouse_example.symmetry_rule.ddl inc2.out base.out`
5. `./4-merge.sh spouse_example.ddl`

## Non-incremental Workflow

1. `export DBNAME=deepdive_spouse_ddlog`
1. `./0-setup.sh spouse_example.ddl spouse_example.application.out normal`
1. `./1-nonincremental_run.sh spouse_example.ddl  spouse_example.application.out`
