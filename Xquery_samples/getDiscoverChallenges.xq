let $takes_part:= (
let $exist_in := doc("exist_in.xml")
for $ex in $exist_in/exist_in_rel/exist_in
where data($ex/@fk_username) eq "achatzimichail"
return $ex/@fk_title/string()
)
return doc("challenges.xml")/challenges/challenge/@title/string()[not(.=$takes_part)]