$(document).ready(function() {
	$("input[type=checkbox]").uniform();
	$('input[placeholder], textarea[placeholder]').placeholder();
	 $('.slides').bxSlider({
	 	mode:'fade',
	  	speed:1000,
	  	touchEnabled: false,
	  	pager: false,
	  	controls: false,
	  	auto: true
	});
	$(document).on("click",".btn-close", function() {
        if($(".bottom-holder").css('display') == 'none') {
            $('.top-bar').stop(true,true).animate({height:'show'}, 400);
        }
        else {
             $('.top-bar').stop(true,true).animate({height:'hide'}, 300);
        }
        return false;
    });
	
	$( "input" ).each(function( index ) {
		$(this).data('holder',$(this).attr('placeholder'));
	});
    $('input').focusin(function(){
        $(this).attr('placeholder','');
        $(this).parent().addClass('active');
    });
    $('input').focusout(function(){
        $(this).attr('placeholder',$(this).data('holder'));
        if($(this).val()=='')
        	$(this).parent().removeClass('active');
    });
});  