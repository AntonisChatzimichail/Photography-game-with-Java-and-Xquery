let $players := doc("players.xml")
for $pl in $players/players/player
where data($pl/@username) eq "achatzimichail"
return data($pl/password)