from authentication import Authentication

class LoginData:

    def __init__(self):
        self.authentication = Authentication()
        self.authentication.session_init()

        if self.authentication.is_logged_in():
            if self.authentication.is_admin():
                responseMsg = self.authentication.get_name() + ":admin"
            else:
                responseMsg = self.authentication.get_name() + ":notadmin"
        else:
            responseMsg = self.authentication.get_error()
            response.setStatus(500)
        writer = response.getPrintWriter("text/html; charset=UTF-8")
        writer.println(responseMsg)
        writer.close()

scriptObject = LoginData()
