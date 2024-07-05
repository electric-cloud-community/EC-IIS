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
   id =>        "appCreated",
   pattern =>          q{APP object "(.+)" added},
   action =>           q{

              setProperty("outcome", "success");
              setProperty("summary", "Web Application $1 created successfully\n");

   },
  },

  {
   id => "warning",
   pattern => q{ERROR \( message:Failed to add duplicate collection element (.+)\)},
   action => q{

              setProperty("outcome", "warning");
              setProperty("summary", "Duplicate collection element $1\n");

   },
  },

  {
   id =>        "error",
   pattern =>          q{ERROR \( message:(.+)\)},
   action =>           q{

              setProperty("outcome", "error");
              setProperty("summary", "$1\n");

   },
  },

);
