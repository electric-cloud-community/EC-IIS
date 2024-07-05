#
#  Copyright 2015 Electric Cloud, Inc.
#
#  Licensed under the Apache License, Version 2.0 (the "License");
#  you may not use this file except in compliance with the License.
#  You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
#  limitations under the License.
#


push (@::gMatchers,
  
  {
   id =>        "appChanged",
   pattern =>          q{Application (.+) moved to Application Pool (.+)},
   action =>           q{
    
              my $description = "Application $1 moved \n to Application Pool $2";
              setProperty("summary", $description . "\n");
    
   },
  },
  
  {
   id =>        "appPoolCreated",
   pattern =>          q{APPPOOL object "(.+)" added},
   action =>           q{
    
              my $description = "Application Pool $1 \n created successfully";
              setProperty("summary", $description . "\n");
    
   },
  },
  
  {
   id =>        "appPoolSiteNotFound",
   pattern =>          q{.*Cannot find SITE object with identifier "(.+)".*},
   action =>           q{
    
              my $description = "Site $1 \n doesn't exist";
              setProperty("summary", $description . "\n");
    
   },
  },
  
  {
   id =>        "appPoolNotFound",
   pattern =>          q{Application Pool (.+) doesn't exist},
   action =>           q{
    
              my $description = "Application Pool \n $1 doesn't exist";
              setProperty("summary", $description . "\n");
    
   },
  },
  
  
   {
   id =>        "error",
   pattern =>          q{ERROR \( message:(.+)\)},
   action =>           q{
    
              my $description = "$1";
              setProperty("summary", $description . "\n");
    
   },
  },
  
  
  
);

