#if($self.hasHtml() || $isPackage)
<script type="text/javascript">
  $(function() {
      $("#blog-this, #blog-cancel").click(function() {
          $("#blog-this").toggleClass("selected");
          $("#blog-message").empty();
          $("#blog-form").toggle("blind");
          return false;
      });
      $("#blog-accept").click(function() {
          $("#blog-message").empty();
          $("#blog-loading").show();
          jQuery.post("$portalPath/actions/blog.ajax",
              {
                  url: $("#blog_url").val(),
                  title: $("#blog_title").val(),
                  username: $("#blog_username").val(),
                  password: $("#blog_password").val(),
                  oid: "$oid"
              },
              function(data, status) {
                  $("#blog-loading").hide();
                  $("#blog-message").html(data);
              }
          );
      });
      $("#blog_url").autocomplete("$portalPath/actions/blog.ajax?func=url-history");
  });
</script>
#end
