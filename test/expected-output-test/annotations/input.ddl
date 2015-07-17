@textspan(from="start_pos", length="len")
mention_candidate(
  doc_id text,

  @textspan_start
  start_pos int,

  @textspan_length
  len int
).


@foo @bar(x=1) @qux(y=2.345)
R(
    p text,
    q int,
    r float
).
