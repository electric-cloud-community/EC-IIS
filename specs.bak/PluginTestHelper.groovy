import spock.lang.*
import com.electriccloud.spec.*

class PluginTestHelper extends PluginSpockTestSupport {

    static def helperProjName = 'IIS Helper Project'
    static def helperProcedure = 'Run App Cmd'

    def redirectLogs(String parentProperty = '/myJob') {
        def propertyLogName = parentProperty + '/debug_logs'
        dsl """
            setProperty(
                propertyName: "/plugins/EC-IIS/project/ec_debug_logToProperty",
                value: "$propertyLogName"
            )
        """
        return propertyLogName
    }

    def redirectLogsToPipeline() {
        def propertyName = '/myPipelineRuntime/debugLogs'
        dsl """
            setProperty(
                propertyName: "/plugins/EC-IIS/project/ec_debug_logToProperty",
                value: "$propertyName"
            )
        """
        propertyName
    }

    def getJobLogs(def jobId) {
        assert jobId
        def logs
        try {
            logs = getJobProperty("/myJob/debug_logs", jobId)
        } catch (Throwable e) {
            logs = "Possible exception in logs; check job"
        }
        logs
    }

    def getPipelineLogs(flowRuntimeId) {
        assert flowRuntimeId
        getPipelineProperty('/myPipelineRuntime/debugLogs', flowRuntimeId)
    }


    def runProcedureDsl(dslString) {
        redirectLogs()
        assert dslString

        def result = dsl(dslString)
        assert result.jobId
        waitUntil {
            jobCompleted result.jobId
        }
        def logs = getJobLogs(result.jobId)
        def outcome = jobStatus(result.jobId).outcome
        logger.debug("DSL: $dslString")
        logger.debug("Logs: $logs")
        logger.debug("Outcome: $outcome")
        [logs: logs, outcome: outcome, jobId: result.jobId]
    }

    def createIISResource() {
        def hostname = System.getenv('IIS_RESOURCE_HOST')
        def resources = dsl "getResources()"
        logger.debug(objectToJson(resources))

        def resource = resources.resource.find {
            it.hostName == hostname || it.resourceName == 'IIS'
        }
        if (resource) {
            logger.debug("IIS resource already exists")
            return resource.resourceName
        }

        def port = System.getenv('IIS_RESOURCE_PORT') ?: '7800'
        def workspaceName = randomize("IIS")

        def workspaceResult = dsl """
            createWorkspace(
                workspaceName: '${workspaceName}',
                agentDrivePath: 'c:/tmp/workspace',
                agentUncPath: 'c:\\\\tmp\\\\workspace',
                local: '1'
            )
        """

        logger.debug(objectToJson(workspaceResult))

        def result = dsl """
            createResource(
                resourceName: '${randomize("IIS")}',
                hostName: '$hostname',
                port: '$port',
                workspaceName: '$workspaceName'
            )
        """

        logger.debug(objectToJson(result))
        def resName = result?.resource?.resourceName
        assert resName
        resName
    }

    def createHelperProject(resName) {
        def appcmdPath = 'c:/windows/system32/inetsrv/appcmd.exe'

        dsl """
            project '$helperProjName', {

                procedure 'Run App Cmd', {
                    resourceName = '$resName'
                    step 'Run Command', {
                        command = '$appcmdPath \$[appCmd]'
                        logFileName = 'RunCommand.log'
                    }
                    step 'Read Log', {
                        shell = 'cb-perl'
                        command = '''
                            use strict;
                            use warnings;
                            use ElectricCommander;
                            open my \$fh, 'RunCommand.log' or die \$!;
                            my \$content = join('', <\$fh>);
                            close \$fh;
                            my \$ec = ElectricCommander->new;
                            \$ec->setProperty('/myJob/appCmdLog', \$content);
                        '''
                    }
                    formalParameter 'appCmd', defaultValue: '', {
                        type = 'entry'
                    }
                }

                procedure 'RunCmd', {
                    resourceName = '$resName'
                    step 'runcmd', {
                        command = '''\$[cmd]'''
                        logFileName = 'Command.log'
                        shell = 'powershell'
                    }

                    step 'Read Log', {
                        shell = 'cb-perl'
                        command = '''
                            use strict;
                            use warnings;
                            use ElectricCommander;
                            open my \$fh, 'Command.log' or die \$!;
                            my \$content = join('', <\$fh>);
                            close \$fh;
                            my \$ec = ElectricCommander->new;
                            \$ec->setProperty('/myJob/cmdLog', \$content);
                        '''
                    }

                    formalParameter 'cmd', defaultValue: '', {
                        type = 'textarea'
                    }
                }


                procedure 'mkdir', {
                    resourceName = '$resName'
                    step 'mkdir', {
                        command = 'mkdir "\$[directory]"'
                        shell = 'powershell'
                    }

                    formalParameter 'directory', defaultValue: '', {
                        type = 'entry'
                    }
                }


                procedure 'ls', {
                    resourceName = '$resName'
                    step 'ls', {
                        command = 'ls "\$[directory]"'
                        shell = 'powershell'
                    }
                    formalParameter 'directory', defaultValue: '', {
                        type = 'entry'
                    }
                }

                procedure 'checkDir', {
                    resourceName = '$resName'
                    step 'check', {
                        command = '''
                        use strict;
                        use warnings;

                        my \$dir = '\$[dir]';
                        if (-e \$dir) {
                            print "Exists\n";
                        }
                        else {
                            print "Does not exist\n";
                        }
                        '''
                        shell = 'cb-perl'
                        logFileName = 'Command.log'
                    }

                    step 'Read Log', {
                        shell = 'cb-perl'
                        command = '''
                            use strict;
                            use warnings;
                            use ElectricCommander;
                            open my \$fh, 'Command.log' or die \$!;
                            my \$content = join('', <\$fh>);
                            close \$fh;
                            my \$ec = ElectricCommander->new;
                            \$ec->setProperty('/myJob/cmdLog', \$content);
                        '''
                    }


                    formalParameter 'dir', defaultValue: '', {
                        type = 'entry'
                    }
                }

            }
        """
    }

