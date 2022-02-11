declare variable $doc external;
for $ph in $doc/photos/photo
where data($ph/@filename) eq "bird.jpg" and
data($ph/@fk_username) eq "mliagka"
return delete nodes $ph
