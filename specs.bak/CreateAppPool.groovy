import spock.lang.*
import com.electriccloud.spec.*

class CreateAppPool extends PluginTestHelper {
    static def projectName = 'EC-IIS Specs CreateAppPool'
    static def iisHandler
    static def procName = 'CreateAppPool'

    def doSetupSpec() {
        dsl 'setProperty(propertyName: "/plugins/EC-IIS/project/ec_debug_logToProperty", value: "/myJob/debug_logs")'
        def resName = createIISResource()
        dslFile 'dsl/RunProcedure.dsl', [
            projName: projectName,
            resName: resName,
            procName: procName,
            params: [
                apppoolname: '',
                managedRuntimeVersion: '',
                enable32BitAppOnWin64: '',
                managedPipelineMode: '',
                queueLength: '',
                autoStart: '',
                'cpu.limit': '',
                'cpu.action': '',
                'cpu.resetInterval': '',
                'cpu.smpAffinitized': '',
                'cpu.smpProcessorAffinityMask': '',
                'processModel.identityType': '',
                'processModel.idleTimeout': '',
                'processModel.loadUserProfile': '',
                'processModel.maxProcesses': '',
                'processModel.pingingEnabled': '',
                'processModel.pingResponseTime': '',
                'processModel.pingInterval': '',
                'processModel.shutdownTimeLimit': '',
                'processModel.startupTimeLimit': '',
                'failure.orphanWorkerProcess': '',
                'failure.orphanActionExe': '',
                'failure.orphanActionParams': '',
                'failure.loadBalancerCapabilities': '',
                'failure.rapidFailProtection': '',
                'failure.rapidFailProtectionInterval': '',
                'failure.rapidFailProtectionMaxCrashes': '',
                'failure.autoShutdownExe': '',
                'failure.autoShutdownParams': '',
                'recycling.disallowOverlappingRotation': '',
                'recycling.disallowRotationOnConfigChange': '',
                'recycling.periodicRestart.privateMemory': '',
                'recycling.periodicRestart.time': '',
                'recycling.periodicRestart.requests': '',
                'recycling.periodicRestart.schedule': '',
                'recycling.periodicRestart.memory': '',
                'appPoolAdditionalSettings': '',
            ]
        ]
        createHelperProject(resName)
    }

    def doCleanupSpec() {
        dsl "deleteProject '$projectName'"
    }

    @Unroll

    def "create with params #paramName, #value"() {
        given:
            def name = randomize('AppPool')
        when:
            def result = runProcedureDsl """
                runProcedure(
                    projectName: "$projectName",
                    procedureName: '$procName',
                    actualParameter: [
                        apppoolname: '$name',
                        '$paramName': '$value'
                    ]
                )
            """
        then:
            assert result.outcome == 'success'
            def details = runAppCmdLogs("list apppool /apppool.name:\"$name\" /text:*")
            logger.debug(details)
            def pname = paramName.split(/\./).last()
            if (paramName != 'recycling.periodicRestart.schedule') {
                assert details =~ /(?i)${pname}:"${convert(pname, value)}"/
            }
            else {
                logger.debug(details)
                def times = value.split(/\s*,\s*/)
                times.each {
                    assert details =~ /$it/
                }
            }
        cleanup:
            removeAppPool(name)
        where:
            paramName                                            | value
            'managedRuntimeVersion'                              | 'v2.0'
            'managedRuntimeVersion'                              | 'v4.0'
            'enable32BitAppOnWin64'                              | 'True'
            'enable32BitAppOnWin64'                              | 'False'
            'managedPipelineMode'                                | 'Classic'
            'managedPipelineMode'                                | 'Integrated'
            'queueLength'                                        | '99'
            'queueLength'                                        | '65'
            'autoStart'                                          | 'False'
            'autoStart'                                          | 'True'
            'cpu.limit'                                          | '10'
            'cpu.action'                                         | 'NoAction'
            'cpu.resetInterval'                                  | '2'
            'cpu.smpAffinitized'                                 | 'True'
            'cpu.smpAffinitized'                                 | 'False'
            'cpu.smpProcessorAffinityMask'                       | '0'
            'processModel.identityType'                          | 'LocalService'
            'processModel.idleTimeout'                           | '2'
            'processModel.loadUserProfile'                       | 'True'
            'processModel.loadUserProfile'                       | 'False'
            'processModel.maxProcesses'                          | '2'
            'processModel.pingingEnabled'                        | 'True'
            'processModel.pingingEnabled'                        | 'False'
            'processModel.pingResponseTime'                      | '3'
            'processModel.pingInterval'                          | '5'
            'processModel.shutdownTimeLimit'                     | '3'
            'processModel.startupTimeLimit'                      | '5'
            'failure.orphanWorkerProcess'                        | 'False'
            'failure.orphanWorkerProcess'                        | 'True'
            'failure.orphanActionExe'                            | 'ntsd.exe'
            'failure.orphanActionParams'                         | '-g -p'
            'failure.loadBalancerCapabilities'                   | 'TcpLevel'
            'failure.loadBalancerCapabilities'                   | 'HttpLevel'
            'failure.rapidFailProtection'                        | 'True'
            'failure.rapidFailProtection'                        | 'False'
            'failure.rapidFailProtectionInterval'                | '5'
            'failure.rapidFailProtectionMaxCrashes'              | '2'
            'failure.autoShutdownExe'                            | 'test.exe'
            'failure.autoShutdownParams'                         | 'params'
            'recycling.disallowOverlappingRotation'              | 'True'
            'recycling.disallowOverlappingRotation'              | 'False'
            'recycling.disallowRotationOnConfigChange'           | 'False'
            'recycling.disallowRotationOnConfigChange'           | 'True'
            'recycling.periodicRestart.privateMemory'            | '2'
            'recycling.periodicRestart.time'                     | '5'
            'recycling.periodicRestart.requests'                 | '5'
            'recycling.periodicRestart.memory'                   | '5'
            'recycling.periodicRestart.schedule'                 | '13:00:00, 12:00:00'
            'recycling.periodicRestart.schedule'                 | '13:00:00'

    }

