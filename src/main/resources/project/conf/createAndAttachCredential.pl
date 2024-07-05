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

##########################
# createAndAttachCredential.pl
##########################

use ElectricCommander;

use constant {
	SUCCESS => 0,
	ERROR   => 1,
};

my $ec = new ElectricCommander();
$ec->abortOnError(0);

my $credName = '$[/myJob/config]';
my $xpath = $ec->getFullCredential("credential");
my $userName = $xpath->findvalue("//userName");
my $password = $xpath->findvalue("//password");

# Create credential
my $projName = '@PLUGIN_KEY@-@PLUGIN_VERSION@';

$ec->deleteCredential($projName, $credName);
$xpath = $ec->createCredential($projName, $credName, $userName, $password);
my $errors = $ec->checkAllErrors($xpath);

# Give config the credential's real name
my $configPath = "/projects/$projName/iis_cfgs/$credName";
$xpath = $ec->setProperty($configPath . "/credential", $credName);
$errors .= $ec->checkAllErrors($xpath);

# Give job launcher full permissions on the credential
my $user = '$[/myJob/launchedByUser]';
$xpath = $ec->createAclEntry("user", $user,
    {projectName => $projName,
     credentialName => $credName,
     readPrivilege => allow,
     modifyPrivilege => allow,
     executePrivilege => allow,
     changePermissionsPrivilege => allow});
$errors .= $ec->checkAllErrors($xpath);

# Attach credential to steps that will need it
$xpath = $ec->attachCredential($projName, $credName,
    {procedureName => 'AddWebSiteBinding',
     stepName => 'AddWebSiteBinding'});
$errors .= $ec->checkAllErrors($xpath);


$xpath = $ec->attachCredential($projName, $credName,
    {procedureName => 'AssignAppToAppPool',
     stepName => 'AssignApp'});
$errors .= $ec->checkAllErrors($xpath);

$xpath = $ec->attachCredential($projName, $credName,
    {procedureName => 'CheckServerStatus',
     stepName => 'CheckServerStatus'});
$errors .= $ec->checkAllErrors($xpath);

$xpath = $ec->attachCredential($projName, $credName,
    {procedureName => 'CreateAppPool',
     stepName => 'CreateAppPool'});
$errors .= $ec->checkAllErrors($xpath);

$xpath = $ec->attachCredential($projName, $credName,
    {procedureName => 'CreateVirtualDirectory',
     stepName => 'CreateVirtualDirectory'});
$errors .= $ec->checkAllErrors($xpath);

$xpath = $ec->attachCredential($projName, $credName,
    {procedureName => 'CreateWebApplication',
     stepName => 'CreateWebApplication'});
$errors .= $ec->checkAllErrors($xpath);

$xpath = $ec->attachCredential($projName, $credName,
    {procedureName => 'DeleteVirtualDirectory',
     stepName => 'DeleteVirtualDirectory'});
$errors .= $ec->checkAllErrors($xpath);

$xpath = $ec->attachCredential($projName, $credName,
    {procedureName => 'DeleteWebApplication',
     stepName => 'DeleteWebApplication'});
$errors .= $ec->checkAllErrors($xpath);

$xpath = $ec->attachCredential($projName, $credName,
    {procedureName => 'DeployCopy',
     stepName => 'Deploy'});
$errors .= $ec->checkAllErrors($xpath);

$xpath = $ec->attachCredential($projName, $credName,
    {procedureName => 'CreateWebSite',
     stepName => 'CreateWebSite'});
$errors .= $ec->checkAllErrors($xpath);
 
$xpath = $ec->attachCredential($projName, $credName,
    {procedureName => 'DeleteWebSite',
     stepName => 'DeleteWebSite'});
$errors .= $ec->checkAllErrors($xpath);
 
$xpath = $ec->attachCredential($projName, $credName,
    {procedureName => 'DeleteAppPool',
     stepName => 'DeleteAppPool'});
$errors .= $ec->checkAllErrors($xpath);
 
$xpath = $ec->attachCredential($projName, $credName,
    {procedureName => 'ListSiteApps',
     stepName => 'ListSiteApps'});
$errors .= $ec->checkAllErrors($xpath);

$xpath = $ec->attachCredential($projName, $credName,
    {procedureName => 'ListSites',
     stepName => 'ListSites'});
$errors .= $ec->checkAllErrors($xpath);

$xpath = $ec->attachCredential($projName, $credName,
    {procedureName => 'StartAppPool',
     stepName => 'StartAppPool'});
$errors .= $ec->checkAllErrors($xpath);

$xpath = $ec->attachCredential($projName, $credName,
    {procedureName => 'StartWebSite',
     stepName => 'StartWebSite'});
$errors .= $ec->checkAllErrors($xpath);

$xpath = $ec->attachCredential($projName, $credName,
    {procedureName => 'StopAppPool',
     stepName => 'StopAppPool'});
$errors .= $ec->checkAllErrors($xpath);

$xpath = $ec->attachCredential($projName, $credName,
    {procedureName => 'StopWebSite',
     stepName => 'StopWebSite'});
$errors .= $ec->checkAllErrors($xpath);
     
if ($errors ne '') {
    
    # Cleanup the partially created configuration we just created
    $ec->deleteProperty($configPath);
    $ec->deleteCredential($projName, $credName);
    my $errMsg = 'Error creating configuration credential: ' . $errors;
    $ec->setProperty("/myJob/configError", $errMsg);
    print $errMsg;
    exit ERROR;
    
}
