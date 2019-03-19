/* jquery.nicescroll
-- version 3.4.0
-- copyright 2011-12-13 InuYaksa*2013
-- licensed under the MIT
--
-- http://areaaperta.com/nicescroll
-- https://github.com/inuyaksa/jquery.nicescroll
--
*/

(function(jQuery){

  // globals
  var domfocus = false;
  var mousefocus = false;
  var zoomactive = false;
  var tabindexcounter = 5000;
  var ascrailcounter = 2000;
  var globalmaxzindex = 0;

  var $ = jQuery;  // sandbox

  // http://stackoverflow.com/questions/2161159/get-script-path
  function getScriptPath() {
    var scripts=document.getElementsByTagName('script');
    var path=scripts[scripts.length-1].src.split('?')[0];
    return (path.split('/').length>0) ? path.split('/').slice(0,-1).join('/')+'/' : '';
  }
  var scriptpath = getScriptPath();

// derived by Paul Irish https://gist.github.com/paulirish/1579671 - thanks for your code!

  if (!Array.prototype.forEach) {  // JS 1.6 polyfill
    Array.prototype.forEach = function(fn, scope) {
      for(var i = 0, len = this.length; i < len; ++i) {
        fn.call(scope, this[i], i, this);
      }
    }
  }

  var vendors = ['ms','moz','webkit','o'];

  var setAnimationFrame = window.requestAnimationFrame||false;
  var clearAnimationFrame = window.cancelAnimationFrame||false;

  vendors.forEach(function(v){
    if (!setAnimationFrame) setAnimationFrame = window[v+'RequestAnimationFrame'];
    if (!clearAnimationFrame) clearAnimationFrame = window[v+'CancelAnimationFrame']||window[v+'CancelRequestAnimationFrame'];
  });

  var clsMutationObserver = window.MutationObserver || window.WebKitMutationObserver || false;

  var _globaloptions = {
      zindex:"auto",
      cursoropacitymin:0,
      cursoropacitymax:1,
      cursorcolor:"#424242",
      cursorwidth:"5px",
      cursorborder:"1px solid #fff",
      cursorborderradius:"5px",
      scrollspeed:60,
      mousescrollstep:8*3,
      touchbehavior:false,
      hwacceleration:true,
      usetransition:true,
      boxzoom:false,
      dblclickzoom:true,
      gesturezoom:true,
      grabcursorenabled:true,
      autohidemode:true,
      background:"",
      iframeautoresize:true,
      cursorminheight:32,
      preservenativescrolling:true,
      railoffset:false,
      bouncescroll:true,
      spacebarenabled:true,
      railpadding:{top:0,right:0,left:0,bottom:0},
      disableoutline:true,
      horizrailenabled:true,
      railalign:"right",
      railvalign:"bottom",
      enabletranslate3d:true,
      enablemousewheel:true,
      enablekeyboard:true,
      smoothscroll:true,
      sensitiverail:true,
      enablemouselockapi:true,
//      cursormaxheight:false,
      cursorfixedheight:false,
      directionlockdeadzone:6,
      hidecursordelay:400,
      nativeparentscrolling:true,
      enablescrollonselection:true,
      overflowx:true,
      overflowy:true,
      cursordragspeed:0.3,
      rtlmode:false,
      cursordragontouch:false
  }

  var browserdetected = false;

  var getBrowserDetection = function() {

    if (browserdetected) return browserdetected;

    var domtest = document.createElement('DIV');

    var d = {};

        d.haspointerlock = "pointerLockElement" in document || "mozPointerLockElement" in document || "webkitPointerLockElement" in document;

    d.isopera = ("opera" in window);
    d.isopera12 = (d.isopera&&("getUserMedia" in navigator));

    d.isie = (("all" in document) && ("attachEvent" in domtest) && !d.isopera);
    d.isieold = (d.isie && !("msInterpolationMode" in domtest.style));  // IE6 and older
    d.isie7 = d.isie&&!d.isieold&&(!("documentMode" in document)||(document.documentMode==7));
    d.isie8 = d.isie&&("documentMode" in document)&&(document.documentMode==8);
    d.isie9 = d.isie&&("performance" in window)&&(document.documentMode>=9);
    d.isie10 = d.isie&&("performance" in window)&&(document.documentMode>=10);

    d.isie9mobile = /iemobile.9/i.test(navigator.userAgent);  //wp 7.1 mango
    if (d.isie9mobile) d.isie9 = false;
    d.isie7mobile = (!d.isie9mobile&&d.isie7) && /iemobile/i.test(navigator.userAgent);  //wp 7.0

    d.ismozilla = ("MozAppearance" in domtest.style);

    d.iswebkit = ("WebkitAppearance" in domtest.style);

    d.ischrome = ("chrome" in window);
        d.ischrome22 = (d.ischrome&&d.haspointerlock);
    d.ischrome26 = (d.ischrome&&("transition" in domtest.style));  // issue with transform detection (maintain prefix)

    d.cantouch = ("ontouchstart" in document.documentElement)||("ontouchstart" in window);  // detection for Chrome Touch Emulation
    d.hasmstouch = (window.navigator.msPointerEnabled||false);  // IE10+ pointer events

    d.ismac = /^mac$/i.test(navigator.platform);

    d.isios = (d.cantouch && /iphone|ipad|ipod/i.test(navigator.platform));
    d.isios4 = ((d.isios)&&!("seal" in Object));

    d.isandroid = (/android/i.test(navigator.userAgent));

    d.trstyle = false;
    d.hastransform = false;
    d.hastranslate3d = false;
    d.transitionstyle = false;
    d.hastransition = false;
    d.transitionend = false;

    var check = ['transform','msTransform','webkitTransform','MozTransform','OTransform'];
    for(var a=0;a<check.length;a++){
      if (typeof domtest.style[check[a]] != "undefined") {
        d.trstyle = check[a];
        break;
      }
    }
    d.hastransform = (d.trstyle != false);
    if (d.hastransform) {
      domtest.style[d.trstyle] = "translate3d(1px,2px,3px)";
      d.hastranslate3d = /translate3d/.test(domtest.style[d.trstyle]);
    }

    d.transitionstyle = false;
    d.prefixstyle = '';
    d.transitionend = false;
    var check = ['transition','webkitTransition','MozTransition','OTransition','OTransition','msTransition','KhtmlTransition'];
    var prefix = ['','-webkit-','-moz-','-o-','-o','-ms-','-khtml-'];
    var evs = ['transitionend','webkitTransitionEnd','transitionend','otransitionend','oTransitionEnd','msTransitionEnd','KhtmlTransitionEnd'];
    for(var a=0;a<check.length;a++) {
      if (check[a] in domtest.style) {
        d.transitionstyle = check[a];
        d.prefixstyle = prefix[a];
        d.transitionend = evs[a];
        break;
      }
    }
    if (d.ischrome26) {  // use always prefix
      d.prefixstyle = prefix[1];
    }

    d.hastransition = (d.transitionstyle);

    function detectCursorGrab() {
      var lst = ['-moz-grab','-webkit-grab','grab'];
      if ((d.ischrome&&!d.ischrome22)||d.isie) lst=[];  // force setting for IE returns false positive and chrome cursor bug
      for(var a=0;a<lst.length;a++) {
        var p = lst[a];
        domtest.style['cursor']=p;
        if (domtest.style['cursor']==p) return p;
      }
      return 'url(http://www.google.com/intl/en_ALL/mapfiles/openhand.cur),n-resize';  // thank you google for custom cursor!
    }
    d.cursorgrabvalue = detectCursorGrab();

    d.hasmousecapture = ("setCapture" in domtest);

    d.hasMutationObserver = (clsMutationObserver !== false);

    domtest = null;  //memory released

    browserdetected = d;

    return d;
  }

  var NiceScrollClass = function(myopt,me) {

    var self = this;

    this.version = '3.4.0';
    this.name = 'nicescroll';

    this.me = me;

    this.opt = {
      doc:$("body"),
      win:false
    };

    $.extend(this.opt,_globaloptions);

// Options for internal use
    this.opt.snapbackspeed = 80;

    if (myopt||false) {
      for(var a in self.opt) {
        if (typeof myopt[a] != "undefined") self.opt[a] = myopt[a];
      }
    }

    this.doc = self.opt.doc;
    this.iddoc = (this.doc&&this.doc[0])?this.doc[0].id||'':'';
    this.ispage = /BODY|HTML/.test((self.opt.win)?self.opt.win[0].nodeName:this.doc[0].nodeName);
    this.haswrapper = (self.opt.win!==false);
    this.win = self.opt.win||(this.ispage?$(window):this.doc);
    this.docscroll = (this.ispage&&!this.haswrapper)?$(window):this.win;
    this.body = $("body");
    this.viewport = false;

    this.isfixed = false;

    this.iframe = false;
    this.isiframe = ((this.doc[0].nodeName == 'IFRAME') && (this.win[0].nodeName == 'IFRAME'));

    this.istextarea = (this.win[0].nodeName == 'TEXTAREA');

    this.forcescreen = false; //force to use screen position on events

    this.canshowonmouseevent = (self.opt.autohidemode!="scroll");

// Events jump table
    this.onmousedown = false;
    this.onmouseup = false;
    this.onmousemove = false;
    this.onmousewheel = false;
    this.onkeypress = false;
    this.ongesturezoom = false;
    this.onclick = false;

// Nicescroll custom events
    this.onscrollstart = false;
    this.onscrollend = false;
    this.onscrollcancel = false;

    this.onzoomin = false;
    this.onzoomout = false;

// Let's start!
    this.view = false;
    this.page = false;

    this.scroll = {x:0,y:0};
    this.scrollratio = {x:0,y:0};
    this.cursorheight = 20;
    this.scrollvaluemax = 0;

    this.checkrtlmode = false;

    this.scrollrunning = false;

    this.scrollmom = false;

    this.observer = false;
    this.observerremover = false;  // observer on parent for remove detection

    do {
      this.id = "ascrail"+(ascrailcounter++);
    } while (document.getElementById(this.id));

    this.rail = false;
    this.cursor = false;
    this.cursorfreezed = false;
    this.selectiondrag = false;

    this.zoom = false;
    this.zoomactive = false;

    this.hasfocus = false;
    this.hasmousefocus = false;

    this.visibility = true;
    this.locked = false;
    this.hidden = false; // rails always hidden
    this.cursoractive = true; // user can interact with cursors

    this.overflowx = self.opt.overflowx;
    this.overflowy = self.opt.overflowy;

    this.nativescrollingarea = false;
    this.checkarea = 0;

    this.events = [];  // event list for unbind

    this.saved = {};

    this.delaylist = {};
    this.synclist = {};

    this.lastdeltax = 0;
    this.lastdeltay = 0;

    this.detected = getBrowserDetection();

    var cap = $.extend({},this.detected);

    this.canhwscroll = (cap.hastransform&&self.opt.hwacceleration);
    this.ishwscroll = (this.canhwscroll&&self.haswrapper);

    this.istouchcapable = false;  // desktop devices with touch screen support

//## Check Chrome desktop with touch support
    if (cap.cantouch&&cap.ischrome&&!cap.isios&&!cap.isandroid) {
      this.istouchcapable = true;
      cap.cantouch = false;  // parse normal desktop events
    }

//## Firefox 18 nightly build (desktop) false positive (or desktop with touch support)
    if (cap.cantouch&&cap.ismozilla&&!cap.isios) {
      this.istouchcapable = true;
      cap.cantouch = false;  // parse normal desktop events
    }

//## disable MouseLock API on user request

    if (!self.opt.enablemouselockapi) {
      cap.hasmousecapture = false;
      cap.haspointerlock = false;
    }

    this.delayed = function(name,fn,tm,lazy) {
      var dd = self.delaylist[name];
      var nw = (new Date()).getTime();
      if (!lazy&&dd&&dd.tt) return false;
      if (dd&&dd.tt) clearTimeout(dd.tt);
      if (dd&&dd.last+tm>nw&&!dd.tt) {
        self.delaylist[name] = {
          last:nw+tm,
          tt:setTimeout(function(){self.delaylist[name].tt=0;fn.call();},tm)
        }
      }
      else if (!dd||!dd.tt) {
        self.delaylist[name] = {
          last:nw,
          tt:0
        }
        setTimeout(function(){fn.call();},0);
      }
    };

    this.debounced = function(name,fn,tm) {
      var dd = self.delaylist[name];
      var nw = (new Date()).getTime();
      self.delaylist[name] = fn;
      if (!dd) {
        setTimeout(function(){var fn=self.delaylist[name];self.delaylist[name]=false;fn.call();},tm);
      }
    }

    this.synched = function(name,fn) {

      function requestSync() {
        if (self.onsync) return;
        setAnimationFrame(function(){
          self.onsync = false;
          for(name in self.synclist){
            var fn = self.synclist[name];
            if (fn) fn.call(self);
            self.synclist[name] = false;
          }
        });
        self.onsync = true;
      };

      self.synclist[name] = fn;
      requestSync();
      return name;
    };

    this.unsynched = function(name) {
      if (self.synclist[name]) self.synclist[name] = false;
    }

    this.css = function(el,pars) {  // save & set
      for(var n in pars) {
        self.saved.css.push([el,n,el.css(n)]);
        el.css(n,pars[n]);
      }
    };

    this.scrollTop = function(val) {
      return (typeof val == "undefined") ? self.getScrollTop() : self.setScrollTop(val);
    };

    this.scrollLeft = function(val) {
      return (typeof val == "undefined") ? self.getScrollLeft() : self.setScrollLeft(val);
    };

// derived by by Dan Pupius www.pupius.net
    BezierClass = function(st,ed,spd,p1,p2,p3,p4) {
      this.st = st;
      this.ed = ed;
      this.spd = spd;

      this.p1 = p1||0;
      this.p2 = p2||1;
      this.p3 = p3||0;
      this.p4 = p4||1;

      this.ts = (new Date()).getTime();
      this.df = this.ed-this.st;
    };
    BezierClass.prototype = {
      B2:function(t){ return 3*t*t*(1-t) },
      B3:function(t){ return 3*t*(1-t)*(1-t) },
      B4:function(t){ return (1-t)*(1-t)*(1-t) },
      getNow:function(){
        var nw = (new Date()).getTime();
        var pc = 1-((nw-this.ts)/this.spd);
        var bz = this.B2(pc) + this.B3(pc) + this.B4(pc);
        return (pc<0) ? this.ed : this.st+Math.round(this.df*bz);
      },
      update:function(ed,spd){
        this.st = this.getNow();
        this.ed = ed;
        this.spd = spd;
        this.ts = (new Date()).getTime();
        this.df = this.ed-this.st;
        return this;
      }
    };

    if (this.ishwscroll) {
    // hw accelerated scroll
      this.doc.translate = {x:0,y:0,tx:"0px",ty:"0px"};

      //this one can help to enable hw accel on ios6 http://indiegamr.com/ios6-html-hardware-acceleration-changes-and-how-to-fix-them/
      if (cap.hastranslate3d&&cap.isios) this.doc.css("-webkit-backface-visibility","hidden");  // prevent flickering http://stackoverflow.com/questions/3461441/

      //derived from http://stackoverflow.com/questions/11236090/
      function getMatrixValues() {
        var tr = self.doc.css(cap.trstyle);
        if (tr&&(tr.substr(0,6)=="matrix")) {
          return tr.replace(/^.*\((.*)\)$/g, "$1").replace(/px/g,'').split(/, +/);
        }
        return false;
      }

      this.getScrollTop = function(last) {
        if (!last) {
          var mtx = getMatrixValues();
          if (mtx) return (mtx.length==16) ? -mtx[13] : -mtx[5];  //matrix3d 16 on IE10
          if (self.timerscroll&&self.timerscroll.bz) return self.timerscroll.bz.getNow();
        }
        return self.doc.translate.y;
      };

      this.getScrollLeft = function(last) {
        if (!last) {
          var mtx = getMatrixValues();
          if (mtx) return (mtx.length==16) ? -mtx[12] : -mtx[4];  //matrix3d 16 on IE10
          if (self.timerscroll&&self.timerscroll.bh) return self.timerscroll.bh.getNow();
        }
        return self.doc.translate.x;
      };

      if (document.createEvent) {
        this.notifyScrollEvent = function(el) {
          var e = document.createEvent("UIEvents");
          e.initUIEvent("scroll", false, true, window, 1);
          el.dispatchEvent(e);
        };
      }
      else if (document.fireEvent) {
        this.notifyScrollEvent = function(el) {
          var e = document.createEventObject();
          el.fireEvent("onscroll");
          e.cancelBubble = true;
        };
      }
      else {
        this.notifyScrollEvent = function(el,add) {}; //NOPE
      }

      if (cap.hastranslate3d&&self.opt.enabletranslate3d) {
        this.setScrollTop = function(val,silent) {
          self.doc.translate.y = val;
          self.doc.translate.ty = (val*-1)+"px";
          self.doc.css(cap.trstyle,"translate3d("+self.doc.translate.tx+","+self.doc.translate.ty+",0px)");
          if (!silent) self.notifyScrollEvent(self.win[0]);
        };
        this.setScrollLeft = function(val,silent) {
          self.doc.translate.x = val;
          self.doc.translate.tx = (val*-1)+"px";
          self.doc.css(cap.trstyle,"translate3d("+self.doc.translate.tx+","+self.doc.translate.ty+",0px)");
          if (!silent) self.notifyScrollEvent(self.win[0]);
        };
      } else {
        this.setScrollTop = function(val,silent) {
          self.doc.translate.y = val;
          self.doc.translate.ty = (val*-1)+"px";
          self.doc.css(cap.trstyle,"translate("+self.doc.translate.tx+","+self.doc.translate.ty+")");
          if (!silent) self.notifyScrollEvent(self.win[0]);
        };
        this.setScrollLeft = function(val,silent) {
          self.doc.translate.x = val;
          self.doc.translate.tx = (val*-1)+"px";
          self.doc.css(cap.trstyle,"translate("+self.doc.translate.tx+","+self.doc.translate.ty+")");
          if (!silent) self.notifyScrollEvent(self.win[0]);
        };
      }
    } else {
    // native scroll
      this.getScrollTop = function() {
        return self.docscroll.scrollTop();
      };
      this.setScrollTop = function(val) {
        return self.docscroll.scrollTop(val);
      };
      this.getScrollLeft = function() {
        return self.docscroll.scrollLeft();
      };
      this.setScrollLeft = function(val) {
        return self.docscroll.scrollLeft(val);
      };
    }

    this.getTarget = function(e) {
      if (!e) return false;
      if (e.target) return e.target;
      if (e.srcElement) return e.srcElement;
      return false;
    };

    this.hasParent = function(e,id) {
      if (!e) return false;
      var el = e.target||e.srcElement||e||false;
      while (el && el.id != id) {
        el = el.parentNode||false;
      }
      return (el!==false);
    };

    function getZIndex() {
      var dom = self.win;
      if ("zIndex" in dom) return dom.zIndex();  // use jQuery UI method when available
      while (dom.length>0) {
        if (dom[0].nodeType==9) return false;
        var zi = dom.css('zIndex');
        if (!isNaN(zi)&&zi!=0) return parseInt(zi);
        dom = dom.parent();
      }
      return false;
    };

//inspired by http://forum.jquery.com/topic/width-includes-border-width-when-set-to-thin-medium-thick-in-ie
    var _convertBorderWidth = {"thin":1,"medium":3,"thick":5};
    function getWidthToPixel(dom,prop,chkheight) {
      var wd = dom.css(prop);
      var px = parseFloat(wd);
      if (isNaN(px)) {
        px = _convertBorderWidth[wd]||0;
        var brd = (px==3) ? ((chkheight)?(self.win.outerHeight() - self.win.innerHeight()):(self.win.outerWidth() - self.win.innerWidth())) : 1; //DON'T TRUST CSS
        if (self.isie8&&px) px+=1;
        return (brd) ? px : 0;
      }
      return px;
    };

    this.getOffset = function() {
      if (self.isfixed) return {top:parseFloat(self.win.css('top')),left:parseFloat(self.win.css('left'))};
      if (!self.viewport) return self.win.offset();
      var ww = self.win.offset();
      var vp = self.viewport.offset();
      return {top:ww.top-vp.top+self.viewport.scrollTop(),left:ww.left-vp.left+self.viewport.scrollLeft()};
    };

    this.updateScrollBar = function(len) {
      if (self.ishwscroll) {
        self.rail.css({height:self.win.innerHeight()});
        if (self.railh) self.railh.css({width:self.win.innerWidth()});
      } else {
        var wpos = self.getOffset();
        var pos = {top:wpos.top,left:wpos.left};
        pos.top+= getWidthToPixel(self.win,'border-top-width',true);
        var brd = (self.win.outerWidth() - self.win.innerWidth())/2;
        pos.left+= (self.rail.align) ? self.win.outerWidth() - getWidthToPixel(self.win,'border-right-width') - self.rail.width : getWidthToPixel(self.win,'border-left-width');

        var off = self.opt.railoffset;
        if (off) {
          if (off.top) pos.top+=off.top;
          if (self.rail.align&&off.left) pos.left+=off.left;
        }

                if (!self.locked) self.rail.css({top:pos.top,left:pos.left,height:(len)?len.h:self.win.innerHeight()});

                if (self.zoom) {
                  self.zoom.css({top:pos.top+1,left:(self.rail.align==1) ? pos.left-20 : pos.left+self.rail.width+4});
              }

                if (self.railh&&!self.locked) {
                    var pos = {top:wpos.top,left:wpos.left};
                    var y = (self.railh.align) ? pos.top + getWidthToPixel(self.win,'border-top-width',true) + self.win.innerHeight() - self.railh.height : pos.top + getWidthToPixel(self.win,'border-top-width',true);
                    var x = pos.left + getWidthToPixel(self.win,'border-left-width');
                    self.railh.css({top:y,left:x,width:self.railh.width});
                }


      }
    };

    this.doRailClick = function(e,dbl,hr) {

      var fn,pg,cur,pos;

//      if (self.rail.drag&&self.rail.drag.pt!=1) return;
      if (self.locked) return;
//      if (self.rail.drag) return;

//      self.cancelScroll();

      self.cancelEvent(e);

      if (dbl) {
        fn = (hr) ? self.doScrollLeft : self.doScrollTop;
        cur = (hr) ? ((e.pageX - self.railh.offset().left - (self.cursorwidth/2)) * self.scrollratio.x) : ((e.pageY - self.rail.offset().top - (self.cursorheight/2)) * self.scrollratio.y);
        fn(cur);
      } else {
//        console.log(e.pageY);
        fn = (hr) ? self.doScrollLeftBy : self.doScrollBy;
        cur = (hr) ? self.scroll.x : self.scroll.y;
        pos = (hr) ? e.pageX - self.railh.offset().left : e.pageY - self.rail.offset().top;
        pg = (hr) ? self.view.w : self.view.h;
        (cur>=pos) ? fn(pg) : fn(-pg);
      }

    }

    self.hasanimationframe = (setAnimationFrame);
    self.hascancelanimationframe = (clearAnimationFrame);

    if (!self.hasanimationframe) {
      setAnimationFrame=function(fn){return setTimeout(fn,15-Math.floor((+new Date)/1000)%16)}; // 1000/60)};
      clearAnimationFrame=clearInterval;
    }
    else if (!self.hascancelanimationframe) clearAnimationFrame=function(){self.cancelAnimationFrame=true};

    this.init = function() {

      self.saved.css = [];

      if (cap.isie7mobile) return true; // SORRY, DO NOT WORK!

      if (cap.hasmstouch) self.css((self.ispage)?$("html"):self.win,{'-ms-touch-action':'none'});

      self.zindex = "auto";
      if (!self.ispage&&self.opt.zindex=="auto") {
        self.zindex = getZIndex()||"auto";
      } else {
        self.zindex = self.opt.zindex;
      }

      if (!self.ispage&&self.zindex!="auto") {
        if (self.zindex>globalmaxzindex) globalmaxzindex=self.zindex;
      }

      if (self.isie&&self.zindex==0&&self.opt.zindex=="auto") {  // fix IE auto == 0
        self.zindex="auto";
      }

/*
      self.ispage = true;
      self.haswrapper = true;
//      self.win = $(window);
      self.docscroll = $("body");
//      self.doc = $("body");
*/

      if (!self.ispage || (!cap.cantouch && !cap.isieold && !cap.isie9mobile)) {

        var cont = self.docscroll;
        if (self.ispage) cont = (self.haswrapper)?self.win:self.doc;

        if (!cap.isie9mobile) self.css(cont,{'overflow-y':'hidden'});

        if (self.ispage&&cap.isie7) {
          if (self.doc[0].nodeName=='BODY') self.css($("html"),{'overflow-y':'hidden'});  //IE7 double scrollbar issue
          else if (self.doc[0].nodeName=='HTML') self.css($("body"),{'overflow-y':'hidden'});  //IE7 double scrollbar issue
        }

        if (cap.isios&&!self.ispage&&!self.haswrapper) self.css($("body"),{"-webkit-overflow-scrolling":"touch"});  //force hw acceleration

        var cursor = $(document.createElement('div'));
        cursor.css({
          position:"relative",top:0,"float":"right",width:self.opt.cursorwidth,height:"0px",
          'background-color':self.opt.cursorcolor,
          border:self.opt.cursorborder,
          'background-clip':'padding-box',
          '-webkit-border-radius':self.opt.cursorborderradius,
          '-moz-border-radius':self.opt.cursorborderradius,
          'border-radius':self.opt.cursorborderradius
        });

        cursor.hborder = parseFloat(cursor.outerHeight() - cursor.innerHeight());
        self.cursor = cursor;

        var rail = $(document.createElement('div'));
        rail.attr('id',self.id);
        rail.addClass('nicescroll-rails');

        var v,a,kp = ["left","right"];  //"top","bottom"
        for(var n in kp) {
          a=kp[n];
          v = self.opt.railpadding[a];
          (v) ? rail.css("padding-"+a,v+"px") : self.opt.railpadding[a] = 0;
        }

        rail.append(cursor);

        rail.width = Math.max(parseFloat(self.opt.cursorwidth),cursor.outerWidth()) + self.opt.railpadding['left'] + self.opt.railpadding['right'];
        rail.css({width:rail.width+"px",'zIndex':self.zindex,"background":self.opt.background,cursor:"default"});

        rail.visibility = true;
        rail.scrollable = true;

        rail.align = (self.opt.railalign=="left") ? 0 : 1;

        self.rail = rail;

        self.rail.drag = false;

        var zoom = false;
        if (self.opt.boxzoom&&!self.ispage&&!cap.isieold) {
          zoom = document.createElement('div');
          self.bind(zoom,"click",self.doZoom);
          self.zoom = $(zoom);
          self.zoom.css({"cursor":"pointer",'z-index':self.zindex,'backgroundImage':'url('+scriptpath+'zoomico.png)','height':18,'width':18,'backgroundPosition':'0px 0px'});
          if (self.opt.dblclickzoom) self.bind(self.win,"dblclick",self.doZoom);
          if (cap.cantouch&&self.opt.gesturezoom) {
            self.ongesturezoom = function(e) {
              if (e.scale>1.5) self.doZoomIn(e);
              if (e.scale<0.8) self.doZoomOut(e);
              return self.cancelEvent(e);
            };
            self.bind(self.win,"gestureend",self.ongesturezoom);
          }
        }

// init HORIZ

        self.railh = false;

        if (self.opt.horizrailenabled) {

          self.css(cont,{'overflow-x':'hidden'});

          var cursor = $(document.createElement('div'));
          cursor.css({
            position:"relative",top:0,height:self.opt.cursorwidth,width:"0px",
            'background-color':self.opt.cursorcolor,
            border:self.opt.cursorborder,
            'background-clip':'padding-box',
            '-webkit-border-radius':self.opt.cursorborderradius,
            '-moz-border-radius':self.opt.cursorborderradius,
            'border-radius':self.opt.cursorborderradius
          });

          cursor.wborder = parseFloat(cursor.outerWidth() - cursor.innerWidth());
          self.cursorh = cursor;

          var railh = $(document.createElement('div'));
          railh.attr('id',self.id+'-hr');
          railh.addClass('nicescroll-rails');
          railh.height = Math.max(parseFloat(self.opt.cursorwidth),cursor.outerHeight());
          railh.css({height:railh.height+"px",'zIndex':self.zindex,"background":self.opt.background});

          railh.append(cursor);

          railh.visibility = true;
          railh.scrollable = true;

          railh.align = (self.opt.railvalign=="top") ? 0 : 1;

          self.railh = railh;

          self.railh.drag = false;

        }

//

        if (self.ispage) {
          rail.css({position:"fixed",top:"0px",height:"100%"});
          (rail.align) ? rail.css({right:"0px"}) : rail.css({left:"0px"});
          self.body.append(rail);
          if (self.railh) {
            railh.css({position:"fixed",left:"0px",width:"100%"});
            (railh.align) ? railh.css({bottom:"0px"}) : railh.css({top:"0px"});
            self.body.append(railh);
          }
        } else {
          if (self.ishwscroll) {
            if (self.win.css('position')=='static') self.css(self.win,{'position':'relative'});
            var bd = (self.win[0].nodeName == 'HTML') ? self.body : self.win;
            if (self.zoom) {
              self.zoom.css({position:"absolute",top:1,right:0,"margin-right":rail.width+4});
              bd.append(self.zoom);
            }
            rail.css({position:"absolute",top:0});
            (rail.align) ? rail.css({right:0}) : rail.css({left:0});
            bd.append(rail);
            if (railh) {
              railh.css({position:"absolute",left:0,bottom:0});
              (railh.align) ? railh.css({bottom:0}) : railh.css({top:0});
              bd.append(railh);
            }
          } else {
            self.isfixed = (self.win.css("position")=="fixed");
            var rlpos = (self.isfixed) ? "fixed" : "absolute";

            if (!self.isfixed) self.viewport = self.getViewport(self.win[0]);
            if (self.viewport) {
              self.body = self.viewport;
              if ((/relative|absolute/.test(self.viewport.css("position")))==false) self.css(self.viewport,{"position":"relative"});
            }

            rail.css({position:rlpos});
            if (self.zoom) self.zoom.css({position:rlpos});
            self.updateScrollBar();
            self.body.append(rail);
            if (self.zoom) self.body.append(self.zoom);
            if (self.railh) {
              railh.css({position:rlpos});
              self.body.append(railh);
            }
          }

          if (cap.isios) self.css(self.win,{'-webkit-tap-highlight-color':'rgba(0,0,0,0)','-webkit-touch-callout':'none'});  // prevent grey layer on click

                    if (cap.isie&&self.opt.disableoutline) self.win.attr("hideFocus","true");  // IE, prevent dotted rectangle on focused div
                    if (cap.iswebkit&&self.opt.disableoutline) self.win.css({"outline":"none"});

        }

        if (self.opt.autohidemode===false) {
          self.autohidedom = false;
          self.rail.css({opacity:self.opt.cursoropacitymax});
          if (self.railh) self.railh.css({opacity:self.opt.cursoropacitymax});
        }
        else if (self.opt.autohidemode===true) {
          self.autohidedom = $().add(self.rail);
          if (cap.isie8) self.autohidedom=self.autohidedom.add(self.cursor);
          if (self.railh) self.autohidedom=self.autohidedom.add(self.railh);
          if (self.railh&&cap.isie8) self.autohidedom=self.autohidedom.add(self.cursorh);
        }
        else if (self.opt.autohidemode=="scroll") {
          self.autohidedom = $().add(self.rail);
          if (self.railh) self.autohidedom=self.autohidedom.add(self.railh);
        }
        else if (self.opt.autohidemode=="cursor") {
          self.autohidedom = $().add(self.cursor);
          if (self.railh) self.autohidedom=self.autohidedom.add(self.cursorh);
        }
        else if (self.opt.autohidemode=="hidden") {
          self.autohidedom = false;
          self.hide();
          self.locked = false;
        }

        if (cap.isie9mobile) {

          self.scrollmom = new ScrollMomentumClass2D(self);

          /*
          var trace = function(msg) {
            var db = $("#debug");
            if (isNaN(msg)&&(typeof msg != "string")) {
              var x = [];
              for(var a in msg) {
                x.push(a+":"+msg[a]);
              }
              msg ="{"+x.join(",")+"}";
            }
            if (db.children().length>0) {
              db.children().eq(0).before("<div>"+msg+"</div>");
            } else {
              db.append("<div>"+msg+"</div>");
            }
          }
          window.onerror = function(msg,url,ln) {
            trace("ERR: "+msg+" at "+ln);
          }
*/

          self.onmangotouch = function(e) {
            var py = self.getScrollTop();
            var px = self.getScrollLeft();

            if ((py == self.scrollmom.lastscrolly)&&(px == self.scrollmom.lastscrollx)) return true;
//            $("#debug").html('DRAG:'+py);

            var dfy = py-self.mangotouch.sy;
            var dfx = px-self.mangotouch.sx;
            var df = Math.round(Math.sqrt(Math.pow(dfx,2)+Math.pow(dfy,2)));
            if (df==0) return;

            var dry = (dfy<0)?-1:1;
            var drx = (dfx<0)?-1:1;

            var tm = +new Date();
            if (self.mangotouch.lazy) clearTimeout(self.mangotouch.lazy);

            if (((tm-self.mangotouch.tm)>80)||(self.mangotouch.dry!=dry)||(self.mangotouch.drx!=drx)) {
//              trace('RESET+'+(tm-self.mangotouch.tm));
              self.scrollmom.stop();
              self.scrollmom.reset(px,py);
              self.mangotouch.sy = py;
              self.mangotouch.ly = py;
              self.mangotouch.sx = px;
              self.mangotouch.lx = px;
              self.mangotouch.dry = dry;
              self.mangotouch.drx = drx;
              self.mangotouch.tm = tm;
            } else {

              self.scrollmom.stop();
              self.scrollmom.update(self.mangotouch.sx-dfx,self.mangotouch.sy-dfy);
              var gap = tm - self.mangotouch.tm;
              self.mangotouch.tm = tm;

//              trace('MOVE:'+df+" - "+gap);

              var ds = Math.max(Math.abs(self.mangotouch.ly-py),Math.abs(self.mangotouch.lx-px));
              self.mangotouch.ly = py;
              self.mangotouch.lx = px;

              if (ds>2) {
                self.mangotouch.lazy = setTimeout(function(){
//                  trace('END:'+ds+'+'+gap);
                  self.mangotouch.lazy = false;
                  self.mangotouch.dry = 0;
                  self.mangotouch.drx = 0;
                  self.mangotouch.tm = 0;
                  self.scrollmom.doMomentum(30);
                },100);
              }
            }
          }

          var top = self.getScrollTop();
          var lef = self.getScrollLeft();
          self.mangotouch = {sy:top,ly:top,dry:0,sx:lef,lx:lef,drx:0,lazy:false,tm:0};

          self.bind(self.docscroll,"scroll",self.onmangotouch);

        } else {

          if (cap.cantouch||self.istouchcapable||self.opt.touchbehavior||cap.hasmstouch) {

            self.scrollmom = new ScrollMomentumClass2D(self);

            self.ontouchstart = function(e) {
              if (e.pointerType&&e.pointerType!=2) return false;

              if (!self.locked) {

                if (cap.hasmstouch) {
                  var tg = (e.target) ? e.target : false;
                  while (tg) {
                    var nc = $(tg).getNiceScroll();
                    if ((nc.length>0)&&(nc[0].me == self.me)) break;
                    if (nc.length>0) return false;
                    if ((tg.nodeName=='DIV')&&(tg.id==self.id)) break;
                    tg = (tg.parentNode) ? tg.parentNode : false;
                  }
                }

                self.cancelScroll();

                var tg = self.getTarget(e);

                if (tg) {
                  var skp = (/INPUT/i.test(tg.nodeName))&&(/range/i.test(tg.type));
                  if (skp) return self.stopPropagation(e);
                }

                if (!("clientX" in e) && ("changedTouches" in e)) {
                  e.clientX = e.changedTouches[0].clientX;
                  e.clientY = e.changedTouches[0].clientY;
                }

                if (self.forcescreen) {
                  var le = e;
                  var e = {"original":(e.original)?e.original:e};
                  e.clientX = le.screenX;
                  e.clientY = le.screenY;
                }

                self.rail.drag = {x:e.clientX,y:e.clientY,sx:self.scroll.x,sy:self.scroll.y,st:self.getScrollTop(),sl:self.getScrollLeft(),pt:2,dl:false};

                if (self.ispage||!self.opt.directionlockdeadzone) {
                  self.rail.drag.dl = "f";
                } else {

                  var view = {
                    w:$(window).width(),
                    h:$(window).height()
                  };

                  var page = {
                    w:Math.max(document.body.scrollWidth,document.documentElement.scrollWidth),
                    h:Math.max(document.body.scrollHeight,document.documentElement.scrollHeight)
                  }

                  var maxh = Math.max(0,page.h - view.h);
                  var maxw = Math.max(0,page.w - view.w);

                  if (!self.rail.scrollable&&self.railh.scrollable) self.rail.drag.ck = (maxh>0) ? "v" : false;
                  else if (self.rail.scrollable&&!self.railh.scrollable) self.rail.drag.ck = (maxw>0) ? "h" : false;
                  else self.rail.drag.ck = false;
                  if (!self.rail.drag.ck) self.rail.drag.dl = "f";
                }

                if (self.opt.touchbehavior&&self.isiframe&&cap.isie) {
                  var wp = self.win.position();
                  self.rail.drag.x+=wp.left;
                  self.rail.drag.y+=wp.top;
                }

                self.hasmoving = false;
                self.lastmouseup = false;
                self.scrollmom.reset(e.clientX,e.clientY);
                if (!cap.cantouch&&!this.istouchcapable&&!cap.hasmstouch) {

                  var ip = (tg)?/INPUT|SELECT|TEXTAREA/i.test(tg.nodeName):false;
                  if (!ip) {
                    if (!self.ispage&&cap.hasmousecapture) tg.setCapture();
                    return self.cancelEvent(e);
                  }
                  if (/SUBMIT|CANCEL|BUTTON/i.test($(tg).attr('type'))) {
                    pc = {"tg":tg,"click":false};
                    self.preventclick = pc;
                  }

                }
              }

            };

            self.ontouchend = function(e) {
              if (e.pointerType&&e.pointerType!=2) return false;
              if (self.rail.drag&&(self.rail.drag.pt==2)) {
                self.scrollmom.doMomentum();
                self.rail.drag = false;
                if (self.hasmoving) {
                  self.hasmoving = false;
                  self.lastmouseup = true;
                  self.hideCursor();
                  if (cap.hasmousecapture) document.releaseCapture();
                  if (!cap.cantouch) return self.cancelEvent(e);
                }
              }

            };

            var moveneedoffset = (self.opt.touchbehavior&&self.isiframe&&!cap.hasmousecapture);

            self.ontouchmove = function(e,byiframe) {

              if (e.pointerType&&e.pointerType!=2) return false;

              if (self.rail.drag&&(self.rail.drag.pt==2)) {
                if (cap.cantouch&&(typeof e.original == "undefined")) return true;  // prevent ios "ghost" events by clickable elements

                self.hasmoving = true;

                if (self.preventclick&&!self.preventclick.click) {
                  self.preventclick.click = self.preventclick.tg.onclick||false;
                  self.preventclick.tg.onclick = self.onpreventclick;
                }

                var ev = $.extend({"original":e},e);
                e = ev;

                if (("changedTouches" in e)) {
                  e.clientX = e.changedTouches[0].clientX;
                  e.clientY = e.changedTouches[0].clientY;
                }

                if (self.forcescreen) {
                  var le = e;
                  var e = {"original":(e.original)?e.original:e};
                  e.clientX = le.screenX;
                  e.clientY = le.screenY;
                }

                var ofx = ofy = 0;

                if (moveneedoffset&&!byiframe) {
                  var wp = self.win.position();
                  ofx=-wp.left;
                  ofy=-wp.top;
                }

                var fy = e.clientY + ofy;
                var my = (fy-self.rail.drag.y);
                var fx = e.clientX + ofx;
                var mx = (fx-self.rail.drag.x);

                var ny = self.rail.drag.st-my;

                if (self.ishwscroll&&self.opt.bouncescroll) {
                  if (ny<0) {
                    ny = Math.round(ny/2);
//                    fy = 0;
                  }
                  else if (ny>self.page.maxh) {
                    ny = self.page.maxh+Math.round((ny-self.page.maxh)/2);
//                    fy = 0;
                  }
                } else {
                  if (ny<0) {ny=0;fy=0}
                  if (ny>self.page.maxh) {ny=self.page.maxh;fy=0}
                }

                if (self.railh&&self.railh.scrollable) {
                  var nx = self.rail.drag.sl-mx;

                  if (self.ishwscroll&&self.opt.bouncescroll) {
                    if (nx<0) {
                      nx = Math.round(nx/2);
//                      fx = 0;
                    }
                    else if (nx>self.page.maxw) {
                      nx = self.page.maxw+Math.round((nx-self.page.maxw)/2);
//                      fx = 0;
                    }
                  } else {
                    if (nx<0) {nx=0;fx=0}
                    if (nx>self.page.maxw) {nx=self.page.maxw;fx=0}
                  }

                }

                var grabbed = false;
                if (self.rail.drag.dl) {
                  grabbed = true;
                  if (self.rail.drag.dl=="v") nx = self.rail.drag.sl;
                  else if (self.rail.drag.dl=="h") ny = self.rail.drag.st;
                } else {
                  var ay = Math.abs(my);
                  var ax = Math.abs(mx);
                  var dz = self.opt.directionlockdeadzone;
                  if (self.rail.drag.ck=="v") {
                    if (ay>dz&&(ax<=(ay*0.3))) {
                      self.rail.drag = false;
                      return true;
                    }
                    else if (ax>dz) {
                      self.rail.drag.dl="f";
                      $("body").scrollTop($("body").scrollTop());  // stop iOS native scrolling (when active javascript has blocked)
                    }
                  }
                  else if (self.rail.drag.ck=="h") {
                    if (ax>dz&&(ay<=(az*0.3))) {
                      self.rail.drag = false;
                      return true;
                    }
                    else if (ay>dz) {
                      self.rail.drag.dl="f";
                      $("body").scrollLeft($("body").scrollLeft());  // stop iOS native scrolling (when active javascript has blocked)
                    }
                  }
                }

                self.synched("touchmove",function(){
                  if (self.rail.drag&&(self.rail.drag.pt==2)) {
                    if (self.prepareTransition) self.prepareTransition(0);
                    if (self.rail.scrollable) self.setScrollTop(ny);
                    self.scrollmom.update(fx,fy);
                    if (self.railh&&self.railh.scrollable) {
                      self.setScrollLeft(nx);
                      self.showCursor(ny,nx);
                    } else {
                      self.showCursor(ny);
                    }
                    if (cap.isie10) document.selection.clear();
                  }
                });

                if (cap.ischrome&&self.istouchcapable) grabbed=false;  //chrome touch emulation doesn't like!
                if (grabbed) return self.cancelEvent(e);
              }

            };

          }

          self.onmousedown = function(e,hronly) {
            if (self.rail.drag&&self.rail.drag.pt!=1) return;
            if (self.locked) return self.cancelEvent(e);
            self.cancelScroll();
            self.rail.drag = {x:e.clientX,y:e.clientY,sx:self.scroll.x,sy:self.scroll.y,pt:1,hr:(!!hronly)};
            var tg = self.getTarget(e);
            if (!self.ispage&&cap.hasmousecapture) tg.setCapture();
            if (self.isiframe&&!cap.hasmousecapture) {
              self.saved["csspointerevents"] = self.doc.css("pointer-events");
              self.css(self.doc,{"pointer-events":"none"});
            }
            return self.cancelEvent(e);
          };

          self.onmouseup = function(e) {
            if (self.rail.drag) {
              if (cap.hasmousecapture) document.releaseCapture();
              if (self.isiframe&&!cap.hasmousecapture) self.doc.css("pointer-events",self.saved["csspointerevents"]);
              if(self.rail.drag.pt!=1)return;
              self.rail.drag = false;
              //if (!self.rail.active) self.hideCursor();
              return self.cancelEvent(e);
            }
          };

          self.onmousemove = function(e) {
            if (self.rail.drag) {
              if(self.rail.drag.pt!=1)return;

              if (cap.ischrome&&e.which==0) return self.onmouseup(e);

              self.cursorfreezed = true;

              if (self.rail.drag.hr) {
                self.scroll.x = self.rail.drag.sx + (e.clientX-self.rail.drag.x);
                if (self.scroll.x<0) self.scroll.x=0;
                var mw = self.scrollvaluemaxw;
                if (self.scroll.x>mw) self.scroll.x=mw;
              } else {
                self.scroll.y = self.rail.drag.sy + (e.clientY-self.rail.drag.y);
                if (self.scroll.y<0) self.scroll.y=0;
                var my = self.scrollvaluemax;
                if (self.scroll.y>my) self.scroll.y=my;
              }

              self.synched('mousemove',function(){
                if (self.rail.drag&&(self.rail.drag.pt==1)) {
                  self.showCursor();
                  if (self.rail.drag.hr) self.doScrollLeft(Math.round(self.scroll.x*self.scrollratio.x),self.opt.cursordragspeed);
                  else self.doScrollTop(Math.round(self.scroll.y*self.scrollratio.y),self.opt.cursordragspeed);
                }
              });

              return self.cancelEvent(e);
            }
/*
            else {
              self.checkarea = true;
            }
*/
          };

          if (cap.cantouch||self.opt.touchbehavior) {

            self.onpreventclick = function(e) {
              if (self.preventclick) {
                self.preventclick.tg.onclick = self.preventclick.click;
                self.preventclick = false;
                return self.cancelEvent(e);
              }
            }

//            self.onmousedown = self.ontouchstart;
//            self.onmouseup = self.ontouchend;
//            self.onmousemove = self.ontouchmove;

            self.bind(self.win,"mousedown",self.ontouchstart);  // control content dragging

            self.onclick = (cap.isios) ? false : function(e) {
              if (self.lastmouseup) {
                self.lastmouseup = false;
                return self.cancelEvent(e);
              } else {
                return true;
              }
            };

            if (self.opt.grabcursorenabled&&cap.cursorgrabvalue) {
              self.css((self.ispage)?self.doc:self.win,{'cursor':cap.cursorgrabvalue});
              self.css(self.rail,{'cursor':cap.cursorgrabvalue});
            }

          } else {

            function checkSelectionScroll(e) {
              if (!self.selectiondrag) return;

              if (e) {
                var ww = self.win.outerHeight();
                var df = (e.pageY - self.selectiondrag.top);
                if (df>0&&df<ww) df=0;
                if (df>=ww) df-=ww;
                self.selectiondrag.df = df;
              }
              if (self.selectiondrag.df==0) return;

              var rt = -Math.floor(self.selectiondrag.df/6)*2;
//              self.doScrollTop(self.getScrollTop(true)+rt);
              self.doScrollBy(rt);

              self.debounced("doselectionscroll",function(){checkSelectionScroll()},50);
            }

            if ("getSelection" in document) {  // A grade - Major browsers
              self.hasTextSelected = function() {
                return (document.getSelection().rangeCount>0);
              }
            }
            else if ("selection" in document) {  //IE9-
              self.hasTextSelected = function() {
                return (document.selection.type != "None");
              }
            }
            else {
              self.hasTextSelected = function() {  // no support
                return false;
              }
            }

            self.onselectionstart = function(e) {
              if (self.ispage) return;
              self.selectiondrag = self.win.offset();
            }
            self.onselectionend = function(e) {
              self.selectiondrag = false;
            }
            self.onselectiondrag = function(e) {
              if (!self.selectiondrag) return;
              if (self.hasTextSelected()) self.debounced("selectionscroll",function(){checkSelectionScroll(e)},250);
            }


          }

          if (cap.hasmstouch) {
            self.css(self.rail,{'-ms-touch-action':'none'});
            self.css(self.cursor,{'-ms-touch-action':'none'});

            self.bind(self.win,"MSPointerDown",self.ontouchstart);
            self.bind(document,"MSPointerUp",self.ontouchend);
            self.bind(document,"MSPointerMove",self.ontouchmove);
            self.bind(self.cursor,"MSGestureHold",function(e){e.preventDefault()});
            self.bind(self.cursor,"contextmenu",function(e){e.preventDefault()});
          }

          if (this.istouchcapable) {  //desktop with screen touch enabled
            self.bind(self.win,"touchstart",self.ontouchstart);
            self.bind(document,"touchend",self.ontouchend);
            self.bind(document,"touchcancel",self.ontouchend);
            self.bind(document,"touchmove",self.ontouchmove);
          }

          self.bind(self.cursor,"mousedown",self.onmousedown);
          self.bind(self.cursor,"mouseup",self.onmouseup);

          if (self.railh) {
            self.bind(self.cursorh,"mousedown",function(e){self.onmousedown(e,true)});
            self.bind(self.cursorh,"mouseup",function(e){
              if (self.rail.drag&&self.rail.drag.pt==2) return;
              self.rail.drag = false;
              self.hasmoving = false;
              self.hideCursor();
              if (cap.hasmousecapture) document.releaseCapture();
              return self.cancelEvent(e);
            });
          }

          if (self.opt.cursordragontouch||!cap.cantouch&&!self.opt.touchbehavior) {

            self.rail.css({"cursor":"default"});
            self.railh&&self.railh.css({"cursor":"default"});

            self.jqbind(self.rail,"mouseenter",function() {
              if (self.canshowonmouseevent) self.showCursor();
              self.rail.active = true;
            });
            self.jqbind(self.rail,"mouseleave",function() {
              self.rail.active = false;
              if (!self.rail.drag) self.hideCursor();
            });

            if (self.opt.sensitiverail) {
              self.bind(self.rail,"click",function(e){self.doRailClick(e,false,false)});
              self.bind(self.rail,"dblclick",function(e){self.doRailClick(e,true,false)});
              self.bind(self.cursor,"click",function(e){self.cancelEvent(e)});
              self.bind(self.cursor,"dblclick",function(e){self.cancelEvent(e)});
            }

            if (self.railh) {
              self.jqbind(self.railh,"mouseenter",function() {
                if (self.canshowonmouseevent) self.showCursor();
                self.rail.active = true;
              });
              self.jqbind(self.railh,"mouseleave",function() {
                self.rail.active = false;
                if (!self.rail.drag) self.hideCursor();
              });

              if (self.opt.sensitiverail) {
                self.bind(self.railh, "click", function(e){self.doRailClick(e,false,true)});
                self.bind(self.railh, "dblclick", function(e){self.doRailClick(e, true, true) });
                self.bind(self.cursorh, "click", function (e) { self.cancelEvent(e) });
                self.bind(self.cursorh, "dblclick", function (e) { self.cancelEvent(e) });
              }

            }

          }

          if (!cap.cantouch&&!self.opt.touchbehavior) {

            self.bind((cap.hasmousecapture)?self.win:document,"mouseup",self.onmouseup);
            self.bind(document,"mousemove",self.onmousemove);
            if (self.onclick) self.bind(document,"click",self.onclick);

            if (!self.ispage&&self.opt.enablescrollonselection) {
              self.bind(self.win[0],"mousedown",self.onselectionstart);
              self.bind(document,"mouseup",self.onselectionend);
              self.bind(self.cursor,"mouseup",self.onselectionend);
              if (self.cursorh) self.bind(self.cursorh,"mouseup",self.onselectionend);
              self.bind(document,"mousemove",self.onselectiondrag);
            }

                        if (self.zoom) {
                            self.jqbind(self.zoom,"mouseenter",function() {
                                if (self.canshowonmouseevent) self.showCursor();
                                self.rail.active = true;
                            });
                            self.jqbind(self.zoom,"mouseleave",function() {
                                self.rail.active = false;
                                if (!self.rail.drag) self.hideCursor();
                            });
                        }

          } else {

            self.bind((cap.hasmousecapture)?self.win:document,"mouseup",self.ontouchend);
            self.bind(document,"mousemove",self.ontouchmove);
            if (self.onclick) self.bind(document,"click",self.onclick);

            if (self.opt.cursordragontouch) {
              self.bind(self.cursor,"mousedown",self.onmousedown);
              self.bind(self.cursor,"mousemove",self.onmousemove);
              self.cursorh&&self.bind(self.cursorh,"mousedown",self.onmousedown);
              self.cursorh&&self.bind(self.cursorh,"mousemove",self.onmousemove);
            }

          }

                    if (self.opt.enablemousewheel) {
                        if (!self.isiframe) self.bind((cap.isie&&self.ispage) ? document : self.docscroll,"mousewheel",self.onmousewheel);
                        self.bind(self.rail,"mousewheel",self.onmousewheel);
                        if (self.railh) self.bind(self.railh,"mousewheel",self.onmousewheelhr);
                    }

          if (!self.ispage&&!cap.cantouch&&!(/HTML|BODY/.test(self.win[0].nodeName))) {
            if (!self.win.attr("tabindex")) self.win.attr({"tabindex":tabindexcounter++});

            self.jqbind(self.win,"focus",function(e) {
              domfocus = (self.getTarget(e)).id||true;
              self.hasfocus = true;
              if (self.canshowonmouseevent) self.noticeCursor();
            });
            self.jqbind(self.win,"blur",function(e) {
              domfocus = false;
              self.hasfocus = false;
            });

            self.jqbind(self.win,"mouseenter",function(e) {
              mousefocus = (self.getTarget(e)).id||true;
              self.hasmousefocus = true;
              if (self.canshowonmouseevent) self.noticeCursor();
            });
            self.jqbind(self.win,"mouseleave",function() {
              mousefocus = false;
              self.hasmousefocus = false;
            });

          };

        }  // !ie9mobile

        //Thanks to http://www.quirksmode.org !!
        self.onkeypress = function(e) {
          if (self.locked&&self.page.maxh==0) return true;

          e = (e) ? e : window.e;
          var tg = self.getTarget(e);
          if (tg&&/INPUT|TEXTAREA|SELECT|OPTION/.test(tg.nodeName)) {
            var tp = tg.getAttribute('type')||tg.type||false;
            if ((!tp)||!(/submit|button|cancel/i.tp)) return true;
          }

          if (self.hasfocus||(self.hasmousefocus&&!domfocus)||(self.ispage&&!domfocus&&!mousefocus)) {
            var key = e.keyCode;

            if (self.locked&&key!=27) return self.cancelEvent(e);

            var ctrl = e.ctrlKey||false;
            var shift = e.shiftKey || false;

            var ret = false;
            switch (key) {
              case 38:
              case 63233: //safari
                self.doScrollBy(24*3);
                ret = true;
                break;
              case 40:
              case 63235: //safari
                self.doScrollBy(-24*3);
                ret = true;
                break;
              case 37:
              case 63232: //safari
                if (self.railh) {
                  (ctrl) ? self.doScrollLeft(0) : self.doScrollLeftBy(24*3);
                  ret = true;
                }
                break;
              case 39:
              case 63234: //safari
                if (self.railh) {
                  (ctrl) ? self.doScrollLeft(self.page.maxw) : self.doScrollLeftBy(-24*3);
                  ret = true;
                }
                break;
              case 33:
              case 63276: // safari
                self.doScrollBy(self.view.h);
                ret = true;
                break;
              case 34:
              case 63277: // safari
                self.doScrollBy(-self.view.h);
                ret = true;
                break;
              case 36:
              case 63273: // safari
                (self.railh&&ctrl) ? self.doScrollPos(0,0) : self.doScrollTo(0);
                ret = true;
                break;
              case 35:
              case 63275: // safari
                (self.railh&&ctrl) ? self.doScrollPos(self.page.maxw,self.page.maxh) : self.doScrollTo(self.page.maxh);
                ret = true;
                break;
              case 32:
                if (self.opt.spacebarenabled) {
                  (shift) ? self.doScrollBy(self.view.h) : self.doScrollBy(-self.view.h);
                  ret = true;
                }
                break;
              case 27: // ESC
                if (self.zoomactive) {
                  self.doZoom();
                  ret = true;
                }
                break;
            }
            if (ret) return self.cancelEvent(e);
          }
        };

        if (self.opt.enablekeyboard) self.bind(document,(cap.isopera&&!cap.isopera12)?"keypress":"keydown",self.onkeypress);

        self.bind(window,'resize',self.lazyResize);
        self.bind(window,'orientationchange',self.lazyResize);

        self.bind(window,"load",self.lazyResize);

        if (cap.ischrome&&!self.ispage&&!self.haswrapper) { //chrome void scrollbar bug - it persists in version 26
          var tmp=self.win.attr("style");
                    var ww = parseFloat(self.win.css("width"))+1;
          self.win.css('width',ww);
          self.synched("chromefix",function(){self.win.attr("style",tmp)});
        }


// Trying a cross-browser implementation - good luck!

        self.onAttributeChange = function(e) {
          self.lazyResize(250);
        }

        if (!self.ispage&&!self.haswrapper) {
          // redesigned MutationObserver for Chrome18+/Firefox14+/iOS6+ with support for: remove div, add/remove content
          if (clsMutationObserver !== false) {
            self.observer = new clsMutationObserver(function(mutations) {
              mutations.forEach(self.onAttributeChange);
            });
            self.observer.observe(self.win[0],{childList: true, characterData: false, attributes: true, subtree: false});

            self.observerremover = new clsMutationObserver(function(mutations) {
               mutations.forEach(function(mo){
                 if (mo.removedNodes.length>0) {
                   for (var dd in mo.removedNodes) {
                     if (mo.removedNodes[dd]==self.win[0]) return self.remove();
                   }
                 }
               });
            });
            self.observerremover.observe(self.win[0].parentNode,{childList: true, characterData: false, attributes: false, subtree: false});

          } else {
            self.bind(self.win,(cap.isie&&!cap.isie9)?"propertychange":"DOMAttrModified",self.onAttributeChange);
            if (cap.isie9) self.win[0].attachEvent("onpropertychange",self.onAttributeChange); //IE9 DOMAttrModified bug
            self.bind(self.win,"DOMNodeRemoved",function(e){
              if (e.target==self.win[0]) self.remove();
            });
          }
        }

//

        if (!self.ispage&&self.opt.boxzoom) self.bind(window,"resize",self.resizeZoom);
        if (self.istextarea) self.bind(self.win,"mouseup",self.lazyResize);

        self.checkrtlmode = true;
        self.lazyResize(30);

      }

      if (this.doc[0].nodeName == 'IFRAME') {
        function oniframeload(e) {
          self.iframexd = false;
          try {
            var doc = 'contentDocument' in this ? this.contentDocument : this.contentWindow.document;
            var a = doc.domain;
          } catch(e){self.iframexd = true;doc=false};

          if (self.iframexd) {
            if ("console" in window) console.log('NiceScroll error: policy restriced iframe');
            return true;  //cross-domain - I can't manage this
          }

          self.forcescreen = true;

          if (self.isiframe) {
            self.iframe = {
              "doc":$(doc),
              "html":self.doc.contents().find('html')[0],
              "body":self.doc.contents().find('body')[0]
            };
            self.getContentSize = function(){
              return {
                w:Math.max(self.iframe.html.scrollWidth,self.iframe.body.scrollWidth),
                h:Math.max(self.iframe.html.scrollHeight,self.iframe.body.scrollHeight)
              }
            }
            self.docscroll = $(self.iframe.body);//$(this.contentWindow);
          }

          if (!cap.isios&&self.opt.iframeautoresize&&!self.isiframe) {
            self.win.scrollTop(0); // reset position
            self.doc.height("");  //reset height to fix browser bug
            var hh=Math.max(doc.getElementsByTagName('html')[0].scrollHeight,doc.body.scrollHeight);
            self.doc.height(hh);
          }
          self.lazyResize(30);

          if (cap.isie7) self.css($(self.iframe.html),{'overflow-y':'hidden'});
          //self.css($(doc.body),{'overflow-y':'hidden'});
          self.css($(self.iframe.body),{'overflow-y':'hidden'});

          if ('contentWindow' in this) {
            self.bind(this.contentWindow,"scroll",self.onscroll);  //IE8 & minor
          } else {
            self.bind(doc,"scroll",self.onscroll);
          }

          if (self.opt.enablemousewheel) {
            self.bind(doc,"mousewheel",self.onmousewheel);
          }

          if (self.opt.enablekeyboard) self.bind(doc,(cap.isopera)?"keypress":"keydown",self.onkeypress);

          if (cap.cantouch||self.opt.touchbehavior) {
            self.bind(doc,"mousedown",self.onmousedown);
            self.bind(doc,"mousemove",function(e){self.onmousemove(e,true)});
            if (self.opt.grabcursorenabled&&cap.cursorgrabvalue) self.css($(doc.body),{'cursor':cap.cursorgrabvalue});
          }

          self.bind(doc,"mouseup",self.onmouseup);

          if (self.zoom) {
            if (self.opt.dblclickzoom) self.bind(doc,'dblclick',self.doZoom);
            if (self.ongesturezoom) self.bind(doc,"gestureend",self.ongesturezoom);
          }
        };

        if (this.doc[0].readyState&&this.doc[0].readyState=="complete"){
          setTimeout(function(){oniframeload.call(self.doc[0],false)},500);
        }
        self.bind(this.doc,"load",oniframeload);

      }

    };

    this.showCursor = function(py,px) {
      if (self.cursortimeout) {
        clearTimeout(self.cursortimeout);
        self.cursortimeout = 0;
      }
      if (!self.rail) return;
      if (self.autohidedom) {
        self.autohidedom.stop().css({opacity:self.opt.cursoropacitymax});
        self.cursoractive = true;
      }

      if (!self.rail.drag||self.rail.drag.pt!=1) {
        if ((typeof py != "undefined")&&(py!==false)) {
          self.scroll.y = Math.round(py * 1/self.scrollratio.y);
        }
        if (typeof px != "undefined") {
          self.scroll.x = Math.round(px * 1/self.scrollratio.x);
        }
      }

      self.cursor.css({height:self.cursorheight,top:self.scroll.y});
      if (self.cursorh) {
        (!self.rail.align&&self.rail.visibility) ? self.cursorh.css({width:self.cursorwidth,left:self.scroll.x+self.rail.width}) : self.cursorh.css({width:self.cursorwidth,left:self.scroll.x});
        self.cursoractive = true;
      }

      if (self.zoom) self.zoom.stop().css({opacity:self.opt.cursoropacitymax});
    };

    this.hideCursor = function(tm) {
      if (self.cursortimeout) return;
      if (!self.rail) return;
      if (!self.autohidedom) return;
      self.cursortimeout = setTimeout(function() {
         if (!self.rail.active||!self.showonmouseevent) {
           self.autohidedom.stop().animate({opacity:self.opt.cursoropacitymin});
           if (self.zoom) self.zoom.stop().animate({opacity:self.opt.cursoropacitymin});
           self.cursoractive = false;
         }
         self.cursortimeout = 0;
      },tm||self.opt.hidecursordelay);
    };

    this.noticeCursor = function(tm,py,px) {
      self.showCursor(py,px);
      if (!self.rail.active) self.hideCursor(tm);
    };

    this.getContentSize =
      (self.ispage) ?
        function(){
          return {
            w:Math.max(document.body.scrollWidth,document.documentElement.scrollWidth),
            h:Math.max(document.body.scrollHeight,document.documentElement.scrollHeight)
          }
        }
      : (self.haswrapper) ?
        function(){
          return {
            w:self.doc.outerWidth()+parseInt(self.win.css('paddingLeft'))+parseInt(self.win.css('paddingRight')),
            h:self.doc.outerHeight()+parseInt(self.win.css('paddingTop'))+parseInt(self.win.css('paddingBottom'))
          }
        }
      : function() {
        return {
          w:self.docscroll[0].scrollWidth,
          h:self.docscroll[0].scrollHeight
        }
      };

    this.onResize = function(e,page) {

      if (!self.win) return false;

      if (!self.haswrapper&&!self.ispage) {
        if (self.win.css('display')=='none') {
          if (self.visibility) self.hideRail().hideRailHr();
          return false;
        } else {
          if (!self.hidden&&!self.visibility) self.showRail().showRailHr();
        }
      }

      var premaxh = self.page.maxh;
      var premaxw = self.page.maxw;

      var preview = {h:self.view.h,w:self.view.w};

      self.view = {
        w:(self.ispage) ? self.win.width() : parseInt(self.win[0].clientWidth),
        h:(self.ispage) ? self.win.height() : parseInt(self.win[0].clientHeight)
      };

      self.page = (page) ? page : self.getContentSize();

      self.page.maxh = Math.max(0,self.page.h - self.view.h);
      self.page.maxw = Math.max(0,self.page.w - self.view.w);

      if ((self.page.maxh==premaxh)&&(self.page.maxw==premaxw)&&(self.view.w==preview.w)) {
        // test position
        if (!self.ispage) {
          var pos = self.win.offset();
          if (self.lastposition) {
            var lst = self.lastposition;
            if ((lst.top==pos.top)&&(lst.left==pos.left)) return self; //nothing to do
          }
          self.lastposition = pos;
        } else {
          return self; //nothing to do
        }
      }

      if (self.page.maxh==0) {
        self.hideRail();
        self.scrollvaluemax = 0;
        self.scroll.y = 0;
        self.scrollratio.y = 0;
        self.cursorheight = 0;
        self.setScrollTop(0);
        self.rail.scrollable = false;
      } else {
        self.rail.scrollable = true;
      }

      if (self.page.maxw==0) {
        self.hideRailHr();
        self.scrollvaluemaxw = 0;
        self.scroll.x = 0;
        self.scrollratio.x = 0;
        self.cursorwidth = 0;
        self.setScrollLeft(0);
        self.railh.scrollable = false;
      } else {
        self.railh.scrollable = true;
      }

      self.locked = (self.page.maxh==0)&&(self.page.maxw==0);
      if (self.locked) {
                if (!self.ispage) self.updateScrollBar(self.view);
              return false;
          }

      if (!self.hidden&&!self.visibility) {
        self.showRail().showRailHr();
      }
      else if (!self.hidden&&!self.railh.visibility) self.showRailHr();

      if (self.istextarea&&self.win.css('resize')&&self.win.css('resize')!='none') self.view.h-=20;

      self.cursorheight = Math.min(self.view.h,Math.round(self.view.h * (self.view.h / self.page.h)));
      self.cursorheight = (self.opt.cursorfixedheight) ? self.opt.cursorfixedheight : Math.max(self.opt.cursorminheight,self.cursorheight);

      self.cursorwidth = Math.min(self.view.w,Math.round(self.view.w * (self.view.w / self.page.w)));
      self.cursorwidth = (self.opt.cursorfixedheight) ? self.opt.cursorfixedheight : Math.max(self.opt.cursorminheight,self.cursorwidth);

      self.scrollvaluemax = self.view.h-self.cursorheight-self.cursor.hborder;

      if (self.railh) {
        self.railh.width = (self.page.maxh>0) ? (self.view.w-self.rail.width) : self.view.w;
        self.scrollvaluemaxw = self.railh.width-self.cursorwidth-self.cursorh.wborder;
      }

      if (self.checkrtlmode&&self.railh) {
        self.checkrtlmode = false;
        if (self.opt.rtlmode&&self.scroll.x==0) self.setScrollLeft(self.page.maxw);
      }

      if (!self.ispage) self.updateScrollBar(self.view);

      self.scrollratio = {
        x:(self.page.maxw/self.scrollvaluemaxw),
        y:(self.page.maxh/self.scrollvaluemax)
      };

      var sy = self.getScrollTop();
      if (sy>self.page.maxh) {
        self.doScrollTop(self.page.maxh);
      } else {
        self.scroll.y = Math.round(self.getScrollTop() * (1/self.scrollratio.y));
        self.scroll.x = Math.round(self.getScrollLeft() * (1/self.scrollratio.x));
        if (self.cursoractive) self.noticeCursor();
      }

      if (self.scroll.y&&(self.getScrollTop()==0)) self.doScrollTo(Math.floor(self.scroll.y*self.scrollratio.y));

      return self;
    };

    this.resize = self.onResize;

    this.lazyResize = function(tm) {   // event debounce
      tm = (isNaN(tm)) ? 30 : tm;
      self.delayed('resize',self.resize,tm);
      return self;
    }

// modified by MDN https://developer.mozilla.org/en-US/docs/DOM/Mozilla_event_reference/wheel
    function _modernWheelEvent(dom,name,fn,bubble) {
      self._bind(dom,name,function(e){
        var  e = (e) ? e : window.event;
        var event = {
          original: e,
          target: e.target || e.srcElement,
          type: "wheel",
          deltaMode: e.type == "MozMousePixelScroll" ? 0 : 1,
          deltaX: 0,
          deltaZ: 0,
          preventDefault: function() {
            e.preventDefault ? e.preventDefault() : e.returnValue = false;
            return false;
          },
          stopImmediatePropagation: function() {
            (e.stopImmediatePropagation) ? e.stopImmediatePropagation() : e.cancelBubble = true;
          }
        };

        if (name=="mousewheel") {
          event.deltaY = - 1/40 * e.wheelDelta;
          e.wheelDeltaX && (event.deltaX = - 1/40 * e.wheelDeltaX);
        } else {
          event.deltaY = e.detail;
        }

        return fn.call(dom,event);
      },bubble);
    };

    this._bind = function(el,name,fn,bubble) {  // primitive bind
      self.events.push({e:el,n:name,f:fn,b:bubble,q:false});
      if (el.addEventListener) {
        el.addEventListener(name,fn,bubble||false);
      }
      else if (el.attachEvent) {
        el.attachEvent("on"+name,fn);
      }
      else {
        el["on"+name] = fn;
      }
    };

    this.jqbind = function(dom,name,fn) {  // use jquery bind for non-native events (mouseenter/mouseleave)
      self.events.push({e:dom,n:name,f:fn,q:true});
      $(dom).bind(name,fn);
    }

    this.bind = function(dom,name,fn,bubble) {  // touch-oriented & fixing jquery bind
      var el = ("jquery" in dom) ? dom[0] : dom;

      if (name=='mousewheel') {
        if ("onwheel" in self.win) {
          self._bind(el,"wheel",fn,bubble||false);
        } else {
          var wname = (typeof document.onmousewheel != "undefined") ? "mousewheel" : "DOMMouseScroll";  // older IE/Firefox
          _modernWheelEvent(el,wname,fn,bubble||false);
          if (wname=="DOMMouseScroll") _modernWheelEvent(el,"MozMousePixelScroll",fn,bubble||false);  // Firefox legacy
        }
      }
      else if (el.addEventListener) {
        if (cap.cantouch && /mouseup|mousedown|mousemove/.test(name)) {  // touch device support
          var tt=(name=='mousedown')?'touchstart':(name=='mouseup')?'touchend':'touchmove';
          self._bind(el,tt,function(e){
            if (e.touches) {
              if (e.touches.length<2) {var ev=(e.touches.length)?e.touches[0]:e;ev.original=e;fn.call(this,ev);}
            }
            else if (e.changedTouches) {var ev=e.changedTouches[0];ev.original=e;fn.call(this,ev);}  //blackberry
          },bubble||false);
        }
        self._bind(el,name,fn,bubble||false);
        if (cap.cantouch && name=="mouseup") self._bind(el,"touchcancel",fn,bubble||false);
      }
      else {
        self._bind(el,name,function(e) {
          e = e||window.event||false;
          if (e) {
            if (e.srcElement) e.target=e.srcElement;
          }
          if (!("pageY" in e)) {
            e.pageX = e.clientX + document.documentElement.scrollLeft;
            e.pageY = e.clientY + document.documentElement.scrollTop;
          }
          return ((fn.call(el,e)===false)||bubble===false) ? self.cancelEvent(e) : true;
        });
      }
    };

    this._unbind = function(el,name,fn,bub) {  // primitive unbind
      if (el.removeEventListener) {
        el.removeEventListener(name,fn,bub);
      }
      else if (el.detachEvent) {
        el.detachEvent('on'+name,fn);
      } else {
        el['on'+name] = false;
      }
    };

    this.unbindAll = function() {
      for(var a=0;a<self.events.length;a++) {
        var r = self.events[a];
        (r.q) ? r.e.unbind(r.n,r.f) : self._unbind(r.e,r.n,r.f,r.b);
      }
    };

    // Thanks to http://www.switchonthecode.com !!
    this.cancelEvent = function(e) {
      var e = (e.original) ? e.original : (e) ? e : window.event||false;
      if (!e) return false;
      if(e.preventDefault) e.preventDefault();
      if(e.stopPropagation) e.stopPropagation();
      if(e.preventManipulation) e.preventManipulation();  //IE10
      e.cancelBubble = true;
      e.cancel = true;
      e.returnValue = false;
      return false;
    };

    this.stopPropagation = function(e) {
      var e = (e.original) ? e.original : (e) ? e : window.event||false;
      if (!e) return false;
      if (e.stopPropagation) return e.stopPropagation();
      if (e.cancelBubble) e.cancelBubble=true;
      return false;
    }

    this.showRail = function() {
      if ((self.page.maxh!=0)&&(self.ispage||self.win.css('display')!='none')) {
        self.visibility = true;
        self.rail.visibility = true;
        self.rail.css('display','block');
      }
      return self;
    };

    this.showRailHr = function() {
      if (!self.railh) return self;
      if ((self.page.maxw!=0)&&(self.ispage||self.win.css('display')!='none')) {
        self.railh.visibility = true;
        self.railh.css('display','block');
      }
      return self;
    };

    this.hideRail = function() {
      self.visibility = false;
      self.rail.visibility = false;
      self.rail.css('display','none');
      return self;
    };

    this.hideRailHr = function() {
      if (!self.railh) return self;
      self.railh.visibility = false;
      self.railh.css('display','none');
      return self;
    };

    this.show = function() {
      self.hidden = false;
      self.locked = false;
      return self.showRail().showRailHr();
    };

    this.hide = function() {
      self.hidden = true;
      self.locked = true;
      return self.hideRail().hideRailHr();
    };

    this.toggle = function() {
      return (self.hidden) ? self.show() : self.hide();
    };

    this.remove = function() {
      self.stop();
      if (self.cursortimeout) clearTimeout(self.cursortimeout);
      self.doZoomOut();
      self.unbindAll();
      if (self.observer !== false) self.observer.disconnect();
      if (self.observerremover !== false) self.observerremover.disconnect();
      self.events = [];
      if (self.cursor) {
        self.cursor.remove();
        self.cursor = null;
      }
      if (self.cursorh) {
        self.cursorh.remove();
        self.cursorh = null;
      }
      if (self.rail) {
        self.rail.remove();
        self.rail = null;
      }
      if (self.railh) {
        self.railh.remove();
        self.railh = null;
      }
      if (self.zoom) {
        self.zoom.remove();
        self.zoom = null;
      }
      for(var a=0;a<self.saved.css.length;a++) {
        var d=self.saved.css[a];
        d[0].css(d[1],(typeof d[2]=="undefined") ? '' : d[2]);
      }
      self.saved = false;
      self.me.data('__nicescroll',''); //erase all traces
      self.me = null;
      self.doc = null;
      self.docscroll = null;
      self.win = null;
      return self;
    };

    this.scrollstart = function(fn) {
      this.onscrollstart = fn;
      return self;
    }
    this.scrollend = function(fn) {
      this.onscrollend = fn;
      return self;
    }
    this.scrollcancel = function(fn) {
      this.onscrollcancel = fn;
      return self;
    }

    this.zoomin = function(fn) {
      this.onzoomin = fn;
      return self;
    }
    this.zoomout = function(fn) {
      this.onzoomout = fn;
      return self;
    }

    this.isScrollable = function(e) {
      var dom = (e.target) ? e.target : e;
      if (dom.nodeName == 'OPTION') return true;
      while (dom&&(dom.nodeType==1)&&!(/BODY|HTML/.test(dom.nodeName))) {
        var dd = $(dom);
        var ov = dd.css('overflowY')||dd.css('overflowX')||dd.css('overflow')||'';
        if (/scroll|auto/.test(ov)) return (dom.clientHeight!=dom.scrollHeight);
        dom = (dom.parentNode) ? dom.parentNode : false;
      }
      return false;
    };

    this.getViewport = function(me) {
      var dom = (me&&me.parentNode) ? me.parentNode : false;
      while (dom&&(dom.nodeType==1)&&!(/BODY|HTML/.test(dom.nodeName))) {
        var dd = $(dom);
        var ov = dd.css('overflowY')||dd.css('overflowX')||dd.css('overflow')||'';
        if ((/scroll|auto/.test(ov))&&(dom.clientHeight!=dom.scrollHeight)) return dd;
        if (dd.getNiceScroll().length>0) return dd;
        dom = (dom.parentNode) ? dom.parentNode : false;
      }
      return false;
    };

    function execScrollWheel(e,hr,chkscroll) {
      var px,py;
      var rt = 1;

      if (e.deltaMode==0) {  // PIXEL
        px = -Math.floor(e.deltaX*(self.opt.mousescrollstep/(18*3)));
        py = -Math.floor(e.deltaY*(self.opt.mousescrollstep/(18*3)));
      }
      else if (e.deltaMode==1) {  // LINE
        px = -Math.floor(e.deltaX*self.opt.mousescrollstep);
        py = -Math.floor(e.deltaY*self.opt.mousescrollstep);
      }

      if (hr&&(px==0)&&py) {  // classic vertical-only mousewheel + browser with x/y support
        px = py;
        py = 0;
      }

      if (px) {
        if (self.scrollmom) {self.scrollmom.stop()}
        self.lastdeltax+=px;
        self.debounced("mousewheelx",function(){var dt=self.lastdeltax;self.lastdeltax=0;if(!self.rail.drag){self.doScrollLeftBy(dt)}},120);
      }
      if (py) {
        if (self.opt.nativeparentscrolling&&chkscroll&&!self.ispage&&!self.zoomactive) {
          if (py<0) {
            if (self.getScrollTop()>=self.page.maxh) return true;
          } else {
            if (self.getScrollTop()<=0) return true;
          }
        }
        if (self.scrollmom) {self.scrollmom.stop()}
        self.lastdeltay+=py;
        self.debounced("mousewheely",function(){var dt=self.lastdeltay;self.lastdeltay=0;if(!self.rail.drag){self.doScrollBy(dt)}},120);
      }

      e.stopImmediatePropagation();
      return e.preventDefault();
//      return self.cancelEvent(e);
    };

    this.onmousewheel = function(e) {
      if (self.locked) return true;
      if (self.rail.drag) return self.cancelEvent(e);

      if (!self.rail.scrollable) {
        if (self.railh&&self.railh.scrollable) {
          return self.onmousewheelhr(e);
        } else {
          return true;
        }
      }

      var nw = +(new Date());
      var chk = false;
      if (self.opt.preservenativescrolling&&((self.checkarea+600)<nw)) {
//        self.checkarea = false;
        self.nativescrollingarea = self.isScrollable(e);
        chk = true;
      }
      self.checkarea = nw;
      if (self.nativescrollingarea) return true; // this isn't my business
//      if (self.locked) return self.cancelEvent(e);
      var ret = execScrollWheel(e,false,chk);
      if (ret) self.checkarea = 0;
      return ret;
    };

    this.onmousewheelhr = function(e) {
      if (self.locked||!self.railh.scrollable) return true;
      if (self.rail.drag) return self.cancelEvent(e);

      var nw = +(new Date());
      var chk = false;
      if (self.opt.preservenativescrolling&&((self.checkarea+600)<nw)) {
//        self.checkarea = false;
        self.nativescrollingarea = self.isScrollable(e);
        chk = true;
      }
      self.checkarea = nw;
      if (self.nativescrollingarea) return true; // this isn't my business
      if (self.locked) return self.cancelEvent(e);

      return execScrollWheel(e,true,chk);
    };

    this.stop = function() {
      self.cancelScroll();
      if (self.scrollmon) self.scrollmon.stop();
      self.cursorfreezed = false;
      self.scroll.y = Math.round(self.getScrollTop() * (1/self.scrollratio.y));
      self.noticeCursor();
      return self;
    };

    this.getTransitionSpeed = function(dif) {
      var sp = Math.round(self.opt.scrollspeed*10);
      var ex = Math.min(sp,Math.round((dif / 20) * self.opt.scrollspeed));
      return (ex>20) ? ex : 0;
    }

    if (!self.opt.smoothscroll) {
      this.doScrollLeft = function(x,spd) {  //direct
        var y = self.getScrollTop();
        self.doScrollPos(x,y,spd);
      }
      this.doScrollTop = function(y,spd) {   //direct
        var x = self.getScrollLeft();
        self.doScrollPos(x,y,spd);
      }
      this.doScrollPos = function(x,y,spd) {  //direct
        var nx = (x>self.page.maxw) ? self.page.maxw : x;
        if (nx<0) nx=0;
        var ny = (y>self.page.maxh) ? self.page.maxh : y;
        if (ny<0) ny=0;
        self.synched('scroll',function(){
          self.setScrollTop(ny);
          self.setScrollLeft(nx);
        });
      }
      this.cancelScroll = function() {}; // direct
    }
    else if (self.ishwscroll&&cap.hastransition&&self.opt.usetransition) {
      this.prepareTransition = function(dif,istime) {
        var ex = (istime) ? ((dif>20)?dif:0) : self.getTransitionSpeed(dif);
        var trans = (ex) ? cap.prefixstyle+'transform '+ex+'ms ease-out' : '';
        if (!self.lasttransitionstyle||self.lasttransitionstyle!=trans) {
          self.lasttransitionstyle = trans;
          self.doc.css(cap.transitionstyle,trans);
        }
        return ex;
      };

      this.doScrollLeft = function(x,spd) {  //trans
        var y = (self.scrollrunning) ? self.newscrolly : self.getScrollTop();
        self.doScrollPos(x,y,spd);
      }

      this.doScrollTop = function(y,spd) {   //trans
        var x = (self.scrollrunning) ? self.newscrollx : self.getScrollLeft();
        self.doScrollPos(x,y,spd);
      }

      this.doScrollPos = function(x,y,spd) {  //trans

        var py = self.getScrollTop();
        var px = self.getScrollLeft();

        if (((self.newscrolly-py)*(y-py)<0)||((self.newscrollx-px)*(x-px)<0)) self.cancelScroll();  //inverted movement detection

        if (self.opt.bouncescroll==false) {
          if (y<0) y=0;
          else if (y>self.page.maxh) y=self.page.maxh;
          if (x<0) x=0;
          else if (x>self.page.maxw) x=self.page.maxw;
        }

        if (self.scrollrunning&&x==self.newscrollx&&y==self.newscrolly) return false;

        self.newscrolly = y;
        self.newscrollx = x;

        self.newscrollspeed = spd||false;

        if (self.timer) return false;

        self.timer = setTimeout(function(){

          var top = self.getScrollTop();
          var lft = self.getScrollLeft();

          var dst = {};
          dst.x = x-lft;
          dst.y = y-top;
          dst.px = lft;
          dst.py = top;

          var dd = Math.round(Math.sqrt(Math.pow(dst.x,2)+Math.pow(dst.y,2)));

//          var df = (self.newscrollspeed) ? self.newscrollspeed : dd;

          var ms = (self.newscrollspeed && self.newscrollspeed>1) ? self.newscrollspeed : self.getTransitionSpeed(dd);
          if (self.newscrollspeed&&self.newscrollspeed<=1) ms*=self.newscrollspeed;

          self.prepareTransition(ms,true);

          if (self.timerscroll&&self.timerscroll.tm) clearInterval(self.timerscroll.tm);

          if (ms>0) {

            if (!self.scrollrunning&&self.onscrollstart) {
              var info = {"type":"scrollstart","current":{"x":lft,"y":top},"request":{"x":x,"y":y},"end":{"x":self.newscrollx,"y":self.newscrolly},"speed":ms};
              self.onscrollstart.call(self,info);
            }

            if (cap.transitionend) {
              if (!self.scrollendtrapped) {
                self.scrollendtrapped = true;
                self.bind(self.doc,cap.transitionend,self.onScrollEnd,false); //I have got to do something usefull!!
              }
            } else {
              if (self.scrollendtrapped) clearTimeout(self.scrollendtrapped);
              self.scrollendtrapped = setTimeout(self.onScrollEnd,ms);  // simulate transitionend event
            }

            var py = top;
            var px = lft;
            self.timerscroll = {
              bz: new BezierClass(py,self.newscrolly,ms,0,0,0.58,1),
              bh: new BezierClass(px,self.newscrollx,ms,0,0,0.58,1)
            };
            if (!self.cursorfreezed) self.timerscroll.tm=setInterval(function(){self.showCursor(self.getScrollTop(),self.getScrollLeft())},60);

          }

          self.synched("doScroll-set",function(){
            self.timer = 0;
            if (self.scrollendtrapped) self.scrollrunning = true;
            self.setScrollTop(self.newscrolly);
            self.setScrollLeft(self.newscrollx);
            if (!self.scrollendtrapped) self.onScrollEnd();
          });


        },50);

      };

      this.cancelScroll = function() {
        if (!self.scrollendtrapped) return true;
        var py = self.getScrollTop();
        var px = self.getScrollLeft();
        self.scrollrunning = false;
        if (!cap.transitionend) clearTimeout(cap.transitionend);
        self.scrollendtrapped = false;
        self._unbind(self.doc,cap.transitionend,self.onScrollEnd);
        self.prepareTransition(0);
        self.setScrollTop(py); // fire event onscroll
        if (self.railh) self.setScrollLeft(px);
        if (self.timerscroll&&self.timerscroll.tm) clearInterval(self.timerscroll.tm);
        self.timerscroll = false;

        self.cursorfreezed = false;

        //self.noticeCursor(false,py,px);
        self.showCursor(py,px);
        return self;
      };
      this.onScrollEnd = function() {
        if (self.scrollendtrapped) self._unbind(self.doc,cap.transitionend,self.onScrollEnd);
        self.scrollendtrapped = false;
        self.prepareTransition(0);
        if (self.timerscroll&&self.timerscroll.tm) clearInterval(self.timerscroll.tm);
        self.timerscroll = false;
        var py = self.getScrollTop();
        var px = self.getScrollLeft();
        self.setScrollTop(py);  // fire event onscroll
        if (self.railh) self.setScrollLeft(px);  // fire event onscroll left

        self.noticeCursor(false,py,px);

        self.cursorfreezed = false;

        if (py<0) py=0
        else if (py>self.page.maxh) py=self.page.maxh;
        if (px<0) px=0
        else if (px>self.page.maxw) px=self.page.maxw;
        if((py!=self.newscrolly)||(px!=self.newscrollx)) return self.doScrollPos(px,py,self.opt.snapbackspeed);

        if (self.onscrollend&&self.scrollrunning) {
          var info = {"type":"scrollend","current":{"x":px,"y":py},"end":{"x":self.newscrollx,"y":self.newscrolly}};
          self.onscrollend.call(self,info);
        }
        self.scrollrunning = false;

      };

    } else {

      this.doScrollLeft = function(x,spd) {  //no-trans
        var y = (self.scrollrunning) ? self.newscrolly : self.getScrollTop();
        self.doScrollPos(x,y,spd);
      }

      this.doScrollTop = function(y,spd) {  //no-trans
        var x = (self.scrollrunning) ? self.newscrollx : self.getScrollLeft();
        self.doScrollPos(x,y,spd);
      }

      this.doScrollPos = function(x,y,spd) {  //no-trans
        var y = ((typeof y == "undefined")||(y===false)) ? self.getScrollTop(true) : y;

        if  ((self.timer)&&(self.newscrolly==y)&&(self.newscrollx==x)) return true;

        if (self.timer) clearAnimationFrame(self.timer);
        self.timer = 0;

        var py = self.getScrollTop();
        var px = self.getScrollLeft();

        if (((self.newscrolly-py)*(y-py)<0)||((self.newscrollx-px)*(x-px)<0)) self.cancelScroll();  //inverted movement detection

        self.newscrolly = y;
        self.newscrollx = x;

        if (!self.bouncescroll||!self.rail.visibility) {
          if (self.newscrolly<0) {
            self.newscrolly = 0;
          }
          else if (self.newscrolly>self.page.maxh) {
            self.newscrolly = self.page.maxh;
          }
        }
        if (!self.bouncescroll||!self.railh.visibility) {
          if (self.newscrollx<0) {
            self.newscrollx = 0;
          }
          else if (self.newscrollx>self.page.maxw) {
            self.newscrollx = self.page.maxw;
          }
        }

        self.dst = {};
        self.dst.x = x-px;
        self.dst.y = y-py;
        self.dst.px = px;
        self.dst.py = py;

        var dst = Math.round(Math.sqrt(Math.pow(self.dst.x,2)+Math.pow(self.dst.y,2)));

        self.dst.ax = self.dst.x / dst;
        self.dst.ay = self.dst.y / dst;

        var pa = 0;
        var pe = dst;

        if (self.dst.x==0) {
          pa = py;
          pe = y;
          self.dst.ay = 1;
          self.dst.py = 0;
        } else if (self.dst.y==0) {
          pa = px;
          pe = x;
          self.dst.ax = 1;
          self.dst.px = 0;
        }

        var ms = self.getTransitionSpeed(dst);
        if (spd&&spd<=1) ms*=spd;
        if (ms>0) {
          self.bzscroll = (self.bzscroll) ? self.bzscroll.update(pe,ms) : new BezierClass(pa,pe,ms,0,1,0,1);
        } else {
          self.bzscroll = false;
        }

        if (self.timer) return;

        if ((py==self.page.maxh&&y>=self.page.maxh)||(px==self.page.maxw&&x>=self.page.maxw)) self.checkContentSize();

        var sync = 1;

        function scrolling() {
          if (self.cancelAnimationFrame) return true;

          self.scrollrunning = true;

          sync = 1-sync;
          if (sync) return (self.timer = setAnimationFrame(scrolling)||1);

          var done = 0;

          var sc = sy = self.getScrollTop();
          if (self.dst.ay) {
            sc = (self.bzscroll) ? self.dst.py + (self.bzscroll.getNow()*self.dst.ay) : self.newscrolly;
            var dr=sc-sy;
            if ((dr<0&&sc<self.newscrolly)||(dr>0&&sc>self.newscrolly)) sc = self.newscrolly;
            self.setScrollTop(sc);
            if (sc == self.newscrolly) done=1;
          } else {
            done=1;
          }

          var scx = sx = self.getScrollLeft();
          if (self.dst.ax) {
            scx = (self.bzscroll) ? self.dst.px + (self.bzscroll.getNow()*self.dst.ax) : self.newscrollx;
            var dr=scx-sx;
            if ((dr<0&&scx<self.newscrollx)||(dr>0&&scx>self.newscrollx)) scx = self.newscrollx;
            self.setScrollLeft(scx);
            if (scx == self.newscrollx) done+=1;
          } else {
            done+=1;
          }

          if (done==2) {
            self.timer = 0;
            self.cursorfreezed = false;
            self.bzscroll = false;
            self.scrollrunning = false;
            if (sc<0) sc=0;
            else if (sc>self.page.maxh) sc=self.page.maxh;
            if (scx<0) scx=0;
            else if (scx>self.page.maxw) scx=self.page.maxw;
            if ((scx!=self.newscrollx)||(sc!=self.newscrolly)) self.doScrollPos(scx,sc);
            else {
              if (self.onscrollend) {
                var info = {"type":"scrollend","current":{"x":sx,"y":sy},"end":{"x":self.newscrollx,"y":self.newscrolly}};
                self.onscrollend.call(self,info);
              }
            }
          } else {
            self.timer = setAnimationFrame(scrolling)||1;
          }
        };
        self.cancelAnimationFrame=false;
        self.timer = 1;

        if (self.onscrollstart&&!self.scrollrunning) {
          var info = {"type":"scrollstart","current":{"x":px,"y":py},"request":{"x":x,"y":y},"end":{"x":self.newscrollx,"y":self.newscrolly},"speed":ms};
          self.onscrollstart.call(self,info);
        }

        scrolling();

        if ((py==self.page.maxh&&y>=py)||(px==self.page.maxw&&x>=px)) self.checkContentSize();

        self.noticeCursor();
      };

      this.cancelScroll = function() {
        if (self.timer) clearAnimationFrame(self.timer);
        self.timer = 0;
        self.bzscroll = false;
        self.scrollrunning = false;
        return self;
      };

    }

    this.doScrollBy = function(stp,relative) {
      var ny = 0;
      if (relative) {
        ny = Math.floor((self.scroll.y-stp)*self.scrollratio.y)
      } else {
        var sy = (self.timer) ? self.newscrolly : self.getScrollTop(true);
        ny = sy-stp;
      }
      if (self.bouncescroll) {
        var haf = Math.round(self.view.h/2);
        if (ny<-haf) ny=-haf
        else if (ny>(self.page.maxh+haf)) ny = (self.page.maxh+haf);
      }
      self.cursorfreezed = false;

      py = self.getScrollTop(true);
      if (ny<0&&py<=0) return self.noticeCursor();
      else if (ny>self.page.maxh&&py>=self.page.maxh) {
        self.checkContentSize();
        return self.noticeCursor();
      }

      self.doScrollTop(ny);
    };

    this.doScrollLeftBy = function(stp,relative) {
      var nx = 0;
      if (relative) {
        nx = Math.floor((self.scroll.x-stp)*self.scrollratio.x)
      } else {
        var sx = (self.timer) ? self.newscrollx : self.getScrollLeft(true);
        nx = sx-stp;
      }
      if (self.bouncescroll) {
        var haf = Math.round(self.view.w/2);
        if (nx<-haf) nx=-haf
        else if (nx>(self.page.maxw+haf)) nx = (self.page.maxw+haf);
      }
      self.cursorfreezed = false;

      px = self.getScrollLeft(true);
      if (nx<0&&px<=0) return self.noticeCursor();
      else if (nx>self.page.maxw&&px>=self.page.maxw) return self.noticeCursor();

      self.doScrollLeft(nx);
    };

    this.doScrollTo = function(pos,relative) {
      var ny = (relative) ? Math.round(pos*self.scrollratio.y) : pos;
      if (ny<0) ny=0
      else if (ny>self.page.maxh) ny = self.page.maxh;
      self.cursorfreezed = false;
      self.doScrollTop(pos);
    };

    this.checkContentSize = function() {
      var pg = self.getContentSize();
      if ((pg.h!=self.page.h)||(pg.w!=self.page.w)) self.resize(false,pg);
    };

    self.onscroll = function(e) {
      if (self.rail.drag) return;
      if (!self.cursorfreezed) {
        self.synched('scroll',function(){
          self.scroll.y = Math.round(self.getScrollTop() * (1/self.scrollratio.y));
          if (self.railh) self.scroll.x = Math.round(self.getScrollLeft() * (1/self.scrollratio.x));
          self.noticeCursor();
        });
      }
    };
    self.bind(self.docscroll,"scroll",self.onscroll);

    this.doZoomIn = function(e) {
      if (self.zoomactive) return;
      self.zoomactive = true;

      self.zoomrestore = {
        style:{}
      };
      var lst = ['position','top','left','zIndex','backgroundColor','marginTop','marginBottom','marginLeft','marginRight'];
      var win = self.win[0].style;
      for(var a in lst) {
        var pp = lst[a];
        self.zoomrestore.style[pp] = (typeof win[pp] != "undefined") ? win[pp] : '';
      }

      self.zoomrestore.style.width = self.win.css('width');
      self.zoomrestore.style.height = self.win.css('height');

      self.zoomrestore.padding = {
        w:self.win.outerWidth()-self.win.width(),
        h:self.win.outerHeight()-self.win.height()
      };

      if (cap.isios4) {
        self.zoomrestore.scrollTop = $(window).scrollTop();
        $(window).scrollTop(0);
      }

      self.win.css({
        "position":(cap.isios4)?"absolute":"fixed",
        "top":0,
        "left":0,
        "z-index":globalmaxzindex+100,
        "margin":"0px"
      });
      var bkg = self.win.css("backgroundColor");
      if (bkg==""||/transparent|rgba\(0, 0, 0, 0\)|rgba\(0,0,0,0\)/.test(bkg)) self.win.css("backgroundColor","#fff");
      self.rail.css({"z-index":globalmaxzindex+101});
      self.zoom.css({"z-index":globalmaxzindex+102});
      self.zoom.css('backgroundPosition','0px -18px');
      self.resizeZoom();

      if (self.onzoomin) self.onzoomin.call(self);

      return self.cancelEvent(e);
    };

    this.doZoomOut = function(e) {
      if (!self.zoomactive) return;
      self.zoomactive = false;

      self.win.css("margin","");
      self.win.css(self.zoomrestore.style);

      if (cap.isios4) {
        $(window).scrollTop(self.zoomrestore.scrollTop);
      }

      self.rail.css({"z-index":self.zindex});
      self.zoom.css({"z-index":self.zindex});
      self.zoomrestore = false;
      self.zoom.css('backgroundPosition','0px 0px');
      self.onResize();

      if (self.onzoomout) self.onzoomout.call(self);

      return self.cancelEvent(e);
    };

    this.doZoom = function(e) {
      return (self.zoomactive) ? self.doZoomOut(e) : self.doZoomIn(e);
    };

    this.resizeZoom = function() {
      if (!self.zoomactive) return;

      var py = self.getScrollTop(); //preserve scrolling position
      self.win.css({
        width:$(window).width()-self.zoomrestore.padding.w+"px",
        height:$(window).height()-self.zoomrestore.padding.h+"px"
      });
      self.onResize();

      self.setScrollTop(Math.min(self.page.maxh,py));
    };

    this.init();

    $.nicescroll.push(this);

  };

// Inspired by the work of Kin Blas
// http://webpro.host.adobe.com/people/jblas/momentum/includes/jquery.momentum.0.7.js


  var ScrollMomentumClass2D = function(nc) {
    var self = this;
    this.nc = nc;

    this.lastx = 0;
    this.lasty = 0;
    this.speedx = 0;
    this.speedy = 0;
    this.lasttime = 0;
    this.steptime = 0;
    this.snapx = false;
    this.snapy = false;
    this.demulx = 0;
    this.demuly = 0;

    this.lastscrollx = -1;
    this.lastscrolly = -1;

    this.chkx = 0;
    this.chky = 0;

    this.timer = 0;

    this.time = function() {
      return +new Date();//beautifull hack
    };

    this.reset = function(px,py) {
      self.stop();
      var now = self.time();
      self.steptime = 0;
      self.lasttime = now;
      self.speedx = 0;
      self.speedy = 0;
      self.lastx = px;
      self.lasty = py;
      self.lastscrollx = -1;
      self.lastscrolly = -1;
    };

    this.update = function(px,py) {
      var now = self.time();
      self.steptime = now - self.lasttime;
      self.lasttime = now;
      var dy = py - self.lasty;
      var dx = px - self.lastx;
      var sy = self.nc.getScrollTop();
      var sx = self.nc.getScrollLeft();
      var newy = sy + dy;
      var newx = sx + dx;
      self.snapx = (newx<0)||(newx>self.nc.page.maxw);
      self.snapy = (newy<0)||(newy>self.nc.page.maxh);
      self.speedx = dx;
      self.speedy = dy;
      self.lastx = px;
      self.lasty = py;
    };

    this.stop = function() {
      self.nc.unsynched("domomentum2d");
      if (self.timer) clearTimeout(self.timer);
      self.timer = 0;
      self.lastscrollx = -1;
      self.lastscrolly = -1;
    };

    this.doSnapy = function(nx,ny) {
      var snap = false;

      if (ny<0) {
        ny=0;
        snap=true;
      }
      else if (ny>self.nc.page.maxh) {
        ny=self.nc.page.maxh;
        snap=true;
      }

      if (nx<0) {
        nx=0;
        snap=true;
      }
      else if (nx>self.nc.page.maxw) {
        nx=self.nc.page.maxw;
        snap=true;
      }

      if (snap) self.nc.doScrollPos(nx,ny,self.nc.opt.snapbackspeed);
    };

    this.doMomentum = function(gp) {
      var t = self.time();
      var l = (gp) ? t+gp : self.lasttime;

      var sl = self.nc.getScrollLeft();
      var st = self.nc.getScrollTop();

      var pageh = self.nc.page.maxh;
      var pagew = self.nc.page.maxw;

      self.speedx = (pagew>0) ? Math.min(60,self.speedx) : 0;
      self.speedy = (pageh>0) ? Math.min(60,self.speedy) : 0;

      var chk = l && (t - l) <= 50;

      if ((st<0)||(st>pageh)||(sl<0)||(sl>pagew)) chk = false;

      var sy = (self.speedy && chk) ? self.speedy : false;
      var sx = (self.speedx && chk) ? self.speedx : false;

      if (sy||sx) {
        var tm = Math.max(16,self.steptime); //timeout granularity

        if (tm>50) {  // do smooth
          var xm = tm/50;
          self.speedx*=xm;
          self.speedy*=xm;
          tm = 50;
        }

        self.demulxy = 0;

        self.lastscrollx = self.nc.getScrollLeft();
        self.chkx = self.lastscrollx;
        self.lastscrolly = self.nc.getScrollTop();
        self.chky = self.lastscrolly;

        var nx = self.lastscrollx;
        var ny = self.lastscrolly;

        var onscroll = function(){
          var df = ((self.time()-t)>600) ? 0.04 : 0.02;

          if (self.speedx) {
            nx = Math.floor(self.lastscrollx - (self.speedx*(1-self.demulxy)));
            self.lastscrollx = nx;
            if ((nx<0)||(nx>pagew)) df=0.10;
          }

          if (self.speedy) {
            ny = Math.floor(self.lastscrolly - (self.speedy*(1-self.demulxy)));
            self.lastscrolly = ny;
            if ((ny<0)||(ny>pageh)) df=0.10;
          }

          self.demulxy = Math.min(1,self.demulxy+df);

          self.nc.synched("domomentum2d",function(){

            if (self.speedx) {
              var scx = self.nc.getScrollLeft();
              if (scx!=self.chkx) self.stop();
              self.chkx=nx;
              self.nc.setScrollLeft(nx);
            }

            if (self.speedy) {
              var scy = self.nc.getScrollTop();
              if (scy!=self.chky) self.stop();
              self.chky=ny;
              self.nc.setScrollTop(ny);
            }

            if(!self.timer) {
              self.nc.hideCursor();
              self.doSnapy(nx,ny);
            }

          });

          if (self.demulxy<1) {
            self.timer = setTimeout(onscroll,tm);
          } else {
            self.stop();
            self.nc.hideCursor();
            self.doSnapy(nx,ny);
          }
        };

        onscroll();

      } else {
        self.doSnapy(self.nc.getScrollLeft(),self.nc.getScrollTop());
      }

    }

  };


// override jQuery scrollTop

  var _scrollTop = jQuery.fn.scrollTop; // preserve original function

  jQuery.cssHooks["pageYOffset"] = {
    get: function(elem,computed,extra) {
      var nice = $.data(elem,'__nicescroll')||false;
      return (nice&&nice.ishwscroll) ? nice.getScrollTop() : _scrollTop.call(elem);
    },
    set: function(elem,value) {
      var nice = $.data(elem,'__nicescroll')||false;
      (nice&&nice.ishwscroll) ? nice.setScrollTop(parseInt(value)) : _scrollTop.call(elem,value);
      return this;
    }
  };

/*
  $.fx.step["scrollTop"] = function(fx){
    $.cssHooks["scrollTop"].set( fx.elem, fx.now + fx.unit );
  };
*/

  jQuery.fn.scrollTop = function(value) {
    if (typeof value == "undefined") {
      var nice = (this[0]) ? $.data(this[0],'__nicescroll')||false : false;
      return (nice&&nice.ishwscroll) ? nice.getScrollTop() : _scrollTop.call(this);
    } else {
      return this.each(function() {
        var nice = $.data(this,'__nicescroll')||false;
        (nice&&nice.ishwscroll) ? nice.setScrollTop(parseInt(value)) : _scrollTop.call($(this),value);
      });
    }
  }

// override jQuery scrollLeft

  var _scrollLeft = jQuery.fn.scrollLeft; // preserve original function

  $.cssHooks.pageXOffset = {
    get: function(elem,computed,extra) {
      var nice = $.data(elem,'__nicescroll')||false;
      return (nice&&nice.ishwscroll) ? nice.getScrollLeft() : _scrollLeft.call(elem);
    },
    set: function(elem,value) {
      var nice = $.data(elem,'__nicescroll')||false;
      (nice&&nice.ishwscroll) ? nice.setScrollLeft(parseInt(value)) : _scrollLeft.call(elem,value);
      return this;
    }
  };

/*
  $.fx.step["scrollLeft"] = function(fx){
    $.cssHooks["scrollLeft"].set( fx.elem, fx.now + fx.unit );
  };
*/

  jQuery.fn.scrollLeft = function(value) {
    if (typeof value == "undefined") {
      var nice = (this[0]) ? $.data(this[0],'__nicescroll')||false : false;
      return (nice&&nice.ishwscroll) ? nice.getScrollLeft() : _scrollLeft.call(this);
    } else {
      return this.each(function() {
        var nice = $.data(this,'__nicescroll')||false;
        (nice&&nice.ishwscroll) ? nice.setScrollLeft(parseInt(value)) : _scrollLeft.call($(this),value);
      });
    }
  }

  var NiceScrollArray = function(doms) {
    var self = this;
    this.length = 0;
    this.name = "nicescrollarray";

    this.each = function(fn) {
      for(var a=0;a<self.length;a++) fn.call(self[a]);
      return self;
    };

    this.push = function(nice) {
      self[self.length]=nice;
      self.length++;
    };

    this.eq = function(idx) {
      return self[idx];
    };

    if (doms) {
      for(a=0;a<doms.length;a++) {
        var nice = $.data(doms[a],'__nicescroll')||false;
        if (nice) {
          this[this.length]=nice;
          this.length++;
        }
      };
    }

    return this;
  };

  function mplex(el,lst,fn) {
    for(var a=0;a<lst.length;a++) fn(el,lst[a]);
  };
  mplex(
    NiceScrollArray.prototype,
    ['show','hide','toggle','onResize','resize','remove','stop','doScrollPos'],
    function(e,n) {
      e[n] = function(){
        var args = arguments;
        return this.each(function(){
          this[n].apply(this,args);
        });
      };
    }
  );

  jQuery.fn.getNiceScroll = function(index) {
    if (typeof index == "undefined") {
      return new NiceScrollArray(this);
    } else {
      var nice = $.data(this[index],'__nicescroll')||false;
      return nice;
    }
  };

  jQuery.extend(jQuery.expr[':'], {
    nicescroll: function(a) {
      return ($.data(a,'__nicescroll'))?true:false;
    }
  });

  $.fn.niceScroll = function(wrapper,opt) {
    if (typeof opt=="undefined") {
      if ((typeof wrapper=="object")&&!("jquery" in wrapper)) {
        opt = wrapper;
        wrapper = false;
      }
    }
    var ret = new NiceScrollArray();
    if (typeof opt=="undefined") opt = {};

    if (wrapper||false) {
      opt.doc = $(wrapper);
      opt.win = $(this);
    }
    var docundef = !("doc" in opt);
    if (!docundef&&!("win" in opt)) opt.win = $(this);

    this.each(function() {
      var nice = $(this).data('__nicescroll')||false;
      if (!nice) {
        opt.doc = (docundef) ? $(this) : opt.doc;
        nice = new NiceScrollClass(opt,$(this));
        $(this).data('__nicescroll',nice);
      }
      ret.push(nice);
    });
    return (ret.length==1) ? ret[0] : ret;
  };

  window.NiceScroll = {
    getjQuery:function(){return jQuery}
  };

  if (!$.nicescroll) {
   $.nicescroll = new NiceScrollArray();
   $.nicescroll.options = _globaloptions;
  }

})( jQuery );

