class UploadData:

    def __init__(self):
        self.roles = page.authentication.get_roles_list()
        self.uploader = toolkit.getFileUploader(self.roles)

    def render_upload_form(self):
        return self.uploader.renderForm()

scriptObject = UploadData()
