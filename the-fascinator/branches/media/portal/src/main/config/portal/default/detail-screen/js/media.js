#if($self.mimeType.startsWith("image/"))
<script type="text/javascript">
$(function() {
    var oid = "$oid";
    var filenameNoExt = "$filenameNoExt";
    
    var mimeType = "$self.mimeType";
    var player = {};

    var src = "$portalPath/download/$oid/$self.getPreview('$portalPath/download/$oid')";
    $("#content").html(
        '<div id="$oid" class="annotatable"><div class="image">' +
          '<img class="image" id="image-content" src="' + src + '" style="max-width: 100%" />' +
        '</div><div class="clear"></div></div>');
});
</script>

#elseif($self.mimeType.startsWith("audio/") || $self.mimeType.startsWith("video/"))
<script type="text/javascript">
$(function() {
    var oid = "$oid";
    var filenameNoExt = "$filenameNoExt";
    
    var mimeType = "$self.mimeType";
    var player = {};

        var height = 300;
        if (mimeType.indexOf("audio/") == 0) {
            height = 24;
        }
        var style = "display: block; width: 425px; height: " + height + "px";
        //var filename = oid.substring(oid.lastIndexOf("/") + 1);
        var filename = "$sourceId";
        filename = filename.substring(filename.lastIndexOf("/") + 1);
        var ext = filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
        
        var hasFlv = "$self.hasFlv()";
        if (hasFlv!='') {
            filename = hasFlv;
        }
        if (hasFlv!='' || ext=="mp3" || ext=="m4a") {
            var href = "$portalPath/download/$oid/" + filename;
            var player1 = '<div href="' + href + '" id="player" style="' + style + '"></div>'; 
            $("#player-container").attr("style", "width: 300px; height: " + height + "px").html(player1);
            player = flowplayer("player", {src:"$portalPath/flowplayer/flowplayer-3.1.5.swf", wmode:'opaque'},
                { clip: { autoPlay: true, autoBuffering: true },
                  play: {
                    label: "Play",
                    replayLabel: "Click to play again"
                  }
                });
        }
        
        $("#player_seek_start").live("click", function() {
            player.seek(0);
        });
        $("#player_rewind").live("click", function() {
            player.seek(player.getTime() - 1);
        });
        $("#player_play").live("click", function() {
            player.toggle();
        });
        $("#player_forward").live("click", function() {
            player.seek(player.getTime()+1);
        });
        $("#player_seek_end").live("click", function() {
            player.seek(player.getClip().fullDuration-1);
            player.pause();
        });
        
        $("#player_mark_start_time").live("click", function() {
            $("#txtStartMark").attr("value", player.getTime());
            $("#player_mark_end_time").attr("disabled", false);
            $("#txtEndMark").attr("disabled", false);
        });
        
        $("#txtStartMark").live("keyup", function() {
            txtStartMarkVal = $('#txtStartMark').val();
            if (txtStartMarkVal != "") {
                $("#player_mark_end_time").attr("disabled", false);
                $("#txtEndMark").attr("disabled", false);
            } else {
                $("#player_mark_end_time").attr("disabled", true);
                $("#txtEndMark").attr("disabled", true);
            }
        });
        
        $("#player_mark_end_time").live("click", function() {
            $("#txtEndMark").attr("value", player.getTime());
            start = $('#txtStartMark').val();
            end = $('#txtEndMark').val();
            processClip(start, end);
        });
        
        $("#txtEndMark").live("keyup", function() {
            start = $('#txtStartMark').val();
            end = $('#txtEndMark').val();
            processClip(start, end);
        });
        
        function processClip(start, end) {
            uri = player.getClip().url + "#";
            if (start != "" && end != "") {
                //Use Normal Play Time as described in http://www.ietf.org/rfc/rfc2326.txt
                uri += "t=npt:";
                if (start != "" && start > 0) {
                    uri += start + "s"
                    if (player.getClip().duration < start) {
                        //warning += "<li>The requested start-point is greater than the video's duration.</li>";
                    }
                }
                if (end != "") {
                    uri += "," + end + "s";
                    if (player.getClip().duration < end) {
                        //warning += "<li>The requested end-point is greater than the video's duration.</li>";
                    }
                }
            }
            $(".video-results-list").attr("rel", uri);
        }
        
        $(".player_clear_fragment").live("click", function() {
            $("#txtStartMark").attr("value", "");
            $("#txtEndMark").attr("value", "");
            $("#media_clip").attr("rel", "");
            if (player.getClip()) {
                player.pause();
                player.getClip().update({duration:player.getClip().fullDuration});
            }
        });
        
        $("#player_play_fragment").live("click", function() {
            player.pause();
            player.seek($('#txtStartMark').val());
            player.getClip().update({duration:$('#txtEndMark').val()});
            player.play();
        });
        
        $(".player_play_clip").live("click", function() {
            player.pause();
            var startTime = $(this).siblings(".startTime").text().replace("s", "");
            var endTime = $(this).siblings(".endTime").text().replace("s", "");
            player.seek(startTime);
            player.getClip().update({duration:endTime});
            player.play();
        });
});
</script>
#end
