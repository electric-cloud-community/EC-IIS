
push (@::gMatchers,
  {
   id =>        "appPoolRecycled",
   pattern =>          q{Application Pool (.+) recycled successfully},
   action =>           q{
    
              my $description = "Application Pool $1 recycled successfully";
                              
              setProperty("summary", $description . "\n");
    
   },
  },
  
  
  
 
);

