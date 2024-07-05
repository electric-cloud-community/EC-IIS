
push (@::gMatchers,
  {
   id =>        "serviceStartedOk",
   pattern =>          q{The(.*)started successfully(.*)},
   action =>           q{
    
              my $description = ((defined $::gProperties{"summary"}) ? 
                    $::gProperties{"summary"} : '');
                    
              $description .= "$1 started successfully";
                              
              setProperty("summary", $description . "\n");
    
   },
  },
  {
   id =>        "serviceStoppedOk",
   pattern =>          q{(.*)stopped successfully(.*)},
   action =>           q{
    
              my $description = ((defined $::gProperties{"summary"}) ? 
                    $::gProperties{"summary"} : '');
                    
              $description .= "$1 stopped successfully";
                              
              setProperty("summary", $description . "\n");
    
   },
  },
  
  {
   id =>        "servicesStarted",
   pattern =>          q{(Successfully started .+)},
   action =>           q{
    
              my $description = "$1";
                              
              setProperty("summary", $description . "\n");
    
   },
  },
  
  {
   id =>        "servicesStopped",
   pattern =>          q{(Successfully stopped .+)},
   action =>           q{
    
              my $description = "$1";
                              
              setProperty("summary", $description . "\n");
    
   },
  },
  
);

