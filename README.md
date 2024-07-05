# EC IIS plugin

The IIS plugin allows users to interact with the Internet Information
Services Server 7 (IIS7) and higher versions and accomplish tasks such
as managing website and virtual directories using the specific tools of
IIS7 through the command line. Using this plugin, users can configure
IIS from scripts or executables, run tasks as create a virtual directory
and create a website. Also, users can check the status of running
processes to see if they ran successfully or if an error existed, and
determine what caused the error.

The IIS plugin uses `appcmd.exe` to interact with IIS, so the procedures
run on the resource with the IIS server.

The **Deploy** and **AdvancedDeploy** procedures use the WebDeploy
utility. WebDeploy must be installed separately; it is not included in
the default IIS setup. It can be downloaded from the [Microsoft
site](https://www.iis.net/downloads/microsoft/web-deploy). After
installation, WebDeploy can typically be found in
`C:\Program Files\IIS\Microsoft Web Deploy V3` directory.

## Prerequisites

This plugin uses an updated version of Perl, cb-perl shell (Perl v5.32),
and requires CloudBees CD/RO agents version 10.3 or later to work.

## Integrated version

The IIS plugin supports the following versions of IIS:

-   7.0

-   7.5

-   8.0

-   8.5

-   10

The IIS plugin supports the following versions of Web Deploy:

-   3.5

-   3.6

## Compile

Run gradlew to compile the plugin

`./gradlew`

## Create IIS plugin configurations

Plugin configurations are sets of parameters that can be applied across
some, or all, plugin procedures. They can reduce the repetition of
common values, create predefined parameter sets, and securely store
credentials. Each configuration is given a unique name that is entered
in the designated parameter for the plugin procedures that use them. The
following steps illustrate how to create a plugin configuration that can
be used by one or more plugin procedures.

To create a plugin configuration:

1.  Navigate to **DevOps Essentials &gt; Plugin Management &gt; Plugin
    configurations**.

2.  Select **Add plugin configuration** to create a new configuration.

3.  In the **New Configuration** window, specify a **Name** for the
    configuration.

4.  Select the **Project** that the configuration belongs to.

5.  Optionally, add a **Description** for the configuration.

6.  Select the appropriate **Plugin** for the configuration.

7.  Configure the plugin configuration parameters.

    For more information, refer to [CloudBees CD/RO
    plugins](https://docs.cloudbees.com/docs/cloudbees-cd-plugin-docs/latest/).

8.  Select **OK**.

Depending on your plugin configuration and how you run procedures, the
**Input parameters &gt; Configuration name** field may behave
differently in the CloudBees CD/RO UI. For more information, refer to
[Differences in plugin UI
behavior](https://docs.cloudbees.com/docs/cloudbees-cd-plugin-docs/latest/#plugin-ui-differences).

### IIS plugin configuration parameters

The configuration is only used with the [CheckServerStatus](#checkserverstatus) procedure.

<table>
<colgroup>
<col style="width: 50%" />
<col style="width: 50%" />
</colgroup>
<thead>
<tr class="header">
<th style="text-align: left;">Parameter</th>
<th style="text-align: left;">Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td style="text-align: left;"><p>Configuration Name</p></td>
<td style="text-align: left;"><p>Required. The unique name for the
configuration.</p></td>
</tr>
<tr class="even">
<td style="text-align: left;"><p>IIS IP Address</p></td>
<td style="text-align: left;"><p>URL for the IIS server. It must include
the protocol (for example, <code>\http://192.168.1.100</code>).</p></td>
</tr>
<tr class="odd">
<td style="text-align: left;"><p>IIS Port</p></td>
<td style="text-align: left;"><p>Port for the IIS server. This port is
used in conjunction with the IP address to conform the URL (for example,
<code>8081</code>). If not provided, CloudBees CD/RO uses the default
port <code>80</code>.</p></td>
</tr>
<tr class="even">
<td style="text-align: left;"><p>Computer Name (DEPRECATED)</p></td>
<td style="text-align: left;"><p>Computer name or IP address without
backslashes (for example, <code>server01</code>).</p>
<div class="warning">
<p>This parameter has been deprecated and will be removed in the
future.</p>
</div></td>
</tr>
<tr class="odd">
<td style="text-align: left;"><p>Login as</p></td>
<td style="text-align: left;"><ul>
<li><p><strong>Username:</strong> Username that CloudBees CD/RO uses to
communicate with the IIS server.</p></li>
<li><p><strong>Password:</strong> Password for the specified
username.</p></li>
<li><p><strong>Retype Password:</strong> Retype the password.</p></li>
</ul></td>
</tr>
</tbody>
</table>

## Create IIS plugin procedures

Plugin procedures can be used in [procedure
steps](https://docs.cloudbees.com/docs/cloudbees-cd/latest/procedures/),
[process
steps](https://docs.cloudbees.com/docs/cloudbees-cd/latest/applications-processes/plugin-process-steps),
and [pipeline
tasks](https://docs.cloudbees.com/docs/cloudbees-cd/latest/pipelines/example-plugin-task),
allowing you to orchestrate third-party tools at the appropriate time in
your component, application process, or pipeline.

Depending on your plugin configuration and how you run procedures, the
**Input parameters &gt; Configuration name** field may behave
differently in the CloudBees CD/RO UI. For more information, refer to
[Differences in plugin UI
behavior](https://docs.cloudbees.com/docs/cloudbees-cd-plugin-docs/latest/#plugin-ui-differences).

### CheckServerStatus

Checks the status of the specified server.

<table>
<colgroup>
<col style="width: 50%" />
<col style="width: 50%" />
</colgroup>
<thead>
<tr class="header">
<th style="text-align: left;">Parameter</th>
<th style="text-align: left;">Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td style="text-align: left;"><p>Configuration name</p></td>
<td style="text-align: left;"><p>Provide the name of the configuration
that holds connection information for the IIS server. Credentials are
only used from the plugin configuration if the <strong>Use
Credentials</strong> parameter is selected.</p></td>
</tr>
<tr class="even">
<td style="text-align: left;"><p>Use Credentials (Deprecated)</p></td>
<td style="text-align: left;"><p>Indicate if credentials must be used.
If selected, CloudBees CD/RO uses the username and password from the
plugin configuration.</p>
<div class="warning">
<p>This parameter has been deprecated and will be removed in the future.
Credentials are used if they are provided.</p>
</div></td>
</tr>
<tr class="odd">
<td style="text-align: left;"><p>Credential</p></td>
<td style="text-align: left;"><p>Username and password for basic
authentication.</p></td>
</tr>
<tr class="even">
<td style="text-align: left;"><p>Link to Check For</p></td>
<td style="text-align: left;"><p>URL to check. If not specified, a URL
is constructed from the IIS configuration.</p></td>
</tr>
<tr class="odd">
<td style="text-align: left;"><p>Expected Status</p></td>
<td style="text-align: left;"><p>3-digit HTTP status to wait for.
Default is <code>200</code>. This can also be a regular expression (for
example, <code>200|201</code>).</p></td>
</tr>
<tr class="even">
<td style="text-align: left;"><p>Check Unavailable</p></td>
<td style="text-align: left;"><p>If selected, the
<code>server:port</code> is checked for availability and the URL path
and status parameters are ignored. If the server is available, the
procedure fails.</p></td>
</tr>
<tr class="odd">
<td style="text-align: left;"><p>Timeout</p></td>
<td style="text-align: left;"><p>Connection timeout. Default is
<code>30</code> seconds.</p></td>
</tr>
<tr class="even">
<td style="text-align: left;"><p>Retries</p></td>
<td style="text-align: left;"><p>Number of retries. This only affects
connecting to server, and not the status returned by the server. Default
is <code>1</code>.</p></td>
</tr>
</tbody>
</table>

### CreateAppPool

Creates an IIS application pool or updates the existed one.

<table>
<colgroup>
<col style="width: 50%" />
<col style="width: 50%" />
</colgroup>
<thead>
<tr class="header">
<th style="text-align: left;">Parameter</th>
<th style="text-align: left;">Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td style="text-align: left;"><p>Application pool name</p></td>
<td style="text-align: left;"><p>Name of the application pool to create
(for example, <code>FirstAppPool</code>).</p></td>
</tr>
<tr class="even">
<td style="text-align: left;"></td>
<td style="text-align: left;"><p>Configures the application pool to load
a specific version of the .NET Framework.</p>
<div class="note">
<p>If <strong>No Managed Code</strong> is selected, all ASP.NET requests
will fail.</p>
</div></td>
</tr>
<tr class="odd">
<td style="text-align: left;"><p>Enable 32-bit applications</p></td>
<td style="text-align: left;"><p>If selected for an application pool on
a 64-bit operating system, the worker processes serving the application
pool run in WOW64 (Windows on Windows64) mode. In WOW64 mode, 32-bit
processes load only 32-bit applications.</p></td>
</tr>
<tr class="even">
<td style="text-align: left;"><p>Managed pipeline mode</p></td>
<td style="text-align: left;"><p>Configures ASP.NET to run in classic
mode as an ISAPI extension or in integrated mode where managed code is
integrated into the request-processing pipeline.</p></td>
</tr>
<tr class="odd">
<td style="text-align: left;"><p>Queue length</p></td>
<td style="text-align: left;"><p>Maximum number of requests that
HTTP.sys queues for the application pool. When the queue is full, new
requests receive a 503 "Service Unavailable" response.</p></td>
</tr>
<tr class="even">
<td style="text-align: left;"><p>Start automatically</p></td>
<td style="text-align: left;"><p>If selected, the application pool
starts on creation or when IIS starts. Starting an application pool sets
this property to <code>True</code>. Stopping an application sets this
property to <code>False</code>.</p></td>
</tr>
<tr class="odd">
<td style="text-align: left;"><p>Limit</p></td>
<td style="text-align: left;"><p>Configures the maximum percentage of
CPU time (in 1/1000ths of a percent) that the worker processes in an
application pool are allowed to consume over a period of time, as
indicated by the <strong>Limit Interval</strong> parameter
(<code>resetInterval</code> property). If the limit set by this
parameter is exceeded, the event is written to the event log and an
optional set of events can be triggered or determined by the
<strong>Limit action</strong> parameter (action property). Setting the
value to <code>0</code> disables the limiting of worker processes to a
percentage of CPU time.</p></td>
</tr>
<tr class="even">
<td style="text-align: left;"><p>Limit action</p></td>
<td style="text-align: left;"><p>Specifies the action to take when the
specified <strong>Limit</strong> is exceeded.</p>
<ul>
<li><p><strong>NoAction:</strong> An event log entry is
generated.</p></li>
<li><p><strong>KillW3WP:</strong> An event log entry is generated and
the application pool is shut down for the duration of the reset
interval.</p></li>
</ul></td>
</tr>
<tr class="odd">
<td style="text-align: left;"><p>Limit interval (minutes)</p></td>
<td style="text-align: left;"><p>Specifies the reset period, in minutes,
for CPU monitoring and throttling limits on the application pool. When
the number of minutes elapsed since the last process accounting reset
equals the <strong>Limit interval</strong>, IIS resets the CPU timers
for both the logging and limit intervals. Setting the value of
<strong>Limit interval</strong> to <code>0</code> disables CPU
monitoring.</p></td>
</tr>
<tr class="even">
<td style="text-align: left;"><p>Processor affinity enabled</p></td>
<td style="text-align: left;"><p>If selected, the worker processes
serving this application pool are forced to run on specific CPUs. This
enables sufficient use of CPU caches on multiprocessor servers.</p></td>
</tr>
<tr class="odd">
<td style="text-align: left;"><p>Processor affinity mask</p></td>
<td style="text-align: left;"><p>Hexadecimal mask that forces the worker
processes for this application pool to run on a specific CPU. If
selected, a value of <code>0</code> results in an error
condition.</p></td>
</tr>
<tr class="even">
<td style="text-align: left;"><p>Identity</p></td>
<td style="text-align: left;"><p>Configures the application pool to run
as a built-in account, such as <strong>Network Service</strong>
(recommended), <strong>Local Service</strong>, or as a specific user
identity.</p></td>
</tr>
<tr class="odd">
<td style="text-align: left;"><p>Idle timeout (minutes)</p></td>
<td style="text-align: left;"><p>Amount of time, in minutes, a worker
process remains idle before it shuts down. A worker process is idle if
it is not processing requests and no new requests are received.</p></td>
</tr>
<tr class="even">
<td style="text-align: left;"><p>Load user profile</p></td>
<td style="text-align: left;"><p>If selected, IIS loads the user profile
for the application pool identity. If not selected, IIS 6.0 behavior is
used.</p></td>
</tr>
<tr class="odd">
<td style="text-align: left;"><p>Maximum worker processes</p></td>
<td style="text-align: left;"><p>Maximum number of worker processes
permitted to service requests for the application pool. If this number
is greater than <code>1</code>, the application pool is referred to as a
"Web Garden".</p></td>
</tr>
<tr class="even">
<td style="text-align: left;"><p>Ping enabled</p></td>
<td style="text-align: left;"><p>If selected, the worker processes
serving this application pool are pinged periodically to ensure that
they are still responsive. This process is called health
monitoring.</p></td>
</tr>
<tr class="odd">
<td style="text-align: left;"><p>Ping maximum response time
(seconds)</p></td>
<td style="text-align: left;"><p>Maximum time, in seconds, that a worker
process is given to respond to a health monitoring ping. If the worker
process does not respond, it is terminated.</p></td>
</tr>
<tr class="even">
<td style="text-align: left;"><p>Ping period (seconds)</p></td>
<td style="text-align: left;"><p>Period of time, in seconds, between
health monitoring pings sent to the worker processes serving this
application pool.</p></td>
</tr>
<tr class="odd">
<td style="text-align: left;"><p>Shutdown time limit (seconds)</p></td>
<td style="text-align: left;"><p>Period of time, in seconds, a worker
process is given to finish processing requests and shut down. If the
worker process exceeds the shutdown time limit, it is
terminated.</p></td>
</tr>
<tr class="even">
<td style="text-align: left;"><p>Startup time limit (seconds)</p></td>
<td style="text-align: left;"><p>Period of time, in seconds, a worker
process is given to start up and initialize. If the worker process
initialization exceeds the startup time limit, it is
terminated.</p></td>
</tr>
<tr class="odd">
<td style="text-align: left;"><p>Application pool process orphaning
enabled</p></td>
<td style="text-align: left;"><p>If selected, an unresponsive worker
process is abandoned (orphaned) instead of terminated. This feature can
be used to debug a worker process failure.</p></td>
</tr>
<tr class="even">
<td style="text-align: left;"><p>Orphan action executable</p></td>
<td style="text-align: left;"><p>Executable to run when a worker process
is abandoned (orphaned). For example, <code>C:\dbgtools\ntsd.exe</code>
invokes NTSD to debug a worker process failure.</p></td>
</tr>
<tr class="odd">
<td style="text-align: left;"><p>Orphan action executable
parameters</p></td>
<td style="text-align: left;"><p>Parameters for the executable that are
run when a worker process is abandoned (orphaned). For example,
<code>-g -p %1%</code> is appropriate if the NTSD is the executable
invoked for debugging worker process failures.</p></td>
</tr>
<tr class="even">
<td style="text-align: left;"><p>Service unavailable response
type</p></td>
<td style="text-align: left;"><ul>
<li><p><strong>HttpLevel:</strong> If the application pool is stopped,
HTTP.sys returns an HTTP 503 error.</p></li>
<li><p><strong>TcpLevel:</strong> If the application pool is stopped,
HTTP.sys resets the connection. This is useful if the load balancer
recognizes one of the response types and subsequently redirects
it.</p></li>
</ul></td>
</tr>
<tr class="odd">
<td style="text-align: left;"><p>Rapid fail protection enabled</p></td>
<td style="text-align: left;"><p>If selected, the application pool is
shut down if there are a specified number of worker process failures
(Maximum failures) within a specified period (Failure interval). By
default, an application pool is shut down if there are five failures in
a five-minute period.</p></td>
</tr>
<tr class="even">
<td style="text-align: left;"><p>Failure interval (minutes)</p></td>
<td style="text-align: left;"><p>The time interval, in minutes, during
which the specified number of worker process failures (Maximum failures)
must occur before the application pool is shut down by Rapid Fail
Protection.</p></td>
</tr>
<tr class="odd">
<td style="text-align: left;"><p>Maximum failures</p></td>
<td style="text-align: left;"><p>Maximum number of worker process
failures permitted before the application pool is shut down by Rapid
Fail Protection.</p></td>
</tr>
<tr class="even">
<td style="text-align: left;"><p>Shutdown executable</p></td>
<td style="text-align: left;"><p>Executable to run when an application
pool is shut down by Rapid Fail Protection. This can be used to
configure a load balancer to redirect traffic for this application to
another server.</p></td>
</tr>
<tr class="odd">
<td style="text-align: left;"><p>Shutdown executable parameters</p></td>
<td style="text-align: left;"><p>Parameters for the executable to run
when an application pool is shut down by Rapid Fail Protection.</p></td>
</tr>
<tr class="even">
<td style="text-align: left;"><p>Disable overlapped recycle</p></td>
<td style="text-align: left;"><p>If selected, when the application pool
recycles, the existing worker process exits before another worker
process is created.</p>
<div class="note">
<p>Select this option if the worker process loads an application that
does not support multiple instances.</p>
</div></td>
</tr>
<tr class="odd">
<td style="text-align: left;"><p>Disable recycling for configuration
changes</p></td>
<td style="text-align: left;"><p>If selected, the application pool does
not recycle when its configuration is changed.</p></td>
</tr>
<tr class="even">
<td style="text-align: left;"><p>Private memory limit (KB)</p></td>
<td style="text-align: left;"><p>Maximum amount of private memory, in
KB, that a worker process can consume before the application pool is
recycled. A value of <code>0</code> means there is no limit.</p></td>
</tr>
<tr class="odd">
<td style="text-align: left;"><p>Regular time interval
(minutes)</p></td>
<td style="text-align: left;"><p>Period of time, in minutes, after which
an application pool recycles. A value of <code>0</code> means the
application pool does not recycle at a regular interval.</p></td>
</tr>
<tr class="even">
<td style="text-align: left;"><p>Request limit</p></td>
<td style="text-align: left;"><p>Maximum number of requests an
application pool can process before it is recycled. A value of
<code>0</code> means the application pool can process an unlimited
number of requests.</p></td>
</tr>
<tr class="odd">
<td style="text-align: left;"><p>Specific times</p></td>
<td style="text-align: left;"><p>A set of specific local times, in
24-hour format, when the application pool is recycled.</p></td>
</tr>
<tr class="even">
<td style="text-align: left;"><p>Virtual memory limit (KB)</p></td>
<td style="text-align: left;"><p>Maximum amount of virtual memory, in
KB, that a worker process can consume before the application pool is
recycled. A value of <code>0</code> means there is no limit.</p></td>
</tr>
<tr class="odd">
<td style="text-align: left;"><p>Additional parameters</p></td>
<td style="text-align: left;"><p>Additional parameters to pass to
appcmd.exe.</p></td>
</tr>
</tbody>
</table>

### CreateVirtualDirectory

Creates a new virtual directory in the specified website or updates the
existing virtual directory.

<table>
<colgroup>
<col style="width: 50%" />
<col style="width: 50%" />
</colgroup>
<thead>
<tr class="header">
<th style="text-align: left;">Parameter</th>
<th style="text-align: left;">Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td style="text-align: left;"><p>Application name</p></td>
<td style="text-align: left;"><p>The website and virtual path to contain
the virtual directory to create (for example,
<code>Default Web Site/myapp02</code>).</p></td>
</tr>
<tr class="even">
<td style="text-align: left;"><p>Virtual path</p></td>
<td style="text-align: left;"><p>Virtual path of the virtual directory
(for example, <code>/myvirtualdir</code>).</p></td>
</tr>
<tr class="odd">
<td style="text-align: left;"><p>Absolute physical path</p></td>
<td style="text-align: left;"><p>The absolute physical path of the
Virtual Directory to create (for example,
<code>c:/Inetpub/wwwroot/myvdir</code>).</p></td>
</tr>
<tr class="even">
<td style="text-align: left;"><p>Create Directory?</p></td>
<td style="text-align: left;"><p>If selected, the specified directory is
created if it does not exist.</p></td>
</tr>
<tr class="odd">
<td style="text-align: left;"><p>Credential</p></td>
<td style="text-align: left;"><p>Credentials used to access the site
folder. It can be used for network paths.</p>
<div class="note">
<p>Passwords are stored in clear text in the IIS configuration. If this
field is not set, application user (pass-through authentication) is
issued. Double quotation marks <code>"</code> are not supported in the
username and password due to escape issues.</p>
</div></td>
</tr>
</tbody>
</table>

### CreateWebApplication

Creates or updates and starts an in-process web application in the given
directory. This procedure assumes that the specified application path
exists as a virtual directory.

<table>
<colgroup>
<col style="width: 50%" />
<col style="width: 50%" />
</colgroup>
<thead>
<tr class="header">
<th style="text-align: left;">Parameter</th>
<th style="text-align: left;">Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td style="text-align: left;"><p>Website Name</p></td>
<td style="text-align: left;"><p>The name of the website to add the
application to (for example, <code>Default Web Site</code>).</p></td>
</tr>
<tr class="even">
<td style="text-align: left;"><p>Virtual Path</p></td>
<td style="text-align: left;"><p>Virtual path of the application (for
example, <code>/myApplication</code>).</p></td>
</tr>
<tr class="odd">
<td style="text-align: left;"><p>Absolute Physical Path</p></td>
<td style="text-align: left;"><p>The absolute physical path of the
application to create (for
example,<code>c:/Inetpub/wwwroot/myApp</code>).</p></td>
</tr>
<tr class="even">
<td style="text-align: left;"><p>Create Directory?</p></td>
<td style="text-align: left;"><p>If selected, the specified directory is
created if it does not already exist.</p></td>
</tr>
<tr class="odd">
<td style="text-align: left;"><p>Credential</p></td>
<td style="text-align: left;"><p>Credentials used to access the site
folder. It can be used for network paths.</p>
<div class="note">
<p>Passwords are stored in clear text in the IIS configuration. If this
field is not set, application user (pass-through authentication) is
issued. Double quotation marks <code>"</code> are not supported in the
username and password due to escape issues</p>
</div></td>
</tr>
</tbody>
</table>

### CreateWebSite

Creates or updates a website configuration on a local or remote
computer.

<table>
<colgroup>
<col style="width: 50%" />
<col style="width: 50%" />
</colgroup>
<thead>
<tr class="header">
<th style="text-align: left;">Parameter</th>
<th style="text-align: left;">Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td style="text-align: left;"><p>Website name</p></td>
<td style="text-align: left;"><p>The name of the website to
create.</p></td>
</tr>
<tr class="even">
<td style="text-align: left;"><p>Website path</p></td>
<td style="text-align: left;"><p>If specified, the root application
containing a root virtual directory pointing to the specified path is
created for this site. If omitted, the site is created without a root
application and cannot be started until one is created.</p></td>
</tr>
<tr class="odd">
<td style="text-align: left;"><p>Website ID</p></td>
<td style="text-align: left;"><p>ID of the website.</p></td>
</tr>
<tr class="even">
<td style="text-align: left;"><p>List of bindings</p></td>
<td style="text-align: left;"><p>Comma-separated list of bindings that
use the friendly form of <code>\http://domain:port,...</code> or raw
form of <code>protocol/bindingInformation,...</code>.</p></td>
</tr>
<tr class="odd">
<td style="text-align: left;"><p>Create Directory?</p></td>
<td style="text-align: left;"><p>If selected, the specified directory is
created if it does not already exist.</p></td>
</tr>
<tr class="even">
<td style="text-align: left;"><p>Credential</p></td>
<td style="text-align: left;"><p>Credentials used to access the site
folder. It can be used for network paths.</p>
<div class="note">
<p>Passwords are stored in clear text in the IIS configuration. If this
field is not set, application user (pass-through authentication) is
issued. Double quotation marks <code>"</code> are not supported in the
username and password due to escape issues.</p>
</div></td>
</tr>
</tbody>
</table>

### DeleteWebApplication

Deletes a web application from the specified website.

<table>
<colgroup>
<col style="width: 50%" />
<col style="width: 50%" />
</colgroup>
<thead>
<tr class="header">
<th style="text-align: left;">Parameter</th>
<th style="text-align: left;">Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td style="text-align: left;"><p>Application Name</p></td>
<td style="text-align: left;"><p>The website that contains the
application to delete (for example, <code>Default Web Site/</code> or
<code>Site1/myapp</code>).</p></td>
</tr>
<tr class="even">
<td style="text-align: left;"><p>Strict Mode</p></td>
<td style="text-align: left;"><p>If selected, the procedure fails if the
specified application does not exist.</p></td>
</tr>
</tbody>
</table>

### DeleteVirtualDirectory

Deletes a virtual directory from the specified website.

<table>
<colgroup>
<col style="width: 50%" />
<col style="width: 50%" />
</colgroup>
<thead>
<tr class="header">
<th style="text-align: left;">Parameter</th>
<th style="text-align: left;">Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td style="text-align: left;"><p>Virtual directory name</p></td>
<td style="text-align: left;"><p>The website and virtual path that
contain the virtual directory to delete (for example,
<code>Default Web Site/</code> or <code>Site1/myapp</code>).</p></td>
</tr>
<tr class="even">
<td style="text-align: left;"><p>Strict mode</p></td>
<td style="text-align: left;"><p>If selected, the procedure fails if the
specified virtual directory does not exist.</p></td>
</tr>
</tbody>
</table>

### DeleteWebSite

Deletes a website.

<table>
<colgroup>
<col style="width: 50%" />
<col style="width: 50%" />
</colgroup>
<thead>
<tr class="header">
<th style="text-align: left;">Parameter</th>
<th style="text-align: left;">Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td style="text-align: left;"><p>Website name</p></td>
<td style="text-align: left;"><p>The name of the website to delete (for
example, <code>Default Web Site/</code> or
<code>Site1/myapp</code>).</p></td>
</tr>
<tr class="even">
<td style="text-align: left;"><p>Strict mode</p></td>
<td style="text-align: left;"><p>If selected, the procedure fails if the
specified application does not exist.</p></td>
</tr>
</tbody>
</table>

### DeleteAppPool

Deletes an application pool.

<table>
<colgroup>
<col style="width: 50%" />
<col style="width: 50%" />
</colgroup>
<thead>
<tr class="header">
<th style="text-align: left;">Parameter</th>
<th style="text-align: left;">Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td style="text-align: left;"><p>Application pool name</p></td>
<td style="text-align: left;"><p>The name of the website to delete (for
example, <code>Default Web Site/</code> or
<code>Site1/myapp</code>).</p></td>
</tr>
<tr class="even">
<td style="text-align: left;"><p>Strict mode</p></td>
<td style="text-align: left;"><p>If selected, the procedure fails if the
specified application pool does not exist.</p></td>
</tr>
</tbody>
</table>

### DeployCopy

Copies the application files recursively to the website application\`s
physical directory.

<table>
<colgroup>
<col style="width: 50%" />
<col style="width: 50%" />
</colgroup>
<thead>
<tr class="header">
<th style="text-align: left;">Parameter</th>
<th style="text-align: left;">Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td style="text-align: left;"><p>Destination path</p></td>
<td style="text-align: left;"><p>Required. Path to the destination
directory. This must be a physical directory, but it may have an IIS
virtual directory pointing to it (for example,
<code>C:\inetpub\wwwroot\copyTest</code>).</p></td>
</tr>
<tr class="even">
<td style="text-align: left;"><p>Path to XCOPY</p></td>
<td style="text-align: left;"><p>Required. Provide the relative or
absolute path to the XCOPY executable.</p></td>
</tr>
<tr class="odd">
<td style="text-align: left;"><p>Source path</p></td>
<td style="text-align: left;"><p>Required. Provide the path to the
source directory (for example,
<code>C:\inetpub\wwwroot\test</code>).</p></td>
</tr>
<tr class="even">
<td style="text-align: left;"><p>Additional options</p></td>
<td style="text-align: left;"><p>Option switches for the XCOPY
executable, excluding source and destination directories. The default
options are those recommended by Microsoft for ASP.NET and IIS website
deployment; exercise caution when changing these options.</p>
<ul>
<li><p><code>/E</code> - Deep copy including empty dirs</p></li>
<li><p><code>/K</code> - Copy attributes</p></li>
<li><p><code>/R</code> - Overwrite read-only files</p></li>
<li><p><code>/H</code> - Copy hidden and system files</p></li>
<li><p><code>/I</code> - If the destination does not exist, and you are
copying more than one file, it is assumed that the destination is a
directory.</p></li>
<li><p><code>/Y</code> - Suppress prompting for overwrite
confirmation</p></li>
</ul></td>
</tr>
</tbody>
</table>

### Deploy

Uses MSDeploy (WebDeploy) to deploy a package or a site from a directory
into the specified destination and allows you to configure the
application pool.

MSDeploy is required for this procedure.

<table>
<colgroup>
<col style="width: 50%" />
<col style="width: 50%" />
</colgroup>
<thead>
<tr class="header">
<th style="text-align: left;">Parameter</th>
<th style="text-align: left;">Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td style="text-align: left;"><p>MS deploy path</p></td>
<td style="text-align: left;"><p>Provide the relative or absolute path
to the MSDeploy executable.</p></td>
</tr>
<tr class="even">
<td style="text-align: left;"><p>Deploy source path</p></td>
<td style="text-align: left;"><p>A path to package (for example,
<code>application.zip</code>) or to a directory that contains the
content to be deployed.</p></td>
</tr>
<tr class="odd">
<td style="text-align: left;"><p>Destination website</p></td>
<td style="text-align: left;"><p>Name of the website to be
deployed.</p></td>
</tr>
<tr class="even">
<td style="text-align: left;"><p>Destination application</p></td>
<td style="text-align: left;"><p>Name of the application to be deployed.
If not provided, the content is placed under the website.</p></td>
</tr>
<tr class="odd">
<td style="text-align: left;"><p>Application pool name</p></td>
<td style="text-align: left;"><p>Application pool name. If the
application pool does not exist, it is created. If not specified, the
application is placed into the default pool, which has the same name as
the website.</p></td>
</tr>
<tr class="even">
<td style="text-align: left;"><pre><code>+.NET+ framework version</code></pre></td>
<td style="text-align: left;"><pre><code>The Microsoft(R) .NET Framework version 3.5 includes all the functionality of earlier versions, introduces new features for the technologies in versions 2.0 and 3.0, and provides additional functionality in the form of new assemblies. To use version 3.5, install the appropriate version of .NET Framework and use the product-specific guidelines.</code></pre></td>
</tr>
<tr class="odd">
<td style="text-align: left;"><p>Enable 32-bit applications</p></td>
<td style="text-align: left;"><p>If selected for an application pool on
a 64-bit operating system, the worker processes serving the application
pool run in WOW64 (Windows on Windows64) mode. In WOW64 mode, 32-bit
processes load only 32-bit applications.</p></td>
</tr>
<tr class="even">
<td style="text-align: left;"><p>Managed pipeline mode</p></td>
<td style="text-align: left;"><p>Configures ASP.NET to run in classic
mode as an ISAPI extension or in integrated mode where managed code is
integrated into the request-processing pipeline.</p></td>
</tr>
<tr class="odd">
<td style="text-align: left;"><p>Queue length</p></td>
<td style="text-align: left;"><p>Maximum number of requests that
HTTP.sys queues for the application pool. When the queue is full, new
requests receive a 503 "Service Unavailable" response.</p></td>
</tr>
<tr class="even">
<td style="text-align: left;"><p>Start automatically</p></td>
<td style="text-align: left;"><p>If selected, the application pool
starts on creation or when IIS starts. Starting an application pool sets
this property to <code>True</code>. Stopping an application sets this
property to <code>False</code>.</p></td>
</tr>
<tr class="odd">
<td style="text-align: left;"><p>Additional settings for application
pool</p></td>
<td style="text-align: left;"><p>Additional parameters to pass to
<code>appcmd.exe</code> for application pool configuration.</p></td>
</tr>
<tr class="even">
<td style="text-align: left;"><p>Additional Parameters</p></td>
<td style="text-align: left;"><p>Additional parameters (for example,
<code>-enableRule:AppOffline</code>) to pass to Web Deploy. For the list
of available settings, refer to <a
href="https://learn.microsoft.com/en-us/previous-versions/windows/it-pro/windows-server-2008-r2-and-2008/dd568991(v=ws.10)">Web
Deploy Command Line Reference</a>.</p></td>
</tr>
</tbody>
</table>

### Undeploy

Uses MSDeploy to undeploy an application or site.

MSDeploy is required for this procedure.

<table>
<colgroup>
<col style="width: 50%" />
<col style="width: 50%" />
</colgroup>
<thead>
<tr class="header">
<th style="text-align: left;">Parameter</th>
<th style="text-align: left;">Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td style="text-align: left;"><p>MS deploy path</p></td>
<td style="text-align: left;"><p>Relative or absolute path to the
MSDeploy executable.</p></td>
</tr>
<tr class="even">
<td style="text-align: left;"><p>Web site name</p></td>
<td style="text-align: left;"><p>Website name to undeploy.</p></td>
</tr>
<tr class="odd">
<td style="text-align: left;"><p>Application name</p></td>
<td style="text-align: left;"><p>An application name to undeploy.</p>
<div class="warning">
<p>If not specified, the website that you specified for the <strong>Web
site name</strong> parameter will be undeployed.</p>
</div></td>
</tr>
<tr class="even">
<td style="text-align: left;"><p>Delete virtual directories?</p></td>
<td style="text-align: left;"><p>Deletes the specified website or web
application, including any virtual directories and their
content.</p></td>
</tr>
<tr class="odd">
<td style="text-align: left;"><p>Strict mode</p></td>
<td style="text-align: left;"><p>If selected, the procedure fail if the
specified website does not exist.</p></td>
</tr>
</tbody>
</table>

### Deploy advanced

An interface to the utility.

MSDeploy is required for this procedure.

<table>
<colgroup>
<col style="width: 50%" />
<col style="width: 50%" />
</colgroup>
<thead>
<tr class="header">
<th style="text-align: left;">Parameter</th>
<th style="text-align: left;">Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td style="text-align: left;"><p>MSDeploy path</p></td>
<td style="text-align: left;"><p>Provide the relative or absolute path
to the MSDeploy executable.</p></td>
</tr>
<tr class="even">
<td style="text-align: left;"><p>Verb</p></td>
<td style="text-align: left;"><p>Web Deploy operations enable you to
gather information from, move, or delete deployment objects like
websites and web applications. Web Deploy operations are specified on
the command line with the <code>-verb</code> argument. The Web Deploy
operations are dump, sync, delete, getDependencies, and
getSystemInfo.</p></td>
</tr>
<tr class="odd">
<td style="text-align: left;"><p>Source provider</p></td>
<td style="text-align: left;"><p>Provider that processes specific source
or destination data for Web Deploy. For example, the
<code>contentPath</code> provider determines how to work with directory,
file, site, and application paths. On the Web Deploy command line, the
provider name is specified immediately after the <code>-source:</code>
or <code>-dest:</code> argument.</p></td>
</tr>
<tr class="even">
<td style="text-align: left;"><p>Source provider object path</p></td>
<td style="text-align: left;"><p>Path of the provider object. Some
providers require a path and some do not. If required, the type of path
depends on the provider.</p></td>
</tr>
<tr class="odd">
<td style="text-align: left;"><p>Source provider settings</p></td>
<td style="text-align: left;"><p>Settings to modify a source provider
using the general syntax.</p></td>
</tr>
<tr class="even">
<td style="text-align: left;"><p>Destination provider</p></td>
<td style="text-align: left;"><p>Providers process specific source or
destination data for Web Deploy. For example, the
<code>contentPath</code> provider determines how to work with directory,
file, site, and application paths. On the Web Deploy command line, the
provider name is specified immediately after the <code>-source:</code>
or <code>-dest:</code> argument.</p></td>
</tr>
<tr class="odd">
<td style="text-align: left;"><p>Destination provider object
path</p></td>
<td style="text-align: left;"><p>Path of the provider object. Some
providers require a path and some do not. If required, the kind of path
depends on the provider.</p></td>
</tr>
<tr class="even">
<td style="text-align: left;"><p>Destination provider settings</p></td>
<td style="text-align: left;"><p>Settings to modify a destination
provider using the general syntax.</p></td>
</tr>
<tr class="odd">
<td style="text-align: left;"><p>Allow untrusted?</p></td>
<td style="text-align: left;"><p>If selected, untrusted server
certificates are allowed when using SSL.</p></td>
</tr>
<tr class="even">
<td style="text-align: left;"><p>Pre-sync command</p></td>
<td style="text-align: left;"><p>A command to execute before the
synchronization on the destination. For instance,
<code>net stop [service name]</code>.</p></td>
</tr>
<tr class="odd">
<td style="text-align: left;"><p>Post-sync command</p></td>
<td style="text-align: left;"><p>A command to execute after the
synchronization on the destination. For instance,
<code>net start [service name]</code>.</p></td>
</tr>
<tr class="even">
<td style="text-align: left;"><p>Additional options</p></td>
<td style="text-align: left;"><p>Additional options to be passed to
<code>msdeploy.exe</code> (for example,
<code>-retryAttempts=5</code>).</p></td>
</tr>
<tr class="odd">
<td style="text-align: left;"><p>Set param file</p></td>
<td style="text-align: left;"><p>Applies parameter settings from an XML
file. This can be a file path or file content.</p></td>
</tr>
<tr class="even">
<td style="text-align: left;"><p>Declare param file</p></td>
<td style="text-align: left;"><p>Includes parameter declarations from an
XML file. This can be a file path or file content.</p></td>
</tr>
</tbody>
</table>

### StartAppPool

Starts an IIS application pool.

<table>
<colgroup>
<col style="width: 50%" />
<col style="width: 50%" />
</colgroup>
<thead>
<tr class="header">
<th style="text-align: left;">Parameter</th>
<th style="text-align: left;">Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td style="text-align: left;"><p>Application pool name</p></td>
<td style="text-align: left;"><p>The name of the application pool to
start (for example, <code>FirstAppPool</code>).</p></td>
</tr>
</tbody>
</table>

### StartWebSite

Starts a website into an IIS Server.

<table>
<colgroup>
<col style="width: 50%" />
<col style="width: 50%" />
</colgroup>
<thead>
<tr class="header">
<th style="text-align: left;">Parameter</th>
<th style="text-align: left;">Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td style="text-align: left;"><p>Website name</p></td>
<td style="text-align: left;"><p>Provide the descriptive name of the
website you want to start (for example,
<code>Default Web Site</code>).</p></td>
</tr>
</tbody>
</table>

### StopAppPool

Stops an IIS application pool.

<table>
<colgroup>
<col style="width: 50%" />
<col style="width: 50%" />
</colgroup>
<thead>
<tr class="header">
<th style="text-align: left;">Parameter</th>
<th style="text-align: left;">Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td style="text-align: left;"><p>Application pool name</p></td>
<td style="text-align: left;"><p>The name of the application pool to
stop (for example, <code>FirstAppPool</code>).</p></td>
</tr>
</tbody>
</table>

### StopWebSite

Stops a website.

<table>
<colgroup>
<col style="width: 50%" />
<col style="width: 50%" />
</colgroup>
<thead>
<tr class="header">
<th style="text-align: left;">Parameter</th>
<th style="text-align: left;">Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td style="text-align: left;"><p>Website name</p></td>
<td style="text-align: left;"><p>Provide the descriptive name of the
website you want to stop (for example,
<code>Default Web Site</code>).</p></td>
</tr>
</tbody>
</table>

### RecycleAppPool

Recycles the specified application pool.

<table>
<colgroup>
<col style="width: 50%" />
<col style="width: 50%" />
</colgroup>
<thead>
<tr class="header">
<th style="text-align: left;">Parameter</th>
<th style="text-align: left;">Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td style="text-align: left;"><p>Application pool name</p></td>
<td style="text-align: left;"><p>Name of the application pool to
recycle.</p></td>
</tr>
</tbody>
</table>

### AssignAppToAppPool

Assigns an application to an application pool.

<table>
<colgroup>
<col style="width: 50%" />
<col style="width: 50%" />
</colgroup>
<thead>
<tr class="header">
<th style="text-align: left;">Parameter</th>
<th style="text-align: left;">Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td style="text-align: left;"><p>Application pool name</p></td>
<td style="text-align: left;"><p>Name of the application pool to assign
the application (for example, <code>FirstAppPool</code>).</p></td>
</tr>
<tr class="even">
<td style="text-align: left;"><p>Application name</p></td>
<td style="text-align: left;"><p>Name of the application to assign (for
example, <code>/test</code>).</p></td>
</tr>
<tr class="odd">
<td style="text-align: left;"><p>Site name</p></td>
<td style="text-align: left;"><p>Name of the site that contains the
application to assign (for example,
<code>Default Web Site</code>).</p></td>
</tr>
</tbody>
</table>

### ListSites

List the sites on a web server and writes the retrieved data under the
specified property.

<table>
<colgroup>
<col style="width: 50%" />
<col style="width: 50%" />
</colgroup>
<thead>
<tr class="header">
<th style="text-align: left;">Parameter</th>
<th style="text-align: left;">Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td style="text-align: left;"><p>User-defined criteria</p></td>
<td style="text-align: left;"><p>User-defined criteria to search the
sites (for example, <code>/bindings:http/*:80:</code>). If not
specified, all sites are listed.</p></td>
</tr>
<tr class="even">
<td style="text-align: left;"><p>Property name</p></td>
<td style="text-align: left;"><p>Property to write retrieved
data.</p></td>
</tr>
<tr class="odd">
<td style="text-align: left;"><p>Dump format</p></td>
<td style="text-align: left;"><p>Format to represent retrieved data.
Data can be represented as XML, JSON, raw (only <code>stdout</code> from
<code>appcmd.exe</code>) and property sheet (hierarchy).</p></td>
</tr>
</tbody>
</table>

### ListSiteApps

Lists website applications.

<table>
<colgroup>
<col style="width: 50%" />
<col style="width: 50%" />
</colgroup>
<thead>
<tr class="header">
<th style="text-align: left;">Parameter</th>
<th style="text-align: left;">Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td style="text-align: left;"><p>Site name</p></td>
<td style="text-align: left;"><p>Name of the site to search for
applications. If not specified, all applications are listed.</p></td>
</tr>
<tr class="even">
<td style="text-align: left;"><p>Property name</p></td>
<td style="text-align: left;"><p>Property to write retrieved
data.</p></td>
</tr>
<tr class="odd">
<td style="text-align: left;"><p>Dump format</p></td>
<td style="text-align: left;"><p>Choose the format to represent
retrieved data. Data can be represented as XML, JSON, raw (only
<code>stdout</code> from <code>appcmd.exe</code>) and property sheet
(hierarchy).</p></td>
</tr>
</tbody>
</table>

### ListAppPools

Lists the application pools.

<table>
<colgroup>
<col style="width: 50%" />
<col style="width: 50%" />
</colgroup>
<thead>
<tr class="header">
<th style="text-align: left;">Parameter</th>
<th style="text-align: left;">Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td style="text-align: left;"><p>User-defined criteria</p></td>
<td style="text-align: left;"><p>User-defined criteria to search the
application pools (for example, <code>/apppool.name:"my pool"</code>).
If not specified, all pools are listed.</p></td>
</tr>
<tr class="even">
<td style="text-align: left;"><p>Property name</p></td>
<td style="text-align: left;"><p>Property to write retrieved
data.</p></td>
</tr>
<tr class="odd">
<td style="text-align: left;"><p>Dump format</p></td>
<td style="text-align: left;"><p>Format to represent retrieved data.
Data can be represented as XML, JSON, raw (only <code>stdout</code> from
<code>appcmd.exe</code>) and property sheet (hierarchy).</p></td>
</tr>
</tbody>
</table>

### ListVirtualDirectories

Lists the virtual directories.

<table>
<colgroup>
<col style="width: 50%" />
<col style="width: 50%" />
</colgroup>
<thead>
<tr class="header">
<th style="text-align: left;">Parameter</th>
<th style="text-align: left;">Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td style="text-align: left;"><p>Virtual directory name</p></td>
<td style="text-align: left;"><p>Virtual directory name to retrieve. If
not provided, all virtual directory names are retrieved.</p></td>
</tr>
<tr class="even">
<td style="text-align: left;"><p>Property name</p></td>
<td style="text-align: left;"><p>Property to write retrieved
data.</p></td>
</tr>
<tr class="odd">
<td style="text-align: left;"><p>Dump format</p></td>
<td style="text-align: left;"><p>Format to represent retrieved data.
Data can be represented as XML, JSON, raw (only <code>stdout</code> from
<code>appcmd.exe</code>) and property sheet (hierarchy).</p></td>
</tr>
</tbody>
</table>

### AddWebSiteBinding

Adds a binding to a website.

<table>
<colgroup>
<col style="width: 50%" />
<col style="width: 50%" />
</colgroup>
<thead>
<tr class="header">
<th style="text-align: left;">Parameter</th>
<th style="text-align: left;">Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td style="text-align: left;"><p>Website Name</p></td>
<td style="text-align: left;"><p>The name of the website to add a
binding, i.e: <code>Default Web Site</code>. The site should exist on
server.</p></td>
</tr>
<tr class="even">
<td style="text-align: left;"><p>Binding Protocol</p></td>
<td style="text-align: left;"><p>Binding protocol to add (for example,
<code>http</code>). Typically, the protocol is <code>http</code> or
<code>https</code>. For FTP binding, refer to <a
href="https://learn.microsoft.com/en-us/previous-versions/windows/it-pro/windows-server-2008-r2-and-2008/cc731692(v=ws.10)">Add
a Binding to a Site (IIS 7)</a>.</p></td>
</tr>
<tr class="odd">
<td style="text-align: left;"><p>Binding Information</p></td>
<td style="text-align: left;"><p>Information of the binding to add,
including the host and the port (for example,
<code>localhost:443</code>, <code>*:81</code>).</p></td>
</tr>
<tr class="even">
<td style="text-align: left;"><p>Host Header</p></td>
<td style="text-align: left;"><p>Host headers (also known as domain
names or host names) that allow you to assign more than one site to a
single IP address on a web server (for example,
<code>myhost.com</code>).</p></td>
</tr>
</tbody>
</table>

### StopServer

Stops the IIS server.

<table>
<colgroup>
<col style="width: 50%" />
<col style="width: 50%" />
</colgroup>
<thead>
<tr class="header">
<th style="text-align: left;">Parameter</th>
<th style="text-align: left;">Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td style="text-align: left;"><p>Absolute location of the IISRESET
utility.</p></td>
<td style="text-align: left;"><p>Absolute path of the script utility
that executes this step. If only <code>iisreset</code> is entered, the
IISRESET tool must be located on the system path
<code>c:/windows/system32</code> (for example, <code>iisreset</code> or
<code>c:/MyDir/IISFiles/iisreset.exe</code>).</p></td>
</tr>
<tr class="even">
<td style="text-align: left;"><p>Additional parameters</p></td>
<td style="text-align: left;"><p>Additional parameters to pass to the
IISRESET utility.</p></td>
</tr>
</tbody>
</table>

### StartServer

Starts the IIS server.

<table>
<colgroup>
<col style="width: 50%" />
<col style="width: 50%" />
</colgroup>
<thead>
<tr class="header">
<th style="text-align: left;">Parameter</th>
<th style="text-align: left;">Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td style="text-align: left;"><p>Absolute location of the IISRESET
utility.</p></td>
<td style="text-align: left;"><p>Absolute path of the script utility
that execute this step. If only <code>iisreset</code> is entered, the
IISRESET tool must be located on the system path
<code>c:/windows/system32</code> (for example, <code>iisreset</code> or
<code>c:/MyDir/IISFiles/iisreset.exe</code>).</p></td>
</tr>
<tr class="even">
<td style="text-align: left;"><p>Additional parameters</p></td>
<td style="text-align: left;"><p>Additional parameters to pass to the
IISRESET utility.</p></td>
</tr>
</tbody>
</table>

### ResetServer

Restarts IIS server.

<table>
<colgroup>
<col style="width: 50%" />
<col style="width: 50%" />
</colgroup>
<thead>
<tr class="header">
<th style="text-align: left;">Parameter</th>
<th style="text-align: left;">Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td style="text-align: left;"><p>Absolute location of the IISRESET
utility.</p></td>
<td style="text-align: left;"><p>Absolute path of the script utility
used to execute this step. If only <code>iisreset</code> is entered, the
IISRESET tool must be located on the system path
<code>c:/windows/system32</code> (for example, <code>iisreset</code> or
<code>c:/MyDir/IISFiles/iisreset.exe</code>).</p></td>
</tr>
<tr class="even">
<td style="text-align: left;"><p>Additional parameters</p></td>
<td style="text-align: left;"><p>Additional parameters to pass to the
IISRESET utility.</p></td>
</tr>
</tbody>
</table>

### AddSSLCertificate

Adds an SSL certificate to the specified port or updates an SSL
certificate if one already exists.

The certificate should be added to IIS certificates storage. For
instructions, refer to [IIS.NET
Forums](https://learn.microsoft.com/en-us/archive/msdn-technet-forums/8a09b2b8-0f72-4920-ae78-4d9d82f1e704).

<table>
<colgroup>
<col style="width: 50%" />
<col style="width: 50%" />
</colgroup>
<thead>
<tr class="header">
<th style="text-align: left;">Parameter</th>
<th style="text-align: left;">Description</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td style="text-align: left;"><p>IP</p></td>
<td style="text-align: left;"><p>IP address for the certificate (for
example, <code>0.0.0.0</code>). Either the IP address or the
<strong>Hostname</strong> parameter hostname must be provided.</p></td>
</tr>
<tr class="even">
<td style="text-align: left;"><p>Hostname</p></td>
<td style="text-align: left;"><p>Hostname for the certificate (for
example, <code>mysite.com</code>). Either this hostname or the
<strong>IP</strong> parameter should be provided.</p>
<div class="note">
<p>This parameter is not supported on Windows Server 2008.</p>
</div></td>
</tr>
<tr class="odd">
<td style="text-align: left;"><p>Port</p></td>
<td style="text-align: left;"><p>Port to add the SSL certificate to (for
example, <code>443</code>).</p></td>
</tr>
<tr class="even">
<td style="text-align: left;"><p>Certificate Store</p></td>
<td style="text-align: left;"><p>The name of the certificate store (for
example, <code>My</code>).</p></td>
</tr>
<tr class="odd">
<td style="text-align: left;"><p>Certificate Hash (Thumbprint)</p></td>
<td style="text-align: left;"><p>The certificate hash. The certificate
hash can be found on the <strong>Server Certificates</strong> tab of the
IIS console (for example,
<code>b4 7c 04 0c 0a 7e fc f5 3f 9e 12 fc df 07 30 ee b1 d6 04 88</code>).
Spaces are not required.</p></td>
</tr>
</tbody>
</table>

## Examples and use cases

### Create a website

This example shows the how to create a website.

1.  Run the CheckServerStatus with the appropriate parameters to verify
    the server availability:

    <figure>
    <img src="htdocs/images/case1/ec-iis7checkserver1.png"
    alt="htdocs/images/case1/ec-iis7checkserver1.png" />
    </figure>

<figure>
<img src="htdocs/images/case1/ec-iis7checkserver1.png"
alt="htdocs/images/case1/ec-iis7checkserver1.png" />
</figure>

1.  Verify the result of the server status; it must be running:

    <figure>
    <img src="htdocs/images/case1/ec-iis7checkserver2.png"
    alt="htdocs/images/case1/ec-iis7checkserver2.png" />
    </figure>

2.  Once the server is running, the website can be created with these
    parameters:

    <figure>
    <img src="htdocs/images/case1/ec-iis7createwebsite1.png"
    alt="htdocs/images/case1/ec-iis7createwebsite1.png" />
    </figure>

3.  Verify the result of the creation of the site:

    <figure>
    <img src="htdocs/images/case1/ec-iis7createwebsite2.png"
    alt="htdocs/images/case1/ec-iis7createwebsite2.png" />
    </figure>

4.  If the creation of the site was successful, the site can be started
    with these parameters:

    <figure>
    <img src="htdocs/images/case1/ec-iis7startwebsite1.png"
    alt="htdocs/images/case1/ec-iis7startwebsite1.png" />
    </figure>

5.  Finally, verify that the application was started successfully:

    <figure>
    <img src="htdocs/images/case1/ec-iis7startwebsite2.png"
    alt="htdocs/images/case1/ec-iis7startwebsite2.png" />
    </figure>

## IIS plugin release notes

### 4.0.1

-   Removed unnecessary diagnostics to reduce false positive error
    reporting and improve performance.

### 4.0.0

-   Upgraded from Perl 5.8 to Perl 5.32. The plugin is not backward
    compatibility with releases prior to CloudBees CD/RO 10.3. Starting
    with this release, a new agent is required to run the plugin
    procedures.

### 3.1.8

-   Added session validation.

### 3.1.7

-   The documentation has been migrated to the main documentation site.

### 3.1.6

-   Renamed to "CloudBees CD/RO"

### 3.1.5

-   Renamed to "CloudBees".

### 3.1.4

-   Configurations can now be created by users with "@" sign in a name.

### 3.1.3

-   The plugin icon has been updated.

### 3.1.2

-   Configured the plugin to allow the ElectricFlow UI to create configs
    inline of a procedure form.

### 3.1.1

-   Configured the plugin to allow the ElectricFlow UI to render the
    plugin procedure parameters entirely using the configured form XMLs.

-   Enabled the plugin for managing the plugin configurations inline
    when defining an application process step or a pipeline stage task.

### 3.1.0

-   The **Computer Name** parameter in the plugin configuration has been
    deprecated.

-   The **Credentials** parameter was added to **CheckServerStatus**
    procedure.

-   The **Configuration Name** parameter is no longer required in the
    **CheckServerStatus** procedure.

-   The deployment logic has been changed; if no application name is
    provided to the **Deploy** procedure, but the application pool
    parameters are specified, the root application of the website (`/`)
    is moved into the specified application pool and parameters are
    applied to this application pool.

-   Support for virtual directory credentials has been added for the
    **CreateWebSite**, **CreateWebApplication**,
    **CreateVirtualDirectory** procedures.

-   The **Create Directory?** parameter was added for the
    **CreateWebSite**, **CreateWebApplication**, and
    **CreateVirtualDirectory** procedures.

-   Support for the **AddSSLCertificate** procedure has been added.

### 3.0.0

-   The plugin has been fully redesigned and IIS version 7 and later is
    now supported.

### 2.0.7

-   Fixed issue with configurations being cached for Internet Explorer.

### 2.0.6

-   Renamed ElectricCommander to ElectricFlow.

-   Added link to the plugin configuration page in the plugin step
    panels.

### 2.0.5

-   Fixed manifest file.

-   Removed need for agent/lib directories.

### 2.0.4

-   Procedure names were changed in the step picker section.

### 2.0.3

-   Improved the documentation.

### 2.0.2

-   Improved the documentation.

### 2.0.1

-   Upgraded to use the new Parameter Form XML.

-   Added a link directly to the new documentation.

### 2.0.0

-   Improved XML parameter panels.

-   Introduced a new documentation format.

## Known issues

Due to escape issues, double quote (`"`) is not supported in parameter
values.
