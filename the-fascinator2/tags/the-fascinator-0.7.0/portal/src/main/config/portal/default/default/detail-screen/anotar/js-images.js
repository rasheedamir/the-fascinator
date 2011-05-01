<script type="text/javascript" src="$portalPath/js/anotar/image.annotate.js"></script>
<script type="text/javascript">
function setupImageTags(selector) {
    var hash = location.hash.substring(1);
    #set($dq = '"')
    #if($isPackage)
        #set($rootUri = "$dq$oid%23$dq+hash")
    #else
        #set($rootUri = "$dq$oid$dq")
    #end
    if (hash != "blank") {
        var baseUrl = "$portalPath/actions/anotar.ajax?rootUri=" + $rootUri + "&action=";
        $(selector).annotateImage({
            editable: true,
            saveUrl: baseUrl + "save-image&creator=$!creator&creatorUri=$!creatorUri",
            getUrl: baseUrl + "get-image",
            deleteUrl: baseUrl + "delete-image",
            deletable: $page.authentication.is_admin().toString().toLowerCase()
        });
    }
}
#if(!$isPackage)$(window).load(function() { setupImageTags("#image-content"); });#end
</script>
