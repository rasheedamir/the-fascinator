from au.edu.usq.fascinator.api import PluginDescription;
from au.edu.usq.fascinator.api.authentication import AuthenticationException;
from au.edu.usq.fascinator.api.authentication import User;
from au.edu.usq.fascinator.api.roles import RolesException;

from __main__ import Services, formData, sessionState

class Authentication:
    active_auth_plugin = None
    active_role_plugin = None
    current_user = None
    has_error = False
    error_message = None
    GUEST_ROLE = 'guest'

    def __init__(self):
        self.auth = Services.getAuthManager()
        self.role = Services.getRoleManager()
        self.check_login()

    def change_password(self, username, password):
        try:
            self.auth.changePassword(username, password)
            self.has_error = False
            return "Password changed."
        except AuthenticationException, e:
            self.error_message = self.parse_error(e)

    def check_login(self):
        action = formData.get("verb")

        # User is logging in
        if (action == "login"):
            username = formData.get("username")
            if username is not None:
                password = formData.get("password")
                self.login(username, password)

        # Normal page render, or logout
        else:
            username = sessionState.get("username")
            source   = sessionState.get("source")
            if username is not None:
                self.current_user = self.get_user(username, source)

        # User is logging out, make sure we ran get_user() first
        if (action == "logout"):
            self.logout()

    def create_role(self, rolename):
        try:
            self.role.createRole(rolename)
            self.has_error = False
            return username
        except RolesException, e:
            self.error_message = self.parse_error(e)

    def create_user(self, username, password):
        try:
            self.auth.createUser(username, password)
            self.has_error = False
            return username
        except AuthenticationException, e:
            self.error_message = self.parse_error(e)

    def delete_role(self, rolename):
        try:
            self.role.deleteRole(rolename)
            self.has_error = False
            return rolename
        except RolesException, e:
            self.error_message = self.parse_error(e)

    def delete_user(self, username):
        try:
            self.auth.deleteUser(username)
            self.has_error = False
            return username
        except AuthenticationException, e:
            self.error_message = self.parse_error(e)

    def get_error(self):
        if self.has_error:
            return self.error_message
        else:
            return None

    def get_name(self):
        if self.current_user is not None:
            return self.current_user.realName()
        else:
            return "Guest";

    def get_plugins_auth(self):
        return self.auth.getPluginList()

    def get_plugins_roles(self):
        return self.role.getPluginList()

    def get_roles(self):
        my_roles = self.get_roles_list()
        length = len(my_roles)
        if length == 0:
            return ""
        elif length > 0:
            response = my_roles[0]

        if length > 1:
            for role in my_roles[1:]:
                response = response + ", " + role

        return response

    def get_roles_list(self):
        try:
            if self.current_user is not None:
                return self.role.getRoles(self.current_user.getUsername())
            else:
                return [this.GUEST_ROLE];
        except RolesException, e:
            self.error_message = self.parse_error(e)
        except AuthenticationException, e:
            self.error_message = self.parse_error(e)

    def get_user(self, username, source):
        try:
            self.active_auth_plugin = source
            self.auth.setActivePlugin(source)
            user = self.auth.getUser(username)
            self.has_error = False
            return user
        except AuthenticationException, e:
            self.error_message = self.parse_error(e)

    def is_admin(self):
        if self.current_user is not None:
            my_roles = self.get_roles_list()
            if "admin" in my_roles:
                return True
            else:
                return False
        else:
            return False

    def is_logged_in(self):
        if self.current_user is not None:
            return True
        else:
            return False

    def list_users(self, rolename):
        try:
            return self.role.getUsersInRole(rolename)
        except RolesException, e:
            self.error_message = self.parse_error(e)

    def login(self, username, password):
        try:
            self.current_user = self.auth.logIn(username, password)
            self.error_message = None
            sessionState.set("username", username)
            sessionState.set("source",   self.current_user.getSource())
            self.has_error = False
        except AuthenticationException, e:
            self.current_user  = None
            self.error_message = self.parse_error(e)

    def logout(self):
        if self.current_user is not None:
            try:
                self.auth.logOut(self.current_user)
                self.current_user  = None
                self.error_message = None
                sessionState.set("username", None)
                sessionState.set("source",   None)
                self.has_error = False
            except AuthenticationException, e:
                self.error_message = self.parse_error(e)

    # Strip out java package names from
    # error strings.
    def parse_error(self, error):
        self.has_error = True
        message = error.getMessage()
        i = message.find(":")
        if i != -1:
            return message[i+1:].strip()
        else:
            return message.strip()

    def remove_role(self, username, rolename):
        try:
            self.role.removeRole(username, rolename)
            self.has_error = False
        except RolesException, e:
            self.error_message = self.parse_error(e)

    def search_roles(self, query, source):
        try:
            self.active_role_plugin = source
            self.role.setActivePlugin(source)
            roles = self.role.searchRoles(query)
            self.has_error = False
            return roles
        except RolesException, e:
            self.error_message = self.parse_error(e)

    def search_users(self, query, source):
        try:
            self.active_auth_plugin = source
            self.auth.setActivePlugin(source)
            users = self.auth.searchUsers(query)
            self.has_error = False
            return users
        except AuthenticationException, e:
            self.error_message = self.parse_error(e)

    def session_init(self):
        # Debugging purpose
        if formData.get("verb") == "clear-session":
            sessionState.clear()

    def set_auth_plugin(self, plugin_id):
        try:
            self.active_auth_plugin = plugin_id
            self.auth.setActivePlugin(plugin_id)
            self.has_error = False
        except AuthenticationException, e:
            self.error_message = self.parse_error(e)

    def set_role(self, username, rolename):
        try:
            self.role.setRole(username, rolename)
            self.has_error = False
        except RolesException, e:
            self.error_message = self.parse_error(e)

    def set_role_plugin(self, plugin_id):
        try:
            self.active_role_plugin = plugin_id
            self.role.setActivePlugin(plugin_id)
            self.has_error = False
        except RolesException, e:
            self.error_message = self.parse_error(e)

