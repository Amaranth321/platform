jQuery(document).ready(function ($) {

    $('li:first-child').addClass('first-item');
    $('li:last-child').addClass('last-item');


    $('.big_option').click(function () {
        $(this).next().toggle();
        $(this).toggleClass('big_option_expanded');
    });

    $('.display_box .btn_close').click(function () {
        $('.display_box').fadeOut();
        return false;
    });

    $('.btn_display').click(function () {
        $('.display_box').fadeIn();
        return false;
    });

    $('.video_list li').click(function () {
        $('.video_list li').removeClass('on');
        $(this).addClass('on');
        return false;
    });

    //equal height down
    function position_monitor() {
        var equal_height = $(window).height() - 76;
        $('#content_black').height($(window).height());

        if (document.getElementById('content_black_thin')) {
            adjustFullView();
        }
        else {
            adjustNormalView();
        }
    }

    $(window).scroll(position_monitor).resize(position_monitor).load(position_monitor);
});

function adjustFullView() {
    var winHeight = $(window).height() - 133;
    var winWidth = (winHeight * 4) / 2;

    $("#vList").height(winHeight);
    $("#vList").width(winWidth);
    $("#vList").show();
    $("#liveViewBox").width(winWidth);
}

function adjustNormalView() {
    var winHeight = $(window).height() - 93;
    var winWidth = (winHeight * 4) / 3;

    var liveViewWidth = winHeight - 120;
    var liveViewHeight = (liveViewWidth * 4) / 3;

    $("#vList").height(winHeight);
    $("#vList").width(liveViewHeight);
    $("#vList").show();
    $("#liveViewBox").width(winWidth);
}

function adjustPageSize(div){
	var height = $("#content_black").height();
	var footerHeight = $("#footernew").height();
	var formHeight = $("#"+div).height();
	$("#"+div).css({"margin-top":(height-footerHeight-formHeight)/2});
}