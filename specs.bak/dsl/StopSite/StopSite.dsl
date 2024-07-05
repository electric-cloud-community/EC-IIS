def projName = args.projName
def resName = args.resName

def params = [
  sitename: '',
]

project projName, {
    procedure 'Stop Site', {
      resourceName = resName

      step 'Stop Site', {
        description = ''
        subprocedure = 'StopWebSite'
        subproject = '/plugins/EC-IIS/project'

        params.each { name, defValue ->
          actualParameter name, '$[' + name + ']'
        }
      }


      params.each {name, defValue ->
        formalParameter name, defaultValue: defValue, {
          type = 'textarea'
        }
      }
  }
}
