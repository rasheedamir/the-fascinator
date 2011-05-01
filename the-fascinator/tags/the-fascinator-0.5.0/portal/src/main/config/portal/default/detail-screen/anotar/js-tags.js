<script type="text/javascript">
    $(function() {
        var jQ = jQuery;
        // Tags annotation
        tagConfig = {
            //debug: true,
            docRoot: "#object-tag-list",
            tagList: "div",
            creator: "$creator",
            creatorUri: "$creatorUri",
            contentInput: "#object-tag-input",
            pageUri: "$oid",
            uriAttr: "rel",
            outputInChild: ".object-tags",
            annotationType: "tag",
            stylePrefix: "tags-",
            interfaceLabel: " <img src='$portalPath/images/icons/add.png'/>",
            formPrepend: true,
            interfaceVisible: true,
            formCustom: "object_tag_form",
            formCancel: ".myTag-cancel",
            formSubmit: ".myTag-submit",
            serverAddress: "$portalPath/actions/anotar.ajax",
            getAnnotation: null,
            disableReply: true,
            serverMode: "fascinator"
        }
        tag = anotarFactory(jQ, tagConfig);
    });
</script>
        