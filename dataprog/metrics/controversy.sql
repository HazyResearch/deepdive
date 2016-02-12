-- this runs very slowly.  Also the keys are not a set, so A,B and B,A both appear
--create temp view candidates as
with candidates as (
select p1_id, p2_id, sum(label) as sum, count(label) as count 
from spouse_label
where rule_id is not null
group by p1_id, p2_id)

select p1_id, p2_id, 1 - @(sum::float / count) as controversy 
from candidates;
