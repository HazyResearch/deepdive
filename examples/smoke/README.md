Simple DeepDive Example: Smoke Example
====

This is a toy example of a DeepDive application. Here, we want to predict whether a person smokes, and whether a person has cancer.

We have two rules. The first one is saying that if a person smokes, he/she might have a cancer:

```
smokes_cancer {
  input_query: """
      SELECT person_has_cancer.id as "person_has_cancer.id",
             person_smokes.id as "person_smokes.id",
             person_smokes.smokes as "person_smokes.smokes",
             person_has_cancer.has_cancer as "person_has_cancer.has_cancer"
        FROM person_has_cancer, person_smokes
       WHERE person_has_cancer.person_id = person_smokes.person_id
    """
  function: "Imply(person_smokes.smokes, person_has_cancer.has_cancer)"
  weight: 0.5
}
```

The other is saying that if a person's friends smoke, he/she might have cancer:

```
friends_smoke {
	input_query: """
	  SELECT p1.id AS "person_smokes.p1.id",
	         p2.id AS "person_smokes.p2.id",
	         p1.smokes AS "person_smokes.p1.smokes",
	         p2.smokes AS "person_smokes.p2.smokes"
	    FROM friends INNER JOIN person_smokes AS p1 ON
	      (friends.person_id = p1.person_id) INNER JOIN person_smokes AS p2 ON (friends.friend_id = p2.person_id)
	"""
	function: "Imply(person_smokes.p1.smokes, person_smokes.p2.smokes)"
	weight: 0.4
}
```