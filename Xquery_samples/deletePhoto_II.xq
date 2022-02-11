declare variable $doc external;
for $ex in $doc/exist_in_rel/exist_in
where data($ex/@fk_filename) eq "bird.jpg" and
data($ex/@fk_username) eq "mliagka"
return delete nodes $ex