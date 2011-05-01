// Annotation script for working with a json version of annotea
// @author Ron Ward

$(function(){
    enableParaAnnotating($);
});

function enableParaAnnotating(jQ)
{
    var myUrl = window.location.href;
    var bodyDiv = jQ("div.body > div:first");
    ajaxUrl = myUrl.split("/portal/default/")[0] + "/portal/default/annotation.ajax";
    //var query = "?w3c_annotates=" + escape(window.location.href);
    //var rUrl = "?w3c_reply_tree=" + escape(url);
    var annotations = {};       // keyed by 'id' (about)

    function mainSetup(){
        jQ.get(ajaxUrl, {method:"info"}, function(json){
            if(json.enabled) {
                pilcrowSetup(bodyDiv);
                addIds();
                loadAnnotations();
            }
        }, "json");
    }

    function annotationJsonCallback(dList){
        //alert(dList.toSource());
        var div;
        if(typeof(dList)=="string") { alert("string and not a list"); return;}
        jQ.each(dList.data, function(c, d){
            if(typeof(d.about)=="undefined") { alert("??"); return; }
            div = createAnnoDiv(d);
            div.attached = false;
            annotations[d.about] = div;
            // If we are a root annotation get any/all of our children (replies)
            if(d.root==d.inReplyTo){
                getRepliesFor(d.about);
            }
        });
        jQ.each(dList.data, function(c, d){
            div = annotations[d.about];
            if(div.attached==false) { div.attached=true; positionAndDisplay(div); }
        });
    }

    function loadAnnotations(){
        getAnnotationsFor(myUrl);
    }

    function getAnnotationsFor(url){
        d = {ajax:"annotea", method:"getAnnotates", url:url};
        jQ.get(ajaxUrl, d, annotationJsonCallback, "json");
    }

    function getRepliesFor(url) {
        if(typeof(url)=="undefined") return;
        d = {ajax:"annotea", method:"getReplies", url:url};
        jQ.get(ajaxUrl, d, annotationJsonCallback, "json");
    }

    function getAnnotation(url){
        d = {ajax:"annotea", "method":"getAnnotation", url:url};
        jQ.get(ajaxUrl, d, annotationJsonCallback, "json");
    }

    function postNewAnnotation(d){
        function _callback(d){
            try {
                if(typeof(d.url)=="undefined"){d.url=d.id;}
                getAnnotation(d.url);
            } catch(e){}
        }
        d.method = "create";
        jQ.post(ajaxUrl, d, _callback, "json");
    }

    function closeAnnotation(url){
        alert("close "+url);
    }

    function deleteAnnotation(url){
        d = {ajax:"annotea", "method":"delete", url:url};
        function callback(j){
            if(j.deleted=="OK"){
                bodyDiv.find("div.inline-annotation").each(function(c, i){
                    if(i.id==url) jQ(i).remove();
                });
            }
        }
        jQ.get(ajaxUrl, d, callback, "json");
    }

    function createAnnoDiv(d){
        var parentId = "";
        try { parentId = d.context.split("#")[1].split('id("')[1].split('")')[0];
        } catch(e) {}
        if (parentId==""){
            parentId = d.inReplyTo
        }
        var s = "<div class='inline-annotation' id='" + d.about + "'>"
        s += "<input name='parentId' value='" + parentId + "' type='hidden'><!-- --></input>";
        s += "<input name='rootUrl' value='" + d.root + "' type='hidden'><!-- --></input>";
        s += "<input name='selfUrl' value='" + d.about + "' type='hidden'><!-- --></input>";
        s += " <div class='orig-content' style='display:none;'> </div>";
        s += " <div class='anno-info'>Comment by: <span>" + d.creator + "</span>";
        s += " &nbsp; <span>" + d.created + " </span></div>";
        s += " <div class='anno-content'>" + d.body + "</div>";
        s += " <div class='anno-children'><!-- --></div>";
        s += "</div>";
        var div = jQ(s);
        // Decorate a annotation with 'Close', 'Delete', 'Reply'
        var decorateAnnotation = function(anno) {
            var d;
            if (anno.hasClass("closed")) d = jQ("<span>&#160; <span class='delete-annotate command'> Delete</span></span>");
            else d = jQ("<span>" +
                "&#160;<span class='annotate-this command'>Reply</span>" +
                //"&#160;<span class='close-annotate command'>Close</span>" +
                "&#160;<span class='delete-annotate command'>Delete</span>" +
                "</span>");
            anno.find("div.anno-info:first").append(d);
            var closeClick = function(e) {
                var id = anno.attr("id");
                closeAnnotation(id);
            }
            var deleteClick = function(e){
                var id = anno.attr("id");
                deleteAnnotation(id);
            }
            var replyClick = function(e) {
                var id = anno.attr("id");
                annotate(anno);
            }
            d.find("span.close-annotate").click(closeClick);
            d.find("span.annotate-this").click(replyClick);
            d.find("span.delete-annotate").click(deleteClick);
        }
        // add close and reply buttons
        decorateAnnotation(div);
        return div;
    }

    function positionAndDisplay(inlineAnnotation){
        var attachAnnotation = function(anno, para) {
            if(typeof(para)!="undefined" && para.size()>0) {
                if(para.hasClass("inline-annotation")) {
                    para.find(">div.anno-children").prepend(anno);
                } else {
                    if(!para.parent().hasClass("inline-anno")) {
                        para.wrap("<div class='inline-anno'/>");
                    }
                    para.after(anno);
                    para.css("margin", "0px");
                }
            }
        }
        var parentId = inlineAnnotation.find("input[name='parentId']").val();
        var p = bodyDiv.find("#" + parentId);
        if(parentId in annotations) p = annotations[parentId];
        //if(parentId==titleId) p = title1;
        if(p.size()==0) {       // if orphan
            inlineAnnotation.find("div.orig-content").show();
            bodyDiv.append(inlineAnnotation);
        }
        attachAnnotation(inlineAnnotation, p);
    }

    // Setup Pilcrow marker
    function pilcrowSetup(bodyDiv){
        // Pilcrow
        var pilcrowSpan = jQ("<span class='pilcrow command'> &#xb6;</span>");
        var prTimer = 0;
        var addPilcrow = function(jqe) { if(prTimer)clearTimeout(prTimer); jqe.append(pilcrowSpan);
                                            pilcrowSpan.unbind(); pilcrowSpan.click(click);
                                            pilcrowSpan.mousedown(function(e){ return false; });
                                            pilcrowSpan.mouseup(click);     // for I.E.
            }
        var removePilcrow = function() {
                prTimer=setTimeout(function(){prTimer=0; pilcrowSpan.remove();}, 100);
            }
        // The pilcrow click function
        var click = function(e) {
            var t = e.target;
            var me = jQ(t).parent();
            var name = me[0].tagName;
            if (name=="P" || name=="DIV" || name.substring(0,1)=="H"){
                removePilcrow();
                annotate(me);
            } else {
                alert(name);
            }
            return false;
        }

        //
        function getSelectedText(){       // used by bodyDiv.mouseover & title1.mouseover
            if(window.getSelection) return window.getSelection();
            if(document.getSelection) return document.getSelection();
            if(document.selection) return document.selection.createRange().text;
            return "";
        }
        //
        bodyDiv.mouseover(function(e) {
            var t = e.target;
            var name = t.tagName;
            if(name!="P" && name.substring(0,1)!="H"){
                if(t.parentNode.tagName=="P") t=t.parentNode;
                else return;
            }
            var me = jQ(t);
            if(me.html()=="") return;
            me.unbind();
            me.mouseover(function(e) { if(getSelectedText()=="") addPilcrow(me); return false;} );
            me.mouseout(function(e) { removePilcrow(); } );
            me.mousedown(function(e) { removePilcrow(); } );
            me.mouseover();
        });
    }

    // Annotate an item (e.g. paragraph)  (called from pilcrow & reply click)
    var annotationComments = {};
    // Annotation Form
    var annotateDiv = "<div class='annotate-form'><textarea cols='80' rows='8'></textarea><br/>";
    annotateDiv += "<button class='cancel'>Cancel</button>&#160;";
    annotateDiv += "<button class='submit'>Submit</button> <span class='info'></span>";
    annotateDiv += "</div>";
    annotateDiv = jQ(annotateDiv);
    var commentOnThis = jQ("<div class='app-label'>Comment on this:</div>");
    var textArea = annotateDiv.find("textarea");
    var last = null;
    function annotate(me) {
        var unWrapLast = function() {
            if(last!=null){
                //last.parent().replaceWith(last);
                annotateDiv.remove();
                commentOnThis.remove();
                last=null;
            }
        }
        var closeClick = function() { unWrapLast(); annotationComments[id] = jQ.trim(textArea.val()); }
        var submitClick = function() {
            var text, html, d, selfUrl;
            unWrapLast();
            annotationComments[id] = "";
            text = jQ.trim(textArea.val());
            if(text=="") return;
            // if this is a reply or not
            html = me.wrap("<div/>").parent().html();    // Hack to access the outerHTML
            html = jQ(html).text();  ///////////////////////
            me.parent().replaceWith(me);
            d = {elemId:id, content:html, body:text };
            selfUrl = me.find("input[name='selfUrl']").val() || "";
            d.root = me.find("input[name='rootUrl']").val() || selfUrl;
            d.inReplyTo = selfUrl;
            if(d.inReplyTo!=""){
                d.content = "";
            }
            d.annotates = myUrl;
            d.annotationType = ""
            postNewAnnotation(d);
        }
        var restore = function() {
            if(id in annotationComments) {textArea.val(annotationComments[id]);} else {textArea.val("");}
        }
        if(last!=null) { unWrapLast(); annotateDiv.find("button.cancel").click(); }
        var id = me.attr("id");
        if(typeof(id)=="undefined") { return; }
        restore();
        // wrap it
        me.wrap("<div class='inline-annotation-form'/>");
        if(me.hasClass("inline-annotation")) {
            //alert(me.parent().html())
            me.find("div.anno-children:first").before(annotateDiv)
        } else {
            me.parent().append(annotateDiv);
        }
        annotateDiv.find("button.clear").click(function(){textArea.val("");});
        annotateDiv.find("button.cancel").click(function(){textArea.val(annotationComments[id]); closeClick();});
        annotateDiv.find("button.close").click(closeClick);
        annotateDiv.find("button.submit").click(submitClick);
        me.parent().prepend(commentOnThis);
        unWrapLast();
        last = me;
        textArea.focus();
    }

    // ================================================
  

    var crcs = {}
    function addIds(){
        var me, crc, c;
        bodyDiv.find("p, h1, h2, h3, h4, h5, h6").each(function(c, i){
            me = jQ(i)
            if(me.attr("id")==""){
                if(i.tagName!="P") c = me.text();
                else c = me.html()
                crc = Crc32(c).toLowerCase();
                if(crcs[crc]) { crcs[crc].push(true); c=crcs[crc].length; }
                else { crcs[crc]=[true]; c=1;}
                me.attr("id", "h"+crc+"p" + c);
                
            }
        });
    }

    mainSetup();
}


