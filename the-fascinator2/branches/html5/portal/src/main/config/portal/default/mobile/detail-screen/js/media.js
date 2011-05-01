#if($self.mimeType.startsWith("image/"))
<script type="text/javascript">
$(function() {
    var oid = "$oid";
    var filenameNoExt = "$filenameNoExt";
    
    var mimeType = "$self.mimeType";
    var player = {};

    var src = "$portalPath/download/$oid/$self.getPreview()";
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
        
        /*
        var hasFlv = "$self.hasFlv()";
        if (hasFlv!='') {
            filename = hasFlv;
        }
        if (hasFlv!='' || ext=="mp3" || ext=="m4a") {
            var href = "$portalPath/download/$oid/" + filename;
            var player1 = '<div href="' + href + '" id="player" style="' + style + '"></div>'; 
            $("#player-container").attr("style", "width: 300px; height: " + height + "px").html(player1);
            player = flowplayer("player", {src:"$portalPath/flowplayer/flowplayer-3.1.5.swf", wmode:'opaque'},
                { clip: { autoPlay: false, autoBuffering: true },
                  play: {
                    label: "Play",
                    replayLabel: "Click to play again"
                  }
                });
        } */
        
        var hasOgg = "$self.hasOgg()";
        if (hasOgg!='') {
            var src = "$portalPath/download/$oid/" + filename;
            var oggSrc = "$portalPath/download/$oid/" + hasOgg;
            var player1 = '<video id="player" controls="true" autoplay="false"><source src="' + src + '" type="video/mp4" /><source src="' + oggSrc + '" type="video/ogg" /></video>'; 
            $("#player-container").attr("style", "width: 300px; height: " + height + "px").html(player1);
        
        
			if (!!document.createElement('video').play) {
					window.addEventListener('load',function() {
					
					//var video=$("#player");
					var video = document.getElementById('player');
					
					//video.playbackRate;  allow you to rewind/fastforward
					$("#player_play").live("click", function() {
						if (video.paused) {
							/* if video is currently paused, play it */
							video.play();
						} else {
							/* video isn't paused... */
							if (video.ended) {
								/* if we're at the end, reset currentTime and play */
								video.currentTime=0;
								video.play();
							} else {
								/* otherwise, just pause */
								video.pause();
							}
						}
					});
					
					$("#player_seek_start").live("click", function() {
						video.currentTime=0;
						video.play();
					});
					
					$("#player_rewind").live("click", function() {
						video.currentTime = video.currentTime - 1;
						video.play();
					});
					
					$("#player_forward").live("click", function() {
						video.currentTime = video.currentTime + 1;
						video.play();
					});
					
					$("#player_seek_end").live("click", function() {
						video.currentTime = video.duration - 1;
						video.pause();
					});
					
					$("#player_mark_start_time").live("click", function() {
						var start=video.currentTime;
						$("#txtStartMark").attr("value", ""+Math.round(start*100)/100);
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
						$("#txtEndMark").attr("value", "" + Math.round(video.currentTime*100)/100);
						start = $('#txtStartMark').val();
						end = $('#txtEndMark').val();
						processClip(start, end);
					});
					
					$("#txtEndMark").live("keyup", function() {
						start = $('#txtStartMark').val();
						end = $('#txtEndMark').val();
						processClip(start, end);
					});
					
					$(".player_clear_fragment").live("click", function() {
						$("#txtStartMark").attr("value", "");
						$("#txtEndMark").attr("value", "");
						$("#media_clip").attr("rel", "");
						//if (player.getClip()) {
						//    player.pause();
						//    player.getClip().update({duration:player.getClip().fullDuration});
						//}
					});
					
					$("#player_play_fragment").live("click", function() {
						video.pause();
						var startTime = $('#txtStartMark').val();
						var endTime = $('#txtEndMark').val();
						
						playDuration(startTime, endTime);
					});
					
					$(".player_play_clip").live("click", function() {
						video.pause();
						var startTime = $(this).siblings(".startTime").text().replace("s", "");
						var endTime = $(this).siblings(".endTime").text().replace("s", "");
						
						playDuration(startTime, endTime);
					});
					
					$(".player_reset").click(function() {
						//video.removeEventListener('timeupdate', playDuration, false);
						video.load();
						
						//NOTE: The video is not reset!
						
						//use load for now
						/*video.currentTime = 0;
						video.addEventListener("timeupdate", function() {
							if (video.currentTime >= video.duration) {
							video.play();
						}}, false);*/
					});
					
					
					function playDuration(startTime, endTime) {
						video.currentTime = startTime;
						video.play();
						video.addEventListener("timeupdate", function() {
							if (video.currentTime >= endTime) {
							video.pause();
						}}, false);
						
						$(".player_reset").removeAttr("disabled");
					}

					function processClip(start, end) {
						//uri = player.getClip().url + "#";
						uri = src + "#";
						if (start != "" && end != "") {
							//Use Normal Play Time as described in http://www.ietf.org/rfc/rfc2326.txt
							uri += "t=npt:";
							if (start != "" && start > 0) {
								uri += start + "s"
								if (video.duration < start) {
									//warning += "<li>The requested start-point is greater than the video's duration.</li>";
								}
							}
							if (end != "") {
								uri += "," + end + "s";
								if (video.duration < end) {
									//warning += "<li>The requested end-point is greater than the video's duration.</li>";
								}
							}
						}
						$(".video-results-list").attr("rel", uri);
					}
					
				}, true);
			}
        }
        
        
        /*
        $("#player_seek_start").live("click", function() {
            //player.seek(0);
        });
        $("#player_rewind").live("click", function() {
            //player.seek(player.getTime() - 1);
        });
        $("#player_play").live("click", function() {
            player_play
            //player.toggle();
        });
        $("#player_forward").live("click", function() {
            //player.seek(player.getTime()+1);
        });
        $("#player_seek_end").live("click", function() {
            //player.seek(player.getClip().fullDuration-1);
            //player.pause();
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
            $(".player_reset").removeAttr("disabled");
        });
        
        $(".player_reset").click(function() {
            if (player.getClip()) {
                player.pause();
                player.getClip().update({duration:player.getClip().fullDuration});
                player.seek(0);
                $(this).attr("disabled", "disabled");
            }
        }); */
});
</script>
#end
