from __main__ import Services, formData, portalId, response

import os.path

from au.edu.usq.fascinator.ims import FileType, ItemType, ManifestType, \
    MetadataType, ObjectFactory, OrganizationType, OrganizationsType, \
    ResourceType, ResourcesType

from java.io import FileOutputStream, StringWriter
from java.util.zip import ZipEntry, ZipOutputStream
from javax.xml.bind import JAXBContext, Marshaller

from org.apache.commons.io import IOUtils

class ImsPackage:
    def __init__(self, outputFile=None):
        self.__portal = Services.getPortalManager().get(portalId)
        self.__portalManifest = self.__portal.getJsonMap("manifest")
        print formData
        url = formData.get("url")
        if outputFile is None and url is None:
            self.__createPackage()
        elif url is not None and outputFile is not None:
            self.__createPackage(outputFile)
    
    def __createPackage(self, outputFile=None):
        manifest = self.__createManifest()
        context = JAXBContext.newInstance("au.edu.usq.fascinator.ims")
        m = context.createMarshaller()
        m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, True)
        writer = StringWriter()
        jaxbElem = ObjectFactory.createManifest(ObjectFactory(), manifest)
        m.marshal(jaxbElem, writer);
        writer.close()
        
        if outputFile is not None:
            print " * imscp.py: writing to %s..." % outputFile
            out = FileOutputStream(outputFile)
        else:
            print " * imscp.py: writing to http output stream..."
            response.setHeader("Content-Disposition", "attachment; filename=%s.zip" % self.__portal.getName())
            out = response.getOutputStream("application/zip")
        
        zipOut = ZipOutputStream(out)
        
        zipOut.putNextEntry(ZipEntry("imsmanifest.xml"))
        IOUtils.write(writer.toString(), zipOut)
        zipOut.closeEntry()
        
        for key in self.__portalManifest.keySet():
            item = self.__portalManifest.get(key)
            oid = item.get("id")
            obj = Services.getStorage().getObject(oid)
            for payload in obj.getPayloadList():
                pid = payload.getId()
                if pid != "SOF-META":
                    zipOut.putNextEntry(ZipEntry("%s/%s" % (key[5:], pid)))
                    IOUtils.copy(payload.getInputStream(), zipOut)
                    zipOut.closeEntry()
        
        zipOut.close()
        out.close()
    
    def __createManifest(self):
        manifest = ManifestType()
        meta = MetadataType()
        meta.setSchema("IMS Content")
        meta.setSchemaversion("1.1.4")
        manifest.setMetadata(meta)
        
        portal = Services.getPortalManager().get(portalId)
        portalManifest = portal.getJsonMap("manifest")
        
        orgs = OrganizationsType()
        org = OrganizationType()
        org.setIdentifier("default")
        org.setTitle(portal.getDescription())
        orgs.getOrganization().add(org)
        orgs.setDefault(org)
        manifest.setOrganizations(orgs)
        
        resources = ResourcesType()
        manifest.setResources(resources)
        for key in portalManifest.keySet():
            jsonRes = portalManifest.get(key)
            # item
            visible = jsonRes.get("hidden", "false") != "true"
            item = ItemType()
            item.setIdentifier("default-" + key)
            item.setIdentifierref(key)
            item.setIsvisible(visible)
            item.setTitle(jsonRes.get("title"))
            # webcontent
            webRes = ResourceType()
            webRes.setIdentifier(key)
            webRes.setType("webcontent")
            oid = jsonRes.get("id")
            _, filename = os.path.split(oid)
            baseName = os.path.splitext(filename)[0]
            webRes.setHref("%s/%s.htm" % (key[5:], baseName))
            obj = Services.getStorage().getObject(oid)
            for payload in obj.getPayloadList():
                pid = payload.getId()
                if pid != "SOF-META":
                    file = FileType()
                    file.setHref(pid)
                    webRes.getFile().add(file)
            
            org.getItem().add(item)
            resources.getResource().add(webRes)
        
        return manifest
    
    def __getPortalManifest(self):
        return self.__getPortal().getJsonMap("manifest")
    
    def __getPortal(self):
        return Services.portalManager.get(portalId)

if __name__ == "__main__":
    scriptObject = ImsPackage()
