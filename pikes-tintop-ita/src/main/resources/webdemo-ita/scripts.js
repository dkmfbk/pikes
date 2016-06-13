google.charts.load('current', {'packages': ['corechart', 'gauge', 'bar']});

var gaugeOptions = {
    redFrom: 0,
    redTo: 40,
    yellowFrom: 40,
    yellowTo: 80,
    greenFrom: 80,
    greenTo: 100,
    minorTicks: 5,
    width: 130,
    height: 130
};
var gaugeLevelsOptions = {
    redFrom: 0,
    redTo: 40,
    yellowFrom: 40,
    yellowTo: 80,
    greenFrom: 80,
    greenTo: 100,
    minorTicks: 5,
    width: 360,
    height: 120
};
var posChartOptions = {
    width: 500,
    height: 250,
    orientation: "vertical"
};

function escapeAttrNodeValue(value) {
    return value.replace(/(&)|(")|(\u00A0)/g, function (match, amp, quote) {
        if (amp) return "&amp;";
        if (quote) return "&quot;";
        return "&nbsp;";
    });
}

function addLI(ul, title, value) {
    var li = $("<li></li>");
    li.addClass("list-group-item");
    var span = $("<span></span>");
    span.addClass("badge");
    span.append(value);
    li.append(span);
    li.append(" " + title);
    // var b = $("<b></b>");
    // b.append(title);
    // li.append(b);
    // li.append(" " + value);
    ul.append(li);
}

$(function () {
    $('button.has-spinner').click(function () {
        $(this).toggleClass('active');
        $(this).toggleClass('disabled');
        $('#text').attr('disabled', 'disabled');

        $.ajax("/simp", {
            dataType: "json",
            data: {
                text: $('#text').val()
            },
            success: function (data) {

                $.each(data.sentences, function (i, item) {
                    var p = $("<p></p>");

                    var text = item.text;
                    item.descriptions.reverse().forEach(function (value) {
                        console.log(value.begin);
                        var begin = value.begin - item.begin;
                        var end = value.end - item.begin;
                        var formID = "form" + value.begin;
                        text = text.replace(new RegExp('(.{' + begin + '})(.{' + (end - begin) + '})'),
                            '$1<a data-content="' + value.text + '" title="' + value.form +
                            '" tabindex="0" role="button" class="my-popover label label-primary" id="' +
                            formID + '">$2</a>');
                    });

                    p.append(text);
                    p.attr("id", "sentence" + i);
                    p.addClass("sentence");
                    if (item.tooLong) {
                        p.addClass("too-long")
                    }
                    $("#parsed-text").append(p);
                });

                $("#part2").tooltip({
                    selector: '.too-long',
                    title: "Sentence too long"
                });
                $("#part2").popover({
                    selector: ".my-popover",
                    trigger: "focus"
                });

                // Gauges

                var gulpease = data.statistics.gulpease;
                var level1 = 100.0 * data.statistics.level1WordSize / data.statistics.contentEasyWordSize;
                var level2 = 100.0 * data.statistics.level2WordSize / data.statistics.contentWordSize;
                var level3 = 100.0 * data.statistics.level3WordSize / data.statistics.contentWordSize;

                var gulpeaseChart = new google.visualization.Gauge(document.getElementById('gauge-gulpease'));
                gulpeaseChart.draw(google.visualization.arrayToDataTable([
                    ['Label', 'Value'],
                    ['Gulpease', gulpease]
                ]), gaugeOptions);

                var levelsChart = new google.visualization.Gauge(document.getElementById('gauge-levels'));
                levelsChart.draw(google.visualization.arrayToDataTable([
                    ['Label', 'Value'],
                    ['Level1', level1],
                    ['Level2', level2],
                    ['Level3', level3]
                ]), gaugeLevelsOptions);

                // Statistics

                var ul = $('<ul></ul>');
                ul.addClass("list-group");
                addLI(ul, "Sentences:", data.statistics.sentenceCount);
                addLI(ul, "Tokens:", data.statistics.tokenCount);
                addLI(ul, "Words:", data.statistics.wordCount);
                addLI(ul, "Content words:", data.statistics.contentWordSize);
                $("#statistics").append(ul);

                // Pos chart

                var posData = new google.visualization.DataTable();
                posData.addColumn("string", "POS tag");
                posData.addColumn("number", "Count");
                $.each(data.statistics.spos, function (index, val) {
                    posData.addRow([index, val]);
                });
                var posChart = new google.visualization.ColumnChart(document.getElementById('pos-stats'));
                posChart.draw(posData, posChartOptions);

                $("#part1").slideUp(500);
                $("#part2").show();
            }
        });
        return false;
    });
});