    @Unroll
    def "update app pool, #paramName, oldValue #originalValue, newValue: #newValue"() {
        given:
            def name = randomize('appPool')
            def resultStart = runProcedureDsl """
                runProcedure(
                    projectName: "$projectName",
                    procedureName: '$procName',
                    actualParameter: [
                        apppoolname: '$name',
                        '$paramName': '$originalValue',
                    ]
                )
            """
            assert resultStart.outcome == 'success'

        when:
            def result = runProcedureDsl """
                runProcedure(
                    projectName: "$projectName",
                    procedureName: '$procName',
                    actualParameter: [
                        apppoolname: '$name',
                        '$paramName': '$newValue',
                    ]
                )
            """
        then:
            assert result.outcome == 'success'
            logger.debug(result.logs)

            def details = runAppCmdLogs("list apppool /apppool.name:\"$name\" /text:*")
            def times = newValue.split(/\s*,\s*/)
            times.each {
                assert details =~ /$it/
            }
        where:
            paramName                            |     originalValue     |     newValue

            'recycling.periodicRestart.schedule' |  '13:00:00'           | '10:00:00'
            'recycling.periodicRestart.schedule' |  '13:00:00'           | '10:00:00, 12:00:00'

    }



    def "create app pool"() {
        when:
            def result = runProcedureDsl """
                runProcedure(
                    projectName: "$projectName",
                    procedureName: '$procName',
                    actualParameter: [
                        apppoolname: '$name',
                    ]
                )
            """
        then:
            assert result.outcome == 'success'
            def pool = getAppPool(name)
            assert pool
        cleanup:
            removeAppPool(name)
        where:
            name << ['AppPool']
    }


    def "negative: invalid name"() {
        when:
            def result = runProcedureDsl """
                runProcedure(
                    projectName: "$projectName",
                    procedureName: '$procName',
                    actualParameter: [
                        apppoolname: '$name'
                    ]
                )
            """
        then:
            assert result.outcome == 'error'
            assert result.logs =~ /Reason: Invalid application pool name/
        cleanup:
            removeAppPool(name)
        where:
            name << ['!*(#&*(']

    }

    def convert(name, value) {
        def retval
        switch(name) {
            case ~/resetInterval|rapidFailProtectionInterval|idleTimeout|time/:
                retval = "00:0$value:00"
                break
            case ~/smpProcessorAffinityMask/:
                retval = '4294967295'
                break
            case ~/shutdownTimeLimit|pingInterval|pingResponseTime|startupTimeLimit/:
                retval = "00:00:0$value"
                break
            default:
                retval = value
        }

        return retval
    }
}
