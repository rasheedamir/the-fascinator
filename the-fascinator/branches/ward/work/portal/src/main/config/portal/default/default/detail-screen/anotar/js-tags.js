<script type="text/javascript">
function setupAnotarTags() {
    var hash = location.hash.substring(1);
    #set($dq = '"')
    #if($isPackage)
        #set($annoUri = "$dq$oid#$dq+hash")
        #set($relUri = "hash")
    #else
        #set($annoUri = "$dq$oid$dq")
        #set($relUri = $annoUri)
    #end
    if (hash != "blank") {
        var tagConfig = {
            docRoot: "div[rel='" + $relUri + "-tags']",
            tagList: ".object-tag-list:not([anotar-hash])",
            #if("$!creator" != "")creator: "$creator",#end
            #if("$!creatorUri" != "")creatorUri: "$creatorUri",#end
            contentInput: "#object-tag-input",
            pageUri: $annoUri,
            uriAttr: "rel",
            outputInChild: ".object-tags",
            annotationType: "tag",
            stylePrefix: "tags-",
            interfaceLabel: " <img src='$portalPath/images/icons/add.png' title='Add tag' />",
            displayCustom: "object_tag_display",
            formPrepend: true,
            interfaceVisible: true,
            formCustom: "object_tag_form",
            formCancel: ".myTag-cancel",
            formSubmit: ".myTag-submit",
            disableReply: true,
            loadAnnotations: function() { loadAnnotations(this, $annoUri) },
            submitAnnotation: function(data, annotateDiv) { submitAnnotation(this, $annoUri, data, annotateDiv) }
        }
        anotarFactory(jQuery, tagConfig);
    }
}
#if(!$isPackage)$(function() { setupAnotarTags(); });#end
</script>
