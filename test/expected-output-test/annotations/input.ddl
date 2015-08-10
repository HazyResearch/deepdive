@textspan(from="start_pos", length="len")
mention_candidate(
  doc_id text,

  @textspan_start
  start_pos int,

  @textspan_length
  len int
).


@foo @bar(x=1) @qux(y=2.345)
@unnamed_args_list(1,"two",3.0)
@unnamed_args_list("four",5.6,7)
R(
    p text,
    q int,
    @empty_args()
    r float
).

@fact
has_spouse?(
    @key
    @references(relation="couples", column="id")
    couple_id text
).
