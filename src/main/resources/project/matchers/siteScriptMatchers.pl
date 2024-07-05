
push (@::gMatchers,
  {
   id =>        "siteNotFound",
   pattern =>          q{(Site\(s\) not found)},
   action =>           q{
    
              my $description = "$1";
                              
              setProperty("summary", $description . "\n");
    
   },
  },
  
  {
   id =>        "createWebsite",
   pattern =>          q{(Status(\s+)=(\s+)STOPPED|Status(\s+)=(\s+)STARTED)},
   action =>           q{
    
              my $description = "Website successfully created.";
                              
              setProperty("summary", $description . "\n");
    
   },
  },
  
 {
   id =>        "createVDir",
   pattern =>          q{Virtual Path  = (.+)},
   action =>           q{
    
              my $description = "Virtual directory successfully created at $1.";
                              
              setProperty("summary", $description . "\n");
    
   },
  },
  
  {
   id =>        "vdirNameExistsError",
   pattern =>          q{The virtual directory (.+) already exists},
   action =>           q{
    
              my $description = "Virtual directory at $1 already exists. Try a different name.";
                              
              setProperty("summary", $description . "\n");
    
   },
  },
  
  {
   id =>        "pauseWebsite",
   pattern =>          q{Server (.+) has been PAUSED},
   action =>           q{
    
              my $description = "Server $1 has been successfully paused";
                              
              setProperty("summary", $description . "\n");
    
   },
  },
  
  {
   id =>        "websiteContinued",
   pattern =>          q{Server (.+) has been CONTINUED},
   action =>           q{
    
              my $description = "Server $1 has been successfully continued";
                              
              setProperty("summary", $description . "\n");
    
   },
  },
  
    {
   id =>        "websiteStopped",
   pattern =>          q{Server (.+) has been STOPPED},
   action =>           q{
    
              my $description = "Server $1 has been successfully stopped";
                              
              setProperty("summary", $description . "\n");
    
   },
  },
  
  {
   id =>        "serverCannotBeStopped",
   pattern =>          q{Server cannot be stopped in its current state},
   action =>           q{
    
              my $description = "$1";
                              
              setProperty("summary", $description . "\n");
    
   },
  },
  
  {
   id =>        "vdirDoesntExist",
   pattern =>          q{The virtual directory (.+) does not exist},
   action =>           q{
    
              my $description = "The virtual directory $1 does not exist";
                              
              setProperty("summary", $description . "\n");
    
   },
  },
  
  {
   id =>        "vdirDeleted",
   pattern =>          q{Web directory (.+) has been DELETED},
   action =>           q{
    
              my $description = "The virtual directory $1 has been successfully deleted";
                              
              setProperty("summary", $description . "\n");
    
   },
  },
  
  {
   id =>        "siteDeleted",
   pattern =>          q{Server (.+) has been deleted},
   action =>           q{
    
              my $description = "Website $1 successfully deleted";
                              
              setProperty("summary", $description . "\n");
    
   },
  },
  
 
);

