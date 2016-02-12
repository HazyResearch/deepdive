select rule_id, (count(p1_id)::float/(
	select count(p1_id) 
	from spouse_label 
	where rule_id is null)) as coverage 
from spouse_label 
where rule_id is not null 
group by rule_id;