//    if(false){
//        // enable annotations for the Title as well
//        var titles = jQ("div.title");
//        var title1 = titles.filter(":eq(0)");
//        var title2 = titles.filter(":eq(1)");
//        var titleId = title2.attr("id");
//        if(typeof(titleId)=="undefined") titleId="h_titleIdt";
//        titleId = titleId.toLowerCase();
//        title2.attr("id", "_" + titleId);
//        title1.attr("id", titleId);
//        //
//        title1.mouseover(function(e) {
//            var me = jQ(e.target);
//            me.unbind();
//            me.mouseover(function(e) { if(getSelectedText()=="") addPilcrow(me); return false;} );
//            me.mouseout(function(e) { removePilcrow(); } );
//            me.mousedown(function(e) { removePilcrow(); } );
//            me.mouseover();
//        });
//    }
//    //
//    if(false){
//        var commentOnThis = jQ("div.annotateThis");
//        commentOnThis.find("span").addClass("command");
//        commentOnThis.find("span").click(click);
//        function hideShowAnnotations() {
//            var me = jQ(this);
//            var text = me.text();
//            var inlineAnnotations = jQ("div.inline-annotation");
//            var l = inlineAnnotations.size();
//            var h = inlineAnnotations.filter(".closed").size();
//            if(text.substring(0,1)=="H") {
//                me.text("Show comments ("+(l-h)+" opened, "+h+" closed)");
//                inlineAnnotations.hide();
//            }
//            else {
//                me.text("Hide comments ("+(l-h)+" opened, "+h+" closed)");
//                inlineAnnotations.show();
//            }
//        }
//        jQ("div.ice-toolbar .hideShowAnnotations").click(hideShowAnnotations).text("S").click();
//    }


