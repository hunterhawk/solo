/*
 * Copyright (c) 2009, 2010, 2011, 2012, 2013, B3log Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.b3log.solo.plugin.broadcast;


import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.b3log.latke.Keys;
import org.b3log.latke.Latkes;
import org.b3log.latke.servlet.HTTPRequestContext;
import org.b3log.latke.servlet.HTTPRequestMethod;
import org.b3log.latke.servlet.annotation.RequestProcessing;
import org.b3log.latke.servlet.annotation.RequestProcessor;
import org.b3log.latke.servlet.renderer.JSONRenderer;
import org.b3log.latke.urlfetch.HTTPRequest;
import org.b3log.latke.urlfetch.URLFetchService;
import org.b3log.latke.urlfetch.URLFetchServiceFactory;
import org.b3log.latke.util.Requests;
import org.b3log.latke.util.Strings;
import org.b3log.solo.SoloServletListener;
import org.b3log.solo.model.Option;
import org.b3log.solo.model.Preference;
import org.b3log.solo.service.OptionMgmtService;
import org.b3log.solo.service.OptionQueryService;
import org.b3log.solo.service.PreferenceQueryService;
import org.b3log.solo.util.QueryResults;
import org.b3log.solo.util.Users;
import org.json.JSONObject;


/**
 * Broadcast chance processor.
 * 
 * @author <a href="mailto:DL88250@gmail.com">Liang Ding</a>
 * @version 1.0.0.7, May 16, 2012
 * @since 0.6.0
 */
@RequestProcessor
public final class ChanceProcessor {

    /**
     * Logger.
     */
    private static final Logger LOGGER = Logger.getLogger(ChanceProcessor.class.getName());

    /**
     * Option management service.
     */
    private OptionMgmtService optionMgmtService = OptionMgmtService.getInstance();

    /**
     * Option query service.
     */
    private OptionQueryService optionQueryService = OptionQueryService.getInstance();

    /**
     * URL fetch service.
     */
    private final URLFetchService urlFetchService = URLFetchServiceFactory.getURLFetchService();

    /**
     * User utilities.
     */
    private Users userUtils = Users.getInstance();

    /**
     * URL of adding article to Rhythm.
     */
    private static final URL ADD_BROADCAST_URL;

    static {
        try {
            ADD_BROADCAST_URL = new URL(SoloServletListener.B3LOG_RHYTHM_ADDRESS + "/broadcast");
        } catch (final MalformedURLException e) {
            LOGGER.log(Level.SEVERE, "Creates remote service address[rhythm add broadcast] error!");
            throw new IllegalStateException(e);
        }
    }

    /**
     * Adds a broadcast chance to option repository.
     * 
     * <p>
     * Renders the response with a json object, for example,
     * <pre>
     * {
     *     "sc": boolean,
     *     "msg": "" // optional
     * }
     * </pre>
     * </p>
     *
     * @param context the specified http request context
     * @param request the specified http servlet request
     * @param response the specified http servlet response
     * @throws Exception 
     */
    @RequestProcessing(value = "/console/plugins/b3log-broadcast/chance", method = HTTPRequestMethod.POST)
    public void addChance(final HTTPRequestContext context, final HttpServletRequest request, final HttpServletResponse response)
        throws Exception {
        final JSONRenderer renderer = new JSONRenderer();

        context.setRenderer(renderer);

        final JSONObject ret = new JSONObject();

        renderer.setJSONObject(ret);

        try {
            // TODO: verify b3 key

            final String time = request.getParameter("broadcastChanceExpirationTime");

            if (Strings.isEmptyOrNull(time)) {
                ret.put(Keys.STATUS_CODE, false);

                return;
            }

            final long expirationTime = Long.valueOf(time);

            final JSONObject option = new JSONObject();

            option.put(Keys.OBJECT_ID, Option.ID_C_BROADCAST_CHANCE_EXPIRATION_TIME);
            option.put(Option.OPTION_VALUE, expirationTime);
            option.put(Option.OPTION_CATEGORY, Option.CATEGORY_C_BROADCAST);

            optionMgmtService.addOption(option);

            ret.put(Keys.STATUS_CODE, true);
        } catch (final Exception e) {
            final String msg = "Broadcast plugin exception";

            LOGGER.log(Level.SEVERE, msg, e);

            final JSONObject jsonObject = QueryResults.defaultResult();

            renderer.setJSONObject(jsonObject);
            jsonObject.put(Keys.MSG, msg);
        }
    }

