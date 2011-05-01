<script type="text/javascript">
$(function() {
    $("#txtLatitude, #txtLongitude").live("keyup", function() {
        var latitude = $("#txtLatitude").attr("value");
        var longitude = $("#txtLongitude").attr("value");
        if (latitude == undefined) latitude = 0;
        if (longitude == undefined) longitude = 0;
        
        geoHash = "http://www.geohash.org/" + encodeGeoHash(latitude, longitude);
        $("#location-tag-list").attr("rel", geoHash);
        $("#txtName").attr("value", "Lat: " + latitude + ", Long: " + longitude);
    });
});
</script>
