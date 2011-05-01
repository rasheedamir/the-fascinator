#if(!$self.mimeType.startsWith("audio/") && !$self.mimeType.startsWith("video/"))
<script type="text/javascript">
function setupAnotarComments() {
    var hash = location.hash.substring(1);
    #set($dq = '"')
    #if($isPackage)
        #set($annoUri = "$dq$oid#$dq+hash")
    #else
        #set($annoUri = "$dq$oid$dq")
    #end
    if (hash != "blank") {
        var commentConfig = {
            pageUri: $annoUri,
            #if("$!creator" != "")creator: "$creator",#end
            #if("$!creatorUri" != "")creatorUri: "$creatorUri",#end
            #if($isPackage)
                tagList: "p:not([anotar-hash]), " +
                         "h1:not([anotar-hash]), " +
                         "h2:not([anotar-hash]), " +
                         "h3:not([anotar-hash]), " +
                         "h4:not([anotar-hash]), " +
                         "h5:not([anotar-hash]), " +
                         "h6:not([anotar-hash]), " +
                         ".annotatable:not([anotar-hash])",
            #else
                tagList: "p, h1, h2, h3, h4, h5, h6, .annotatable",
            #end
            interfaceLabel: " <img src='$portalPath/images/icons/comment_add.png' title='Add new comment' /> Comments:",
            replyLabel: " <img src='$portalPath/images/icons/comments_add.png' title='Reply to this comment' />",
            interfaceVisible: $self.mimeType.startsWith("image/").toString().toLowerCase(),
            displayCustom: "object_comment_display",
            loadAnnotations: function() { loadAnnotations(this, $annoUri) },
            submitAnnotation: function(data, annotateDiv) { submitAnnotation(this, $annoUri, data, annotateDiv) }
        }
        anotarFactory(jQuery, commentConfig);
    }
}
#if(!$isPackage)$(function() { setupAnotarComments(); });#end
</script>
#end
