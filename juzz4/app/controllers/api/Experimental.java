package controllers.api;

import controllers.interceptors.APIInterceptor;
import play.mvc.With;

/**
 * @author Aye Maung
 * @since v4.4
 */
@With(APIInterceptor.class)
public class Experimental extends APIController
{

}
