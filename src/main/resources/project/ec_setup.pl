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

if ($upgradeAction eq "upgrade") {
    my $query = $commander->newBatch();
    my $newcfg = $query->getProperty(
        "/plugins/$pluginName/project/iis_cfgs");
    my $oldcfgs = $query->getProperty(
        "/plugins/$otherPluginName/project/iis_cfgs");
	my $creds = $query->getCredentials(
        "\$[/plugins/$otherPluginName]");

	local $self->{abortOnError} = 0;
    $query->submit();

    # if new plugin does not already have cfgs
    if ($query->findvalue($newcfg,"code") eq "NoSuchProperty") {
        # if old cfg has some cfgs to copy
        if ($query->findvalue($oldcfgs,"code") ne "NoSuchProperty") {
            $batch->clone({
                path => "/plugins/$otherPluginName/project/iis_cfgs",
                cloneName => "/plugins/$pluginName/project/iis_cfgs"
            });
        }
    }


    my $olddiscovery = $query->getProperty("/plugins/$otherPluginName/project/ec_discovery/discovered_data");

    local $self->{abortOnError} = 0;
    $query->submit();


    # Copy discovered data
    if ($query->findvalue($olddiscovery, "code") ne "NoSuchProperty") {
        $batch->clone({
            path => "/plugins/$otherPluginName/project/ec_discovery/discovered_data",
            cloneName => "/plugins/$pluginName/project/ec_discovery/discovered_data"
        });
    }

    # my $old_debug_level = $query->getProperty("/plugins/$otherPluginName/project/debugLevel");
    # local $self->{abortOnError} = 0;
    # $query->submit();
    # if ($query->findvalue($old_debug_level, 'code') ne 'NoSuchProperty') {
    #     $batch->clone({
    #         path => "/plugins/$otherPluginName/project/debugLevel",
    #         cloneName => "/plugins/$pluginName/project/debugLevel"
    #     });
    # }


    # Copy configuration credentials and attach them to the appropriate steps
    my $nodes = $query->find($creds);
    if ($nodes) {
        my @nodes = $nodes->findnodes('credential/credentialName');
        for (@nodes) {
            my $cred = $_->string_value;

            # Clone the credential
            $batch->clone({
                path => "/plugins/$otherPluginName/project/credentials/$cred",
                cloneName => "/plugins/$pluginName/project/credentials/$cred"
            });

            # Make sure the credential has an ACL entry for the new project principal
            my $xpath = $commander->getAclEntry("user", "project: $pluginName", {
                projectName => $otherPluginName,
                credentialName => $cred
            });
            if ($xpath->findvalue("//code") eq "NoSuchAclEntry") {
                $batch->deleteAclEntry("user", "project: $otherPluginName", {
                    projectName => $pluginName,
                    credentialName => $cred
                });
                $batch->createAclEntry("user", "project: $pluginName", {
                    projectName => $pluginName,
                    credentialName => $cred,
                    readPrivilege => 'allow',
                    modifyPrivilege => 'allow',
                    executePrivilege => 'allow',
                    changePermissionsPrivilege => 'allow'
                });
            }

            $batch->attachCredential("\$[/plugins/$pluginName/project]", $cred, {
                procedureName => 'CheckServerStatus',
                stepName => 'CheckServerStatus'
            });

        }
    }
}

