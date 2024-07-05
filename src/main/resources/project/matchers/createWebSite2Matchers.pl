
push (@::gMatchers,
  {
   id =>        "vdirCreated",
   pattern =>          q{WebSite (.*) created successfully with ID (.+)},
   action =>           q{
    
              my $description = "WebSite \"$1\" created successfully.\nWebSite ID: $2.";
              setProperty("summary", $description . "\n");
    
   },
  },
 
);

