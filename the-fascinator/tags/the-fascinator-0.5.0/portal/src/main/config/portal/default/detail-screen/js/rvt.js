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
    rvt.contentLoadedCallback = function() {
        var oid = window.location.hash.substring(1);
        fixLinks("", "#package-content a", "href", oid);
        fixLinks("", "#package-content img", "src", oid);
        $(".article-heading").html($(window.location.hash).children("a").text());
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
                    window.location.hash = $(node).attr("rel");
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
