import sys

# Add path to find includes
sys.path.append(scriptsPath)

from authentication import Authentication

class LoginData:

    def __init__(self):
        print "Scripts path = ", scriptsPath
        self.authentication = Authentication(self);
        self.authentication.session_init();

        if self.authentication.is_logged_in():
            responseMsg = self.authentication.get_name();
        else:
            responseMsg = self.authentication.get_error();
            response.setStatus(500)
        writer = response.getPrintWriter("text/html")
        writer.println(responseMsg)
        writer.close()

    # An access point for included files
    #   to get the bound Jython globals.
    def __call__(self, var):
        return globals()[var];

scriptObject = LoginData()
