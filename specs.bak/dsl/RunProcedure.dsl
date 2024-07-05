def projName = args.projName
def resName = args.resName
def params = args.params
def procName = args.procName


def hasCredentials = false
project projName, {
    procedure procName, {
      resourceName = resName

      params.each {name, defValue ->
        if (name != 'credential') {
          formalParameter name, defaultValue: defValue, {
            type = 'textarea'
          }
        }
        else {
          hasCredentials = true
          formalParameter name, defaultValue: defValue, {
            type = 'credential'
          }
        }
      }

      step 'Run IIS procedure', {
        description = ''
        subprocedure = procName
        subproject = '/plugins/EC-IIS/project'

        params.each { name, defValue ->
          actualParameter name, '$[' + name + ']'
        }
        if (hasCredentials) {
          attachParameter(formalParameterName: 'credential')
        }
      }


  }
}