    def removeSite(siteName) {
        siteName = siteName.replaceAll(~/%/, '%%')
        def result = dsl """
            runProcedure(
                projectName: '$helperProjName',
                procedureName: 'Run App Cmd',
                actualParameter: [
                    appCmd: 'delete site "$siteName"'
                ]
            )
        """
        assert result.jobId
        waitUntil {
            jobCompleted result.jobId
        }
    }

    def createSite(siteName, bindings = '', path = '', id = '') {
        if (!bindings)
            bindings = 'http://*:9900'
        if (!path)
            path = "c:/tmp/test_path"
        def idString = id ? "/id:${id}" : ''
        siteName = siteName.replaceAll(~/%/, '%%')
        runAppCmd("add site /name:\"$siteName\" /bindings:\"$bindings\" /physicalpath:\"$path\" $idString")
    }


    def moveAppToPool(siteName, appName, poolName) {
        def name = siteName + '/'
        if (appName) {
            name += appName
        }
        runAppCmd("set app /app.name:\"${name}\" /applicationPool:\"${poolName}\"")

    }


    def runAppCmd(command) {

        def result = dsl """
            runProcedure(
                projectName: '$helperProjName',
                procedureName: '$helperProcedure',
                actualParameter: [
                    appCmd: '''$command'''
                ]
            )
        """
        assert result.jobId
        waitUntil {
            jobCompleted result.jobId
        }
        return result.jobId
    }


    def runAppCmdLogs(cmd) {
        def jobId = runAppCmd(cmd)
        def logs = getJobProperty('/myJob/appCmdLog', jobId)
        return logs
    }

    def runCmd(command) {
        def result = dsl """
            runProcedure(
                projectName: '$helperProjName',
                procedureName: 'RunCmd',
                actualParameter: [
                    cmd: '''$command'''
                ]
            )
        """
        assert result.jobId
        waitUntil {
            jobCompleted result.jobId
        }
        def logs = getJobProperty('/myJob/cmdLog', result.jobId)
        return logs
    }

    def dirExists(dir) {
        def result = dsl """
            runProcedure(
                projectName: '$helperProjName',
                procedureName: 'checkDir',
                actualParameter: [
                    dir: '''$dir'''
                ]
            )
        """
        assert result.jobId
        waitUntil {
            jobCompleted result.jobId
        }
        def logs = getJobProperty('/myJob/cmdLog', result.jobId)
        return logs
    }

    def createAppPool(name) {
        runAppCmd("add apppool /apppool.name:\"${name}\"")
    }

    def stopAppPool(name) {
        runAppCmd("stop apppool /apppool.name:\"${name}\"")
    }

    def startAppPool(name) {
        runAppCmd("start apppool /apppool.name:\"${name}\"")
    }


    def removeAppPool(name) {
        runAppCmd("delete apppool /apppool.name:\"${name}\"")
    }


    def stopSite(siteName) {
        def result = dsl """
            runProcedure(
                projectName: '$helperProjName',
                procedureName: '$helperProcedure',
                actualParameter: [
                    appCmd: 'stop site /name:"$siteName" '
                ]
            )
        """
        assert result.jobId
        waitUntil {
            jobCompleted result.jobId
        }
    }

    def startSite(siteName) {
        def result = dsl """
            runProcedure(
                projectName: '$helperProjName',
                procedureName: '$helperProcedure',
                actualParameter: [
                    appCmd: 'start site /name:"$siteName" '
                ]
            )
        """
        assert result.jobId
        waitUntil {
            jobCompleted result.jobId
        }
    }

