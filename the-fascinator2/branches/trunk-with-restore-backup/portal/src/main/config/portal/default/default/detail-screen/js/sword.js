
#if($isPackage)
<script type="text/javascript">
$(function() {
    $("#sword-view, #sword-cancel").click(function() {
        $("#sword-view").toggleClass("selected");
        $("#sword-collections").hide();
        $("#sword_collections").empty();
        $("#sword-accept").attr("disabled", "disabled");
        $("#sword-message").empty();
        $("#sword-form").toggle("blind");
        return false;
    });
    $("#sword-start").click(function() {
        $("#sword-message").empty();
        $("#sword-loading").show();
        jQuery.post("' + $portalPath + '/actions/sword.ajax?func=collections",
            {
                url: $("#sword_url").val(),
                username: $("#sword_username").val(),
                password: $("#sword_password").val()
            },
            function(data, status) {
                $("#sword-loading").hide();
                $("#sword_collections").empty();
                if (data.error) {
                    $("#sword-message").html(data.error);
                } else {
                    jQuery.each(data.collections, function(i, val) {
                        $("#sword_collections")
                            .append('<option value="' + val.location + '>' + val.title + '</option>');
                        $("#sword-collections").show();
                        $("#sword-accept").removeAttr("disabled");
                    });
                }
                $("#sword-collections-form").show();
            },
            "json"
        );
    });
    $("#sword-accept").click(function() {
        $("#sword-message").empty();
        $("#sword-loading").show();
        $("#sword-start, #sword-accept").attr("disabled", "disabled");
        jQuery.post("$portalPath/actions/sword.ajax?func=post&oid=$oid",
            {
                url: $("#sword_collections").val(),
                username: $("#sword_username").val(),
                password: $("#sword_password").val()
            },
            function(data, status) {
                $("#sword-loading").hide();
                $("#sword-message").html("<p>Successful</p>");
                $("#sword-start, #sword-accept").removeAttr("disabled");
            }
        );
    });
});
</script>
#end