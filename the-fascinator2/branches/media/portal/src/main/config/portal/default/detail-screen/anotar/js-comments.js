#if(!$self.mimeType.startsWith("audio/") && !$self.mimeType.startsWith("video/"))
<script type="text/javascript">
    $(function() {
        noteConfig={
            pageUri: "$oid",
            creator: "$creator",
            creatorUri: "$creatorUri",
            tagList: "p, h1, h2, h3, h4, h5, h6, .annotatable",
            interfaceLabel: " <img src='$portalPath/images/icons/comment_add.png'/> Comments:",
            replyLabel: " <img src='$portalPath/images/icons/comments_add.png'/>",
            serverAddress: "$portalPath/actions/anotar.ajax",
            serverMode: "fascinator",
            #if($self.mimeType.startsWith("image/"))
                interfaceVisible: true
            #else
                interfaceVisible: false
            #end
        }
        noteTag = anotarFactory(jQuery, noteConfig);
    });
</script>
#end
