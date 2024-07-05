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
   id =>        "appPoolStarted",
   pattern =>          q{"(.+)" successfully started},
   action =>           q{
    
              my $description = "Application Pool $1 \n started successfully";
                              
              setProperty("summary", $description . "\n");
    
   },
  },
  
  {
   id =>        "appPoolNotFound",
   pattern =>          q{.*Cannot find APPPOOL object with identifier "(.+)".*},
   action =>           q{
    
              my $description = "Cannot find Application \n Pool $1";
                              
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

