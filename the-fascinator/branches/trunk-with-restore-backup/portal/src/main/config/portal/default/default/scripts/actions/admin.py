from authentication import Authentication

class LoginData:

    def __init__(self):
        self.authentication = Authentication()
        self.authentication.session_init()

        self.writer = response.getPrintWriter("text/html; charset=UTF-8")

        if self.authentication.is_logged_in() and self.authentication.is_admin():
            self.process()
        else:
            self.throw_error("Only administrative users can access this feature")

    def add_user(self):
        username = formData.get("field")
        rolename = formData.get("hidden")
        source = formData.get("source")
        self.authentication.set_role_plugin(source)
        self.authentication.set_role(username, rolename)

        err = self.authentication.get_error()
        if err is None:
            self.writer.println(username)
            self.writer.close()

        else:
            self.throw_error(err)

    def change_password(self):
        username = formData.get("username")
        password = formData.get("password")
        password_confirm = formData.get("password_confirm")

        if password != password_confirm:
            self.throw_error("The confirm password field does not match the password.")

        else:
            source = formData.get("source")
            self.authentication.set_auth_plugin(source)
            self.authentication.change_password(username, password)

            err = self.authentication.get_error()
            if err is None:
                self.writer.println(username)
                self.writer.close()

            else:
                self.throw_error(err)

    def create_role(self):
        rolename = formData.get("field")
        source   = formData.get("source")
        self.authentication.set_role_plugin(source)
        self.authentication.create_role(rolename)

        err = self.authentication.get_error()
        if err is None:
            self.writer.println(rolename)
            self.writer.close()

        else:
            self.throw_error(err)

    def create_user(self):
        username = formData.get("username")
        password = formData.get("password")
        password_confirm = formData.get("password_confirm")

        if password != password_confirm:
            self.throw_error("The confirm password field does not match the password.")

        else:
            source = formData.get("source")
            self.authentication.set_auth_plugin(source)
            self.authentication.create_user(username, password)

            err = self.authentication.get_error()
            if err is None:
                self.writer.println(username)
                self.writer.close()

            else:
                self.throw_error(err)

    def delete_role(self):
        rolename = formData.get("rolename")
        source = formData.get("source")
        self.authentication.set_role_plugin(source)
        self.authentication.delete_role(rolename)

        err = self.authentication.get_error()
        if err is None:
            self.writer.println(rolename)
            self.writer.close()

        else:
            self.throw_error(err)

    def delete_user(self):
        username = formData.get("username")
        source = formData.get("source")
        self.authentication.set_auth_plugin(source)
        self.authentication.delete_user(username)

        err = self.authentication.get_error()
        if err is None:
            self.writer.println(username)
            self.writer.close()

        else:
            self.throw_error(err)

    def get_current_access(self):
        record = formData.get("record")
        roles_list = self.authentication.get_access_roles_list(record)

        err = self.authentication.get_error()
        if err is None:
            # We need a JSON string for javascript
            plugin_strings = []
            for plugin in roles_list.keys():
                roles = roles_list[plugin]
                if len(roles) > 0:
                    plugin_strings.append("'" + plugin + "' : ['" + "','".join(roles) + "']")
                else:
                    plugin_strings.append("'" + plugin + "' : []")
            responseMessage = "{" + ",".join(plugin_strings) + "}"
            self.writer.println(responseMessage)
            self.writer.close()

        else:
            self.throw_error(err)

    def grant_access(self):
        record = formData.get("record")
        role   = formData.get("role")
        source = formData.get("source")
        self.authentication.set_access_plugin(source)
        self.authentication.grant_access(record, role)

        err = self.authentication.get_error()
        if err is None:
            self.writer.println(role)
            self.writer.close()
            self.reindex_record(record)

        else:
            self.throw_error(err)

    def list_users(self):
        rolename = formData.get("rolename")
        source = formData.get("source")
        self.authentication.set_auth_plugin(source)
        user_list = self.authentication.list_users(rolename)

        err = self.authentication.get_error()
        if err is None:
            # We need a JSON string for javascript
            responseMessage = "{['" + "','".join(user_list) + "']}"
            self.writer.println(responseMessage)
            self.writer.close()

        else:
            self.throw_error(err)

    def process(self):
        action = formData.get("verb")

        switch = {
            "add-user"           : self.add_user,
            "create-role"        : self.create_role,
            "create-user"        : self.create_user,
            "delete-role"        : self.delete_role,
            "delete-user"        : self.delete_user,
            "change-password"    : self.change_password,
            "get-current-access" : self.get_current_access,
            "grant-access"       : self.grant_access,
            "list-users"         : self.list_users,
            "remove-user"        : self.remove_user,
            "revoke-access"      : self.revoke_access
        }
        switch.get(action, self.unknown_action)()

    def reindex_record(self, recordId):
        portalManager = Services.getPortalManager()
        portalManager.reHarvestObject(recordId)

    def remove_user(self):
        username = formData.get("username")
        rolename = formData.get("rolename")
        source = formData.get("source")
        self.authentication.set_role_plugin(source)
        self.authentication.remove_role(username, rolename)

        err = self.authentication.get_error()
        if err is None:
            self.writer.println(username)
            self.writer.close()

        else:
            self.throw_error(err)

    def revoke_access(self):
        record = formData.get("record")
        role   = formData.get("role")
        source = formData.get("source")
        self.authentication.set_access_plugin(source)
        self.authentication.revoke_access(record, role)

        err = self.authentication.get_error()
        if err is None:
            self.writer.println(role)
            self.writer.close()
            self.reindex_record(record)

        else:
            self.throw_error(err)

    def throw_error(self, message):
        response.setStatus(500)
        self.writer.println("Error: " + message)
        self.writer.close()

    def unknown_action(self):
        self.throw_error("Unknown action requested - '" + formData.get("verb") + "'")

scriptObject = LoginData()
