#if($self.mimeType.startsWith("audio/") || $self.mimeType.startsWith("video/"))
<script type="text/javascript">
    $(function() {
        // Audio/Video Tags annotation
        var videoAnoLabel = "<img src='$portalPath/images/icons/comment_add.png'/>&#160;Comment on this media:";
        videoConfig={
            debug: true,
            label: videoAnoLabel,
            //objectLiteral: "$oid",
            pageUri: "$oid",
            docRoot: ".video-results-list",
            tagList: ".video-result-list",
            uriAttr: "anotar-uri",
            replyLabel: "<img src='$portalPath/images/icons/comments_add.png'/>",
            //formPrepend: true,
            //stylePrefix: "anno-",
            creator: "$creator",
            creatorUri: "$creatorUri",
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
            serverAddress: "$portalPath/actions/anotar.ajax",
            disableReply: false,
            serverMode: "fascinator"
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
        
        videoTag = anotarFactory(jQuery, videoConfig);
    });
</script>
#end
