def projName = args.projName
def resName = args.resName

def params = [
  sitename: '',
]

project projName, {
    procedure 'Start Site', {
      resourceName = resName

      step 'Start Site', {
        description = ''
        subprocedure = 'StartWebSite'
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