    /**
     * Dose the client has a broadcast chance.
     * 
     * <p>
     * If the request come from a user not administrator, consider it is no broadcast chance.
     * </p>
     * 
     * <p>
     * Renders the response with a json object, for example,
     * <pre>
     * {
     *     "sc": boolean, // if has a chance, the value will be true
     *     "broadCastChanceExpirationTime": long, // if has a chance, the value will larger then 0L
     *     "msg": "" // optional
     * }
     * </pre>
     * </p>
     *
     * @param context the specified http request context
     * @param request the specified http servlet request
     * @param response the specified http servlet response
     * @throws Exception 
     */
    @RequestProcessing(value = "/console/plugins/b3log-broadcast/chance", method = HTTPRequestMethod.GET)
    public void hasChance(final HTTPRequestContext context, final HttpServletRequest request, final HttpServletResponse response)
        throws Exception {
        if (!userUtils.isLoggedIn(request, response)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);

            return;
        }

        final JSONRenderer renderer = new JSONRenderer();

        context.setRenderer(renderer);

        final JSONObject ret = new JSONObject();

        renderer.setJSONObject(ret);

        final Users users = Users.getInstance();

        if (!users.isAdminLoggedIn(request)) {
            ret.put(Option.ID_C_BROADCAST_CHANCE_EXPIRATION_TIME, 0L);
            ret.put(Keys.STATUS_CODE, false);

            return;
        }

        try {
            final JSONObject option = optionQueryService.getOptionById(Option.ID_C_BROADCAST_CHANCE_EXPIRATION_TIME);

            if (null == option) {
                ret.put(Option.ID_C_BROADCAST_CHANCE_EXPIRATION_TIME, 0L);
                ret.put(Keys.STATUS_CODE, false);

                return;
            }

            ret.put(Option.ID_C_BROADCAST_CHANCE_EXPIRATION_TIME, option.getLong(Option.OPTION_VALUE));
            ret.put(Keys.STATUS_CODE, true);
        } catch (final Exception e) {
            final String msg = "Broadcast plugin exception";

            LOGGER.log(Level.SEVERE, msg, e);

            final JSONObject jsonObject = QueryResults.defaultResult();

            renderer.setJSONObject(jsonObject);
            jsonObject.put(Keys.MSG, msg);
        }
    }

    /**
     * Submits a broadcast.
     * 
     * <p>
     * Renders the response with a json object, for example,
     * <pre>
     * {
     *     "sc": boolean,
     *     "msg": "" // optional
     * }
     * </pre>
     * </p>
     *
     * @param context the specified http request context
     * @param request the specified http servlet request, for example,
     * <pre>
     * {
     *     "broadcast": {
     *         "title": "",
     *         "content": "",
     *         "link": ""
     *     }
     * }
     * </pre>
     * @param response the specified http servlet response
     * @throws Exception 
     */
    @RequestProcessing(value = "/console/plugins/b3log-broadcast", method = HTTPRequestMethod.POST)
    public void submitBroadcast(final HTTPRequestContext context, final HttpServletRequest request, final HttpServletResponse response)
        throws Exception {
        if (!userUtils.isAdminLoggedIn(request)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);

            return;
        }

        final JSONRenderer renderer = new JSONRenderer();

        context.setRenderer(renderer);

        final JSONObject ret = new JSONObject();

        renderer.setJSONObject(ret);

        try {
            final JSONObject requestJSONObject = Requests.parseRequestJSONObject(request, response);

            final JSONObject broadcast = requestJSONObject.getJSONObject("broadcast");
            final JSONObject preference = PreferenceQueryService.getInstance().getPreference();
            final String b3logKey = preference.getString(Preference.KEY_OF_SOLO);
            final String email = preference.getString(Preference.ADMIN_EMAIL);
            final String clientName = "B3log Solo";
            final String clientVersion = SoloServletListener.VERSION;
            final String clientTitle = preference.getString(Preference.BLOG_TITLE);
            final String clientRuntimeEnv = Latkes.getRuntimeEnv().name();
            final String blogHost = Latkes.getServePath();

            final JSONObject broadcastRequest = new JSONObject();

            broadcastRequest.put("b3logKey", b3logKey);
            broadcastRequest.put("email", email);
            broadcastRequest.put("broadcast", broadcast);
            broadcastRequest.put("clientRuntimeEnv", clientRuntimeEnv);
            broadcastRequest.put("clientTitle", clientTitle);
            broadcastRequest.put("clientVersion", clientVersion);
            broadcastRequest.put("clientName", clientName);
            broadcastRequest.put("clientHost", blogHost);
            
            final HTTPRequest httpRequest = new HTTPRequest();

            httpRequest.setURL(ADD_BROADCAST_URL);
            httpRequest.setRequestMethod(HTTPRequestMethod.POST);
            httpRequest.setPayload(broadcastRequest.toString().getBytes("UTF-8"));

            urlFetchService.fetchAsync(httpRequest);

            ret.put(Keys.STATUS_CODE, true);
        } catch (final Exception e) {
            LOGGER.log(Level.SEVERE, "Submits broadcast failed", e);

            final JSONObject jsonObject = QueryResults.defaultResult();

            renderer.setJSONObject(jsonObject);
            jsonObject.put(Keys.MSG, e.getMessage());
        }
    }
}
