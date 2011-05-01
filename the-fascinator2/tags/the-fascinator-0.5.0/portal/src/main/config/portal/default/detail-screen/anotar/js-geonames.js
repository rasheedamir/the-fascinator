<script type="text/javascript">
    $(function() {
        // Location Tags annotation
        locationConfig={
            //debug: true,
            docRoot: "#location-tag-list",
            tagList: "div",
            creator: "$creator",
            creatorUri: "$creatorUri",
            contentInput: "#txtName",
            pageUri: "$oid",
            uriAttr: "rel",
            outputInChild: ".location-tags",
            annotationType: "tag",
            stylePrefix: "tags-",
            interfaceLabel: " <img src='$portalPath/images/icons/add.png'/>",
            //formPrepend: true,
            interfaceVisible: true,
            formCustom: "location_form",
            formCancel: ".locTag-cancel",
            formSubmit: ".locTag-submit",
            displayCustom: "location_display",
            formPopup: true,
            formPopupSettings: {
                width: 500,
                title: "Location Tag"
            }, 
            onFormDisplay: function(node){
                $("#byName").click();
                $(node).autocomplete('$portalPath/actions/geonames.ajax', {
                    extraParams: {"func":"placeName"}, 
                    formatItem: function(row) {
                        return row[0];
                    }
                }).result(function(event, item) {
                    if (item) {
                        $("#location-tag-list").attr("rel", "http://www.geonames.org/" + item[1]);
                    }
                });
            },
            getContentUri: function(node) {
                var rel = node.attr("rel");
                if (rel == null) {
                    // content is not a URI
                    return { isUri: false };
                }
                
                return { isUri: true, contentUri: rel };
            },
            serverAddress: "$portalPath/actions/anotar.ajax",
            disableReply: true,
            serverMode: "fascinator"
        }
        /// Use formCloseFunction to overwrite close function
        locationTag = anotarFactory(jQuery, locationConfig);
    });

    function attach(node) {
        if (!$(node).attr("attach")) {
            $(node).attr("attach", "true");
            $(node).autocomplete('$portalPath/actions/geonames.ajax', {
                extraParams: {"func":"placeName"}, 
                formatItem: function(row) {
                    return row[0];
                }
            }).result(function(event, item) {
                if (item) {
                    $("#location-tag-list").attr("rel", item[1]);
                }
            });
        }
    }

</script>
