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
   id =>        "appDeleted",
   pattern =>          q{APP object "(.+)" deleted},
   action =>           q{

              setProperty("outcome", "success");
              setProperty("summary", "Web Application $1 deleted successfully\n");

   },
  },

   {
   id =>        "appNotFound",
   pattern =>          q{ERROR \( message:Cannot find APP object with identifier (.+)\)},
   action =>           q{

              setProperty("outcome", "warning");
              setProperty("summary", "Application $1 not found\n");

   },
  },

   {
   id =>        "appNotSpecified",
   pattern =>          q{ERROR \( message:Must.+APP.+with},
   action =>           q{

              setProperty("outcome", "warning");
              setProperty("summary", "Application name not specified\n");

   },
  },

   {
   id =>        "error",
   pattern =>          q{ERROR \( message:(.+). \)},
   action =>           q{

              setProperty("outcome", "error");
              setProperty("summary", "$1\n");

   },
  },

);