/*!
 * Bootstrap v3.1.1 (http://getbootstrap.com)
 * Copyright 2011-2014 Twitter, Inc.
 * Licensed under MIT (https://github.com/twbs/bootstrap/blob/master/LICENSE)
 */
if("undefined"==typeof jQuery)throw new Error("Bootstrap's JavaScript requires jQuery");+function(a){"use strict";function b(){var a=document.createElement("bootstrap"),b={WebkitTransition:"webkitTransitionEnd",MozTransition:"transitionend",OTransition:"oTransitionEnd otransitionend",transition:"transitionend"};for(var c in b)if(void 0!==a.style[c])return{end:b[c]};return!1}a.fn.emulateTransitionEnd=function(b){var c=!1,d=this;a(this).one(a.support.transition.end,function(){c=!0});var e=function(){c||a(d).trigger(a.support.transition.end)};return setTimeout(e,b),this},a(function(){a.support.transition=b()})}(jQuery),+function(a){"use strict";var b='[data-dismiss="alert"]',c=function(c){a(c).on("click",b,this.close)};c.prototype.close=function(b){function c(){f.trigger("closed.bs.alert").remove()}var d=a(this),e=d.attr("data-target");e||(e=d.attr("href"),e=e&&e.replace(/.*(?=#[^\s]*$)/,""));var f=a(e);b&&b.preventDefault(),f.length||(f=d.hasClass("alert")?d:d.parent()),f.trigger(b=a.Event("close.bs.alert")),b.isDefaultPrevented()||(f.removeClass("in"),a.support.transition&&f.hasClass("fade")?f.one(a.support.transition.end,c).emulateTransitionEnd(150):c())};var d=a.fn.alert;a.fn.alert=function(b){return this.each(function(){var d=a(this),e=d.data("bs.alert");e||d.data("bs.alert",e=new c(this)),"string"==typeof b&&e[b].call(d)})},a.fn.alert.Constructor=c,a.fn.alert.noConflict=function(){return a.fn.alert=d,this},a(document).on("click.bs.alert.data-api",b,c.prototype.close)}(jQuery),+function(a){"use strict";var b=function(c,d){this.$element=a(c),this.options=a.extend({},b.DEFAULTS,d),this.isLoading=!1};b.DEFAULTS={loadingText:"loading..."},b.prototype.setState=function(b){var c="disabled",d=this.$element,e=d.is("input")?"val":"html",f=d.data();b+="Text",f.resetText||d.data("resetText",d[e]()),d[e](f[b]||this.options[b]),setTimeout(a.proxy(function(){"loadingText"==b?(this.isLoading=!0,d.addClass(c).attr(c,c)):this.isLoading&&(this.isLoading=!1,d.removeClass(c).removeAttr(c))},this),0)},b.prototype.toggle=function(){var a=!0,b=this.$element.closest('[data-toggle="buttons"]');if(b.length){var c=this.$element.find("input");"radio"==c.prop("type")&&(c.prop("checked")&&this.$element.hasClass("active")?a=!1:b.find(".active").removeClass("active")),a&&c.prop("checked",!this.$element.hasClass("active")).trigger("change")}a&&this.$element.toggleClass("active")};var c=a.fn.button;a.fn.button=function(c){return this.each(function(){var d=a(this),e=d.data("bs.button"),f="object"==typeof c&&c;e||d.data("bs.button",e=new b(this,f)),"toggle"==c?e.toggle():c&&e.setState(c)})},a.fn.button.Constructor=b,a.fn.button.noConflict=function(){return a.fn.button=c,this},a(document).on("click.bs.button.data-api","[data-toggle^=button]",function(b){var c=a(b.target);c.hasClass("btn")||(c=c.closest(".btn")),c.button("toggle"),b.preventDefault()})}(jQuery),+function(a){"use strict";var b=function(b,c){this.$element=a(b),this.$indicators=this.$element.find(".carousel-indicators"),this.options=c,this.paused=this.sliding=this.interval=this.$active=this.$items=null,"hover"==this.options.pause&&this.$element.on("mouseenter",a.proxy(this.pause,this)).on("mouseleave",a.proxy(this.cycle,this))};b.DEFAULTS={interval:5e3,pause:"hover",wrap:!0},b.prototype.cycle=function(b){return b||(this.paused=!1),this.interval&&clearInterval(this.interval),this.options.interval&&!this.paused&&(this.interval=setInterval(a.proxy(this.next,this),this.options.interval)),this},b.prototype.getActiveIndex=function(){return this.$active=this.$element.find(".item.active"),this.$items=this.$active.parent().children(),this.$items.index(this.$active)},b.prototype.to=function(b){var c=this,d=this.getActiveIndex();return b>this.$items.length-1||0>b?void 0:this.sliding?this.$element.one("slid.bs.carousel",function(){c.to(b)}):d==b?this.pause().cycle():this.slide(b>d?"next":"prev",a(this.$items[b]))},b.prototype.pause=function(b){return b||(this.paused=!0),this.$element.find(".next, .prev").length&&a.support.transition&&(this.$element.trigger(a.support.transition.end),this.cycle(!0)),this.interval=clearInterval(this.interval),this},b.prototype.next=function(){return this.sliding?void 0:this.slide("next")},b.prototype.prev=function(){return this.sliding?void 0:this.slide("prev")},b.prototype.slide=function(b,c){var d=this.$element.find(".item.active"),e=c||d[b](),f=this.interval,g="next"==b?"left":"right",h="next"==b?"first":"last",i=this;if(!e.length){if(!this.options.wrap)return;e=this.$element.find(".item")[h]()}if(e.hasClass("active"))return this.sliding=!1;var j=a.Event("slide.bs.carousel",{relatedTarget:e[0],direction:g});return this.$element.trigger(j),j.isDefaultPrevented()?void 0:(this.sliding=!0,f&&this.pause(),this.$indicators.length&&(this.$indicators.find(".active").removeClass("active"),this.$element.one("slid.bs.carousel",function(){var b=a(i.$indicators.children()[i.getActiveIndex()]);b&&b.addClass("active")})),a.support.transition&&this.$element.hasClass("slide")?(e.addClass(b),e[0].offsetWidth,d.addClass(g),e.addClass(g),d.one(a.support.transition.end,function(){e.removeClass([b,g].join(" ")).addClass("active"),d.removeClass(["active",g].join(" ")),i.sliding=!1,setTimeout(function(){i.$element.trigger("slid.bs.carousel")},0)}).emulateTransitionEnd(1e3*d.css("transition-duration").slice(0,-1))):(d.removeClass("active"),e.addClass("active"),this.sliding=!1,this.$element.trigger("slid.bs.carousel")),f&&this.cycle(),this)};var c=a.fn.carousel;a.fn.carousel=function(c){return this.each(function(){var d=a(this),e=d.data("bs.carousel"),f=a.extend({},b.DEFAULTS,d.data(),"object"==typeof c&&c),g="string"==typeof c?c:f.slide;e||d.data("bs.carousel",e=new b(this,f)),"number"==typeof c?e.to(c):g?e[g]():f.interval&&e.pause().cycle()})},a.fn.carousel.Constructor=b,a.fn.carousel.noConflict=function(){return a.fn.carousel=c,this},a(document).on("click.bs.carousel.data-api","[data-slide], [data-slide-to]",function(b){var c,d=a(this),e=a(d.attr("data-target")||(c=d.attr("href"))&&c.replace(/.*(?=#[^\s]+$)/,"")),f=a.extend({},e.data(),d.data()),g=d.attr("data-slide-to");g&&(f.interval=!1),e.carousel(f),(g=d.attr("data-slide-to"))&&e.data("bs.carousel").to(g),b.preventDefault()}),a(window).on("load",function(){a('[data-ride="carousel"]').each(function(){var b=a(this);b.carousel(b.data())})})}(jQuery),+function(a){"use strict";var b=function(c,d){this.$element=a(c),this.options=a.extend({},b.DEFAULTS,d),this.transitioning=null,this.options.parent&&(this.$parent=a(this.options.parent)),this.options.toggle&&this.toggle()};b.DEFAULTS={toggle:!0},b.prototype.dimension=function(){var a=this.$element.hasClass("width");return a?"width":"height"},b.prototype.show=function(){if(!this.transitioning&&!this.$element.hasClass("in")){var b=a.Event("show.bs.collapse");if(this.$element.trigger(b),!b.isDefaultPrevented()){var c=this.$parent&&this.$parent.find("> .panel > .in");if(c&&c.length){var d=c.data("bs.collapse");if(d&&d.transitioning)return;c.collapse("hide"),d||c.data("bs.collapse",null)}var e=this.dimension();this.$element.removeClass("collapse").addClass("collapsing")[e](0),this.transitioning=1;var f=function(){this.$element.removeClass("collapsing").addClass("collapse in")[e]("auto"),this.transitioning=0,this.$element.trigger("shown.bs.collapse")};if(!a.support.transition)return f.call(this);var g=a.camelCase(["scroll",e].join("-"));this.$element.one(a.support.transition.end,a.proxy(f,this)).emulateTransitionEnd(350)[e](this.$element[0][g])}}},b.prototype.hide=function(){if(!this.transitioning&&this.$element.hasClass("in")){var b=a.Event("hide.bs.collapse");if(this.$element.trigger(b),!b.isDefaultPrevented()){var c=this.dimension();this.$element[c](this.$element[c]())[0].offsetHeight,this.$element.addClass("collapsing").removeClass("collapse").removeClass("in"),this.transitioning=1;var d=function(){this.transitioning=0,this.$element.trigger("hidden.bs.collapse").removeClass("collapsing").addClass("collapse")};return a.support.transition?void this.$element[c](0).one(a.support.transition.end,a.proxy(d,this)).emulateTransitionEnd(350):d.call(this)}}},b.prototype.toggle=function(){this[this.$element.hasClass("in")?"hide":"show"]()};var c=a.fn.collapse;a.fn.collapse=function(c){return this.each(function(){var d=a(this),e=d.data("bs.collapse"),f=a.extend({},b.DEFAULTS,d.data(),"object"==typeof c&&c);!e&&f.toggle&&"show"==c&&(c=!c),e||d.data("bs.collapse",e=new b(this,f)),"string"==typeof c&&e[c]()})},a.fn.collapse.Constructor=b,a.fn.collapse.noConflict=function(){return a.fn.collapse=c,this},a(document).on("click.bs.collapse.data-api","[data-toggle=collapse]",function(b){var c,d=a(this),e=d.attr("data-target")||b.preventDefault()||(c=d.attr("href"))&&c.replace(/.*(?=#[^\s]+$)/,""),f=a(e),g=f.data("bs.collapse"),h=g?"toggle":d.data(),i=d.attr("data-parent"),j=i&&a(i);g&&g.transitioning||(j&&j.find('[data-toggle=collapse][data-parent="'+i+'"]').not(d).addClass("collapsed"),d[f.hasClass("in")?"addClass":"removeClass"]("collapsed")),f.collapse(h)})}(jQuery),+function(a){"use strict";function b(b){a(d).remove(),a(e).each(function(){var d=c(a(this)),e={relatedTarget:this};d.hasClass("open")&&(d.trigger(b=a.Event("hide.bs.dropdown",e)),b.isDefaultPrevented()||d.removeClass("open").trigger("hidden.bs.dropdown",e))})}function c(b){var c=b.attr("data-target");c||(c=b.attr("href"),c=c&&/#[A-Za-z]/.test(c)&&c.replace(/.*(?=#[^\s]*$)/,""));var d=c&&a(c);return d&&d.length?d:b.parent()}var d=".dropdown-backdrop",e="[data-toggle=dropdown]",f=function(b){a(b).on("click.bs.dropdown",this.toggle)};f.prototype.toggle=function(d){var e=a(this);if(!e.is(".disabled, :disabled")){var f=c(e),g=f.hasClass("open");if(b(),!g){"ontouchstart"in document.documentElement&&!f.closest(".navbar-nav").length&&a('<div class="dropdown-backdrop"/>').insertAfter(a(this)).on("click",b);var h={relatedTarget:this};if(f.trigger(d=a.Event("show.bs.dropdown",h)),d.isDefaultPrevented())return;f.toggleClass("open").trigger("shown.bs.dropdown",h),e.focus()}return!1}},f.prototype.keydown=function(b){if(/(38|40|27)/.test(b.keyCode)){var d=a(this);if(b.preventDefault(),b.stopPropagation(),!d.is(".disabled, :disabled")){var f=c(d),g=f.hasClass("open");if(!g||g&&27==b.keyCode)return 27==b.which&&f.find(e).focus(),d.click();var h=" li:not(.divider):visible a",i=f.find("[role=menu]"+h+", [role=listbox]"+h);if(i.length){var j=i.index(i.filter(":focus"));38==b.keyCode&&j>0&&j--,40==b.keyCode&&j<i.length-1&&j++,~j||(j=0),i.eq(j).focus()}}}};var g=a.fn.dropdown;a.fn.dropdown=function(b){return this.each(function(){var c=a(this),d=c.data("bs.dropdown");d||c.data("bs.dropdown",d=new f(this)),"string"==typeof b&&d[b].call(c)})},a.fn.dropdown.Constructor=f,a.fn.dropdown.noConflict=function(){return a.fn.dropdown=g,this},a(document).on("click.bs.dropdown.data-api",b).on("click.bs.dropdown.data-api",".dropdown form",function(a){a.stopPropagation()}).on("click.bs.dropdown.data-api",e,f.prototype.toggle).on("keydown.bs.dropdown.data-api",e+", [role=menu], [role=listbox]",f.prototype.keydown)}(jQuery),+function(a){"use strict";var b=function(b,c){this.options=c,this.$element=a(b),this.$backdrop=this.isShown=null,this.options.remote&&this.$element.find(".modal-content").load(this.options.remote,a.proxy(function(){this.$element.trigger("loaded.bs.modal")},this))};b.DEFAULTS={backdrop:!0,keyboard:!0,show:!0},b.prototype.toggle=function(a){return this[this.isShown?"hide":"show"](a)},b.prototype.show=function(b){var c=this,d=a.Event("show.bs.modal",{relatedTarget:b});this.$element.trigger(d),this.isShown||d.isDefaultPrevented()||(this.isShown=!0,this.escape(),this.$element.on("click.dismiss.bs.modal",'[data-dismiss="modal"]',a.proxy(this.hide,this)),this.backdrop(function(){var d=a.support.transition&&c.$element.hasClass("fade");c.$element.parent().length||c.$element.appendTo(document.body),c.$element.show().scrollTop(0),d&&c.$element[0].offsetWidth,c.$element.addClass("in").attr("aria-hidden",!1),c.enforceFocus();var e=a.Event("shown.bs.modal",{relatedTarget:b});d?c.$element.find(".modal-dialog").one(a.support.transition.end,function(){c.$element.focus().trigger(e)}).emulateTransitionEnd(300):c.$element.focus().trigger(e)}))},b.prototype.hide=function(b){b&&b.preventDefault(),b=a.Event("hide.bs.modal"),this.$element.trigger(b),this.isShown&&!b.isDefaultPrevented()&&(this.isShown=!1,this.escape(),a(document).off("focusin.bs.modal"),this.$element.removeClass("in").attr("aria-hidden",!0).off("click.dismiss.bs.modal"),a.support.transition&&this.$element.hasClass("fade")?this.$element.one(a.support.transition.end,a.proxy(this.hideModal,this)).emulateTransitionEnd(300):this.hideModal())},b.prototype.enforceFocus=function(){a(document).off("focusin.bs.modal").on("focusin.bs.modal",a.proxy(function(a){this.$element[0]===a.target||this.$element.has(a.target).length||this.$element.focus()},this))},b.prototype.escape=function(){this.isShown&&this.options.keyboard?this.$element.on("keyup.dismiss.bs.modal",a.proxy(function(a){27==a.which&&this.hide()},this)):this.isShown||this.$element.off("keyup.dismiss.bs.modal")},b.prototype.hideModal=function(){var a=this;this.$element.hide(),this.backdrop(function(){a.removeBackdrop(),a.$element.trigger("hidden.bs.modal")})},b.prototype.removeBackdrop=function(){this.$backdrop&&this.$backdrop.remove(),this.$backdrop=null},b.prototype.backdrop=function(b){var c=this.$element.hasClass("fade")?"fade":"";if(this.isShown&&this.options.backdrop){var d=a.support.transition&&c;if(this.$backdrop=a('<div class="modal-backdrop '+c+'" />').appendTo(document.body),this.$element.on("click.dismiss.bs.modal",a.proxy(function(a){a.target===a.currentTarget&&("static"==this.options.backdrop?this.$element[0].focus.call(this.$element[0]):this.hide.call(this))},this)),d&&this.$backdrop[0].offsetWidth,this.$backdrop.addClass("in"),!b)return;d?this.$backdrop.one(a.support.transition.end,b).emulateTransitionEnd(150):b()}else!this.isShown&&this.$backdrop?(this.$backdrop.removeClass("in"),a.support.transition&&this.$element.hasClass("fade")?this.$backdrop.one(a.support.transition.end,b).emulateTransitionEnd(150):b()):b&&b()};var c=a.fn.modal;a.fn.modal=function(c,d){return this.each(function(){var e=a(this),f=e.data("bs.modal"),g=a.extend({},b.DEFAULTS,e.data(),"object"==typeof c&&c);f||e.data("bs.modal",f=new b(this,g)),"string"==typeof c?f[c](d):g.show&&f.show(d)})},a.fn.modal.Constructor=b,a.fn.modal.noConflict=function(){return a.fn.modal=c,this},a(document).on("click.bs.modal.data-api",'[data-toggle="modal"]',function(b){var c=a(this),d=c.attr("href"),e=a(c.attr("data-target")||d&&d.replace(/.*(?=#[^\s]+$)/,"")),f=e.data("bs.modal")?"toggle":a.extend({remote:!/#/.test(d)&&d},e.data(),c.data());c.is("a")&&b.preventDefault(),e.modal(f,this).one("hide",function(){c.is(":visible")&&c.focus()})}),a(document).on("show.bs.modal",".modal",function(){a(document.body).addClass("modal-open")}).on("hidden.bs.modal",".modal",function(){a(document.body).removeClass("modal-open")})}(jQuery),+function(a){"use strict";var b=function(a,b){this.type=this.options=this.enabled=this.timeout=this.hoverState=this.$element=null,this.init("tooltip",a,b)};b.DEFAULTS={animation:!0,placement:"top",selector:!1,template:'<div class="tooltip"><div class="tooltip-arrow"></div><div class="tooltip-inner"></div></div>',trigger:"hover focus",title:"",delay:0,html:!1,container:!1},b.prototype.init=function(b,c,d){this.enabled=!0,this.type=b,this.$element=a(c),this.options=this.getOptions(d);for(var e=this.options.trigger.split(" "),f=e.length;f--;){var g=e[f];if("click"==g)this.$element.on("click."+this.type,this.options.selector,a.proxy(this.toggle,this));else if("manual"!=g){var h="hover"==g?"mouseenter":"focusin",i="hover"==g?"mouseleave":"focusout";this.$element.on(h+"."+this.type,this.options.selector,a.proxy(this.enter,this)),this.$element.on(i+"."+this.type,this.options.selector,a.proxy(this.leave,this))}}this.options.selector?this._options=a.extend({},this.options,{trigger:"manual",selector:""}):this.fixTitle()},b.prototype.getDefaults=function(){return b.DEFAULTS},b.prototype.getOptions=function(b){return b=a.extend({},this.getDefaults(),this.$element.data(),b),b.delay&&"number"==typeof b.delay&&(b.delay={show:b.delay,hide:b.delay}),b},b.prototype.getDelegateOptions=function(){var b={},c=this.getDefaults();return this._options&&a.each(this._options,function(a,d){c[a]!=d&&(b[a]=d)}),b},b.prototype.enter=function(b){var c=b instanceof this.constructor?b:a(b.currentTarget)[this.type](this.getDelegateOptions()).data("bs."+this.type);return clearTimeout(c.timeout),c.hoverState="in",c.options.delay&&c.options.delay.show?void(c.timeout=setTimeout(function(){"in"==c.hoverState&&c.show()},c.options.delay.show)):c.show()},b.prototype.leave=function(b){var c=b instanceof this.constructor?b:a(b.currentTarget)[this.type](this.getDelegateOptions()).data("bs."+this.type);return clearTimeout(c.timeout),c.hoverState="out",c.options.delay&&c.options.delay.hide?void(c.timeout=setTimeout(function(){"out"==c.hoverState&&c.hide()},c.options.delay.hide)):c.hide()},b.prototype.show=function(){var b=a.Event("show.bs."+this.type);if(this.hasContent()&&this.enabled){if(this.$element.trigger(b),b.isDefaultPrevented())return;var c=this,d=this.tip();this.setContent(),this.options.animation&&d.addClass("fade");var e="function"==typeof this.options.placement?this.options.placement.call(this,d[0],this.$element[0]):this.options.placement,f=/\s?auto?\s?/i,g=f.test(e);g&&(e=e.replace(f,"")||"top"),d.detach().css({top:0,left:0,display:"block"}).addClass(e),this.options.container?d.appendTo(this.options.container):d.insertAfter(this.$element);var h=this.getPosition(),i=d[0].offsetWidth,j=d[0].offsetHeight;if(g){var k=this.$element.parent(),l=e,m=document.documentElement.scrollTop||document.body.scrollTop,n="body"==this.options.container?window.innerWidth:k.outerWidth(),o="body"==this.options.container?window.innerHeight:k.outerHeight(),p="body"==this.options.container?0:k.offset().left;e="bottom"==e&&h.top+h.height+j-m>o?"top":"top"==e&&h.top-m-j<0?"bottom":"right"==e&&h.right+i>n?"left":"left"==e&&h.left-i<p?"right":e,d.removeClass(l).addClass(e)}var q=this.getCalculatedOffset(e,h,i,j);this.applyPlacement(q,e),this.hoverState=null;var r=function(){c.$element.trigger("shown.bs."+c.type)};a.support.transition&&this.$tip.hasClass("fade")?d.one(a.support.transition.end,r).emulateTransitionEnd(150):r()}},b.prototype.applyPlacement=function(b,c){var d,e=this.tip(),f=e[0].offsetWidth,g=e[0].offsetHeight,h=parseInt(e.css("margin-top"),10),i=parseInt(e.css("margin-left"),10);isNaN(h)&&(h=0),isNaN(i)&&(i=0),b.top=b.top+h,b.left=b.left+i,a.offset.setOffset(e[0],a.extend({using:function(a){e.css({top:Math.round(a.top),left:Math.round(a.left)})}},b),0),e.addClass("in");var j=e[0].offsetWidth,k=e[0].offsetHeight;if("top"==c&&k!=g&&(d=!0,b.top=b.top+g-k),/bottom|top/.test(c)){var l=0;b.left<0&&(l=-2*b.left,b.left=0,e.offset(b),j=e[0].offsetWidth,k=e[0].offsetHeight),this.replaceArrow(l-f+j,j,"left")}else this.replaceArrow(k-g,k,"top");d&&e.offset(b)},b.prototype.replaceArrow=function(a,b,c){this.arrow().css(c,a?50*(1-a/b)+"%":"")},b.prototype.setContent=function(){var a=this.tip(),b=this.getTitle();a.find(".tooltip-inner")[this.options.html?"html":"text"](b),a.removeClass("fade in top bottom left right")},b.prototype.hide=function(){function b(){"in"!=c.hoverState&&d.detach(),c.$element.trigger("hidden.bs."+c.type)}var c=this,d=this.tip(),e=a.Event("hide.bs."+this.type);return this.$element.trigger(e),e.isDefaultPrevented()?void 0:(d.removeClass("in"),a.support.transition&&this.$tip.hasClass("fade")?d.one(a.support.transition.end,b).emulateTransitionEnd(150):b(),this.hoverState=null,this)},b.prototype.fixTitle=function(){var a=this.$element;(a.attr("title")||"string"!=typeof a.attr("data-original-title"))&&a.attr("data-original-title",a.attr("title")||"").attr("title","")},b.prototype.hasContent=function(){return this.getTitle()},b.prototype.getPosition=function(){var b=this.$element[0];return a.extend({},"function"==typeof b.getBoundingClientRect?b.getBoundingClientRect():{width:b.offsetWidth,height:b.offsetHeight},this.$element.offset())},b.prototype.getCalculatedOffset=function(a,b,c,d){return"bottom"==a?{top:b.top+b.height,left:b.left+b.width/2-c/2}:"top"==a?{top:b.top-d,left:b.left+b.width/2-c/2}:"left"==a?{top:b.top+b.height/2-d/2,left:b.left-c}:{top:b.top+b.height/2-d/2,left:b.left+b.width}},b.prototype.getTitle=function(){var a,b=this.$element,c=this.options;return a=b.attr("data-original-title")||("function"==typeof c.title?c.title.call(b[0]):c.title)},b.prototype.tip=function(){return this.$tip=this.$tip||a(this.options.template)},b.prototype.arrow=function(){return this.$arrow=this.$arrow||this.tip().find(".tooltip-arrow")},b.prototype.validate=function(){this.$element[0].parentNode||(this.hide(),this.$element=null,this.options=null)},b.prototype.enable=function(){this.enabled=!0},b.prototype.disable=function(){this.enabled=!1},b.prototype.toggleEnabled=function(){this.enabled=!this.enabled},b.prototype.toggle=function(b){var c=b?a(b.currentTarget)[this.type](this.getDelegateOptions()).data("bs."+this.type):this;c.tip().hasClass("in")?c.leave(c):c.enter(c)},b.prototype.destroy=function(){clearTimeout(this.timeout),this.hide().$element.off("."+this.type).removeData("bs."+this.type)};var c=a.fn.tooltip;a.fn.tooltip=function(c){return this.each(function(){var d=a(this),e=d.data("bs.tooltip"),f="object"==typeof c&&c;(e||"destroy"!=c)&&(e||d.data("bs.tooltip",e=new b(this,f)),"string"==typeof c&&e[c]())})},a.fn.tooltip.Constructor=b,a.fn.tooltip.noConflict=function(){return a.fn.tooltip=c,this}}(jQuery),+function(a){"use strict";var b=function(a,b){this.init("popover",a,b)};if(!a.fn.tooltip)throw new Error("Popover requires tooltip.js");b.DEFAULTS=a.extend({},a.fn.tooltip.Constructor.DEFAULTS,{placement:"right",trigger:"click",content:"",template:'<div class="popover"><div class="arrow"></div><h3 class="popover-title"></h3><div class="popover-content"></div></div>'}),b.prototype=a.extend({},a.fn.tooltip.Constructor.prototype),b.prototype.constructor=b,b.prototype.getDefaults=function(){return b.DEFAULTS},b.prototype.setContent=function(){var a=this.tip(),b=this.getTitle(),c=this.getContent();a.find(".popover-title")[this.options.html?"html":"text"](b),a.find(".popover-content")[this.options.html?"string"==typeof c?"html":"append":"text"](c),a.removeClass("fade top bottom left right in"),a.find(".popover-title").html()||a.find(".popover-title").hide()},b.prototype.hasContent=function(){return this.getTitle()||this.getContent()},b.prototype.getContent=function(){var a=this.$element,b=this.options;return a.attr("data-content")||("function"==typeof b.content?b.content.call(a[0]):b.content)},b.prototype.arrow=function(){return this.$arrow=this.$arrow||this.tip().find(".arrow")},b.prototype.tip=function(){return this.$tip||(this.$tip=a(this.options.template)),this.$tip};var c=a.fn.popover;a.fn.popover=function(c){return this.each(function(){var d=a(this),e=d.data("bs.popover"),f="object"==typeof c&&c;(e||"destroy"!=c)&&(e||d.data("bs.popover",e=new b(this,f)),"string"==typeof c&&e[c]())})},a.fn.popover.Constructor=b,a.fn.popover.noConflict=function(){return a.fn.popover=c,this}}(jQuery),+function(a){"use strict";function b(c,d){var e,f=a.proxy(this.process,this);this.$element=a(a(c).is("body")?window:c),this.$body=a("body"),this.$scrollElement=this.$element.on("scroll.bs.scroll-spy.data-api",f),this.options=a.extend({},b.DEFAULTS,d),this.selector=(this.options.target||(e=a(c).attr("href"))&&e.replace(/.*(?=#[^\s]+$)/,"")||"")+" .nav li > a",this.offsets=a([]),this.targets=a([]),this.activeTarget=null,this.refresh(),this.process()}b.DEFAULTS={offset:10},b.prototype.refresh=function(){var b=this.$element[0]==window?"offset":"position";this.offsets=a([]),this.targets=a([]);{var c=this;this.$body.find(this.selector).map(function(){var d=a(this),e=d.data("target")||d.attr("href"),f=/^#./.test(e)&&a(e);return f&&f.length&&f.is(":visible")&&[[f[b]().top+(!a.isWindow(c.$scrollElement.get(0))&&c.$scrollElement.scrollTop()),e]]||null}).sort(function(a,b){return a[0]-b[0]}).each(function(){c.offsets.push(this[0]),c.targets.push(this[1])})}},b.prototype.process=function(){var a,b=this.$scrollElement.scrollTop()+this.options.offset,c=this.$scrollElement[0].scrollHeight||this.$body[0].scrollHeight,d=c-this.$scrollElement.height(),e=this.offsets,f=this.targets,g=this.activeTarget;if(b>=d)return g!=(a=f.last()[0])&&this.activate(a);if(g&&b<=e[0])return g!=(a=f[0])&&this.activate(a);for(a=e.length;a--;)g!=f[a]&&b>=e[a]&&(!e[a+1]||b<=e[a+1])&&this.activate(f[a])},b.prototype.activate=function(b){this.activeTarget=b,a(this.selector).parentsUntil(this.options.target,".active").removeClass("active");var c=this.selector+'[data-target="'+b+'"],'+this.selector+'[href="'+b+'"]',d=a(c).parents("li").addClass("active");d.parent(".dropdown-menu").length&&(d=d.closest("li.dropdown").addClass("active")),d.trigger("activate.bs.scrollspy")};var c=a.fn.scrollspy;a.fn.scrollspy=function(c){return this.each(function(){var d=a(this),e=d.data("bs.scrollspy"),f="object"==typeof c&&c;e||d.data("bs.scrollspy",e=new b(this,f)),"string"==typeof c&&e[c]()})},a.fn.scrollspy.Constructor=b,a.fn.scrollspy.noConflict=function(){return a.fn.scrollspy=c,this},a(window).on("load",function(){a('[data-spy="scroll"]').each(function(){var b=a(this);b.scrollspy(b.data())})})}(jQuery),+function(a){"use strict";var b=function(b){this.element=a(b)};b.prototype.show=function(){var b=this.element,c=b.closest("ul:not(.dropdown-menu)"),d=b.data("target");if(d||(d=b.attr("href"),d=d&&d.replace(/.*(?=#[^\s]*$)/,"")),!b.parent("li").hasClass("active")){var e=c.find(".active:last a")[0],f=a.Event("show.bs.tab",{relatedTarget:e});if(b.trigger(f),!f.isDefaultPrevented()){var g=a(d);this.activate(b.parent("li"),c),this.activate(g,g.parent(),function(){b.trigger({type:"shown.bs.tab",relatedTarget:e})})}}},b.prototype.activate=function(b,c,d){function e(){f.removeClass("active").find("> .dropdown-menu > .active").removeClass("active"),b.addClass("active"),g?(b[0].offsetWidth,b.addClass("in")):b.removeClass("fade"),b.parent(".dropdown-menu")&&b.closest("li.dropdown").addClass("active"),d&&d()}var f=c.find("> .active"),g=d&&a.support.transition&&f.hasClass("fade");g?f.one(a.support.transition.end,e).emulateTransitionEnd(150):e(),f.removeClass("in")};var c=a.fn.tab;a.fn.tab=function(c){return this.each(function(){var d=a(this),e=d.data("bs.tab");e||d.data("bs.tab",e=new b(this)),"string"==typeof c&&e[c]()})},a.fn.tab.Constructor=b,a.fn.tab.noConflict=function(){return a.fn.tab=c,this},a(document).on("click.bs.tab.data-api",'[data-toggle="tab"], [data-toggle="pill"]',function(b){b.preventDefault(),a(this).tab("show")})}(jQuery),+function(a){"use strict";var b=function(c,d){this.options=a.extend({},b.DEFAULTS,d),this.$window=a(window).on("scroll.bs.affix.data-api",a.proxy(this.checkPosition,this)).on("click.bs.affix.data-api",a.proxy(this.checkPositionWithEventLoop,this)),this.$element=a(c),this.affixed=this.unpin=this.pinnedOffset=null,this.checkPosition()};b.RESET="affix affix-top affix-bottom",b.DEFAULTS={offset:0},b.prototype.getPinnedOffset=function(){if(this.pinnedOffset)return this.pinnedOffset;this.$element.removeClass(b.RESET).addClass("affix");var a=this.$window.scrollTop(),c=this.$element.offset();return this.pinnedOffset=c.top-a},b.prototype.checkPositionWithEventLoop=function(){setTimeout(a.proxy(this.checkPosition,this),1)},b.prototype.checkPosition=function(){if(this.$element.is(":visible")){var c=a(document).height(),d=this.$window.scrollTop(),e=this.$element.offset(),f=this.options.offset,g=f.top,h=f.bottom;"top"==this.affixed&&(e.top+=d),"object"!=typeof f&&(h=g=f),"function"==typeof g&&(g=f.top(this.$element)),"function"==typeof h&&(h=f.bottom(this.$element));var i=null!=this.unpin&&d+this.unpin<=e.top?!1:null!=h&&e.top+this.$element.height()>=c-h?"bottom":null!=g&&g>=d?"top":!1;if(this.affixed!==i){this.unpin&&this.$element.css("top","");var j="affix"+(i?"-"+i:""),k=a.Event(j+".bs.affix");this.$element.trigger(k),k.isDefaultPrevented()||(this.affixed=i,this.unpin="bottom"==i?this.getPinnedOffset():null,this.$element.removeClass(b.RESET).addClass(j).trigger(a.Event(j.replace("affix","affixed"))),"bottom"==i&&this.$element.offset({top:c-h-this.$element.height()}))}}};var c=a.fn.affix;a.fn.affix=function(c){return this.each(function(){var d=a(this),e=d.data("bs.affix"),f="object"==typeof c&&c;e||d.data("bs.affix",e=new b(this,f)),"string"==typeof c&&e[c]()})},a.fn.affix.Constructor=b,a.fn.affix.noConflict=function(){return a.fn.affix=c,this},a(window).on("load",function(){a('[data-spy="affix"]').each(function(){var b=a(this),c=b.data();c.offset=c.offset||{},c.offsetBottom&&(c.offset.bottom=c.offsetBottom),c.offsetTop&&(c.offset.top=c.offsetTop),b.affix(c)})})}(jQuery);


//! moment.js
//! version : 2.5.1
//! authors : Tim Wood, Iskren Chernev, Moment.js contributors
//! license : MIT
//! momentjs.com
(function(a){function b(){return{empty:!1,unusedTokens:[],unusedInput:[],overflow:-2,charsLeftOver:0,nullInput:!1,invalidMonth:null,invalidFormat:!1,userInvalidated:!1,iso:!1}}function c(a,b){return function(c){return k(a.call(this,c),b)}}function d(a,b){return function(c){return this.lang().ordinal(a.call(this,c),b)}}function e(){}function f(a){w(a),h(this,a)}function g(a){var b=q(a),c=b.year||0,d=b.month||0,e=b.week||0,f=b.day||0,g=b.hour||0,h=b.minute||0,i=b.second||0,j=b.millisecond||0;this._milliseconds=+j+1e3*i+6e4*h+36e5*g,this._days=+f+7*e,this._months=+d+12*c,this._data={},this._bubble()}function h(a,b){for(var c in b)b.hasOwnProperty(c)&&(a[c]=b[c]);return b.hasOwnProperty("toString")&&(a.toString=b.toString),b.hasOwnProperty("valueOf")&&(a.valueOf=b.valueOf),a}function i(a){var b,c={};for(b in a)a.hasOwnProperty(b)&&qb.hasOwnProperty(b)&&(c[b]=a[b]);return c}function j(a){return 0>a?Math.ceil(a):Math.floor(a)}function k(a,b,c){for(var d=""+Math.abs(a),e=a>=0;d.length<b;)d="0"+d;return(e?c?"+":"":"-")+d}function l(a,b,c,d){var e,f,g=b._milliseconds,h=b._days,i=b._months;g&&a._d.setTime(+a._d+g*c),(h||i)&&(e=a.minute(),f=a.hour()),h&&a.date(a.date()+h*c),i&&a.month(a.month()+i*c),g&&!d&&db.updateOffset(a),(h||i)&&(a.minute(e),a.hour(f))}function m(a){return"[object Array]"===Object.prototype.toString.call(a)}function n(a){return"[object Date]"===Object.prototype.toString.call(a)||a instanceof Date}function o(a,b,c){var d,e=Math.min(a.length,b.length),f=Math.abs(a.length-b.length),g=0;for(d=0;e>d;d++)(c&&a[d]!==b[d]||!c&&s(a[d])!==s(b[d]))&&g++;return g+f}function p(a){if(a){var b=a.toLowerCase().replace(/(.)s$/,"$1");a=Tb[a]||Ub[b]||b}return a}function q(a){var b,c,d={};for(c in a)a.hasOwnProperty(c)&&(b=p(c),b&&(d[b]=a[c]));return d}function r(b){var c,d;if(0===b.indexOf("week"))c=7,d="day";else{if(0!==b.indexOf("month"))return;c=12,d="month"}db[b]=function(e,f){var g,h,i=db.fn._lang[b],j=[];if("number"==typeof e&&(f=e,e=a),h=function(a){var b=db().utc().set(d,a);return i.call(db.fn._lang,b,e||"")},null!=f)return h(f);for(g=0;c>g;g++)j.push(h(g));return j}}function s(a){var b=+a,c=0;return 0!==b&&isFinite(b)&&(c=b>=0?Math.floor(b):Math.ceil(b)),c}function t(a,b){return new Date(Date.UTC(a,b+1,0)).getUTCDate()}function u(a){return v(a)?366:365}function v(a){return a%4===0&&a%100!==0||a%400===0}function w(a){var b;a._a&&-2===a._pf.overflow&&(b=a._a[jb]<0||a._a[jb]>11?jb:a._a[kb]<1||a._a[kb]>t(a._a[ib],a._a[jb])?kb:a._a[lb]<0||a._a[lb]>23?lb:a._a[mb]<0||a._a[mb]>59?mb:a._a[nb]<0||a._a[nb]>59?nb:a._a[ob]<0||a._a[ob]>999?ob:-1,a._pf._overflowDayOfYear&&(ib>b||b>kb)&&(b=kb),a._pf.overflow=b)}function x(a){return null==a._isValid&&(a._isValid=!isNaN(a._d.getTime())&&a._pf.overflow<0&&!a._pf.empty&&!a._pf.invalidMonth&&!a._pf.nullInput&&!a._pf.invalidFormat&&!a._pf.userInvalidated,a._strict&&(a._isValid=a._isValid&&0===a._pf.charsLeftOver&&0===a._pf.unusedTokens.length)),a._isValid}function y(a){return a?a.toLowerCase().replace("_","-"):a}function z(a,b){return b._isUTC?db(a).zone(b._offset||0):db(a).local()}function A(a,b){return b.abbr=a,pb[a]||(pb[a]=new e),pb[a].set(b),pb[a]}function B(a){delete pb[a]}function C(a){var b,c,d,e,f=0,g=function(a){if(!pb[a]&&rb)try{require("./lang/"+a)}catch(b){}return pb[a]};if(!a)return db.fn._lang;if(!m(a)){if(c=g(a))return c;a=[a]}for(;f<a.length;){for(e=y(a[f]).split("-"),b=e.length,d=y(a[f+1]),d=d?d.split("-"):null;b>0;){if(c=g(e.slice(0,b).join("-")))return c;if(d&&d.length>=b&&o(e,d,!0)>=b-1)break;b--}f++}return db.fn._lang}function D(a){return a.match(/\[[\s\S]/)?a.replace(/^\[|\]$/g,""):a.replace(/\\/g,"")}function E(a){var b,c,d=a.match(vb);for(b=0,c=d.length;c>b;b++)d[b]=Yb[d[b]]?Yb[d[b]]:D(d[b]);return function(e){var f="";for(b=0;c>b;b++)f+=d[b]instanceof Function?d[b].call(e,a):d[b];return f}}function F(a,b){return a.isValid()?(b=G(b,a.lang()),Vb[b]||(Vb[b]=E(b)),Vb[b](a)):a.lang().invalidDate()}function G(a,b){function c(a){return b.longDateFormat(a)||a}var d=5;for(wb.lastIndex=0;d>=0&&wb.test(a);)a=a.replace(wb,c),wb.lastIndex=0,d-=1;return a}function H(a,b){var c,d=b._strict;switch(a){case"DDDD":return Ib;case"YYYY":case"GGGG":case"gggg":return d?Jb:zb;case"Y":case"G":case"g":return Lb;case"YYYYYY":case"YYYYY":case"GGGGG":case"ggggg":return d?Kb:Ab;case"S":if(d)return Gb;case"SS":if(d)return Hb;case"SSS":if(d)return Ib;case"DDD":return yb;case"MMM":case"MMMM":case"dd":case"ddd":case"dddd":return Cb;case"a":case"A":return C(b._l)._meridiemParse;case"X":return Fb;case"Z":case"ZZ":return Db;case"T":return Eb;case"SSSS":return Bb;case"MM":case"DD":case"YY":case"GG":case"gg":case"HH":case"hh":case"mm":case"ss":case"ww":case"WW":return d?Hb:xb;case"M":case"D":case"d":case"H":case"h":case"m":case"s":case"w":case"W":case"e":case"E":return xb;default:return c=new RegExp(P(O(a.replace("\\","")),"i"))}}function I(a){a=a||"";var b=a.match(Db)||[],c=b[b.length-1]||[],d=(c+"").match(Qb)||["-",0,0],e=+(60*d[1])+s(d[2]);return"+"===d[0]?-e:e}function J(a,b,c){var d,e=c._a;switch(a){case"M":case"MM":null!=b&&(e[jb]=s(b)-1);break;case"MMM":case"MMMM":d=C(c._l).monthsParse(b),null!=d?e[jb]=d:c._pf.invalidMonth=b;break;case"D":case"DD":null!=b&&(e[kb]=s(b));break;case"DDD":case"DDDD":null!=b&&(c._dayOfYear=s(b));break;case"YY":e[ib]=s(b)+(s(b)>68?1900:2e3);break;case"YYYY":case"YYYYY":case"YYYYYY":e[ib]=s(b);break;case"a":case"A":c._isPm=C(c._l).isPM(b);break;case"H":case"HH":case"h":case"hh":e[lb]=s(b);break;case"m":case"mm":e[mb]=s(b);break;case"s":case"ss":e[nb]=s(b);break;case"S":case"SS":case"SSS":case"SSSS":e[ob]=s(1e3*("0."+b));break;case"X":c._d=new Date(1e3*parseFloat(b));break;case"Z":case"ZZ":c._useUTC=!0,c._tzm=I(b);break;case"w":case"ww":case"W":case"WW":case"d":case"dd":case"ddd":case"dddd":case"e":case"E":a=a.substr(0,1);case"gg":case"gggg":case"GG":case"GGGG":case"GGGGG":a=a.substr(0,2),b&&(c._w=c._w||{},c._w[a]=b)}}function K(a){var b,c,d,e,f,g,h,i,j,k,l=[];if(!a._d){for(d=M(a),a._w&&null==a._a[kb]&&null==a._a[jb]&&(f=function(b){var c=parseInt(b,10);return b?b.length<3?c>68?1900+c:2e3+c:c:null==a._a[ib]?db().weekYear():a._a[ib]},g=a._w,null!=g.GG||null!=g.W||null!=g.E?h=Z(f(g.GG),g.W||1,g.E,4,1):(i=C(a._l),j=null!=g.d?V(g.d,i):null!=g.e?parseInt(g.e,10)+i._week.dow:0,k=parseInt(g.w,10)||1,null!=g.d&&j<i._week.dow&&k++,h=Z(f(g.gg),k,j,i._week.doy,i._week.dow)),a._a[ib]=h.year,a._dayOfYear=h.dayOfYear),a._dayOfYear&&(e=null==a._a[ib]?d[ib]:a._a[ib],a._dayOfYear>u(e)&&(a._pf._overflowDayOfYear=!0),c=U(e,0,a._dayOfYear),a._a[jb]=c.getUTCMonth(),a._a[kb]=c.getUTCDate()),b=0;3>b&&null==a._a[b];++b)a._a[b]=l[b]=d[b];for(;7>b;b++)a._a[b]=l[b]=null==a._a[b]?2===b?1:0:a._a[b];l[lb]+=s((a._tzm||0)/60),l[mb]+=s((a._tzm||0)%60),a._d=(a._useUTC?U:T).apply(null,l)}}function L(a){var b;a._d||(b=q(a._i),a._a=[b.year,b.month,b.day,b.hour,b.minute,b.second,b.millisecond],K(a))}function M(a){var b=new Date;return a._useUTC?[b.getUTCFullYear(),b.getUTCMonth(),b.getUTCDate()]:[b.getFullYear(),b.getMonth(),b.getDate()]}function N(a){a._a=[],a._pf.empty=!0;var b,c,d,e,f,g=C(a._l),h=""+a._i,i=h.length,j=0;for(d=G(a._f,g).match(vb)||[],b=0;b<d.length;b++)e=d[b],c=(h.match(H(e,a))||[])[0],c&&(f=h.substr(0,h.indexOf(c)),f.length>0&&a._pf.unusedInput.push(f),h=h.slice(h.indexOf(c)+c.length),j+=c.length),Yb[e]?(c?a._pf.empty=!1:a._pf.unusedTokens.push(e),J(e,c,a)):a._strict&&!c&&a._pf.unusedTokens.push(e);a._pf.charsLeftOver=i-j,h.length>0&&a._pf.unusedInput.push(h),a._isPm&&a._a[lb]<12&&(a._a[lb]+=12),a._isPm===!1&&12===a._a[lb]&&(a._a[lb]=0),K(a),w(a)}function O(a){return a.replace(/\\(\[)|\\(\])|\[([^\]\[]*)\]|\\(.)/g,function(a,b,c,d,e){return b||c||d||e})}function P(a){return a.replace(/[-\/\\^$*+?.()|[\]{}]/g,"\\$&")}function Q(a){var c,d,e,f,g;if(0===a._f.length)return a._pf.invalidFormat=!0,a._d=new Date(0/0),void 0;for(f=0;f<a._f.length;f++)g=0,c=h({},a),c._pf=b(),c._f=a._f[f],N(c),x(c)&&(g+=c._pf.charsLeftOver,g+=10*c._pf.unusedTokens.length,c._pf.score=g,(null==e||e>g)&&(e=g,d=c));h(a,d||c)}function R(a){var b,c,d=a._i,e=Mb.exec(d);if(e){for(a._pf.iso=!0,b=0,c=Ob.length;c>b;b++)if(Ob[b][1].exec(d)){a._f=Ob[b][0]+(e[6]||" ");break}for(b=0,c=Pb.length;c>b;b++)if(Pb[b][1].exec(d)){a._f+=Pb[b][0];break}d.match(Db)&&(a._f+="Z"),N(a)}else a._d=new Date(d)}function S(b){var c=b._i,d=sb.exec(c);c===a?b._d=new Date:d?b._d=new Date(+d[1]):"string"==typeof c?R(b):m(c)?(b._a=c.slice(0),K(b)):n(c)?b._d=new Date(+c):"object"==typeof c?L(b):b._d=new Date(c)}function T(a,b,c,d,e,f,g){var h=new Date(a,b,c,d,e,f,g);return 1970>a&&h.setFullYear(a),h}function U(a){var b=new Date(Date.UTC.apply(null,arguments));return 1970>a&&b.setUTCFullYear(a),b}function V(a,b){if("string"==typeof a)if(isNaN(a)){if(a=b.weekdaysParse(a),"number"!=typeof a)return null}else a=parseInt(a,10);return a}function W(a,b,c,d,e){return e.relativeTime(b||1,!!c,a,d)}function X(a,b,c){var d=hb(Math.abs(a)/1e3),e=hb(d/60),f=hb(e/60),g=hb(f/24),h=hb(g/365),i=45>d&&["s",d]||1===e&&["m"]||45>e&&["mm",e]||1===f&&["h"]||22>f&&["hh",f]||1===g&&["d"]||25>=g&&["dd",g]||45>=g&&["M"]||345>g&&["MM",hb(g/30)]||1===h&&["y"]||["yy",h];return i[2]=b,i[3]=a>0,i[4]=c,W.apply({},i)}function Y(a,b,c){var d,e=c-b,f=c-a.day();return f>e&&(f-=7),e-7>f&&(f+=7),d=db(a).add("d",f),{week:Math.ceil(d.dayOfYear()/7),year:d.year()}}function Z(a,b,c,d,e){var f,g,h=U(a,0,1).getUTCDay();return c=null!=c?c:e,f=e-h+(h>d?7:0)-(e>h?7:0),g=7*(b-1)+(c-e)+f+1,{year:g>0?a:a-1,dayOfYear:g>0?g:u(a-1)+g}}function $(a){var b=a._i,c=a._f;return null===b?db.invalid({nullInput:!0}):("string"==typeof b&&(a._i=b=C().preparse(b)),db.isMoment(b)?(a=i(b),a._d=new Date(+b._d)):c?m(c)?Q(a):N(a):S(a),new f(a))}function _(a,b){db.fn[a]=db.fn[a+"s"]=function(a){var c=this._isUTC?"UTC":"";return null!=a?(this._d["set"+c+b](a),db.updateOffset(this),this):this._d["get"+c+b]()}}function ab(a){db.duration.fn[a]=function(){return this._data[a]}}function bb(a,b){db.duration.fn["as"+a]=function(){return+this/b}}function cb(a){var b=!1,c=db;"undefined"==typeof ender&&(a?(gb.moment=function(){return!b&&console&&console.warn&&(b=!0,console.warn("Accessing Moment through the global scope is deprecated, and will be removed in an upcoming release.")),c.apply(null,arguments)},h(gb.moment,c)):gb.moment=db)}for(var db,eb,fb="2.5.1",gb=this,hb=Math.round,ib=0,jb=1,kb=2,lb=3,mb=4,nb=5,ob=6,pb={},qb={_isAMomentObject:null,_i:null,_f:null,_l:null,_strict:null,_isUTC:null,_offset:null,_pf:null,_lang:null},rb="undefined"!=typeof module&&module.exports&&"undefined"!=typeof require,sb=/^\/?Date\((\-?\d+)/i,tb=/(\-)?(?:(\d*)\.)?(\d+)\:(\d+)(?:\:(\d+)\.?(\d{3})?)?/,ub=/^(-)?P(?:(?:([0-9,.]*)Y)?(?:([0-9,.]*)M)?(?:([0-9,.]*)D)?(?:T(?:([0-9,.]*)H)?(?:([0-9,.]*)M)?(?:([0-9,.]*)S)?)?|([0-9,.]*)W)$/,vb=/(\[[^\[]*\])|(\\)?(Mo|MM?M?M?|Do|DDDo|DD?D?D?|ddd?d?|do?|w[o|w]?|W[o|W]?|YYYYYY|YYYYY|YYYY|YY|gg(ggg?)?|GG(GGG?)?|e|E|a|A|hh?|HH?|mm?|ss?|S{1,4}|X|zz?|ZZ?|.)/g,wb=/(\[[^\[]*\])|(\\)?(LT|LL?L?L?|l{1,4})/g,xb=/\d\d?/,yb=/\d{1,3}/,zb=/\d{1,4}/,Ab=/[+\-]?\d{1,6}/,Bb=/\d+/,Cb=/[0-9]*['a-z\u00A0-\u05FF\u0700-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF]+|[\u0600-\u06FF\/]+(\s*?[\u0600-\u06FF]+){1,2}/i,Db=/Z|[\+\-]\d\d:?\d\d/gi,Eb=/T/i,Fb=/[\+\-]?\d+(\.\d{1,3})?/,Gb=/\d/,Hb=/\d\d/,Ib=/\d{3}/,Jb=/\d{4}/,Kb=/[+-]?\d{6}/,Lb=/[+-]?\d+/,Mb=/^\s*(?:[+-]\d{6}|\d{4})-(?:(\d\d-\d\d)|(W\d\d$)|(W\d\d-\d)|(\d\d\d))((T| )(\d\d(:\d\d(:\d\d(\.\d+)?)?)?)?([\+\-]\d\d(?::?\d\d)?|\s*Z)?)?$/,Nb="YYYY-MM-DDTHH:mm:ssZ",Ob=[["YYYYYY-MM-DD",/[+-]\d{6}-\d{2}-\d{2}/],["YYYY-MM-DD",/\d{4}-\d{2}-\d{2}/],["GGGG-[W]WW-E",/\d{4}-W\d{2}-\d/],["GGGG-[W]WW",/\d{4}-W\d{2}/],["YYYY-DDD",/\d{4}-\d{3}/]],Pb=[["HH:mm:ss.SSSS",/(T| )\d\d:\d\d:\d\d\.\d{1,3}/],["HH:mm:ss",/(T| )\d\d:\d\d:\d\d/],["HH:mm",/(T| )\d\d:\d\d/],["HH",/(T| )\d\d/]],Qb=/([\+\-]|\d\d)/gi,Rb="Date|Hours|Minutes|Seconds|Milliseconds".split("|"),Sb={Milliseconds:1,Seconds:1e3,Minutes:6e4,Hours:36e5,Days:864e5,Months:2592e6,Years:31536e6},Tb={ms:"millisecond",s:"second",m:"minute",h:"hour",d:"day",D:"date",w:"week",W:"isoWeek",M:"month",y:"year",DDD:"dayOfYear",e:"weekday",E:"isoWeekday",gg:"weekYear",GG:"isoWeekYear"},Ub={dayofyear:"dayOfYear",isoweekday:"isoWeekday",isoweek:"isoWeek",weekyear:"weekYear",isoweekyear:"isoWeekYear"},Vb={},Wb="DDD w W M D d".split(" "),Xb="M D H h m s w W".split(" "),Yb={M:function(){return this.month()+1},MMM:function(a){return this.lang().monthsShort(this,a)},MMMM:function(a){return this.lang().months(this,a)},D:function(){return this.date()},DDD:function(){return this.dayOfYear()},d:function(){return this.day()},dd:function(a){return this.lang().weekdaysMin(this,a)},ddd:function(a){return this.lang().weekdaysShort(this,a)},dddd:function(a){return this.lang().weekdays(this,a)},w:function(){return this.week()},W:function(){return this.isoWeek()},YY:function(){return k(this.year()%100,2)},YYYY:function(){return k(this.year(),4)},YYYYY:function(){return k(this.year(),5)},YYYYYY:function(){var a=this.year(),b=a>=0?"+":"-";return b+k(Math.abs(a),6)},gg:function(){return k(this.weekYear()%100,2)},gggg:function(){return k(this.weekYear(),4)},ggggg:function(){return k(this.weekYear(),5)},GG:function(){return k(this.isoWeekYear()%100,2)},GGGG:function(){return k(this.isoWeekYear(),4)},GGGGG:function(){return k(this.isoWeekYear(),5)},e:function(){return this.weekday()},E:function(){return this.isoWeekday()},a:function(){return this.lang().meridiem(this.hours(),this.minutes(),!0)},A:function(){return this.lang().meridiem(this.hours(),this.minutes(),!1)},H:function(){return this.hours()},h:function(){return this.hours()%12||12},m:function(){return this.minutes()},s:function(){return this.seconds()},S:function(){return s(this.milliseconds()/100)},SS:function(){return k(s(this.milliseconds()/10),2)},SSS:function(){return k(this.milliseconds(),3)},SSSS:function(){return k(this.milliseconds(),3)},Z:function(){var a=-this.zone(),b="+";return 0>a&&(a=-a,b="-"),b+k(s(a/60),2)+":"+k(s(a)%60,2)},ZZ:function(){var a=-this.zone(),b="+";return 0>a&&(a=-a,b="-"),b+k(s(a/60),2)+k(s(a)%60,2)},z:function(){return this.zoneAbbr()},zz:function(){return this.zoneName()},X:function(){return this.unix()},Q:function(){return this.quarter()}},Zb=["months","monthsShort","weekdays","weekdaysShort","weekdaysMin"];Wb.length;)eb=Wb.pop(),Yb[eb+"o"]=d(Yb[eb],eb);for(;Xb.length;)eb=Xb.pop(),Yb[eb+eb]=c(Yb[eb],2);for(Yb.DDDD=c(Yb.DDD,3),h(e.prototype,{set:function(a){var b,c;for(c in a)b=a[c],"function"==typeof b?this[c]=b:this["_"+c]=b},_months:"January_February_March_April_May_June_July_August_September_October_November_December".split("_"),months:function(a){return this._months[a.month()]},_monthsShort:"Jan_Feb_Mar_Apr_May_Jun_Jul_Aug_Sep_Oct_Nov_Dec".split("_"),monthsShort:function(a){return this._monthsShort[a.month()]},monthsParse:function(a){var b,c,d;for(this._monthsParse||(this._monthsParse=[]),b=0;12>b;b++)if(this._monthsParse[b]||(c=db.utc([2e3,b]),d="^"+this.months(c,"")+"|^"+this.monthsShort(c,""),this._monthsParse[b]=new RegExp(d.replace(".",""),"i")),this._monthsParse[b].test(a))return b},_weekdays:"Sunday_Monday_Tuesday_Wednesday_Thursday_Friday_Saturday".split("_"),weekdays:function(a){return this._weekdays[a.day()]},_weekdaysShort:"Sun_Mon_Tue_Wed_Thu_Fri_Sat".split("_"),weekdaysShort:function(a){return this._weekdaysShort[a.day()]},_weekdaysMin:"Su_Mo_Tu_We_Th_Fr_Sa".split("_"),weekdaysMin:function(a){return this._weekdaysMin[a.day()]},weekdaysParse:function(a){var b,c,d;for(this._weekdaysParse||(this._weekdaysParse=[]),b=0;7>b;b++)if(this._weekdaysParse[b]||(c=db([2e3,1]).day(b),d="^"+this.weekdays(c,"")+"|^"+this.weekdaysShort(c,"")+"|^"+this.weekdaysMin(c,""),this._weekdaysParse[b]=new RegExp(d.replace(".",""),"i")),this._weekdaysParse[b].test(a))return b},_longDateFormat:{LT:"h:mm A",L:"MM/DD/YYYY",LL:"MMMM D YYYY",LLL:"MMMM D YYYY LT",LLLL:"dddd, MMMM D YYYY LT"},longDateFormat:function(a){var b=this._longDateFormat[a];return!b&&this._longDateFormat[a.toUpperCase()]&&(b=this._longDateFormat[a.toUpperCase()].replace(/MMMM|MM|DD|dddd/g,function(a){return a.slice(1)}),this._longDateFormat[a]=b),b},isPM:function(a){return"p"===(a+"").toLowerCase().charAt(0)},_meridiemParse:/[ap]\.?m?\.?/i,meridiem:function(a,b,c){return a>11?c?"pm":"PM":c?"am":"AM"},_calendar:{sameDay:"[Today at] LT",nextDay:"[Tomorrow at] LT",nextWeek:"dddd [at] LT",lastDay:"[Yesterday at] LT",lastWeek:"[Last] dddd [at] LT",sameElse:"L"},calendar:function(a,b){var c=this._calendar[a];return"function"==typeof c?c.apply(b):c},_relativeTime:{future:"in %s",past:"%s ago",s:"a few seconds",m:"a minute",mm:"%d minutes",h:"an hour",hh:"%d hours",d:"a day",dd:"%d days",M:"a month",MM:"%d months",y:"a year",yy:"%d years"},relativeTime:function(a,b,c,d){var e=this._relativeTime[c];return"function"==typeof e?e(a,b,c,d):e.replace(/%d/i,a)},pastFuture:function(a,b){var c=this._relativeTime[a>0?"future":"past"];return"function"==typeof c?c(b):c.replace(/%s/i,b)},ordinal:function(a){return this._ordinal.replace("%d",a)},_ordinal:"%d",preparse:function(a){return a},postformat:function(a){return a},week:function(a){return Y(a,this._week.dow,this._week.doy).week},_week:{dow:0,doy:6},_invalidDate:"Invalid date",invalidDate:function(){return this._invalidDate}}),db=function(c,d,e,f){var g;return"boolean"==typeof e&&(f=e,e=a),g={},g._isAMomentObject=!0,g._i=c,g._f=d,g._l=e,g._strict=f,g._isUTC=!1,g._pf=b(),$(g)},db.utc=function(c,d,e,f){var g;return"boolean"==typeof e&&(f=e,e=a),g={},g._isAMomentObject=!0,g._useUTC=!0,g._isUTC=!0,g._l=e,g._i=c,g._f=d,g._strict=f,g._pf=b(),$(g).utc()},db.unix=function(a){return db(1e3*a)},db.duration=function(a,b){var c,d,e,f=a,h=null;return db.isDuration(a)?f={ms:a._milliseconds,d:a._days,M:a._months}:"number"==typeof a?(f={},b?f[b]=a:f.milliseconds=a):(h=tb.exec(a))?(c="-"===h[1]?-1:1,f={y:0,d:s(h[kb])*c,h:s(h[lb])*c,m:s(h[mb])*c,s:s(h[nb])*c,ms:s(h[ob])*c}):(h=ub.exec(a))&&(c="-"===h[1]?-1:1,e=function(a){var b=a&&parseFloat(a.replace(",","."));return(isNaN(b)?0:b)*c},f={y:e(h[2]),M:e(h[3]),d:e(h[4]),h:e(h[5]),m:e(h[6]),s:e(h[7]),w:e(h[8])}),d=new g(f),db.isDuration(a)&&a.hasOwnProperty("_lang")&&(d._lang=a._lang),d},db.version=fb,db.defaultFormat=Nb,db.updateOffset=function(){},db.lang=function(a,b){var c;return a?(b?A(y(a),b):null===b?(B(a),a="en"):pb[a]||C(a),c=db.duration.fn._lang=db.fn._lang=C(a),c._abbr):db.fn._lang._abbr},db.langData=function(a){return a&&a._lang&&a._lang._abbr&&(a=a._lang._abbr),C(a)},db.isMoment=function(a){return a instanceof f||null!=a&&a.hasOwnProperty("_isAMomentObject")},db.isDuration=function(a){return a instanceof g},eb=Zb.length-1;eb>=0;--eb)r(Zb[eb]);for(db.normalizeUnits=function(a){return p(a)},db.invalid=function(a){var b=db.utc(0/0);return null!=a?h(b._pf,a):b._pf.userInvalidated=!0,b},db.parseZone=function(a){return db(a).parseZone()},h(db.fn=f.prototype,{clone:function(){return db(this)},valueOf:function(){return+this._d+6e4*(this._offset||0)},unix:function(){return Math.floor(+this/1e3)},toString:function(){return this.clone().lang("en").format("ddd MMM DD YYYY HH:mm:ss [GMT]ZZ")},toDate:function(){return this._offset?new Date(+this):this._d},toISOString:function(){var a=db(this).utc();return 0<a.year()&&a.year()<=9999?F(a,"YYYY-MM-DD[T]HH:mm:ss.SSS[Z]"):F(a,"YYYYYY-MM-DD[T]HH:mm:ss.SSS[Z]")},toArray:function(){var a=this;return[a.year(),a.month(),a.date(),a.hours(),a.minutes(),a.seconds(),a.milliseconds()]},isValid:function(){return x(this)},isDSTShifted:function(){return this._a?this.isValid()&&o(this._a,(this._isUTC?db.utc(this._a):db(this._a)).toArray())>0:!1},parsingFlags:function(){return h({},this._pf)},invalidAt:function(){return this._pf.overflow},utc:function(){return this.zone(0)},local:function(){return this.zone(0),this._isUTC=!1,this},format:function(a){var b=F(this,a||db.defaultFormat);return this.lang().postformat(b)},add:function(a,b){var c;return c="string"==typeof a?db.duration(+b,a):db.duration(a,b),l(this,c,1),this},subtract:function(a,b){var c;return c="string"==typeof a?db.duration(+b,a):db.duration(a,b),l(this,c,-1),this},diff:function(a,b,c){var d,e,f=z(a,this),g=6e4*(this.zone()-f.zone());return b=p(b),"year"===b||"month"===b?(d=432e5*(this.daysInMonth()+f.daysInMonth()),e=12*(this.year()-f.year())+(this.month()-f.month()),e+=(this-db(this).startOf("month")-(f-db(f).startOf("month")))/d,e-=6e4*(this.zone()-db(this).startOf("month").zone()-(f.zone()-db(f).startOf("month").zone()))/d,"year"===b&&(e/=12)):(d=this-f,e="second"===b?d/1e3:"minute"===b?d/6e4:"hour"===b?d/36e5:"day"===b?(d-g)/864e5:"week"===b?(d-g)/6048e5:d),c?e:j(e)},from:function(a,b){return db.duration(this.diff(a)).lang(this.lang()._abbr).humanize(!b)},fromNow:function(a){return this.from(db(),a)},calendar:function(){var a=z(db(),this).startOf("day"),b=this.diff(a,"days",!0),c=-6>b?"sameElse":-1>b?"lastWeek":0>b?"lastDay":1>b?"sameDay":2>b?"nextDay":7>b?"nextWeek":"sameElse";return this.format(this.lang().calendar(c,this))},isLeapYear:function(){return v(this.year())},isDST:function(){return this.zone()<this.clone().month(0).zone()||this.zone()<this.clone().month(5).zone()},day:function(a){var b=this._isUTC?this._d.getUTCDay():this._d.getDay();return null!=a?(a=V(a,this.lang()),this.add({d:a-b})):b},month:function(a){var b,c=this._isUTC?"UTC":"";return null!=a?"string"==typeof a&&(a=this.lang().monthsParse(a),"number"!=typeof a)?this:(b=this.date(),this.date(1),this._d["set"+c+"Month"](a),this.date(Math.min(b,this.daysInMonth())),db.updateOffset(this),this):this._d["get"+c+"Month"]()},startOf:function(a){switch(a=p(a)){case"year":this.month(0);case"month":this.date(1);case"week":case"isoWeek":case"day":this.hours(0);case"hour":this.minutes(0);case"minute":this.seconds(0);case"second":this.milliseconds(0)}return"week"===a?this.weekday(0):"isoWeek"===a&&this.isoWeekday(1),this},endOf:function(a){return a=p(a),this.startOf(a).add("isoWeek"===a?"week":a,1).subtract("ms",1)},isAfter:function(a,b){return b="undefined"!=typeof b?b:"millisecond",+this.clone().startOf(b)>+db(a).startOf(b)},isBefore:function(a,b){return b="undefined"!=typeof b?b:"millisecond",+this.clone().startOf(b)<+db(a).startOf(b)},isSame:function(a,b){return b=b||"ms",+this.clone().startOf(b)===+z(a,this).startOf(b)},min:function(a){return a=db.apply(null,arguments),this>a?this:a},max:function(a){return a=db.apply(null,arguments),a>this?this:a},zone:function(a){var b=this._offset||0;return null==a?this._isUTC?b:this._d.getTimezoneOffset():("string"==typeof a&&(a=I(a)),Math.abs(a)<16&&(a=60*a),this._offset=a,this._isUTC=!0,b!==a&&l(this,db.duration(b-a,"m"),1,!0),this)},zoneAbbr:function(){return this._isUTC?"UTC":""},zoneName:function(){return this._isUTC?"Coordinated Universal Time":""},parseZone:function(){return this._tzm?this.zone(this._tzm):"string"==typeof this._i&&this.zone(this._i),this},hasAlignedHourOffset:function(a){return a=a?db(a).zone():0,(this.zone()-a)%60===0},daysInMonth:function(){return t(this.year(),this.month())},dayOfYear:function(a){var b=hb((db(this).startOf("day")-db(this).startOf("year"))/864e5)+1;return null==a?b:this.add("d",a-b)},quarter:function(){return Math.ceil((this.month()+1)/3)},weekYear:function(a){var b=Y(this,this.lang()._week.dow,this.lang()._week.doy).year;return null==a?b:this.add("y",a-b)},isoWeekYear:function(a){var b=Y(this,1,4).year;return null==a?b:this.add("y",a-b)},week:function(a){var b=this.lang().week(this);return null==a?b:this.add("d",7*(a-b))},isoWeek:function(a){var b=Y(this,1,4).week;return null==a?b:this.add("d",7*(a-b))},weekday:function(a){var b=(this.day()+7-this.lang()._week.dow)%7;return null==a?b:this.add("d",a-b)},isoWeekday:function(a){return null==a?this.day()||7:this.day(this.day()%7?a:a-7)},get:function(a){return a=p(a),this[a]()},set:function(a,b){return a=p(a),"function"==typeof this[a]&&this[a](b),this},lang:function(b){return b===a?this._lang:(this._lang=C(b),this)}}),eb=0;eb<Rb.length;eb++)_(Rb[eb].toLowerCase().replace(/s$/,""),Rb[eb]);_("year","FullYear"),db.fn.days=db.fn.day,db.fn.months=db.fn.month,db.fn.weeks=db.fn.week,db.fn.isoWeeks=db.fn.isoWeek,db.fn.toJSON=db.fn.toISOString,h(db.duration.fn=g.prototype,{_bubble:function(){var a,b,c,d,e=this._milliseconds,f=this._days,g=this._months,h=this._data;h.milliseconds=e%1e3,a=j(e/1e3),h.seconds=a%60,b=j(a/60),h.minutes=b%60,c=j(b/60),h.hours=c%24,f+=j(c/24),h.days=f%30,g+=j(f/30),h.months=g%12,d=j(g/12),h.years=d},weeks:function(){return j(this.days()/7)},valueOf:function(){return this._milliseconds+864e5*this._days+this._months%12*2592e6+31536e6*s(this._months/12)},humanize:function(a){var b=+this,c=X(b,!a,this.lang());return a&&(c=this.lang().pastFuture(b,c)),this.lang().postformat(c)},add:function(a,b){var c=db.duration(a,b);return this._milliseconds+=c._milliseconds,this._days+=c._days,this._months+=c._months,this._bubble(),this},subtract:function(a,b){var c=db.duration(a,b);return this._milliseconds-=c._milliseconds,this._days-=c._days,this._months-=c._months,this._bubble(),this},get:function(a){return a=p(a),this[a.toLowerCase()+"s"]()},as:function(a){return a=p(a),this["as"+a.charAt(0).toUpperCase()+a.slice(1)+"s"]()},lang:db.fn.lang,toIsoString:function(){var a=Math.abs(this.years()),b=Math.abs(this.months()),c=Math.abs(this.days()),d=Math.abs(this.hours()),e=Math.abs(this.minutes()),f=Math.abs(this.seconds()+this.milliseconds()/1e3);return this.asSeconds()?(this.asSeconds()<0?"-":"")+"P"+(a?a+"Y":"")+(b?b+"M":"")+(c?c+"D":"")+(d||e||f?"T":"")+(d?d+"H":"")+(e?e+"M":"")+(f?f+"S":""):"P0D"}});for(eb in Sb)Sb.hasOwnProperty(eb)&&(bb(eb,Sb[eb]),ab(eb.toLowerCase()));bb("Weeks",6048e5),db.duration.fn.asMonths=function(){return(+this-31536e6*this.years())/2592e6+12*this.years()},db.lang("en",{ordinal:function(a){var b=a%10,c=1===s(a%100/10)?"th":1===b?"st":2===b?"nd":3===b?"rd":"th";return a+c}}),rb?(module.exports=db,cb(!0)):"function"==typeof define&&define.amd?define("moment",function(b,c,d){return d.config&&d.config()&&d.config().noGlobal!==!0&&cb(d.config().noGlobal===a),db}):cb()}).call(this);

/**
* @version: 1.3.7
* @author: Dan Grossman http://www.dangrossman.info/
* @date: 2014-04-29
* @copyright: Copyright (c) 2012-2014 Dan Grossman. All rights reserved.
* @license: Licensed under Apache License v2.0. See http://www.apache.org/licenses/LICENSE-2.0
* @website: http://www.improvely.com/
*/
!function ($, moment) {

    var DateRangePicker = function (element, options, cb) {

        // by default, the daterangepicker element is placed at the bottom of HTML body
        this.parentEl = 'body';

        //element that triggered the date range picker
        this.element = $(element);

        //create the picker HTML object
        var DRPTemplate = 
			  '<div class="daterangepicker dropdown-menu">' +

				'<div class="calendar right"></div>' +
				'<div class="calendar left"></div>' +

				'<div class="calendar-am_date right"></div>' +
				'<div class="calendar-am_date left"></div>' +

                '<div class="ranges">' +
                  '<div class="range_inputs">' +
                    '<div class="daterangepicker_start_input">' +
                      '<label for="daterangepicker_start"></label>' +
                      '<input class="input-mini" type="text" name="daterangepicker_start" value="" disabled="disabled" />' +
                    '</div>' +
                    '<div class="daterangepicker_end_input">' +
                      '<label for="daterangepicker_end"></label>' +
                      '<input class="input-mini" type="text" name="daterangepicker_end" value="" disabled="disabled" />' +
                    '</div>' +
                    '<button class="applyBtn" disabled="disabled"></button>&nbsp;' +
                    '<button class="cancelBtn"></button>' +
                  '</div>' +
                '</div>' +
              '</div>';

        //custom options
        if (typeof options !== 'object' || options === null)
            options = {};

        this.parentEl = (typeof options === 'object' && options.parentEl && $(options.parentEl).length) ? $(options.parentEl) : $(this.parentEl);
        this.container = $(DRPTemplate).appendTo(this.parentEl);

        this.setOptions(options, cb);

        //apply CSS classes and labels to buttons
        var c = this.container;
        $.each(this.buttonClasses, function (idx, val) {
            c.find('button').addClass(val);
        });
        this.container.find('.daterangepicker_start_input label').html(this.locale.fromLabel);
        this.container.find('.daterangepicker_end_input label').html(this.locale.toLabel);
        if (this.applyClass.length)
            this.container.find('.applyBtn').addClass(this.applyClass);
        if (this.cancelClass.length)
            this.container.find('.cancelBtn').addClass(this.cancelClass);
        this.container.find('.applyBtn').html(this.locale.applyLabel);
        this.container.find('.cancelBtn').html(this.locale.cancelLabel);

        //event listeners

        this.container.find('.calendar')
            .on('click.daterangepicker', '.prev', $.proxy(this.clickPrev, this))
            .on('click.daterangepicker', '.next', $.proxy(this.clickNext, this))
            .on('click.daterangepicker', 'td.available', $.proxy(this.clickDate, this))
            .on('mouseenter.daterangepicker', 'td.available', $.proxy(this.enterDate, this))
            .on('mouseleave.daterangepicker', 'td.available', $.proxy(this.updateFormInputs, this))
            .on('change.daterangepicker', 'select.yearselect', $.proxy(this.updateMonthYear, this))
            .on('change.daterangepicker', 'select.monthselect', $.proxy(this.updateMonthYear, this))
            .on('change.daterangepicker', 'select.hourselect,select.minuteselect,select.ampmselect', $.proxy(this.updateTime, this));

        this.container.find('.ranges')
            .on('click.daterangepicker', 'button.applyBtn', $.proxy(this.clickApply, this))
            .on('click.daterangepicker', 'button.cancelBtn', $.proxy(this.clickCancel, this))
            .on('click.daterangepicker', '.daterangepicker_start_input,.daterangepicker_end_input', $.proxy(this.showCalendars, this))
            .on('click.daterangepicker', 'li', $.proxy(this.clickRange, this))
            .on('mouseenter.daterangepicker', 'li', $.proxy(this.enterRange, this))
            .on('mouseleave.daterangepicker', 'li', $.proxy(this.updateFormInputs, this));

        if (this.element.is('input')) {
            this.element.on({
                'click.daterangepicker': $.proxy(this.show, this),
                'focus.daterangepicker': $.proxy(this.show, this),
                'keyup.daterangepicker': $.proxy(this.updateFromControl, this)
            });
        } else {
            this.element.on('click.daterangepicker', $.proxy(this.toggle, this));
        }

    };

    DateRangePicker.prototype = {

        constructor: DateRangePicker,

        setOptions: function(options, callback) {

            this.startDate = moment().startOf('day');
            this.endDate = moment().endOf('day');
            this.minDate = false;
            this.maxDate = false;
            this.dateLimit = false;

            this.showDropdowns = false;
            this.showWeekNumbers = false;
            this.timePicker = false;
            this.timePickerIncrement = 30;
            this.timePicker12Hour = true;
            this.singleDatePicker = false;
            this.ranges = {};

            this.opens = 'right';
            if (this.element.hasClass('pull-right'))
                this.opens = 'left';

            this.buttonClasses = ['btn', 'btn-small'];
            this.applyClass = 'btn-success';
            this.cancelClass = 'btn-default';

            this.format = 'MM/DD/YYYY';
			this.format2 = 'dddd, MMMM D, YYYY';

            this.separator = ' - ';

            this.locale = {
                applyLabel: 'Apply',
                cancelLabel: 'Cancel',
                fromLabel: 'From',
                toLabel: 'To',
                weekLabel: 'W',
                customRangeLabel: 'Custom Range',
                daysOfWeek: moment()._lang._weekdaysMin.slice(),
                monthNames: moment()._lang._monthsShort.slice(),
                firstDay: 0
            };

            this.cb = function () { };

            if (typeof options.format === 'string')
                this.format = options.format;

            if (typeof options.separator === 'string')
                this.separator = options.separator;

            if (typeof options.startDate === 'string')
                this.startDate = moment(options.startDate, this.format);

            if (typeof options.endDate === 'string')
                this.endDate = moment(options.endDate, this.format);

            if (typeof options.minDate === 'string')
                this.minDate = moment(options.minDate, this.format);

            if (typeof options.maxDate === 'string')
                this.maxDate = moment(options.maxDate, this.format);

            if (typeof options.startDate === 'object')
                this.startDate = moment(options.startDate);

            if (typeof options.endDate === 'object')
                this.endDate = moment(options.endDate);

            if (typeof options.minDate === 'object')
                this.minDate = moment(options.minDate);

            if (typeof options.maxDate === 'object')
                this.maxDate = moment(options.maxDate);

            if (typeof options.applyClass === 'string')
                this.applyClass = options.applyClass;

            if (typeof options.cancelClass === 'string')
                this.cancelClass = options.cancelClass;

            if (typeof options.dateLimit === 'object')
                this.dateLimit = options.dateLimit;

            // update day names order to firstDay
            if (typeof options.locale === 'object') {

                if (typeof options.locale.daysOfWeek === 'object') {
                    // Create a copy of daysOfWeek to avoid modification of original
                    // options object for reusability in multiple daterangepicker instances
                    this.locale.daysOfWeek = options.locale.daysOfWeek.slice();
                }

                if (typeof options.locale.monthNames === 'object') {
                  this.locale.monthNames = options.locale.monthNames.slice();
                }

                if (typeof options.locale.firstDay === 'number') {
                    this.locale.firstDay = options.locale.firstDay;
                    var iterator = options.locale.firstDay;
                    while (iterator > 0) {
                        this.locale.daysOfWeek.push(this.locale.daysOfWeek.shift());
                        iterator--;
                    }
                }

                if (typeof options.locale.applyLabel === 'string') {
                  this.locale.applyLabel = options.locale.applyLabel;
                }

                if (typeof options.locale.cancelLabel === 'string') {
                  this.locale.cancelLabel = options.locale.cancelLabel;
                }

                if (typeof options.locale.fromLabel === 'string') {
                  this.locale.fromLabel = options.locale.fromLabel;
                }

                if (typeof options.locale.toLabel === 'string') {
                  this.locale.toLabel = options.locale.toLabel;
                }

                if (typeof options.locale.weekLabel === 'string') {
                  this.locale.weekLabel = options.locale.weekLabel;
                }

                if (typeof options.locale.customRangeLabel === 'string') {
                  this.locale.customRangeLabel = options.locale.customRangeLabel;
                }
            }

            if (typeof options.opens === 'string')
                this.opens = options.opens;

            if (typeof options.showWeekNumbers === 'boolean') {
                this.showWeekNumbers = options.showWeekNumbers;
            }

            if (typeof options.buttonClasses === 'string') {
                this.buttonClasses = [options.buttonClasses];
            }

            if (typeof options.buttonClasses === 'object') {
                this.buttonClasses = options.buttonClasses;
            }

            if (typeof options.showDropdowns === 'boolean') {
                this.showDropdowns = options.showDropdowns;
            }

            if (typeof options.singleDatePicker === 'boolean') {
                this.singleDatePicker = options.singleDatePicker;
            }

            if (typeof options.timePicker === 'boolean') {
                this.timePicker = options.timePicker;
            }

            if (typeof options.timePickerIncrement === 'number') {
                this.timePickerIncrement = options.timePickerIncrement;
            }

            if (typeof options.timePicker12Hour === 'boolean') {
                this.timePicker12Hour = options.timePicker12Hour;
            }

            var start, end, range;

            //if no start/end dates set, check if an input element contains initial values
            if (typeof options.startDate === 'undefined' && typeof options.endDate === 'undefined') {
                if ($(this.element).is('input[type=text]')) {
                    var val = $(this.element).val();
                    var split = val.split(this.separator);
                    start = end = null;
                    if (split.length == 2) {
                        start = moment(split[0], this.format);
                        end = moment(split[1], this.format);
                    } else if (this.singleDatePicker) {
                        start = moment(val, this.format);
                        end = moment(val, this.format);
                    }
                    if (start !== null && end !== null) {
                        this.startDate = start;
                        this.endDate = end;
                    }
                }
            }

            if (typeof options.ranges === 'object') {
                for (range in options.ranges) {

                    start = moment(options.ranges[range][0]);
                    end = moment(options.ranges[range][1]);

                    // If we have a min/max date set, bound this range
                    // to it, but only if it would otherwise fall
                    // outside of the min/max.
                    if (this.minDate && start.isBefore(this.minDate))
                        start = moment(this.minDate);

                    if (this.maxDate && end.isAfter(this.maxDate))
                        end = moment(this.maxDate);

                    // If the end of the range is before the minimum (if min is set) OR
                    // the start of the range is after the max (also if set) don't display this
                    // range option.
                    if ((this.minDate && end.isBefore(this.minDate)) || (this.maxDate && start.isAfter(this.maxDate))) {
                        continue;
                    }

                    this.ranges[range] = [start, end];
                }

                var list = '<ul>';
                for (range in this.ranges) {
                    list += '<li>' + range + '</li>';
                }
                list += '<li>' + this.locale.customRangeLabel + '</li>';
                list += '</ul>';
                this.container.find('.ranges ul').remove();
                this.container.find('.ranges').prepend(list);
            }

            if (typeof callback === 'function') {
                this.cb = callback;
            }

            if (!this.timePicker) {
                this.startDate = this.startDate.startOf('day');
                this.endDate = this.endDate.endOf('day');
            }

            if (this.singleDatePicker) {
                this.opens = 'right';
                this.container.find('.calendar.right').show();
                this.container.find('.calendar.left').hide();
                this.container.find('.ranges').hide();
                if (!this.container.find('.calendar.right').hasClass('single'))
                    this.container.find('.calendar.right').addClass('single');
            } else {
                this.container.find('.calendar.right').removeClass('single');
                this.container.find('.ranges').show();
            }

            this.oldStartDate = this.startDate.clone();
            this.oldEndDate = this.endDate.clone();
            this.oldChosenLabel = this.chosenLabel;

            this.leftCalendar = {
                month: moment([this.startDate.year(), this.startDate.month(), 1, this.startDate.hour(), this.startDate.minute()]),
                calendar: []
            };

            this.rightCalendar = {
                month: moment([this.endDate.year(), this.endDate.month(), 1, this.endDate.hour(), this.endDate.minute()]),
                calendar: []
            };

            if (this.opens == 'right') {
                //swap calendar positions
                var left = this.container.find('.calendar.left');
                var right = this.container.find('.calendar.right');
                left.removeClass('left').addClass('right');
                right.removeClass('right').addClass('left');
            }

            if (typeof options.ranges === 'undefined' && !this.singleDatePicker) {
                this.container.addClass('show-calendar');

				this.container.find('.ranges li').removeClass('active').find('.ranges li:last').addClass('active').html();
            }

            this.container.addClass('opens' + this.opens);

            this.updateView();
            this.updateCalendars();

        },

        setStartDate: function(startDate) {
            if (typeof startDate === 'string')
                this.startDate = moment(startDate, this.format);

            if (typeof startDate === 'object')
                this.startDate = moment(startDate);

            if (!this.timePicker)
                this.startDate = this.startDate.startOf('day');

            this.oldStartDate = this.startDate.clone();

            this.updateView();
            this.updateCalendars();
        },

        setEndDate: function(endDate) {
            if (typeof endDate === 'string')
                this.endDate = moment(endDate, this.format);

            if (typeof endDate === 'object')
                this.endDate = moment(endDate);

            if (!this.timePicker)
                this.endDate = this.endDate.endOf('day');

            this.oldEndDate = this.endDate.clone();

            this.updateView();
            this.updateCalendars();
        },

        updateView: function () {
            this.leftCalendar.month.month(this.startDate.month()).year(this.startDate.year());
            this.rightCalendar.month.month(this.endDate.month()).year(this.endDate.year());
            this.updateFormInputs();
        },

        updateFormInputs: function () {
            this.container.find('input[name=daterangepicker_start]').val(this.startDate.format(this.format));
            this.container.find('input[name=daterangepicker_end]').val(this.endDate.format(this.format));

            if (this.startDate.isSame(this.endDate) || this.startDate.isBefore(this.endDate)) {
                this.container.find('button.applyBtn').removeAttr('disabled');
            } else {
                this.container.find('button.applyBtn').attr('disabled', 'disabled');
            }

			this.container.find('.calendar-am_date.left').text(this.startDate.format(this.format2));
            this.container.find('.calendar-am_date.right').text(this.endDate.format(this.format2));
        },

        updateFromControl: function () {
            if (!this.element.is('input')) return;
            if (!this.element.val().length) return;

            var dateString = this.element.val().split(this.separator),
                start = null,
                end = null;

            if(dateString.length === 2) {
                start = moment(dateString[0], this.format);
                end = moment(dateString[1], this.format);
            }

            if (this.singleDatePicker || start === null || end === null) {
                start = moment(this.element.val(), this.format);
                end = start;
            }

            if (end.isBefore(start)) return;

            this.oldStartDate = this.startDate.clone();
            this.oldEndDate = this.endDate.clone();

            this.startDate = start;
            this.endDate = end;

            if (!this.startDate.isSame(this.oldStartDate) || !this.endDate.isSame(this.oldEndDate))
                this.notify();

            this.updateCalendars();
        },

        notify: function () {
            this.updateView();
            this.cb(this.startDate, this.endDate, this.chosenLabel);
        },

        move: function () {
            var parentOffset = { top: 0, left: 0 };

            if (!this.parentEl.is('body')) {
                parentOffset = {
                    top: this.parentEl.offset().top - this.parentEl.scrollTop(),
                    left: this.parentEl.offset().left - this.parentEl.scrollLeft()
                };
            }

            if (this.opens == 'left') {
                this.container.css({
                    top: this.element.offset().top + this.element.outerHeight() - parentOffset.top,
                    right: $(window).width() - this.element.offset().left - this.element.outerWidth() - parentOffset.left,
                    left: 'auto'
                });
                if (this.container.offset().left < 0) {
                    this.container.css({
                        right: 'auto',
                        left: 9
                    });
                }
            } else {
				
                this.container.css({
                    top: this.element.offset().top + this.element.outerHeight() - parentOffset.top,
                    /*left: this.element.offset().left - parentOffset.left,
                    right: 'auto'*/
					left: 'auto',
					right: 0
                });
                if (this.container.offset().left + this.container.outerWidth() > $(window).width()) {
                    this.container.css({
                        left: 'auto',
                        right: 0
                    });
                }
            }
        },

        toggle: function (e) {
            if (this.element.hasClass('active')) {
                this.hide();
            } else {
                this.show();
            }
        },

        show: function (e) {
            this.element.addClass('active');
            this.container.show();
            this.move();

            // Create a click proxy that is private to this instance of datepicker, for unbinding
            this._outsideClickProxy = $.proxy(function (e) { this.outsideClick(e); }, this);
            // Bind global datepicker mousedown for hiding and
            $(document)
              .on('mousedown.daterangepicker', this._outsideClickProxy)
              // also explicitly play nice with Bootstrap dropdowns, which stopPropagation when clicking them
              .on('click.daterangepicker', '[data-toggle=dropdown]', this._outsideClickProxy)
              // and also close when focus changes to outside the picker (eg. tabbing between controls)
              .on('focusin.daterangepicker', this._outsideClickProxy);

            this.element.trigger('show.daterangepicker', this);
        },

        outsideClick: function (e) {
            var target = $(e.target);
            // if the page is clicked anywhere except within the daterangerpicker/button
            // itself then call this.hide()
            if (
                target.closest(this.element).length ||
                target.closest(this.container).length ||
                target.closest('.calendar-date').length
                ) return;
            this.hide();
        },

        hide: function (e) {
            $(document)
              .off('mousedown.daterangepicker', this._outsideClickProxy)
              .off('click.daterangepicker', this._outsideClickProxy)
              .off('focusin.daterangepicker', this._outsideClickProxy);

            this.element.removeClass('active');
            this.container.hide();

            if (!this.startDate.isSame(this.oldStartDate) || !this.endDate.isSame(this.oldEndDate))
                this.notify();

            this.oldStartDate = this.startDate.clone();
            this.oldEndDate = this.endDate.clone();

            this.element.trigger('hide.daterangepicker', this);
        },

        enterRange: function (e) {
            // mouse pointer has entered a range label
            var label = e.target.innerHTML;
            if (label == this.locale.customRangeLabel) {
                this.updateView();
            } else {
                var dates = this.ranges[label];
                this.container.find('input[name=daterangepicker_start]').val(dates[0].format(this.format));
                this.container.find('input[name=daterangepicker_end]').val(dates[1].format(this.format));
				this.container.find('.calendar-am_date.left').text(dates[0].format(this.format2));
				this.container.find('.calendar-am_date.right').text(dates[1].format(this.format2));
            }
        },

        showCalendars: function() {
            this.container.addClass('show-calendar');			
			this.container.find('.ranges li').removeClass('active');
			this.container.find('.ranges li:last').addClass('active');
            this.move();
        },

        hideCalendars: function() {
            this.container.removeClass('show-calendar');
        },

        updateInputText: function() {
            if (this.element.is('input') && !this.singleDatePicker) {
                this.element.val(this.startDate.format(this.format) + this.separator + this.endDate.format(this.format));
            } else if (this.element.is('input')) {
                this.element.val(this.startDate.format(this.format));
            }
        },

        clickRange: function (e) {
            var label = e.target.innerHTML;
            this.chosenLabel = label;
            if (label == this.locale.customRangeLabel) {
                this.showCalendars();
            } else {
                var dates = this.ranges[label];

                this.startDate = dates[0];
                this.endDate = dates[1];

                if (!this.timePicker) {
                    this.startDate.startOf('day');
                    this.endDate.endOf('day');
                }

                this.leftCalendar.month.month(this.startDate.month()).year(this.startDate.year()).hour(this.startDate.hour()).minute(this.startDate.minute());
                this.rightCalendar.month.month(this.endDate.month()).year(this.endDate.year()).hour(this.endDate.hour()).minute(this.endDate.minute());
                this.updateCalendars();

                this.updateInputText();

                this.hideCalendars();
                this.hide();
                this.element.trigger('apply.daterangepicker', this);
            }
        },

        clickPrev: function (e) {
            var cal = $(e.target).parents('.calendar');
            if (cal.hasClass('left')) {
                this.leftCalendar.month.subtract('month', 1);
            } else {
                this.rightCalendar.month.subtract('month', 1);
            }
            this.updateCalendars();
        },

        clickNext: function (e) {
            var cal = $(e.target).parents('.calendar');
            if (cal.hasClass('left')) {
                this.leftCalendar.month.add('month', 1);
            } else {
                this.rightCalendar.month.add('month', 1);
            }
            this.updateCalendars();
        },

        enterDate: function (e) {

            var title = $(e.target).attr('data-title');
            var row = title.substr(1, 1);
            var col = title.substr(3, 1);
            var cal = $(e.target).parents('.calendar');

            if (cal.hasClass('left')) {
                this.container.find('input[name=daterangepicker_start]').val(this.leftCalendar.calendar[row][col].format(this.format));
				this.container.find('.calendar-am_date.left').text(this.leftCalendar.calendar[row][col].format(this.format2));
            } else {
                this.container.find('input[name=daterangepicker_end]').val(this.rightCalendar.calendar[row][col].format(this.format));
				this.container.find('.calendar-am_date.right').text(this.rightCalendar.calendar[row][col].format(this.format2));
            }
        },

        clickDate: function (e) {
            var title = $(e.target).attr('data-title');
            var row = title.substr(1, 1);
            var col = title.substr(3, 1);
            var cal = $(e.target).parents('.calendar');

            var startDate, endDate;
            if (cal.hasClass('left')) {
                startDate = this.leftCalendar.calendar[row][col];
                endDate = this.endDate;
                if (typeof this.dateLimit === 'object') {
                    var maxDate = moment(startDate).add(this.dateLimit).startOf('day');
                    if (endDate.isAfter(maxDate)) {
                        endDate = maxDate;
                    }
                }
            } else {
                startDate = this.startDate;
                endDate = this.rightCalendar.calendar[row][col];
                if (typeof this.dateLimit === 'object') {
                    var minDate = moment(endDate).subtract(this.dateLimit).startOf('day');
                    if (startDate.isBefore(minDate)) {
                        startDate = minDate;
                    }
                }
            }

            if (this.singleDatePicker && cal.hasClass('left')) {
                endDate = startDate.clone();
            } else if (this.singleDatePicker && cal.hasClass('right')) {
                startDate = endDate.clone();
            }

            cal.find('td').removeClass('active');

            if (startDate.isSame(endDate) || startDate.isBefore(endDate)) {
                $(e.target).addClass('active');
                this.startDate = startDate;
                this.endDate = endDate;
                this.chosenLabel = this.locale.customRangeLabel;
            } else if (startDate.isAfter(endDate)) {
                $(e.target).addClass('active');
                var difference = this.endDate.diff(this.startDate);
                this.startDate = startDate;
                this.endDate = moment(startDate).add('ms', difference);
                this.chosenLabel = this.locale.customRangeLabel;
            }

            this.leftCalendar.month.month(this.startDate.month()).year(this.startDate.year());
            this.rightCalendar.month.month(this.endDate.month()).year(this.endDate.year());
            this.updateCalendars();

            if (!this.timePicker)
                endDate.endOf('day');

            if (this.singleDatePicker)
                this.clickApply();
        },

        clickApply: function (e) {
            this.updateInputText();
            this.hide();
            this.element.trigger('apply.daterangepicker', this);
        },

        clickCancel: function (e) {
            this.startDate = this.oldStartDate;
            this.endDate = this.oldEndDate;
            this.chosenLabel = this.oldChosenLabel;
            this.updateView();
            this.updateCalendars();
            this.hide();
            this.element.trigger('cancel.daterangepicker', this);
        },

        updateMonthYear: function (e) {
            var isLeft = $(e.target).closest('.calendar').hasClass('left'),
                leftOrRight = isLeft ? 'left' : 'right',
                cal = this.container.find('.calendar.'+leftOrRight);

            // Month must be Number for new moment versions
            var month = parseInt(cal.find('.monthselect').val(), 10);
            var year = cal.find('.yearselect').val();

            this[leftOrRight+'Calendar'].month.month(month).year(year);
            this.updateCalendars();
        },

        updateTime: function(e) {

            var cal = $(e.target).closest('.calendar'),
                isLeft = cal.hasClass('left');

            var hour = parseInt(cal.find('.hourselect').val(), 10);
            var minute = parseInt(cal.find('.minuteselect').val(), 10);

            if (this.timePicker12Hour) {
                var ampm = cal.find('.ampmselect').val();
                if (ampm === 'PM' && hour < 12)
                    hour += 12;
                if (ampm === 'AM' && hour === 12)
                    hour = 0;
            }

            if (isLeft) {
                var start = this.startDate.clone();
                start.hour(hour);
                start.minute(minute);
                this.startDate = start;
                this.leftCalendar.month.hour(hour).minute(minute);
            } else {
                var end = this.endDate.clone();
                end.hour(hour);
                end.minute(minute);
                this.endDate = end;
                this.rightCalendar.month.hour(hour).minute(minute);
            }

            this.updateCalendars();
        },

        updateCalendars: function () {
            this.leftCalendar.calendar = this.buildCalendar(this.leftCalendar.month.month(), this.leftCalendar.month.year(), this.leftCalendar.month.hour(), this.leftCalendar.month.minute(), 'left');
            this.rightCalendar.calendar = this.buildCalendar(this.rightCalendar.month.month(), this.rightCalendar.month.year(), this.rightCalendar.month.hour(), this.rightCalendar.month.minute(), 'right');
            this.container.find('.calendar.left').empty().html(this.renderCalendar(this.leftCalendar.calendar, this.startDate, this.minDate, this.maxDate));
            this.container.find('.calendar.right').empty().html(this.renderCalendar(this.rightCalendar.calendar, this.endDate, this.startDate, this.maxDate));

            this.container.find('.ranges li').removeClass('active');
            var customRange = true;
            var i = 0;
            for (var range in this.ranges) {
                if (this.timePicker) {
                    if (this.startDate.isSame(this.ranges[range][0]) && this.endDate.isSame(this.ranges[range][1])) {
                        customRange = false;
                        this.chosenLabel = this.container.find('.ranges li:eq(' + i + ')')
                            .addClass('active').html();
                    }
                } else {
                    //ignore times when comparing dates if time picker is not enabled
                    if (this.startDate.format('YYYY-MM-DD') == this.ranges[range][0].format('YYYY-MM-DD') && this.endDate.format('YYYY-MM-DD') == this.ranges[range][1].format('YYYY-MM-DD')) {
                        customRange = false;
                        this.chosenLabel = this.container.find('.ranges li:eq(' + i + ')')
                            .addClass('active').html();
                    }
                }
                i++;
            }

            if (customRange) {
                this.chosenLabel = this.container.find('.ranges li:last')
                    .addClass('active').html();
            }
        },

        buildCalendar: function (month, year, hour, minute, side) {
            var firstDay = moment([year, month, 1]);
            var lastMonth = moment(firstDay).subtract('month', 1).month();
            var lastYear = moment(firstDay).subtract('month', 1).year();

            var daysInLastMonth = moment([lastYear, lastMonth]).daysInMonth();

            var dayOfWeek = firstDay.day();

            var i;

            //initialize a 6 rows x 7 columns array for the calendar
            var calendar = [];
            for (i = 0; i < 6; i++) {
                calendar[i] = [];
            }

            //populate the calendar with date objects
            var startDay = daysInLastMonth - dayOfWeek + this.locale.firstDay + 1;
            if (startDay > daysInLastMonth)
                startDay -= 7;

            if (dayOfWeek == this.locale.firstDay)
                startDay = daysInLastMonth - 6;

            var curDate = moment([lastYear, lastMonth, startDay, 12, minute]);
            var col, row;
            for (i = 0, col = 0, row = 0; i < 42; i++, col++, curDate = moment(curDate).add('hour', 24)) {
                if (i > 0 && col % 7 === 0) {
                    col = 0;
                    row++;
                }
                calendar[row][col] = curDate.clone().hour(hour);
                curDate.hour(12);
            }

            return calendar;
        },

        renderDropdowns: function (selected, minDate, maxDate) {
            var currentMonth = selected.month();
            var monthHtml = '<select class="monthselect">';
            var inMinYear = false;
            var inMaxYear = false;

            for (var m = 0; m < 12; m++) {
                if ((!inMinYear || m >= minDate.month()) && (!inMaxYear || m <= maxDate.month())) {
                    monthHtml += "<option value='" + m + "'" +
                        (m === currentMonth ? " selected='selected'" : "") +
                        ">" + this.locale.monthNames[m] + "</option>";
                }
            }
            monthHtml += "</select>";

            var currentYear = selected.year();
            var maxYear = (maxDate && maxDate.year()) || (currentYear + 5);
            var minYear = (minDate && minDate.year()) || (currentYear - 50);
            var yearHtml = '<select class="yearselect">';

            for (var y = minYear; y <= maxYear; y++) {
                yearHtml += '<option value="' + y + '"' +
                    (y === currentYear ? ' selected="selected"' : '') +
                    '>' + y + '</option>';
            }

            yearHtml += '</select>';

            return monthHtml + yearHtml;
        },

        renderCalendar: function (calendar, selected, minDate, maxDate) {

            var html = '<div class="calendar-date">';
            html += '<table class="table-condensed">';
            html += '<thead>';
            html += '<tr>';

            // add empty cell for week number
            if (this.showWeekNumbers)
                html += '<th></th>';

            if (!minDate || minDate.isBefore(calendar[1][1])) {
                html += '<th class="prev available"><i class="fa fa-arrow-left icon-arrow-left glyphicon glyphicon-arrow-left"><</i></th>';
            } else {
                html += '<th></th>';
            }

            var dateHtml = this.locale.monthNames[calendar[1][1].month()] + calendar[1][1].format(" YYYY");

            if (this.showDropdowns) {
                dateHtml = this.renderDropdowns(calendar[1][1], minDate, maxDate);
            }

            html += '<th colspan="5" class="month">' + dateHtml + '</th>';
            if (!maxDate || maxDate.isAfter(calendar[1][1])) {
                html += '<th class="next available"><i class="fa fa-arrow-right icon-arrow-right glyphicon glyphicon-arrow-right">></i></th>';
            } else {
                html += '<th></th>';
            }

            html += '</tr>';
            html += '<tr>';

            // add week number label
            if (this.showWeekNumbers)
                html += '<th class="week">' + this.locale.weekLabel + '</th>';

            $.each(this.locale.daysOfWeek, function (index, dayOfWeek) {
                html += '<th>' + dayOfWeek + '</th>';
            });

            html += '</tr>';
            html += '</thead>';
            html += '<tbody>';

            for (var row = 0; row < 6; row++) {
                html += '<tr>';

                // add week number
                if (this.showWeekNumbers)
                    html += '<td class="week">' + calendar[row][0].week() + '</td>';

                for (var col = 0; col < 7; col++) {
                    var cname = 'available ';
                    cname += (calendar[row][col].month() == calendar[1][1].month()) ? '' : 'off';

                    if ((minDate && calendar[row][col].isBefore(minDate, 'day')) || (maxDate && calendar[row][col].isAfter(maxDate, 'day'))) {
                        cname = ' off disabled ';
                    } else if (calendar[row][col].format('YYYY-MM-DD') == selected.format('YYYY-MM-DD')) {
                        cname += ' active ';
                        if (calendar[row][col].format('YYYY-MM-DD') == this.startDate.format('YYYY-MM-DD')) {
                            cname += ' start-date ';
                        }
                        if (calendar[row][col].format('YYYY-MM-DD') == this.endDate.format('YYYY-MM-DD')) {
                            cname += ' end-date ';
                        }
                    } else if (calendar[row][col] >= this.startDate && calendar[row][col] <= this.endDate) {
                        cname += ' in-range ';
                        if (calendar[row][col].isSame(this.startDate)) { cname += ' start-date '; }
                        if (calendar[row][col].isSame(this.endDate)) { cname += ' end-date '; }
                    }

                    var title = 'r' + row + 'c' + col;
                    html += '<td class="' + cname.replace(/\s+/g, ' ').replace(/^\s?(.*?)\s?$/, '$1') + '" data-title="' + title + '">' + calendar[row][col].date() + '</td>';
                }
                html += '</tr>';
            }

            html += '</tbody>';
            html += '</table>';
            html += '</div>';

            var i;
            if (this.timePicker) {

                html += '<div class="calendar-time">';
                html += '<select class="hourselect">';
                var start = 0;
                var end = 23;
                var selected_hour = selected.hour();
                if (this.timePicker12Hour) {
                    start = 1;
                    end = 12;
                    if (selected_hour >= 12)
                        selected_hour -= 12;
                    if (selected_hour === 0)
                        selected_hour = 12;
                }

                for (i = start; i <= end; i++) {
                    if (i == selected_hour) {
                        html += '<option value="' + i + '" selected="selected">' + i + '</option>';
                    } else {
                        html += '<option value="' + i + '">' + i + '</option>';
                    }
                }

                html += '</select> : ';

                html += '<select class="minuteselect">';

                for (i = 0; i < 60; i += this.timePickerIncrement) {
                    var num = i;
                    if (num < 10)
                        num = '0' + num;
                    if (i == selected.minute()) {
                        html += '<option value="' + i + '" selected="selected">' + num + '</option>';
                    } else {
                        html += '<option value="' + i + '">' + num + '</option>';
                    }
                }

                html += '</select> ';

                if (this.timePicker12Hour) {
                    html += '<select class="ampmselect">';
                    if (selected.hour() >= 12) {
                        html += '<option value="AM">AM</option><option value="PM" selected="selected">PM</option>';
                    } else {
                        html += '<option value="AM" selected="selected">AM</option><option value="PM">PM</option>';
                    }
                    html += '</select>';
                }

                html += '</div>';

            }

            return html;

        },

        remove: function() {
            this.container.remove();
            this.element.off('.daterangepicker');
            this.element.removeData('daterangepicker');

        }

    };

    $.fn.daterangepicker = function (options, cb) {
        this.each(function () {
            var el = $(this);
            if (el.data('daterangepicker'))
                el.data('daterangepicker').remove();
            el.data('daterangepicker', new DateRangePicker(el, options, cb));
        });
        return this;
    };

}(window.jQuery, window.moment);


/*

Uniform v2.1.0
Copyright © 2009 Josh Pyles / Pixelmatrix Design LLC
http://pixelmatrixdesign.com

Requires jQuery 1.3 or newer

Much thanks to Thomas Reynolds and Buck Wilson for their help and advice on
this.

Disabling text selection is made possible by Mathias Bynens
<http://mathiasbynens.be/> and his noSelect plugin.
<https://github.com/mathiasbynens/jquery-noselect>, which is embedded.

Also, thanks to David Kaneda and Eugene Bond for their contributions to the
plugin.

Tyler Akins has also rewritten chunks of the plugin, helped close many issues,
and ensured version 2 got out the door.

License:
MIT License - http://www.opensource.org/licenses/mit-license.php

Enjoy!

*/
/*global jQuery, window, document, navigator*/

(function ($, undef) {
  "use strict";

  /**
   * Use .prop() if jQuery supports it, otherwise fall back to .attr()
   *
   * @param jQuery $el jQuery'd element on which we're calling attr/prop
   * @param ... All other parameters are passed to jQuery's function
   * @return The result from jQuery
   */
  function attrOrProp($el) {
    var args = Array.prototype.slice.call(arguments, 1);

    if ($el.prop) {
      // jQuery 1.6+
      return $el.prop.apply($el, args);
    }

    // jQuery 1.5 and below
    return $el.attr.apply($el, args);
  }

  /**
   * For backwards compatibility with older jQuery libraries, only bind
   * one thing at a time.  Also, this function adds our namespace to
   * events in one consistent location, shrinking the minified code.
   *
   * The properties on the events object are the names of the events
   * that we are supposed to add to.  It can be a space separated list.
   * The namespace will be added automatically.
   *
   * @param jQuery $el
   * @param Object options Uniform options for this element
   * @param Object events Events to bind, properties are event names
   */
  function bindMany($el, options, events) {
    var name, namespaced;

    for (name in events) {
      if (events.hasOwnProperty(name)) {
        namespaced = name.replace(/ |$/g, options.eventNamespace);
        $el.bind(namespaced, events[name]);
      }
    }
  }

  /**
   * Bind the hover, active, focus, and blur UI updates
   *
   * @param jQuery $el Original element
   * @param jQuery $target Target for the events (our div/span)
   * @param Object options Uniform options for the element $target
   */
  function bindUi($el, $target, options) {
    bindMany($el, options, {
      focus: function () {
        $target.addClass(options.focusClass);
      },
      blur: function () {
        $target.removeClass(options.focusClass);
        $target.removeClass(options.activeClass);
      },
      mouseenter: function () {
        $target.addClass(options.hoverClass);
      },
      mouseleave: function () {
        $target.removeClass(options.hoverClass);
        $target.removeClass(options.activeClass);
      },
      "mousedown touchbegin": function () {
        if (!$el.is(":disabled")) {
          $target.addClass(options.activeClass);
        }
      },
      "mouseup touchend": function () {
        $target.removeClass(options.activeClass);
      }
    });
  }

  /**
   * Remove the hover, focus, active classes.
   *
   * @param jQuery $el Element with classes
   * @param Object options Uniform options for the element
   */
  function classClearStandard($el, options) {
    $el.removeClass(options.hoverClass + " " + options.focusClass + " " + options.activeClass);
  }

  /**
   * Add or remove a class, depending on if it's "enabled"
   *
   * @param jQuery $el Element that has the class added/removed
   * @param String className Class or classes to add/remove
   * @param Boolean enabled True to add the class, false to remove
   */
  function classUpdate($el, className, enabled) {
    if (enabled) {
      $el.addClass(className);
    } else {
      $el.removeClass(className);
    }
  }

  /**
   * Updating the "checked" property can be a little tricky.  This
   * changed in jQuery 1.6 and now we can pass booleans to .prop().
   * Prior to that, one either adds an attribute ("checked=checked") or
   * removes the attribute.
   *
   * @param jQuery $tag Our Uniform span/div
   * @param jQuery $el Original form element
   * @param Object options Uniform options for this element
   */
  function classUpdateChecked($tag, $el, options) {
    var c = "checked",
      isChecked = $el.is(":" + c);

    if ($el.prop) {
      // jQuery 1.6+
      $el.prop(c, isChecked);
    } else {
      // jQuery 1.5 and below
      if (isChecked) {
        $el.attr(c, c);
      } else {
        $el.removeAttr(c);
      }
    }

    classUpdate($tag, options.checkedClass, isChecked);
  }

  /**
   * Set or remove the "disabled" class for disabled elements, based on
   * if the
   *
   * @param jQuery $tag Our Uniform span/div
   * @param jQuery $el Original form element
   * @param Object options Uniform options for this element
   */
  function classUpdateDisabled($tag, $el, options) {
    classUpdate($tag, options.disabledClass, $el.is(":disabled"));
  }

  /**
   * Wrap an element inside of a container or put the container next
   * to the element.  See the code for examples of the different methods.
   *
   * Returns the container that was added to the HTML.
   *
   * @param jQuery $el Element to wrap
   * @param jQuery $container Add this new container around/near $el
   * @param String method One of "after", "before" or "wrap"
   * @return $container after it has been cloned for adding to $el
   */
  function divSpanWrap($el, $container, method) {
    switch (method) {
    case "after":
      // Result:  <element /> <container />
      $el.after($container);
      return $el.next();
    case "before":
      // Result:  <container /> <element />
      $el.before($container);
      return $el.prev();
    case "wrap":
      // Result:  <container> <element /> </container>
      $el.wrap($container);
      return $el.parent();
    }

    return null;
  }


  /**
   * Create a div/span combo for uniforming an element
   *
   * @param jQuery $el Element to wrap
   * @param Object options Options for the element, set by the user
   * @param Object divSpanConfig Options for how we wrap the div/span
   * @return Object Contains the div and span as properties
   */
  function divSpan($el, options, divSpanConfig) {
    var $div, $span, id;

    if (!divSpanConfig) {
      divSpanConfig = {};
    }

    divSpanConfig = $.extend({
      bind: {},
      divClass: null,
      divWrap: "wrap",
      spanClass: null,
      spanHtml: null,
      spanWrap: "wrap"
    }, divSpanConfig);

    $div = $('<div />');
    $span = $('<span />');

    // Automatically hide this div/span if the element is hidden.
    // Do not hide if the element is hidden because a parent is hidden.
    if (options.autoHide && $el.is(':hidden') && $el.css('display') === 'none') {
      $div.hide();
    }

    if (divSpanConfig.divClass) {
      $div.addClass(divSpanConfig.divClass);
    }

    if (options.wrapperClass) {
      $div.addClass(options.wrapperClass);
    }

    if (divSpanConfig.spanClass) {
      $span.addClass(divSpanConfig.spanClass);
    }

    id = attrOrProp($el, 'id');

    if (options.useID && id) {
      attrOrProp($div, 'id', options.idPrefix + '-' + id);
    }

    if (divSpanConfig.spanHtml) {
      $span.html(divSpanConfig.spanHtml);
    }

    $div = divSpanWrap($el, $div, divSpanConfig.divWrap);
    $span = divSpanWrap($el, $span, divSpanConfig.spanWrap);
    classUpdateDisabled($div, $el, options);
    return {
      div: $div,
      span: $span
    };
  }


  /**
   * Wrap an element with a span to apply a global wrapper class
   *
   * @param jQuery $el Element to wrap
   * @param object options
   * @return jQuery Wrapper element
   */
  function wrapWithWrapperClass($el, options) {
    var $span;

    if (!options.wrapperClass) {
      return null;
    }

    $span = $('<span />').addClass(options.wrapperClass);
    $span = divSpanWrap($el, $span, "wrap");
    return $span;
  }


  /**
   * Test if high contrast mode is enabled.
   *
   * In high contrast mode, background images can not be set and
   * they are always returned as 'none'.
   *
   * @return boolean True if in high contrast mode
   */
  function highContrast() {
    var c, $div, el, rgb;

    // High contrast mode deals with white and black
    rgb = 'rgb(120,2,153)';
    $div = $('<div style="width:0;height:0;color:' + rgb + '">');
    $('body').append($div);
    el = $div.get(0);

    // $div.css() will get the style definition, not
    // the actually displaying style
    if (window.getComputedStyle) {
      c = window.getComputedStyle(el, '').color;
    } else {
      c = (el.currentStyle || el.style || {}).color;
    }

    $div.remove();
    return c.replace(/ /g, '') !== rgb;
  }


  /**
   * Change text into safe HTML
   *
   * @param String text
   * @return String HTML version
   */
  function htmlify(text) {
    if (!text) {
      return "";
    }

    return $('<span />').text(text).html();
  }

  /**
   * If not MSIE, return false.
   * If it is, return the version number.
   *
   * @return false|number
   */
  function isMsie() {
    return navigator.cpuClass && !navigator.product;
  }

  /**
   * Return true if this version of IE allows styling
   *
   * @return boolean
   */
  function isMsieSevenOrNewer() {
    if (typeof window.XMLHttpRequest !== 'undefined') {
      return true;
    }

    return false;
  }

  /**
   * Test if the element is a multiselect
   *
   * @param jQuery $el Element
   * @return boolean true/false
   */
  function isMultiselect($el) {
    var elSize;

    if ($el[0].multiple) {
      return true;
    }

    elSize = attrOrProp($el, "size");

    if (!elSize || elSize <= 1) {
      return false;
    }

    return true;
  }

  /**
   * Meaningless utility function.  Used mostly for improving minification.
   *
   * @return false
   */
  function returnFalse() {
    return false;
  }

  /**
   * noSelect plugin, very slightly modified
   * http://mths.be/noselect v1.0.3
   *
   * @param jQuery $elem Element that we don't want to select
   * @param Object options Uniform options for the element
   */
  function noSelect($elem, options) {
    var none = 'none';
    bindMany($elem, options, {
      'selectstart dragstart mousedown': returnFalse
    });

    $elem.css({
      MozUserSelect: none,
      msUserSelect: none,
      webkitUserSelect: none,
      userSelect: none
    });
  }

  /**
   * Updates the filename tag based on the value of the real input
   * element.
   *
   * @param jQuery $el Actual form element
   * @param jQuery $filenameTag Span/div to update
   * @param Object options Uniform options for this element
   */
  function setFilename($el, $filenameTag, options) {
    var filename = $el.val();

    if (filename === "") {
      filename = options.fileDefaultHtml;
    } else {
      filename = filename.split(/[\/\\]+/);
      filename = filename[(filename.length - 1)];
    }

    $filenameTag.text(filename);
  }


  /**
   * Function from jQuery to swap some CSS values, run a callback,
   * then restore the CSS.  Modified to pass JSLint and handle undefined
   * values with 'use strict'.
   *
   * @param jQuery $el Element
   * @param object newCss CSS values to swap out
   * @param Function callback Function to run
   */
  function swap($elements, newCss, callback) {
    var restore, item;

    restore = [];

    $elements.each(function () {
      var name;

      for (name in newCss) {
        if (Object.prototype.hasOwnProperty.call(newCss, name)) {
          restore.push({
            el: this,
            name: name,
            old: this.style[name]
          });

          this.style[name] = newCss[name];
        }
      }
    });

    callback();

    while (restore.length) {
      item = restore.pop();
      item.el.style[item.name] = item.old;
    }
  }


  /**
   * The browser doesn't provide sizes of elements that are not visible.
   * This will clone an element and add it to the DOM for calculations.
   *
   * @param jQuery $el
   * @param String method
   */
  function sizingInvisible($el, callback) {
    var targets;

    // We wish to target ourselves and any parents as long as
    // they are not visible
    targets = $el.parents();
    targets.push($el[0]);
    targets = targets.not(':visible');
    swap(targets, {
      visibility: "hidden",
      display: "block",
      position: "absolute"
    }, callback);
  }


  /**
   * Standard way to unwrap the div/span combination from an element
   *
   * @param jQuery $el Element that we wish to preserve
   * @param Object options Uniform options for the element
   * @return Function This generated function will perform the given work
   */
  function unwrapUnwrapUnbindFunction($el, options) {
    return function () {
      $el.unwrap().unwrap().unbind(options.eventNamespace);
    };
  }

  var allowStyling = true,  // False if IE6 or other unsupported browsers
    highContrastTest = false,  // Was the high contrast test ran?
    uniformHandlers = [  // Objects that take care of "unification"
      {
        // Buttons
        match: function ($el) {
          return $el.is("a, button, :submit, :reset, input[type='button']");
        },
        apply: function ($el, options) {
          var $div, defaultSpanHtml, ds, getHtml, doingClickEvent;
          defaultSpanHtml = options.submitDefaultHtml;

          if ($el.is(":reset")) {
            defaultSpanHtml = options.resetDefaultHtml;
          }

          if ($el.is("a, button")) {
            // Use the HTML inside the tag
            getHtml = function () {
              return $el.html() || defaultSpanHtml;
            };
          } else {
            // Use the value property of the element
            getHtml = function () {
              return htmlify(attrOrProp($el, "value")) || defaultSpanHtml;
            };
          }

          ds = divSpan($el, options, {
            divClass: options.buttonClass,
            spanHtml: getHtml(),
          });
          $div = ds.div;
          bindUi($el, $div, options);
          doingClickEvent = false;
          bindMany($div, options, {
            "click touchend": function () {
              var ev, res, target, href;

              if (doingClickEvent) {
                return;
              }

              if ($el.is(':disabled')) {
                return;
              }

              doingClickEvent = true;

              if ($el[0].dispatchEvent) {
                ev = document.createEvent("MouseEvents");
                ev.initEvent("click", true, true);
                res = $el[0].dispatchEvent(ev);

                if ($el.is('a') && res) {
                  target = attrOrProp($el, 'target');
                  href = attrOrProp($el, 'href');

                  if (!target || target === '_self') {
                    document.location.href = href;
                  } else {
                    window.open(href, target);
                  }
                }
              } else {
                $el.click();
              }

              doingClickEvent = false;
            }
          });
          noSelect($div, options);
          return {
            remove: function () {
              // Move $el out
              $div.after($el);

              // Remove div and span
              $div.remove();

              // Unbind events
              $el.unbind(options.eventNamespace);
              return $el;
            },
            update: function () {
              classClearStandard($div, options);
              classUpdateDisabled($div, $el, options);
              $el.detach();
              ds.span.html(getHtml()).append($el);
            }
          };
        }
      },
      {
        // Checkboxes
        match: function ($el) {
          return $el.is(":checkbox");
        },
        apply: function ($el, options) {
          var ds, $div, $span;
          ds = divSpan($el, options, {
            divClass: options.checkboxClass
          });
          $div = ds.div;
          $span = ds.span;

          // Add focus classes, toggling, active, etc.
          bindUi($el, $div, options);
          bindMany($el, options, {
            "click touchend": function () {
              classUpdateChecked($span, $el, options);
            }
          });
          classUpdateChecked($span, $el, options);
          return {
            remove: unwrapUnwrapUnbindFunction($el, options),
            update: function () {
              classClearStandard($div, options);
              $span.removeClass(options.checkedClass);
              classUpdateChecked($span, $el, options);
              classUpdateDisabled($div, $el, options);
            }
          };
        }
      },
      {
        // File selection / uploads
        match: function ($el) {
          return $el.is(":file");
        },
        apply: function ($el, options) {
          var ds, $div, $filename, $button;

          // The "span" is the button
          ds = divSpan($el, options, {
            divClass: options.fileClass,
            spanClass: options.fileButtonClass,
            spanHtml: options.fileButtonHtml,
            spanWrap: "after"
          });
          $div = ds.div;
          $button = ds.span;
          $filename = $("<span />").html(options.fileDefaultHtml);
          $filename.addClass(options.filenameClass);
          $filename = divSpanWrap($el, $filename, "after");

          // Set the size
          if (!attrOrProp($el, "size")) {
            attrOrProp($el, "size", $div.width() / 10);
          }

          // Actions
          function filenameUpdate() {
            setFilename($el, $filename, options);
          }

          bindUi($el, $div, options);

          // Account for input saved across refreshes
          filenameUpdate();

          // IE7 doesn't fire onChange until blur or second fire.
          if (isMsie()) {
            // IE considers browser chrome blocking I/O, so it
            // suspends tiemouts until after the file has
            // been selected.
            bindMany($el, options, {
              click: function () {
                $el.trigger("change");
                setTimeout(filenameUpdate, 0);
              }
            });
          } else {
            // All other browsers behave properly
            bindMany($el, options, {
              change: filenameUpdate
            });
          }

          noSelect($filename, options);
          noSelect($button, options);
          return {
            remove: function () {
              // Remove filename and button
              $filename.remove();
              $button.remove();

              // Unwrap parent div, remove events
              return $el.unwrap().unbind(options.eventNamespace);
            },
            update: function () {
              classClearStandard($div, options);
              setFilename($el, $filename, options);
              classUpdateDisabled($div, $el, options);
            }
          };
        }
      },
      {
        // Input fields (text)
        match: function ($el) {
          if ($el.is("input")) {
            var t = (" " + attrOrProp($el, "type") + " ").toLowerCase(),
              allowed = " color date datetime datetime-local email month number password search tel text time url week ";
            return allowed.indexOf(t) >= 0;
          }

          return false;
        },
        apply: function ($el, options) {
          var elType, $wrapper;

          elType = attrOrProp($el, "type");
          $el.addClass(options.inputClass);
          $wrapper = wrapWithWrapperClass($el, options);
          bindUi($el, $el, options);

          if (options.inputAddTypeAsClass) {
            $el.addClass(elType);
          }

          return {
            remove: function () {
              $el.removeClass(options.inputClass);

              if (options.inputAddTypeAsClass) {
                $el.removeClass(elType);
              }

              if ($wrapper) {
                $el.unwrap();
              }
            },
            update: returnFalse
          };
        }
      },
      {
        // Radio buttons
        match: function ($el) {
          return $el.is(":radio");
        },
        apply: function ($el, options) {
          var ds, $div, $span;
          ds = divSpan($el, options, {
            divClass: options.radioClass
          });
          $div = ds.div;
          $span = ds.span;

          // Add classes for focus, handle active, checked
          bindUi($el, $div, options);
          bindMany($el, options, {
            "click touchend": function () {
              // Find all radios with the same name, then update
              // them with $.uniform.update() so the right
              // per-element options are used
              $.uniform.update($(':radio[name="' + attrOrProp($el, "name") + '"]'));
            }
          });
          classUpdateChecked($span, $el, options);
          return {
            remove: unwrapUnwrapUnbindFunction($el, options),
            update: function () {
              classClearStandard($div, options);
              classUpdateChecked($span, $el, options);
              classUpdateDisabled($div, $el, options);
            }
          };
        }
      },
      {
        // Select lists, but do not style multiselects here
        match: function ($el) {
          if ($el.is("select") && !isMultiselect($el)) {
            return true;
          }

          return false;
        },
        apply: function ($el, options) {
          var ds, $div, $span, origElemWidth;

          if (options.selectAutoWidth) {
            sizingInvisible($el, function () {
              origElemWidth = $el.width();
            });
          }

          ds = divSpan($el, options, {
            divClass: options.selectClass,
            spanHtml: ($el.find(":selected:first") || $el.find("option:first")).html(),
            spanWrap: "before"
          });
          $div = ds.div;
          $span = ds.span;

          if (options.selectAutoWidth) {
            // Use the width of the select and adjust the
            // span and div accordingly
            sizingInvisible($el, function () {
              // Force "display: block" - related to bug #287
              swap($([ $span[0], $div[0] ]), {
                display: "block"
              }, function () {
                var spanPad;
                spanPad = $span.outerWidth() - $span.width();
                $div.width(origElemWidth + spanPad);
                $span.width(origElemWidth);
              });
            });
          } else {
            // Force the select to fill the size of the div
            $div.addClass('fixedWidth');
          }

          // Take care of events
          bindUi($el, $div, options);
          bindMany($el, options, {
            change: function () {
              $span.html($el.find(":selected").html());
              $div.removeClass(options.activeClass);
            },
            "click touchend": function () {
              // IE7 and IE8 may not update the value right
              // until after click event - issue #238
              var selHtml = $el.find(":selected").html();

              if ($span.html() !== selHtml) {
                // Change was detected
                // Fire the change event on the select tag
                $el.trigger('change');
              }
            },
            keyup: function () {
              $span.html($el.find(":selected").html());
            }
          });
          noSelect($span, options);
          return {
            remove: function () {
              // Remove sibling span
              $span.remove();

              // Unwrap parent div
              $el.unwrap().unbind(options.eventNamespace);
              return $el;
            },
            update: function () {
              if (options.selectAutoWidth) {
                // Easier to remove and reapply formatting
                $.uniform.restore($el);
                $el.uniform(options);
              } else {
                classClearStandard($div, options);

                // Reset current selected text
                $span.html($el.find(":selected").html());
                classUpdateDisabled($div, $el, options);
              }
            }
          };
        }
      },
      {
        // Select lists - multiselect lists only
        match: function ($el) {
          if ($el.is("select") && isMultiselect($el)) {
            return true;
          }

          return false;
        },
        apply: function ($el, options) {
          var $wrapper;

          $el.addClass(options.selectMultiClass);
          $wrapper = wrapWithWrapperClass($el, options);
          bindUi($el, $el, options);

          return {
            remove: function () {
              $el.removeClass(options.selectMultiClass);

              if ($wrapper) {
                $el.unwrap();
              }
            },
            update: returnFalse
          };
        }
      },
      {
        // Textareas
        match: function ($el) {
          return $el.is("textarea");
        },
        apply: function ($el, options) {
          var $wrapper;

          $el.addClass(options.textareaClass);
          $wrapper = wrapWithWrapperClass($el, options);
          bindUi($el, $el, options);

          return {
            remove: function () {
              $el.removeClass(options.textareaClass);

              if ($wrapper) {
                $el.unwrap();
              }
            },
            update: returnFalse
          };
        }
      }
    ];

  // IE6 can't be styled - can't set opacity on select
  if (isMsie() && !isMsieSevenOrNewer()) {
    allowStyling = false;
  }

  $.uniform = {
    // Default options that can be overridden globally or when uniformed
    // globally:  $.uniform.defaults.fileButtonHtml = "Pick A File";
    // on uniform:  $('input').uniform({fileButtonHtml: "Pick a File"});
    defaults: {
      activeClass: "active",
      autoHide: true,
      buttonClass: "button",
      checkboxClass: "checker",
      checkedClass: "checked",
      disabledClass: "disabled",
      eventNamespace: ".uniform",
      fileButtonClass: "action",
      fileButtonHtml: "Choose File",
      fileClass: "uploader",
      fileDefaultHtml: "No file selected",
      filenameClass: "filename",
      focusClass: "focus",
      hoverClass: "hover",
      idPrefix: "uniform",
      inputAddTypeAsClass: true,
      inputClass: "uniform-input",
      radioClass: "radio",
      resetDefaultHtml: "Reset",
      resetSelector: false,  // We'll use our own function when you don't specify one
      selectAutoWidth: true,
      selectClass: "selector",
      selectMultiClass: "uniform-multiselect",
      submitDefaultHtml: "Submit",  // Only text allowed
      textareaClass: "uniform",
      useID: true,
      wrapperClass: null
    },

    // All uniformed elements - DOM objects
    elements: []
  };

  $.fn.uniform = function (options) {
    var el = this;
    options = $.extend({}, $.uniform.defaults, options);

    // If we are in high contrast mode, do not allow styling
    if (!highContrastTest) {
      highContrastTest = true;

      if (highContrast()) {
        allowStyling = false;
      }
    }

    // Only uniform on browsers that work
    if (!allowStyling) {
      return this;
    }

    // Code for specifying a reset button
    if (options.resetSelector) {
      $(options.resetSelector).mouseup(function () {
        window.setTimeout(function () {
          $.uniform.update(el);
        }, 10);
      });
    }

    return this.each(function () {
      var $el = $(this), i, handler, callbacks;

      // Avoid uniforming elements already uniformed - just update
      if ($el.data("uniformed")) {
        $.uniform.update($el);
        return;
      }

      // See if we have any handler for this type of element
      for (i = 0; i < uniformHandlers.length; i = i + 1) {
        handler = uniformHandlers[i];

        if (handler.match($el, options)) {
          callbacks = handler.apply($el, options);
          $el.data("uniformed", callbacks);

          // Store element in our global array
          $.uniform.elements.push($el.get(0));
          return;
        }
      }

      // Could not style this element
    });
  };

  $.uniform.restore = $.fn.uniform.restore = function (elem) {
    if (elem === undef) {
      elem = $.uniform.elements;
    }

    $(elem).each(function () {
      var $el = $(this), index, elementData;
      elementData = $el.data("uniformed");

      // Skip elements that are not uniformed
      if (!elementData) {
        return;
      }

      // Unbind events, remove additional markup that was added
      elementData.remove();

      // Remove item from list of uniformed elements
      index = $.inArray(this, $.uniform.elements);

      if (index >= 0) {
        $.uniform.elements.splice(index, 1);
      }

      $el.removeData("uniformed");
    });
  };

  $.uniform.update = $.fn.uniform.update = function (elem) {
    if (elem === undef) {
      elem = $.uniform.elements;
    }

    $(elem).each(function () {
      var $el = $(this), elementData;
      elementData = $el.data("uniformed");

      // Skip elements that are not uniformed
      if (!elementData) {
        return;
      }

      elementData.update($el, elementData.options);
    });
  };
}(jQuery));


function hideFormText() {

  var _inputs = document.getElementsByTagName('input');

  var _txt = document.getElementsByTagName('textarea');

  var _value = [];



  if (_inputs) {

    for(var i=0; i<_inputs.length; i++) {

      if (_inputs[i].type == 'text' || _inputs[i].type == 'password') {



        _inputs[i].index = i;

        _value[i] = _inputs[i].value;



        _inputs[i].onfocus = function(){

          if (this.value == _value[this.index])

            this.value = '';

          this.parentNode.parentNode.className+=" focus";

        }

        _inputs[i].onblur = function(){

          if (this.value == '')

            this.value = _value[this.index];

          this.parentNode.parentNode.className=this.parentNode.parentNode.className.replace(" focus", "");

        }

      }

    }

  }

  if (_txt) {

    for(var i=0; i<_txt.length; i++) {

      _txt[i].index = i;

      _value['txt'+i] = _txt[i].value;



      _txt[i].onfocus = function(){

        if (this.value == _value['txt'+this.index])

          this.value = '';

      }

      _txt[i].onblur = function(){

        if (this.value == '')

          this.value = _value['txt'+this.index];

      }

    }

  }

}

if (window.addEventListener)

  window.addEventListener("load", hideFormText, false);

else if (window.attachEvent)

  window.attachEvent("onload", hideFormText);