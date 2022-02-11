declare variable $doc external;
insert nodes 
   <photo filename="test.jpg" fk_username="test">
      <views>0</views>
   </photo> 
as last into $doc/photos