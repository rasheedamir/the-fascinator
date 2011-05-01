<script type="text/javascript">
$(function() {
    /* // standard menu navigation
    $("#package-toc li a").click(function() {
        var item = $(this);
        var node = item.parent("li:first");
        var id = node.attr("rel");
        $("#package-toc li a.selected").removeClass("selected");
        $("#content-loading").show();
        $("#package-content").load(
            "$portalPath/preview?oid=" + escape(id) + " div.body > div",
            function() {
                item.addClass("selected");
                $(".article-heading").html(item.text());
                $("#content-loading").hide();
            }
        );
    });
    */
    
    // RVT
    var rvt = rvtFactory(jQuery);
    rvt.tocSelector = "#package-toc";
    rvt.contentSelector = "#package-content";
    rvt.fixRelativeLinks = false;
    #if($isPackage)
      rvt.contentBaseUrl = "$portalPath/preview?oid=";
    #elseif($isImsPackage)
      rvt.contentBaseUrl = "$portalPath/download/$oid/";
    #end
    rvt.contentLoadedCallback = function(rvt) {
        var oid = window.location.hash.substring(1);
        fixLinks("", "#package-content a", "href", oid);
        fixLinks("", "#package-content img", "src", oid);
        $(".article-heading").html($(window.location.hash).children("a").text());
        
        if (oid == "blank") {
            $("#object-tag-list, #location-tag-list, .annotatable").hide();
        } else {
            var docCommentNode = $("#package-content > div:visible > .annotatable");
            if (docCommentNode.length == 0) {
                $("#package-content > div:visible")
                    .append('<div class="annotatable"><span class="hidden">' + oid + '</span>Comment on this item:</div>');
            }
            
            var tagNode = $("div[rel='" + oid + "-tags']");
            if (tagNode.length == 0) {
                var tagTemplate = '<div rel="' + oid + '-tags">' +
                    '<div class="object-tag-list">Tags: <span class="object-tags"></span></div>' +
                    '<div class="location-tag-list">Location: <span class="location-tags"></span></div>' +
                  '</div>';
                $("#package-content > div:visible > :first").before(tagTemplate);
            }
            
            $("#object-tag-list, #location-tag-list, .annotatable").show();
        }
        
        setupAnotarComments();
        setupAnotarTags();
        setupAnotarLocationTags();
        
        // image tags not working yet
        //var imageNode = $("div[rel='" + oid + "'] > img");
        //imageNode.load(function(){alert("loaded " + src);setupImageTags("div[rel='" + oid + "']");});
    };
    rvt.displayTOC = function(nodes) {
        function fixIds(nodes) {
            jQuery.each(nodes, function(count, node) {
                node.attributes.rel = "#" + node.attributes.id;
                if (node.children) {
                    fixIds(node.children);
                }
            });
        }
        fixIds(nodes);
        var opts = {
            data: {
                type: "json",
                opts: { static: nodes }
            },
            ui: { dots: false },
            types: {
                "default": { draggable: false }
            },
            callback: {
                onselect: function(node, tree) {
                    var rel = $(node).attr("rel");
                    if (rel == "#blank") {
                        $("#object-tag-list, #location-tag-list, .annotatable").hide();
                    } else {
                        $("#object-tag-list, #location-tag-list, .annotatable").show();
                    }
                    window.location.hash = rel;
                    $(".article-heading").html($(node).children("a").text());
                    return false;
                }
            }
        }
        $(rvt.tocSelector).tree(opts);
    }
    #if($isPackage)
      rvt.getManifestJson("$portalPath/workflows/organiser.ajax?func=get-rvt-manifest&oid=$oid");
    #elseif($isImsPackage)
      rvt.getManifestJson("$portalPath/actions/json_ims.ajax?oid=$oid");
    #end
});
</script>
