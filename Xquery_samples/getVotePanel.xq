let $exist_in := doc("exist_in.xml")
for $ex in $exist_in/exist_in_rel/exist_in
where data($ex/@fk_title) eq "Favorite Animal"
and data($ex/@fk_username) ne "achatzimichail"
return ( data($ex/@fk_filename), data($ex/@fk_username) )