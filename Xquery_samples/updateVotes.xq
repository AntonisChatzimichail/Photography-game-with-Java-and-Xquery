(: A photo in a challenge has been voted. Its votes are increased by 1. :)
declare variable $doc external;
for $ex in $doc/exist_in_rel/exist_in
where data($ex/@fk_filename) eq "christmas_cat.jpg" and
data($ex/@fk_username) eq "achatzimichail" and
data($ex/@fk_title) eq "Favorite Animal"
return replace value of node $ex/votes
with {xs:int($ex/votes)+1}