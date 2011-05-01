class UploadData:

    def __init__(self):
        self.writer = response.getPrintWriter("text/html; charset=UTF-8")

        listener = sessionState.get("upload_listener");

        if listener is not None:
            bytesRead = listener.getBytesRead();
            contentLength = listener.getContentLength();
            if bytesRead == contentLength:
                responseMsg = 100;
            else:
                responseMsg = round(100 * (bytesRead / contentLength));
            self.writer.println(responseMsg)
            self.writer.close()
        else:
            self.throw_error("No upload in progress")

    def throw_error(self, message):
        response.setStatus(500)
        self.writer.println("Error: " + message)
        self.writer.close()

scriptObject = UploadData()
