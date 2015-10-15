function showText() {
    $("#divBack").hide();
    $("#divText").show();
    $("#divFrame").hide();
    $("#divFrame iframe").get(0).src = "about:blank";
    return false;
}

function submitForm(form) {
    removeDisabled();

    var output = $('form#sendText input[name=outputformat]:checked').val();

    var action = "api/all";
    switch (output) {
        case "output_rdf":
        action = "api/text2rdf";
        break;

        case "output_naf":
        action = "api/text2naf";
        break;
    }

    $(form).attr("action", action);

    return true;
}

function removeDisabled() {
    $(".annotators input[type=checkbox]").removeAttr("disabled");
    return true;
}

function checkRecursive(id, value) {
    var div = $("#" + id);
    var require = div.data("require");
    if (require === undefined || require.length === 0) {
        return;
    }
    if (value > 20) {
        alert("Recursion limit reached!");
        return;
    }
    var requirements = require.split(",");
    for (var i = 0; i < requirements.length; i++) {
        $("#annotator_" + requirements[i]).find("input[type=checkbox]")
            .prop("checked", true);
        $("#annotator_" + requirements[i]).find("input[type=checkbox]")
            .prop("disabled", true);
        checkRecursive("annotator_" + requirements[i], value + 1);
    }
}

function checkNone() {
    $(".annotators input[type=checkbox]").removeAttr("disabled");
    $(".annotators input[type=checkbox]").prop("checked", false);
    return false;
}

function checkAll() {
    $(".annotators input[type=checkbox]").each(function(index) {
        var cb = $(this);
        var id = cb.parents("div.checkbox").attr("id");
        cb.prop("checked", true);
        checkRecursive(id, 1);
    });
    return false;
}

$(window).on('unload', function() {
    $('button[type=submit]').removeAttr('disabled');
});

// Date.prototype.yyyymmdd = function() {
//     var yyyy = this.getFullYear().toString();
//     var mm = (this.getMonth()+1).toString(); // getMonth() is zero-based
//     var dd = this.getDate().toString();
//     return yyyy + "-" + (mm[1]?mm:"0"+mm[0]) + "-" + (dd[1]?dd:"0"+dd[0]); // padding
// };

$(document).ready(function() {


    $(function () {
        $('[data-toggle="tooltip"]').tooltip()
    })
    Ladda.bind('button[type=submit]');
    checkAll();

    var d = new Date();
    // $("#inputDate").val(d.yyyymmdd());
    $("#inputDate").val(moment().format());

    $(".annotators input[type=checkbox]").click(function() {
        var cb = $(this);
        var id = cb.parents("div.checkbox").attr("id");
        if (cb.is(":checked")) {
            checkRecursive(id, 1);
        } else {
            var div = $("#" + id);
            var require = div.data("require");
            if (require == undefined || require.length == 0) {
                return;
            }
            var requirements = require.split(",");
            for (var i = 0; i < requirements.length; i++) {
                $("#annotator_" + requirements[i]).find(
                    "input[type=checkbox]").prop(
                    "disabled", false);
            }
            $(".annotators input[type=checkbox]").each(
                function(index) {
                    var cbThis = $(this);
                    if (cbThis.is(":checked")) {
                        var idThis = cbThis.parents(
                            "div.checkbox").attr(
                            "id");
                        checkRecursive(idThis, 1);
                    }
                });
        }
    });

    var sentences = new Array();
    sentences[0] = "Dostum favoured a possible return of Afghanistan's former king Mohammad Zaher Shah.";
    sentences[1] = "Lawyers for the survivors have filed a complaint against Sharon in Belgium.";
    sentences[2] = "G. W. Bush and Bono are very strong supporters of the fight of HIV in Africa. Their March 2002 meeting resulted in a 5 billion dollar aid.";
    sentences[3] = "Paul and John were famous musicians; they both started playing the guitar in Liverpool.";
    sentences[4] = "The seizure sent alarm bells ringing in the diplomatic community in this country.";

    for (var sentence of sentences) {
        var text = sentence;
        // var text = sentence.substring(0, 50) + " [...]";
        var option = new Option(text, sentence);
        $('#inputSentence').append(option);
    }

    $('#inputSentence').change(function(data) {
        var val = $(data.target).val();
        if (val != "") {
            $('#inputText').val(val);
        }
        return false;
    });
});