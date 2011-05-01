#if($self.mimeType.startsWith("audio/") || $self.mimeType.startsWith("video/"))
<script type="text/javascript">
$(function() {
    // Audio/Video Tags annotation
    var videoAnoLabel = "<img src='$portalPath/images/icons/comment_add.png' title='Add new comment' />&#160;Comment on this media:";
    videoConfig = {
        label: videoAnoLabel,
        pageUri: "$oid",
        docRoot: ".video-results-list",
        tagList: ".video-result-list",
        uriAttr: "anotar-uri",
        replyLabel: "<img src='$portalPath/images/icons/comments_add.png' title='Reply to this comment' />",
        #if("$!creator" != "")creator: "$creator",#end
        #if("$!creatorUri" != "")creatorUri: "$creatorUri",#end
        interfaceLabel: videoAnoLabel,
        interfaceVisible: true,
        formCustom: "media_clip_form",
        formCancel: ".myTag-cancel",
        formSubmit: ".myTag-submit",
        displayCustom: "media_clip_comment", 
        hashType: "http://www.w3.org/TR/2009/WD-media-frags-20091217",
        hashFunction: function(node) {
            var rel = node.attr("rel");
            if (rel == null) { rel = "$oid"; }
            return rel;
        },
        onFormDisplay: function(node){
            $("#annotate_all").click();
        },
        disableReply: false,
        loadAnnotations: function() { loadAnnotations(this, "$oid") },
        submitAnnotation: function(data, annotateDiv) { submitAnnotation(this, "$oid", data, annotateDiv) }
    }
    
    $("#media_clip textArea").live("focus", function() {
        var replies = $("#media_clip").prev("blockquote.anno-inline-annotation-quote")
                                      .find(".anno-anno-children");
        if (replies.length > 0) {
            $(".annotate_scope").hide();
        } else {
            $(".annotate_scope").show();
        }
        return false;
    });
    
    anotarFactory(jQuery, videoConfig);
});
</script>
#end
