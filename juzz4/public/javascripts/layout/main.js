var mainJS = {
    leftBarActionCount: 0,
    onLeftBarOpenedTasks: [],
    onLeftBarClosedTasks: []
};

$(document).ready(function ()
{
    // NiceScrollUpdate funcionality
    function updateNiceScroll()
    {
        var scrollDiv = $("#sidebar");
        scrollDiv.getNiceScroll().resize();
    }

    $(".toggle-title").on("click", updateNiceScroll);

    // Top dropdown menus funcionality
    $(".profile-error").find(".inner li:last-child()").addClass("last");

    $(".profile-error").find(".inner li").hover(
        function ()
        {
            $(this).addClass("is_hovered");
        }
        , function ()
        {
            $(this).removeClass("is_hovered");
        }
    );

    // if(isMobile.any()) {
    $(".profile-error > li").click(
        function (e)
        {
            e.stopPropagation();
            $(".profile-error > li").not(this).each(function ()
            {
                $(this).removeClass("is_active").find(".inner").css("display", "none");
            });

            $(this).toggleClass("is_active").find(".inner").stop().toggle();
        });
    $("#container").click(function ()
    {
        $(".profile-error > li").removeClass("is_active").find(".inner").stop().css("display", "none");
        $(".export-options").find(".inner").slideUp(300);
        $(".email-options").find(".options").slideUp(300);
        $(".permalink").find(".inner").slideUp(300);
    });


    // Toggle panels funcionality
    function togglePanels()
    {
        var allPanels = $(".toggle-panels .toggle-body").hide();

        $(".toggle-title > .parent").hover(
            function ()
            {
                $(this).addClass("is_hovered");
            }, function ()
            {
                $(this).removeClass("is_hovered");
            }
        )

        $(".toggle-bundle > .parent").hover(
            function ()
            {
                $(this).addClass("is_hovered");
            }, function ()
            {
                $(this).removeClass("is_hovered");
            }
        )

        $(".toggle-title > .parent").click(function ()
        {
            if ($(this).hasClass("is_active"))
            {
                $(this).removeClass("is_active").next(".toggle-body").slideUp(300, updateNiceScroll);
            }
            else
            {
                $(".toggle-title > .parent").not(this).each(function ()
                {
                    $(this).removeClass("is_active").next(".toggle-body").slideUp(300, updateNiceScroll);
                });
                $(this).addClass("is_active").next(".toggle-body").slideDown(300, updateNiceScroll);
            }
        })
    };
    if ($(".toggle-panels").length >= 1)
    {
        togglePanels();
    }
    ;

    $("#sidebar .toggle-bundle .parent.is_active").click(function ()
    {
        $(".toggle-bundle").animate({'marginLeft': "0"}, 300, function ()
        {
            $("li.toggle-bundle").hide();
        });
        $(".sidebar-inner").animate({'marginLeft': "0"}, 300);
    })

    $(function ()
    {
        function responsiveView()
        {
            var wSize = $(window).width();
            if (wSize <= 767)
            {
                $("#container").addClass("sidebar-close");
                $(".menu-toggle-icon").unbind("click").click(function (e)
                {
                    e.preventDefault();
                    $("#sidebar").toggle();
                });
                $(".sidebar-inner").css("width", "inherit");
                $(".toggle-bundle").css("top", 0);
            }

            if (wSize > 767)
            {
                $(".toggle-bundle").css("top", 0);
                if ($("#sidebar").css("display") == 'none')
                {
                    $("#sidebar").show();
                }

                $(".menu-toggle-icon").unbind("click").click(function (e)
                {
                    e.preventDefault();
                    mainJS.toggleLeftBar();
                }, updateNiceScroll());
                $("#container").removeClass("sidebar-close");
                $(".sidebar-inner").css("width", "250px");
            }
            //refreshSideMenu();
        }

        function refreshSideMenu()
        {
            if (parseInt($(".sidebar-inner").css('marginLeft'), 10) < 0)
            {
                var margin = "-" + $("#sidebar").width() + 'px'
                $(".sidebar-inner").css('marginLeft', margin);
            }
        }

        $(window).on("load", responsiveView);
        $(window).on("resize", responsiveView);
    });

    // NiceScroll functionality
    initNiceScroll();
    function initNiceScroll()
    {
        $("#sidebar").niceScroll({
            autohidemode: true,
            cursorcolor: "#f6ae40",
            cursorwidth: '3px',
            cursorborderradius: '10px',
            background: '#141414',
            spacebarenabled: false,
            touchbehavior: false,
            cursordragontouch: true,
            cursorborder: '',
            oneaxismousemode: true
        });
        $("#sidebar").mouseover(function ()
        {
            $("#sidebar").getNiceScroll().resize();
        });
    }
});

mainJS.toggleLeftBar = function ()
{
    $("#container").toggleClass("sidebar-closed");
    mainJS.leftBarActionCount++;

    if ($("#container").hasClass("sidebar-closed"))
    {
        $("#main").css({
            "width": "100%",
            "padding-left": "0"
        });
        $("#footer").css({
            "padding-left": "0"
        });
        $("#sidebar").css({
            "margin-left": "-250px"
        });
        $(".toggle-title > .parent").removeClass("is_active").next(".toggle-body").slideUp(100);
        $("#sidebar").getNiceScroll().hide();
    }
    else
    {
        $("#main").css({
            "width": "100%",
            "padding-left": "250px"
        });
        $("#footer").css({
            "padding-left": "250px"
        });
        $("#sidebar").css({
            "margin-left": "0"
        });
        $(".toggle-title > .parent").removeClass("is_active").next(".toggle-body").slideUp(100);
        $("#sidebar").getNiceScroll().show();
    }

    if (mainJS.isLeftBarActive())
    {
        $.each(mainJS.onLeftBarOpenedTasks, function (i, task)
        {
            task();
        });
    }
    else
    {
        $.each(mainJS.onLeftBarClosedTasks, function (i, task)
        {
            task();
        });
    }
}

mainJS.isLeftBarActive = function ()
{
    return !$("#container").hasClass("sidebar-closed");
};

mainJS.whenLeftBarOpened = function (task)
{
    mainJS.onLeftBarOpenedTasks.push(task);
};

mainJS.whenLeftBarClosed = function (task)
{
    mainJS.onLeftBarClosedTasks.push(task);
};

// End of document ready


