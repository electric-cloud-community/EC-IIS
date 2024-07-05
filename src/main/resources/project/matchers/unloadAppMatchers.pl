
push (@::gMatchers,
    {
   id =>        "appUnloaded",
   pattern =>          q{Application (.+) successfully unloaded},
   action =>           q{
    
              my $description = "Application Pool $1 successfully unloaded";
                              
              setProperty("summary", $description . "\n");
    
   },
  },
);

