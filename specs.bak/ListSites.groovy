import spock.lang.*
import com.electriccloud.spec.*
import groovy.json.JsonSlurper

class ListSites extends PluginTestHelper {
    static def projectName = 'EC-IIS Specs ListSites'
    static def iisHandler
    static def procName = 'ListSites'

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
    def "show one site, property #propertyName, dump format #dumpFormat"() {
        given:
            def siteName = randomize('site')
            createSite(siteName)
        when: "procedure runs"
            def result = runProcedureDsl """
                runProcedure(
                    projectName: "$projectName",
                    procedureName: '$procName',
                    actualParameter: [
                        searchcriteria: '$siteName',
                        dumpFormat: '$dumpFormat',
                        propertyName: '$propertyName'
                    ]
                )
            """
        then: 'it finishes'
            assert result.outcome == 'success'
            logger.debug(result.logs)
            def properties = getJobProperties(result.jobId)
            logger.debug(objectToJson(properties))

            def resultProperty = propertyName ? propertyName : '/myJob/IISSiteList'
            switch(dumpFormat) {
                case '' :
                    validateResultPlaintext(result.jobId, resultProperty, siteName)
                    break
                case 'propertySheet':
                    validateResultPropertySheet(result.jobId, resultProperty, siteName)
                    break
                case 'json':
                    validateResultJson(result.jobId, resultProperty, siteName)
                    break
                case 'xml':
                    validateResultXML(result.jobId, resultProperty, siteName)
                    break
                default:
                    throw new RuntimeException("Don't know how to validate $dumpFormat")
            }
        cleanup:
            removeSite(siteName)
        where:
            dumpFormat << ['', 'propertySheet', 'json', 'xml']
            propertyName = dumpFormat ? '/myJob/result' : ''
    }

    def "check site with multiple bindings"() {
        given:
            def siteName = randomize('site')
            createSite(siteName)
            addBinding(siteName, '*:111122')
        when: "procedure runs"
            def result = runProcedureDsl """
                runProcedure(
                    projectName: "$projectName",
                    procedureName: '$procName',
                    actualParameter: [
                        searchcriteria: '$siteName',
                        dumpFormat: 'json',
                        propertyName: '/myJob/result'
                    ]
                )
            """
        then:
            assert result.outcome == 'success'
            def json = getJobProperty('/myJob/result', result.jobId)
            logger.debug(json)

            def jsonSlurper = new JsonSlurper()
            def object = jsonSlurper.parseText(json)
            assert object[siteName].bindings.size() == 2

    }

    def "Show multiple sites"() {
        given:
            (1..3).each {
                createSite("Site ${it}")
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
                assert saved =~ /SITE \"Site ${it}\"/
            }
        cleanup:
            (1..3).each {
                removeSite("Site ${it}")
            }
        where:
            criteria << ['', '/serverAutoStart:true']
    }

    @Unroll
    def "No sites #failOnEmpty"() {
        when:
            def result = runProcedureDsl """
                runProcedure(
                    projectName: "$projectName",
                    procedureName: '$procName',
                    actualParameter: [
                        searchcriteria: 'no_such_site',
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
                assert result.logs =~ /No sites found/
            }

            logger.debug(result.logs)
        where:
            failOnEmpty << ['1', '0']
    }


    def validateResultPlaintext(jobId, propertyName, name) {
        def result = getJobProperty(propertyName, jobId)
        assert result =~ /SITE \"$name\"/
    }

    def validateResultXML(jobId, propertyName, name) {
        def result = getJobProperty(propertyName, jobId)
        assert result =~ /state/
    }

    def validateResultJson(jobId, propertyName, name) {
        def result = getJobProperty(propertyName, jobId)
        def jsonSlurper = new JsonSlurper()
        def object = jsonSlurper.parseText(result)
        assert object[name].state
        assert object[name].id
        assert object[name].bindings
    }

    def validateResultPropertySheet(jobId, propertyName, name) {
        def result = getJobProperties(jobId)
        logger.debug(objectToJson(result))
        assert result.result[name].state
        assert result.result[name].id
        assert result.result[name].bindings
    }

}
