import time
from au.edu.usq.fascinator.indexer.rules import AddField, New
from org.dom4j.io import SAXReader

from au.edu.usq.fascinator.common.nco import Contact
from au.edu.usq.fascinator.common.nie import InformationElement

#
# Available objects:
#    indexer   : Indexer instance
#    rules     : RuleManager instance
#    object    : DigitalObject to index
#    payloadId : Payload identifier
#    storageId : Storage layer identifier
#

def indexing(names, values):
    for value in values:
        for name in names:
            rules.add(AddField(name, value))

rules.add(New())


if isMetadata:
    solrId = object.getId();
    itemType = "object"
else:
    solrId = object.getId() + "/" + payloadId
    itemType = "datastream"
    rules.add(AddField("identifier", payloadId))


rules.add(AddField("id", solrId))
rules.add(AddField("last_modified", time.strftime("%Y-%m-%dT%H:%M:%SZ")))
rules.add(AddField("storageId", storageId))
rules.add(AddField("item_type", itemType))
rules.add(AddField("group_access", "guest"))
rules.add(AddField("item_class", "document"))
rules.add(AddField("repository_name", params["repository.name"]))
rules.add(AddField("repository_type", params["repository.type"]))

titleList = []
descriptionList = []
creatorList = []
creationDate = []
contributorList = [] 
formatList = []
fulltext = []
subjectList = []
relationDict = {}

rdfPayload = object.getMetadata();

if rdfPayload is not None:
    rdfModel = indexer.getRdfModel(rdfPayload)
    
    #Set write to False so it won't write to the model
    informationElement = InformationElement(rdfModel, object.getId().replace("/content",""), False)
    
    #1. get title 
    allTitles = informationElement.getAllTitle();
    while (allTitles.hasNext()):
        title = allTitles.next()
        titleList.append(title)
        print "**********************" ,title

    print(rdfModel);
    
    #2. get creator 
    #allCreators = informationElement.getAllCreator();
    #while (allCreators.hasNext()):
#        thing = allCreators.next()
#        contacts = Contact(rdfModel, thing.getResource(), False)
#        allFullnames = contacts.getAllFullname()
#        while (allFullnames.hasNext()):
#             creatorList.append(allFullnames.next())
    
    #3. get FullText
    if informationElement.hasPlainTextContent():
        allPlainTextContents = informationElement.getAllPlainTextContent()
        print(allPlainTextContents);
        while(allPlainTextContents.hasNext()):
            fulltextString = allPlainTextContents.next()
            fulltext.append(fulltextString)
            
    #4. get description
    if informationElement.hasDescription():
        allDescription = informationElement.getAllDescription()
        while(allDescription.hasNext()):
            description = allDescription.next();
            descriptionList.append(description)

    
    
    #5. mimeType: only aperture has this information
    #if informationElement.hasMimeType():
    #    allMimeTypes = informationElement.getAllMimeType()
    #    while(allMimeTypes.hasNext()):
    #        formatList.append(allMimeTypes.next())
    
    #6. contentCreated
    #if creationDate == []:
    #    if informationElement.hasContentCreated():
    #        creationDate.append(informationElement.getContentCreated().getTime().toString())
    
    #7. Keywords
#    if informationElement.hasKeyword():
#        keywords = informationElement.getAllKeyword()
#        while(keywords.hasNext()):
#            subjectList.append(keywords.next())
    
    
#Start Indexing....
indexing(["dc_title"], titleList)
#indexing(["dc_creator"], creatorList)  #no dc_author in schema.xml, need to check 

#for key in relationDict:
#    indexing([key], relationDict[key])

indexing(["dc_description"], descriptionList)
indexing(["dc_format"], ["text/xml"])
indexing(["full_text"], fulltext)
#indexing(["dc_date"], creationDate)
indexing(["dc_subject"], subjectList)

   













