
push (@::gMatchers,
  {
   id =>        "vdirExists",
   pattern =>          q{.*-2147024713.*},
   action =>           q{
    
              my $description = "The virtual directory ID already exists in the WebSite.";
              setProperty("summary", $description . "\n");
    
   },
  },
  
  {
   id =>        "vdirCreated",
   pattern =>          q{Virtual directory (.+) successfully created},
   action =>           q{
    
              my $description = "$1 successfully created";
              setProperty("summary", $description . "\n");
    
   },
  },
 
);

