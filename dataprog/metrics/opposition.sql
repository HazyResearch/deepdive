-- This is the magnitude of the support for each rule
with per_rule_totals as (
	select rule_id, count(label) as total
	from spouse_label
	where rule_id is not null
	group by rule_id
),
redundancy_per_rule as (
	with per_candidate_redundancy as (
		-- this is just spouse_label without the null rules
		with rules as (select * from spouse_label where rule_id is not null)
		select a.rule_id, a.p1_id, a.p2_id, count(a.label) as redundancy 
		from rules as a
		join rules as b
		using (p1_id, p2_id)
		where a.rule_id <> b.rule_id and a.label <> b.label 
		group by a.rule_id, a.p1_id, a.p2_id
	)
	-- cases of redundancy grouped by the rule
	select rule_id, count(redundancy) as redundancy_count
	from per_candidate_redundancy
	group by rule_id
)
-- Simply combine the above two tabels to get the ratio of the total
select totals.rule_id, redundancy_count, total, (per_rule.redundancy_count::float / total) as ratio
from per_rule_totals as totals, redundancy_per_rule as per_rule
where totals.rule_id = per_rule.rule_id;