my %checkServerStatus = (
    label       => "IIS - Check Server Status",
    procedure   => "CheckServerStatus",
    description => "Checks the status of the specified server.",
    category    => "Application Server"
);
my %addWebSiteBinding = (
    label       => "IIS - Add Website Binding",
    procedure   => "AddWebSiteBinding",
    description => "Adds a binding to the website.",
    category    => "Application Server"
);
my %assignAppToAppPool = (
    label       => "IIS - Assign App To App Pool",
    procedure   => "AssignAppToAppPool",
    description => "Assigns an application to an application pool.",
    category    => "Application Server"
);
my %createAppPool = (
    label       => "IIS - Create App Pool",
    procedure   => "CreateAppPool",
    description => "Creates an IIS application pool.",
    category    => "Application Server"
);
my %createVirtualDirectory = (
    label       => "IIS - Create Virtual Directory",
    procedure   => "CreateVirtualDirectory",
    description => "Creates a new virtual directory in the specified website.",
    category    => "Application Server"
);
my %createWebApplication = (
    label       => "IIS - Create Web Application",
    procedure   => "CreateWebApplication",
    description => "Creates and starts an in-process web application in the given directory.",
    category    => "Application Server"
);
my %createWebSite = (
    label       => "IIS - Create Website",
    procedure   => "CreateWebSite",
    description => "Creates a website configuration on a local or remote computer.",
    category    => "Application Server"
);
my %deleteAppPool = (
    label       => "IIS - Delete App Pool",
    procedure   => "DeleteAppPool",
    description => "Deletes an application pool.",
    category    => "Application Server"
);
my %deleteVirtualDirectory = (
    label       => "IIS - Delete Virtual Directory",
    procedure   => "DeleteVirtualDirectory",
    description => "Deletes a virtual directory from the specified website.",
    category    => "Application Server"
);
my %deleteWebApplication = (
    label       => "IIS - Delete Web Application",
    procedure   => "DeleteWebApplication",
    description => "Deletes a web application.",
    category    => "Application Server"
);
my %deleteWebSite = (
    label       => "IIS - Delete Website",
    procedure   => "DeleteWebSite",
    description => "Deletes a website.",
    category    => "Application Server"
);
my %deployCopy = (
    label       => "IIS - Deploy Copy",
    procedure   => "DeployCopy",
    description => "Copies the application files recursively to the website application's physical directory (DEPRECATED).",
    category    => "Application Server"
);
my %deploy = (
    label       => "IIS - Deploy",
    procedure   => "Deploy",
    description => "Deploys an application using MS Deploy.",
    category    => "Application Server"
);
my %undeploy = (
    label       => "IIS - Undeploy",
    procedure   => "Undeploy",
    description => "Removes previously deployed content",
    category    => "Application Server"
);
my %deployAdvanced = (
    label       => "IIS - Deploy Advanced",
    procedure   => "DeployAdvanced",
    description => "Uses MS Deploy to deploy an application.",
    category    => "Application Server"
);
my %listSiteApps = (
    label       => "IIS - List Site Apps",
    procedure   => "ListSiteApps",
    description => "List the apps of a Website.",
    category    => "Application Server"
);
my %listSites = (
    label       => "IIS - List Sites",
    procedure   => "ListSites",
    description => "List the sites on a web server.",
    category    => "Application Server"
);
my %listApplicationPools = (
    label       => "IIS - List Application Pools",
    procedure   => "ListApplicationPools",
    description => "List the application pools on a web server.",
    category    => "Application Server"
);
my %startAppPool = (
    label       => "IIS - Start App Pool",
    procedure   => "StartAppPool",
    description => "Starts an IIS application pool.",
    category    => "Application Server"
);
my %startWebSite = (
    label       => "IIS - Start Website",
    procedure   => "StartWebSite",
    description => "Starts a website.",
    category    => "Application Server"
);
my %stopAppPool = (
    label       => "IIS - Stop App Pool",
    procedure   => "StopAppPool",
    description => "Stops an IIS application pool.",
    category    => "Application Server"
);
my %stopWebSite = (
    label       => "IIS - Stop Website",
    procedure   => "StopWebSite",
    description => "Stops an IIS Website.",
    category    => "Application Server"
);

my %resetServer = (
    label       => "IIS - Reset Server",
    procedure   => "ResetServer",
    description => "Uses the iisreset utility to stop a server.",
    category    => "Application Server"
);
my %startServer = (
    label       => "IIS - Start Server",
    procedure   => "StartServer",
    description => "Uses the iisreset utility to start a server.",
    category    => "Application Server"
);
my %stopServer = (
    label       => "IIS - Stop Server",
    procedure   => "StopServer",
    description => "Uses the iisreset utility to stop a server.",
    category    => "Application Server"
);
my %recycleAppPool = (
    label       => "IIS - Recycle App Pool",
    procedure   => "RecycleAppPool",
    description => "Uses the appcmd utility to recycle an app pool.",
    category    => "Application Server"
);
my %listVdirs = (
    label       => "IIS - List Virtual Directories",
    procedure   => "ListVirtualDirectories",
    description => "List the virtual directories on a web server.",
    category    => "Application Server"
);
my %addSSlCertificate = (
    label       => "IIS - Add SSL Certificate",
    procedure   => "AddSSLCertificate",
    description => "Add SSL certificate to the specified port.",
    category    => "Application Server"
);

