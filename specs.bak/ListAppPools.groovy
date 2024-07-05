import spock.lang.*
import com.electriccloud.spec.*
import groovy.json.JsonSlurper

class ListAppPools extends PluginTestHelper {
    static def projectName = 'EC-IIS Specs ListAppPools'
    static def iisHandler
    static def procName = 'ListApplicationPools'

    def doSetupSpec() {
        dsl 'setProperty(propertyName: "/plugins/EC-IIS/project/ec_debug_logToProperty", value: "/myJob/debug_logs")'
        def resName = createIISResource()
        dslFile 'dsl/RunProcedure.dsl', [
            projName: projectName,
            resName: resName,
            procName: procName,
            params: [
                searchcriteria: '',
                propertyName: '',
                dumpFormat: '',
                failOnEmpty: ''
            ]
        ]
        createHelperProject(resName)
    }

    def doCleanupSpec() {
        dsl "deleteProject '$projectName'"
    }

    @Unroll
    def "show one app pool, property #propertyName, dump format #dumpFormat"() {
        given:
            def appPoolName = randomize('appPool')
            createAppPool(appPoolName)
        when: "procedure runs"
            def result = runProcedureDsl """
                runProcedure(
                    projectName: "$projectName",
                    procedureName: '$procName',
                    actualParameter: [
                        searchcriteria: '$appPoolName',
                        dumpFormat: '$dumpFormat',
                        propertyName: '$propertyName'
                    ]
                )
            """
        then: 'it finishes'
            assert result.outcome == 'success'
            logger.debug(result.logs)

            def resultProperty = propertyName ? propertyName : '/myJob/IISSiteList'
            switch(dumpFormat) {
                case '' :
                    validateResultPlaintext(result.jobId, resultProperty, appPoolName)
                    break
                case 'propertySheet':
                    validateResultPropertySheet(result.jobId, resultProperty, appPoolName)
                    break
                case 'json':
                    validateResultJson(result.jobId, resultProperty, appPoolName)
                    break
                case 'xml':
                    validateResultXML(result.jobId, resultProperty, appPoolName)
                    break
                default:
                    throw new RuntimeException("Don't know how to validate $dumpFormat")
            }
        cleanup:
            removeAppPool(appPoolName)
        where:
            dumpFormat << ['', 'propertySheet', 'json', 'xml']
            propertyName = dumpFormat ? '/myJob/result' : ''
    }

    @Unroll
    def "Show multiple pools #criteria"() {
        given:
            (1..3).each {
                createAppPool("Pool ${it}")
            }
        when:
            def result = runProcedureDsl """
                runProcedure(
                    projectName: "$projectName",
                    procedureName: '$procName',
                    actualParameter: [
                        searchcriteria: '$criteria',
                        propertyName: '/myJob/result'
                    ]
                )
            """
        then:
            assert result.outcome == 'success'
            logger.debug(result.log)
            def saved = getJobProperty('/myJob/result', result.jobId)
            logger.debug(saved)
            (1..3).each {
                assert saved =~ /APPPOOL \"Pool ${it}\" \(MgdVersion:v(4.0|2.0),MgdMode:Integrated,state:Started\)/
            }
        cleanup:
            (1..3).each {
                removeAppPool("Pool ${it}")
            }
        where:
            criteria << ['', '/autoStart:true']
    }

    @Unroll
    def "Empty list #failOnEmpty"() {
        when:
            def result = runProcedureDsl """
                runProcedure(
                    projectName: "$projectName",
                    procedureName: '$procName',
                    actualParameter: [
                        searchcriteria: 'no_site',
                        propertyName: '/myJob/result',
                        failOnEmpty: '$failOnEmpty'
                    ]
                )
            """
        then:
            if (failOnEmpty == '1') {
                assert result.outcome == 'error'
            }
            else {
                assert result.outcome == 'warning'
            }
        where:
            failOnEmpty << ['1', '0']
    }

    def validateResultPlaintext(jobId, propertyName, appPoolName) {
        def result = getJobProperty(propertyName, jobId)
        assert result =~ /APPPOOL \"$appPoolName\" \(MgdVersion:v(4.0|2.0),MgdMode:Integrated,state:Started\)/
    }

    def validateResultXML(jobId, propertyName, appPoolName) {
        def result = getJobProperty(propertyName, jobId)
        assert result =~ /managedPipelineMode/
        assert result =~ /state/
    }

    def validateResultJson(jobId, propertyName, appPoolName) {
        def result = getJobProperty(propertyName, jobId)
        def jsonSlurper = new JsonSlurper()
        def object = jsonSlurper.parseText(result)
        assert object[appPoolName].state
        assert object[appPoolName].managedVersion
    }

    def validateResultPropertySheet(jobId, propertyName, appPoolName) {
        def result = getJobProperties(jobId)
        logger.debug(objectToJson(result))
        assert result.result[appPoolName].state
        assert result.result[appPoolName].managedVersion
        assert result.result[appPoolName].managedPipelineMode
    }

}