// =======================================
// Crc32
// =======================================

function Crc32(str) {

    function Crc32Hex(str)
    {
      return Hex32(Crc32Str(str));
    }

    function Crc32Str(str)
    {
      var len = str.length;
      var crc = 0xFFFFFFFF;
      for(var n=0; n<len; n++)
      {
        crc = Crc32Add(crc, str.charCodeAt(n));
      }
      return crc^0xFFFFFFFF;
    }

    function Hex32(val)
    {
      var n;
      var str1;
      var str2;

      n=val&0xFFFF;
      str1=n.toString(16).toUpperCase();
      while (str1.length<4)
      {
        str1="0"+str1;
      }
      n=(val>>>16)&0xFFFF;
      str2=n.toString(16).toUpperCase();
      while (str2.length<4)
      {
        str2="0"+str2;
      }
      return str2+str1;
    }

    var Crc32Tab = [
        0x00000000,0x77073096,0xEE0E612C,0x990951BA,0x076DC419,0x706AF48F,0xE963A535,0x9E6495A3,
        0x0EDB8832,0x79DCB8A4,0xE0D5E91E,0x97D2D988,0x09B64C2B,0x7EB17CBD,0xE7B82D07,0x90BF1D91,
        0x1DB71064,0x6AB020F2,0xF3B97148,0x84BE41DE,0x1ADAD47D,0x6DDDE4EB,0xF4D4B551,0x83D385C7,
        0x136C9856,0x646BA8C0,0xFD62F97A,0x8A65C9EC,0x14015C4F,0x63066CD9,0xFA0F3D63,0x8D080DF5,
        0x3B6E20C8,0x4C69105E,0xD56041E4,0xA2677172,0x3C03E4D1,0x4B04D447,0xD20D85FD,0xA50AB56B,
        0x35B5A8FA,0x42B2986C,0xDBBBC9D6,0xACBCF940,0x32D86CE3,0x45DF5C75,0xDCD60DCF,0xABD13D59,
        0x26D930AC,0x51DE003A,0xC8D75180,0xBFD06116,0x21B4F4B5,0x56B3C423,0xCFBA9599,0xB8BDA50F,
        0x2802B89E,0x5F058808,0xC60CD9B2,0xB10BE924,0x2F6F7C87,0x58684C11,0xC1611DAB,0xB6662D3D,
        0x76DC4190,0x01DB7106,0x98D220BC,0xEFD5102A,0x71B18589,0x06B6B51F,0x9FBFE4A5,0xE8B8D433,
        0x7807C9A2,0x0F00F934,0x9609A88E,0xE10E9818,0x7F6A0DBB,0x086D3D2D,0x91646C97,0xE6635C01,
        0x6B6B51F4,0x1C6C6162,0x856530D8,0xF262004E,0x6C0695ED,0x1B01A57B,0x8208F4C1,0xF50FC457,
        0x65B0D9C6,0x12B7E950,0x8BBEB8EA,0xFCB9887C,0x62DD1DDF,0x15DA2D49,0x8CD37CF3,0xFBD44C65,
        0x4DB26158,0x3AB551CE,0xA3BC0074,0xD4BB30E2,0x4ADFA541,0x3DD895D7,0xA4D1C46D,0xD3D6F4FB,
        0x4369E96A,0x346ED9FC,0xAD678846,0xDA60B8D0,0x44042D73,0x33031DE5,0xAA0A4C5F,0xDD0D7CC9,
        0x5005713C,0x270241AA,0xBE0B1010,0xC90C2086,0x5768B525,0x206F85B3,0xB966D409,0xCE61E49F,
        0x5EDEF90E,0x29D9C998,0xB0D09822,0xC7D7A8B4,0x59B33D17,0x2EB40D81,0xB7BD5C3B,0xC0BA6CAD,
        0xEDB88320,0x9ABFB3B6,0x03B6E20C,0x74B1D29A,0xEAD54739,0x9DD277AF,0x04DB2615,0x73DC1683,
        0xE3630B12,0x94643B84,0x0D6D6A3E,0x7A6A5AA8,0xE40ECF0B,0x9309FF9D,0x0A00AE27,0x7D079EB1,
        0xF00F9344,0x8708A3D2,0x1E01F268,0x6906C2FE,0xF762575D,0x806567CB,0x196C3671,0x6E6B06E7,
        0xFED41B76,0x89D32BE0,0x10DA7A5A,0x67DD4ACC,0xF9B9DF6F,0x8EBEEFF9,0x17B7BE43,0x60B08ED5,
        0xD6D6A3E8,0xA1D1937E,0x38D8C2C4,0x4FDFF252,0xD1BB67F1,0xA6BC5767,0x3FB506DD,0x48B2364B,
        0xD80D2BDA,0xAF0A1B4C,0x36034AF6,0x41047A60,0xDF60EFC3,0xA867DF55,0x316E8EEF,0x4669BE79,
        0xCB61B38C,0xBC66831A,0x256FD2A0,0x5268E236,0xCC0C7795,0xBB0B4703,0x220216B9,0x5505262F,
        0xC5BA3BBE,0xB2BD0B28,0x2BB45A92,0x5CB36A04,0xC2D7FFA7,0xB5D0CF31,0x2CD99E8B,0x5BDEAE1D,
        0x9B64C2B0,0xEC63F226,0x756AA39C,0x026D930A,0x9C0906A9,0xEB0E363F,0x72076785,0x05005713,
        0x95BF4A82,0xE2B87A14,0x7BB12BAE,0x0CB61B38,0x92D28E9B,0xE5D5BE0D,0x7CDCEFB7,0x0BDBDF21,
        0x86D3D2D4,0xF1D4E242,0x68DDB3F8,0x1FDA836E,0x81BE16CD,0xF6B9265B,0x6FB077E1,0x18B74777,
        0x88085AE6,0xFF0F6A70,0x66063BCA,0x11010B5C,0x8F659EFF,0xF862AE69,0x616BFFD3,0x166CCF45,
        0xA00AE278,0xD70DD2EE,0x4E048354,0x3903B3C2,0xA7672661,0xD06016F7,0x4969474D,0x3E6E77DB,
        0xAED16A4A,0xD9D65ADC,0x40DF0B66,0x37D83BF0,0xA9BCAE53,0xDEBB9EC5,0x47B2CF7F,0x30B5FFE9,
        0xBDBDF21C,0xCABAC28A,0x53B39330,0x24B4A3A6,0xBAD03605,0xCDD70693,0x54DE5729,0x23D967BF,
        0xB3667A2E,0xC4614AB8,0x5D681B02,0x2A6F2B94,0xB40BBE37,0xC30C8EA1,0x5A05DF1B,0x2D02EF8D];
    function Crc32Add(crc, c)
    {
      return Crc32Tab[(crc^c)&0xFF]^((crc>>8)&0xFFFFFF);
    }

    return Crc32Hex(str);
}



