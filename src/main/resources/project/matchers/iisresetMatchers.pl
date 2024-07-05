
push (@::gMatchers,
  {
   id =>        "serverStarted",
   pattern =>          q{(Internet services successfully started)},
   action =>           q{
    
              my $description = "$1";
                              
              setProperty("summary", $description . "\n");
    
   },
  },
  
 {
   id =>        "serverStopped",
   pattern =>          q{(Internet services successfully stopped)},
   action =>           q{
    
              my $description =  "$1";
                              
              setProperty("summary", $description . "\n");
    
   },
  },

  {
   id =>        "serverRestarted",
   pattern =>          q{(Internet services successfully restarted)},
   action =>           q{
    
              my $description = "$1";
                              
              setProperty("summary", $description . "\n");
    
   },
  },

);

