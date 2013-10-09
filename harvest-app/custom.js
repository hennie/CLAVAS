jQuery(document).ready(function() {

    $(function() {
        $("#tabs").tabs();
    });

    $("#button3").button().click(function() {
        var cs = $("#cs-value").text();
        var lni = $("#lni-value").text();
        var mlm = $("#mlm-value").text();

        var url = "http://localhost/~hennieb/cgi-bin/iso-639-3.rdf?cs=" +
                cs + "&lni=" + lni + "&mlm=" + mlm;

        window.location.assign(url);
        /*	
         $.ajax({
         type: 'GET',
         //	url: 'http://localhost/~hennieb/cgi-bin/first.pl?cs=' +
         //		 cs + '&lni=' + lni + '&mlm=' + mlm,
         url: 'http://localhost/~hennieb/images.zip',	 
         //	dataType: 'html',
         success: function(data) {
         $("#skosdata").text(data);
         },
         error: function(xhr, textStatus) {
         alert('An error occurred: ' + xhr.status);
         }
         }); 
         */
    });


    $("#button4").button().click(function() {
        var iv = $("#isocat-value").text();

        var url = "http://localhost/~hennieb/cgi-bin/isocat.rdf?url=" + iv;
        window.location.assign(url);
    });

 /*   $("#fileupload").fileupload({
        dataType: 'json',
        done: function(e, data) {
            alert("fileupload done. " + data);
            $.each(data.result.files, function(index, file) { 
                alert(file.name);
                $("#tabs-4").append(file.name);
            });
        },
        progressall: function(e, data) {
            var progress = parseInt(data.loaded / data.total * 100, 10);
            $('#progress .bar').css('width', progress + '%');
        },
        xhrFields: {
            withCredentials: true
        }
    }); */

    $("#langcode-dialog").dialog({
        autoOpen: false,
        width: 600,
        buttons: [
            {
                text: "OK",
                click: function() {
                    $(this).dialog("close");

                    var invalidURL = false;
                    var newCS = $("#cs-input").val();
                    if (ValidUrl(newCS)) {
                        $("#cs-value").text(newCS);
                    }
                    else {
                        invalidURL = true;
                    }

                    var newLNI = $("#lni-input").val();
                    if (ValidUrl(newLNI)) {
                        $("#lni-value").text(newLNI);
                    }
                    else {
                        invalidURL = true;
                    }

                    var newMLM = $("#mlm-input").val();
                    if (ValidUrl(newMLM)) {
                        $("#mlm-value").text(newMLM);
                    }
                    else {
                        invalidURL = true;
                    }

                    if (invalidURL) {
                        alert("invalid url used");
                    }
                }
            },
            {
                text: "Cancel",
                click: function() {
                    $(this).dialog("close");
                }
            }
        ]
    });

    // Link to open the dialog
    $("#langcode-dialog-button").button().click(function( ) {
        $("#langcode-dialog").dialog("open");
    });

    $("#isocat-dialog").dialog({
        autoOpen: false,
        width: 600,
        buttons: [
            {
                text: "OK",
                click: function() {
                    $(this).dialog("close");

                    var invalidURL = false;
                    var newISOcat = $("#isocat-input").val();
                    if (ValidUrl(newISOcat)) {
                        $("#isocat-value").text(newISOcat);
                    }
                    else {
                        invalidURL = true;
                    }

                    if (invalidURL) {
                        alert("invalid url used");
                    }
                }
            },
            {
                text: "Cancel",
                click: function() {
                    $(this).dialog("close");
                }
            }
        ]
    });

    // Link to open the dialog
    $("#isocat-dialog-button").button().click(function( ) {
        $("#isocat-dialog").dialog("open");
    });
});

function ValidUrl(str) {
    var pattern = new RegExp('^(https?:\\/\\/)?' + // protocol
            '((([a-z\\d]([a-z\\d-]*[a-z\\d])*)\\.)+[a-z]{2,}|' + // domain name
            '((\\d{1,3}\\.){3}\\d{1,3}))' + // OR ip (v4) address
            '(\\:\\d+)?(\\/[-a-z\\d%_.~+]*)*' + // port and path
            '(\\?[;&a-z\\d%_.~+=-]*)?' + // query string
            '(\\#[-a-z\\d_]*)?$', 'i'); // fragment locator
    if (!pattern.test(str)) {
        return false;
    } else {
        return true;
    }
}
