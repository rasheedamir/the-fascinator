<script type="text/javascript">
#set($dq = '"')
function setupAnotarLocationTags() {
    var hash = location.hash.substring(1);
    #if($isPackage)
        #set($annoUri = "$dq$oid#$dq+hash")
        #set($relUri = "hash")
    #else
        #set($annoUri = "$dq$oid$dq")
        #set($relUri = $annoUri)
    #end
    if (hash != "blank") {
        var locationConfig = {
            docRoot: "div[rel='" + $relUri + "-tags']",
            tagList: ".location-tag-list:not([anotar-hash])",
            #if("$!creator" != "")creator: "$creator",#end
            #if("$!creatorUri" != "")creatorUri: "$creatorUri",#end
            contentInput: "#txtName",
            pageUri: $annoUri,
            uriAttr: "rel",
            outputInChild: ".location-tags",
            annotationType: "tag",
            stylePrefix: "tags-",
            interfaceLabel: " <img src='$portalPath/images/icons/add.png' title='Add location' />",
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
                        $("div[rel='" + $relUri + "-tags'] > .location-tag-list")
                            .attr("rel", "http://www.geonames.org/" + item[1]);
                    }
                });
            },
            getContentUri: function(node) {
                var rel = node.find(".location-tag-list").attr("rel");
                if (rel == null) {
                    // content is not a URI
                    return { isUri: false };
                }
                
                return { isUri: true, contentUri: rel };
            },
            disableReply: true,
            loadAnnotations: function() { loadAnnotations(this, $annoUri) },
            submitAnnotation: function(data, annotateDiv) { submitAnnotation(this, $annoUri, data, annotateDiv) }
        }
        anotarFactory(jQuery, locationConfig);
    }
}

#if(!$isPackage)$(function() { setupAnotarLocationTags(); });#end

function attach(node) {
    if (!$(node).attr("attach")) {
        $(node).attr("attach", "true");
        $(node).autocomplete('$portalPath/actions/geonames.ajax', {
            extraParams: {"func":"placeName"}, 
            formatItem: function(row) {
                return row[0];
            }
        }).result(function(event, item) {
            var hash = location.hash.substring(1);
            #if($isPackage)
                #set($relUri = "hash")
            #else
                #set($relUri = "$dq$oid$dq")
            #end
            if (item) {
                $("div[rel='" + $relUri + "-tags'] > .location-tag-list").attr("rel", item[1]);
            }
        });
    }
}
</script>
