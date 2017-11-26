// global variables used in hw8
stockPlotOjbect = null;
indPlotObjects = {};
// exportObjects = {};


// There's no need to initialize the Android interface from JavaScript.

var CHARTLENGTH = 128;
var INDICATOR_CHART_ID = "indicator-plot";
var HIST_CHART_ID = "hist-plot";

function zip(arr1, arr2) {
    return arr1.map(function (d, i) {return [d, arr2[i]];})
}

function compressedDates(dates=null) { // changed interface 11.21
    function compress(dates) {
        var end = Math.min(CHARTLENGTH, dates.length);
        return dates.slice(0, end).map(function (d) 
            {return d.slice(5).replace(/-/, '/')});
    }

    if (dates !== null) {
        return compress(dates);
    }
    if (stockPlotOjbect.compressedDates === undefined) {
        dates = compress(stockPlotOjbect.dates);
        stockPlotOjbect.compressedDates = dates;
    }
    else {
        dates = stockPlotOjbect.compressedDates;
    }
    return dates;
}

function plotStockPrice() {

    var YEAR = stockPlotOjbect.dates[0].slice(0, 4);
    var dates = compressedDates();
    var end = Math.min(CHARTLENGTH, dates.length);
    var prices = stockPlotOjbect.prices.slice(0, end);
    var volumes = stockPlotOjbect.volumes.slice(0, end);

    var SYMBOL = stockPlotOjbect['Stock Ticker'];
    var maxVolume = Math.max.apply(null, volumes);
    var maxPrice = Math.max.apply(null, prices);
    var lastDate = dates[0].replace(/-/, '/');
    
    var obj = {
        chart: {
            zoomType: 'x',
            marginTop: 80,
        },
        title: {
            text: SYMBOL + ' Stock Price(' + lastDate + '/' + YEAR + ')'
        },
        subtitle: {
            useHTML: true,
            text: "<a target='_blank' href='https://www.alphavantage.co/'> Source: Alpha Vantage </a>"
        },
        xAxis: {
            categories: dates,
            tickLength: 0,
            showEmpty: false,
            reversed: true,
            labels: {
                autoRotation: [-10, -20, -30, -40, -50, -60, -70]
            }
        },
        yAxis: [
        {
            title: {
                text: 'Stock Price',
            },
            max: Math.ceil(maxPrice)
        },
        {	
            title: {
                text: 'Volume'
            },
            gridLineWidth: 0,
            min: 0,
            max: Math.ceil(maxVolume * 3),
            endOnTick: false,
            opposite: true
        }],
        tooltip: {
            shared: false
        },
        legend: {
            layout: 'horizontal',
            align: 'center',
            verticalAlign: 'bottom'
        },
        plotOptions: {
            area: {
                threshold: null,
                tooltip: {
                    valueDecimals: 2
                }
            },
            column: {
                pointPadding: 0.5,
                borderWidth: 0,
                pointWidth: 1,
                groupPadding: 0,
                shadow: false,
                threshold: null
            }
        },
        series: [{
            yAxis: 0,
            type: 'area',
            name: SYMBOL,
            data: prices
        },
        {
            yAxis: 1,
            type: 'column',
            name: SYMBOL + ' Volume',
            data: volumes,
            color: 'red',
            maxPointWidth: 6
        }]
    }
    // TODO: pass back the export obj
    Highcharts.chart(INDICATOR_CHART_ID, obj);
    return obj;
}


function plotLineChart(title, dates, seriesData) {
    console.log(title);
    var acroynim = title.split(' ').slice(-1)[0].slice(1,-1).toUpperCase();
    var obj = {
        chart: {
            width: null,
            zoomType: "x"
        },
        title: {
            text: title
        },
        subtitle: {
            useHTML: true,
            text: "<a target='_blank' href='https://www.alphavantage.co/'> Source: Alpha Vantage </a>"
        },
        xAxis: {
            categories: dates,
            tickLength: 0,
            reversed: true, // reverse the x-aix
            labels: {
                autoRotation: [-10, -20, -30, -40, -50, -60, -70]
            }
        },
        yAxis: {
            title: {
                text: acroynim
            }
        },
        legend: {
            layout: 'horizontal',
            align: 'center',
            verticalAlign: 'bottom'
        },
        plotOptions: {
            series: {
                marker: {
                    radius: 2
                }
            }
        },
        series: seriesData
    };
    Highcharts.chart(INDICATOR_CHART_ID, obj);
    console.log("plotLineChart: indicator chart plotted");
    // TODO: pass object back to android
    return obj;
}

// reverse conversion: indicator plot obj and export obj
function extractIndPlotObjectFromExportObject(exportObj) {
    return {
        fullName: exportObj.title.text,
        dates: exportObj.xAxis.categories,
        seriesObjs: exportObj.series
    }
}

function processIndicator(indicator, obj) {
    var indicator = indicator.toUpperCase();
    function plot(obj) {
        var symbol = obj.symbol;
        var dates = obj.dates; // originall null (which is purposed to sync with price/volume)
        var fullName = obj.fullIndicator;
        var subIndicators = obj['sub-indicators'];
        var seriesObjs = Array();
        
        dates = compressedDates(dates);
        for (subIndicator of subIndicators) {
            seriesObjs.push({
                name: symbol + ' ' + subIndicator,
                data: obj[subIndicator].slice(0, Math.min(CHARTLENGTH, dates.length))
            });
        }
        indPlotObject = {
            fullName: fullName,
            dates: dates,
            seriesObjs: seriesObjs
        };
        indPlotObjects[indicator] = indPlotObject;
        return plotLineChart(fullName, dates, seriesObjs);
    }

    // use cached data
    if (indPlotObjects[indicator] !== undefined) {
        obj = indPlotObjects[indicator];
        return plotLineChart(obj.fullName, obj.dates, obj.seriesObjs);
    }
    
    return plot(obj);
}

function plotHistChart() {
    var dates = null;
    var prices = stockPlotOjbect.prices;
    
    if (stockPlotOjbect.utcDates === undefined) {
        dates = stockPlotOjbect.dates;
        dates = dates.map(function (date) {
            return Math.round(new Date(date).getTime())
        });
        stockPlotOjbect.utcDates = dates;
    }
    else {
        dates = stockPlotOjbect.utcDates;
    }
    
    var data = zip(dates, prices);
    data = data.slice(0, Math.min(1000, data.length));
    data.reverse();

    var buttons = [
        {
            type: 'month',
            count: 1,
            text: '1m'
        }, {
            type: 'month',
            count: 3,
            text: '3m'
        },{
            type: 'month',
            count: 6,
            text: '6m'
        }, {
            type: 'year',
            count: 1,
            text: '1y'
        }, {
            type: 'all',
            text: 'All'
        }
    ];
    
    Highcharts.stockChart(HIST_CHART_ID, {
        chart: {
            zoomType: 'x'
        },
        rangeSelector: {
            buttons: buttons,
            selected: 0
        },
        yAxis: {
            title: {
                text: 'Historical Prcies'
            }
        },
        title: {
            text: stockPlotOjbect["Stock Ticker"] + ' Stock Value'
        },
        subtitle: {
            useHTML: true,
            text: "<a target='_blank' href='https://www.alphavantage.co/'> Source: Alpha Vantage </a>"
        },
        plotOptions: {
            area: {
                threshold: null,
                tooltip: {
                    valueDecimals: 2
                }
            }
        },
        tooltip: {
            shared: true,
            split: false
        },
        series: [{
            name: 'Price',
            type: 'area',
            data: data
        }]

    });
}