    def createApp(siteName, appName, physicalPath = '') {
        if(!physicalPath)
            physicalPath = 'c:/tmp/path'

        def app = "/path:" + '/"' + appName + '"'
        def result = dsl """
            runProcedure(
                projectName: '$helperProjName',
                procedureName: '$helperProcedure',
                actualParameter: [
                    appCmd: 'add app /site.name:"$siteName" $app /physicalPath:"$physicalPath" '
                ]
            )
        """
        assert result.jobId
        waitUntil {
            jobCompleted result.jobId
        }
    }

    def createVdir(name, path = '', physicalPath = '') {
        path ?: 'app'
        physicalPath ?: 'c:/tmp/path'
        runAppCmd("add vdir /app.name:\"${name}\" /path:\"${path}\" /physicalpath:\"${physicalPath}\"")
    }


    def getSite(siteName) {
        siteName = siteName.replaceAll(~/%/, '%%')
        def result = dsl """
            runProcedure(
                projectName: '$helperProjName',
                procedureName: 'Run App Cmd',
                actualParameter: [
                    appCmd: 'list site /name:"$siteName"'
                ]
            )
        """
        assert result.jobId
        waitUntil {
            jobCompleted result.jobId
        }

        def logs = getJobProperty('/myJob/appCmdLog', result.jobId)
        def group = logs =~ /\(id:(\d+),bindings:(.+),state:(\w+)/
        def bindings = group[0][2].split(',')

        def retval = [
            id: group[0][1],
            bindings: bindings,
            state: group[0][3]
        ]

        return retval
    }


    def getVdir(siteName, appName = '') {
        def vdirPath = siteName + '/'
        if (appName) {
            vdirPath += appName+ '/'
        }
        vdirPath = vdirPath.replaceAll(~/%/, '%%')
        def result = dsl """
            runProcedure(
                projectName: '$helperProjName',
                procedureName: 'Run App Cmd',
                actualParameter: [
                    appCmd: 'list vdir /vdir.name:"${vdirPath}"'
                ]
            )
        """
        assert result.jobId
        waitUntil {
            jobCompleted result.jobId
        }

        def logs = getJobProperty('/myJob/appCmdLog', result.jobId)

        def group = logs =~ /\(physicalPath:(.+)\)/
        def retval = [
            path: group[0][1]
        ]

        return retval
    }


    def getApp(siteName, appName = '') {
        assert siteName

        appName = appName.replace('/', '')

        def result = dsl """
            runProcedure(
                projectName: '$helperProjName',
                procedureName: 'Run App Cmd',
                actualParameter: [
                    appCmd: 'list app /app.name:"${siteName}/${appName}"'
                ]
            )
        """
        assert result.jobId
        waitUntil {
            jobCompleted result.jobId
        }

        def logs = getJobProperty('/myJob/appCmdLog', result.jobId)
        def group = logs =~ /\(applicationPool:(.+)\)/
        def retval = [
            applicationPool: group[0][1]
        ]

        return retval
    }

    def getAppPool(name, all = false) {
        name = name.replaceAll(~/%/, '%%')
        def jobId = runAppCmd("list apppool /apppool.name:\"${name}\"")
        def logs = getJobProperty('/myJob/appCmdLog', jobId)

        def group = logs =~ /\(MgdVersion:(.+),MgdMode:(\w+),state:(\w+)\)/
        def retval = [
            mgdVersion: group[0][1],
            mgdMode: group[0][2],
            state: group[0][3]
        ]

        if (all) {
            jobId = runAppCmd("list apppool /apppool.name:\"${name}\" /text:*")
            logs = getJobProperty('/myJob/appCmdLog', jobId)
            retval.details = logs
        }

        return retval
    }

    def createDir(dirName) {
        def result = dsl """
            runProcedure(
                projectName: '$helperProjName',
                procedureName: 'mkdir',
                actualParameter: [
                    directory: '$dirName'
                ]
            )
        """

        assert result.jobId
        waitUntil {
            jobCompleted result.jobId
        }
    }


    def lsDir(dirName) {
        def result = dsl """
            runProcedure(
                projectName: '$helperProjName',
                procedureName: 'ls',
                actualParameter: [
                    directory: '$dirName'
                ]
            )
        """

        assert result.jobId
        waitUntil {
            jobCompleted result.jobId
        }

         def logs = getJobProperty('/myJob/cmdLog', result.jobId)
         logs
    }

    def addBinding(name, binding) {
        def cmd = "set site /site.name:\"${name}\" /+\"bindings.[protocol='http',bindingInformation='${binding}']\""
        println cmd
        runAppCmd(cmd)
    }

    def startServer() {
        runCmd("iisreset /START")
    }

    def stopServer() {
        runCmd('iisreset /STOP')
    }

    def restoreIISService() {
        def result = dsl """
            runProcedure(
                projectName: '$helperProjName',
                procedureName: 'RunCmd',
                actualParameter: [
                    cmd: '''iisreset'''
                ]
            )
        """
        assert result.jobId


        waitUntil {
            def status = dsl """getJobStatus(jobId: '${result.jobId}')"""
            assert status.status == 'completed'
        }

    }

    def serverStatus() {
        def logs = runCmd('iisreset /STATUS')
        logs
    }

}
