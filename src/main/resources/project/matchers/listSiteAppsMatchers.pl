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
   id =>        "stats",
   pattern =>          q{APP "(.+)" \(applicationPool:(.+)\)},
   action =>           q{
            incValueWithString(
                "apps", 
                "Applications: 0 detected");
            
            updateSummary();
   },
  },
  
  {
   id =>        "error1",
   pattern =>          q{ERROR \( message:(.+)\)},
   action =>           q{
    
              my $description = "$1";
              setProperty("summary", $description . "\n");
    
   },
  },
  
  {
   id =>        "error2",
   pattern =>          q{.*(Failed to process input).*},
   action =>           q{
    
              my $description = "$1. Check entered search criteria.";
              setProperty("summary", $description . "\n");
    
   },
  },
  
  
  
);


sub incValueWithString($;$$) {
 
    my ($name, $patternString, $increment) = @_;

    $increment = 1 unless defined($increment);

    my $localString = (defined $::gProperties{$name}) ? $::gProperties{$name} :
                                                        $patternString;

    $localString =~ /([^\d]*)(\d+)(.*)/;
    my $leading = $1;
    my $numeric = $2;
    my $trailing = $3;
    
    $numeric += $increment;
    $localString = $leading . $numeric . $trailing;

    setProperty ($name, $localString);
}

sub updateSummary() {
 
    my $summary = (defined $::gProperties{"apps"}) ? $::gProperties{"apps"} . "\n" : '';
	setProperty ('summary', $summary);
}

