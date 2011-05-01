<script type="text/javascript">
  var display_help = false;
  function toggleHelp(help_link) {
      if (!display_help) {
          $(".help_p").css("display", "block");
          help_link.text("Hide help");
          display_help = true;
      } else {
          $(".help_p").css("display", "none");
          help_link.html("Show help");
          display_help = false;
      }
  }
</script>