$batch->deleteProperty("/server/ec_customEditors/pickerStep/IIS - Check Server Status");
$batch->deleteProperty("/server/ec_customEditors/pickerStep/IIS - Add Website Binding");
$batch->deleteProperty("/server/ec_customEditors/pickerStep/IIS - Assign App To App Pool");
$batch->deleteProperty("/server/ec_customEditors/pickerStep/IIS - Create App Pool");
$batch->deleteProperty("/server/ec_customEditors/pickerStep/IIS - Create Virtual Directory");
$batch->deleteProperty("/server/ec_customEditors/pickerStep/IIS - Create Web Application");
$batch->deleteProperty("/server/ec_customEditors/pickerStep/IIS - Create Website");
$batch->deleteProperty("/server/ec_customEditors/pickerStep/IIS - Create Web site");
$batch->deleteProperty("/server/ec_customEditors/pickerStep/IIS - Delete App Pool");
$batch->deleteProperty("/server/ec_customEditors/pickerStep/IIS - Delete Virtual Directory");
$batch->deleteProperty("/server/ec_customEditors/pickerStep/IIS - Delete Web Application");
$batch->deleteProperty("/server/ec_customEditors/pickerStep/IIS - Delete Website");
$batch->deleteProperty("/server/ec_customEditors/pickerStep/IIS - Delete Web site");
$batch->deleteProperty("/server/ec_customEditors/pickerStep/IIS - Deploy Copy");
$batch->deleteProperty("/server/ec_customEditors/pickerStep/IIS - Deploy Advanced");
$batch->deleteProperty("/server/ec_customEditors/pickerStep/IIS - Undeploy");
$batch->deleteProperty("/server/ec_customEditors/pickerStep/IIS - List Site Apps");
$batch->deleteProperty("/server/ec_customEditors/pickerStep/IIS - List Sites");
$batch->deleteProperty("/server/ec_customEditors/pickerStep/IIS - Start App Pool");
$batch->deleteProperty("/server/ec_customEditors/pickerStep/IIS - Start Website");
$batch->deleteProperty("/server/ec_customEditors/pickerStep/IIS - Stop App Pool");
$batch->deleteProperty("/server/ec_customEditors/pickerStep/IIS - Stop Website");
$batch->deleteProperty("/server/ec_customEditors/pickerStep/IIS - Reset Server");
$batch->deleteProperty("/server/ec_customEditors/pickerStep/IIS - Start Server");
$batch->deleteProperty("/server/ec_customEditors/pickerStep/IIS - Stop Server");
$batch->deleteProperty("/server/ec_customEditors/pickerStep/IIS - Create Web Site 2");
$batch->deleteProperty("/server/ec_customEditors/pickerStep/IIS - Create Virtual Directory 2");
$batch->deleteProperty("/server/ec_customEditors/pickerStep/IIS - Recycle App Pool");
$batch->deleteProperty("/server/ec_customEditors/pickerStep/IIS - Resume WebSite");
$batch->deleteProperty("/server/ec_customEditors/pickerStep/IIS - Get WebSite IDs");
$batch->deleteProperty("/server/ec_customEditors/pickerStep/IIS - Get Virtual Directories");
$batch->deleteProperty("/server/ec_customEditors/pickerStep/IIS - Stop IIS Services");
$batch->deleteProperty("/server/ec_customEditors/pickerStep/IIS - Start IIS Services");
$batch->deleteProperty("/server/ec_customEditors/pickerStep/IIS - Pause WebSite");
$batch->deleteProperty("/server/ec_customEditors/pickerStep/IIS - Get WebSite Status");
$batch->deleteProperty("/server/ec_customEditors/pickerStep/IIS - List Application Pools");
$batch->deleteProperty("/server/ec_customEditors/pickerStep/IIS - List Virtual Directories");
$batch->deleteProperty("/server/ec_customEditors/pickerStep/IIS - Add SSL Certificate");

@::createStepPickerSteps = (\%checkServerStatus, \%addWebSiteBinding,
                            \%assignAppToAppPool, \%createAppPool,
                            \%createVirtualDirectory, \%createWebApplication,
                            \%createWebSite, \%deleteAppPool,
                            \%deleteVirtualDirectory, \%deleteWebApplication,
                            \%deleteWebSite, \%deployCopy,
                            \%listSiteApps, \%listSites,
                            \%startAppPool, \%startWebSite,
                            \%stopAppPool, \%stopWebSite, \%deployAdvanced,
                            \%undeploy, \%deploy,
                            \%startServer, \%stopServer, \%resetServer,
                            \%recycleAppPool, \%listApplicationPools,
                            \%listVdirs, \%addSSlCertificate
);
