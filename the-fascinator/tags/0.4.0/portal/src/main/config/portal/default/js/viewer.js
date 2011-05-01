// IMS Viewer

$(function(){
    var jQ=jQuery;
    var myUrl = window.location.href.split("#")[0];
    var url = myUrl.replace("/detail/", "/jsonIms/");
    function getDocument(id){
        var url = myUrl.replace("/detail/", "/download/");
        url += "/" + id;
        jQ(".content-preview-inline").empty().append("<div><a name='"+id+"'>loading..</a></div>");
        //jQ.get(url, {}, previewDataCallback, "text");
        jQ(".content-preview-inline").load(url + " .body", function() {
            gFixLinks(url, ".content-preview-inline a", "href");
            gFixLinks(url, ".content-preview-inline img", "src");
        });
        window.location.href = myUrl + "#" + id;
    }
    function createTree(nodes) {
        opts = { data: {type:"json", opts:{"static":nodes}},
                ui: {dots:false},
                types: {"default":{draggable:false}},
                rules: {},
                lang: {},
                callback: {onselect:
                        function(node, t){
                            var id=jQ(node).attr("id");
                            getDocument(id);
                        }
                }
            }
        var d = jQ("<div class='box'><h2>TOC</h2><div class='block'></div></div>");
        jQ("#metadata").before(d);
        d.find("div").tree(opts);
    }
    function callback(data){
        if(data.title){
            //alert(data.title);
            createTree(data.nodes)
            jQ(".content-preview-inline").append("<div/>");
            var id = window.location.href.split("#")[1];
            if(id){
                getDocument(id);
            }
        }
    }
    jQ.get(url, {}, callback, "json");
});

