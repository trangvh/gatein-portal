/*
 * Copyright (C) 2011 eXo Platform SAS.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.exoplatform.portal.controller.resource;

import org.exoplatform.commons.cache.future.FutureMap;
import org.exoplatform.commons.utils.I18N;
import org.exoplatform.commons.utils.Safe;
import org.exoplatform.web.ControllerContext;
import org.exoplatform.web.WebRequestHandler;
import org.exoplatform.web.controller.QualifiedName;
import org.gatein.common.io.IOTools;

import org.gatein.common.logging.Logger;
import org.gatein.common.logging.LoggerFactory;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Locale;
import java.util.Properties;

/**
 * @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a>
 */
public class ResourceRequestHandler extends WebRequestHandler
{

   /** . */
   private static String PATH = "META-INF/maven/org.exoplatform.portal/exo.portal.component.web.resources/pom.properties";

   /** . */
   private static final Logger log = LoggerFactory.getLogger(ResourceRequestHandler.class);

   /** . */
   public static final String VERSION;

   static
   {
      // Detecting version from maven properties
      // empty value is ok
      String version = "";
      URL url = ResourceRequestHandler.class.getClassLoader().getResource(PATH);
      if (url != null)
      {
         log.debug("Loading resource serving version from " + url);
         InputStream in = null;
         try
         {
            in = url.openStream();
            Properties props = new Properties();
            props.load(in);
            version = props.getProperty("version");
         }
         catch (IOException e)
         {
            log.error("Could not read properties from " + url, e);
         }
         finally
         {
            IOTools.safeClose(in);
         }
      }

      //
      log.info("Use version \"" + version + "\" for resource serving");
      VERSION = version;
   }
   
   /** . */
   public static final QualifiedName VERSION_QN  = QualifiedName.create("gtn", "version");

   /** . */
   public static final QualifiedName RESOURCE_QN = QualifiedName.create("gtn", "resource");

   /** . */
   public static final QualifiedName SCOPE_QN = QualifiedName.create("gtn", "scope");

   /** . */
   public static final QualifiedName MODULE_QN = QualifiedName.create("gtn", "module");

   /** . */
   public static final QualifiedName COMPRESS_QN = QualifiedName.create("gtn", "compress");

   /** . */
   public static final QualifiedName LANG_QN = QualifiedName.create("gtn", "lang");

   /** . */
   private final FutureMap<ScriptKey, ScriptResult, ControllerContext> cache;

   public ResourceRequestHandler()
   {
      this.cache = new FutureMap<ScriptKey, ScriptResult, ControllerContext>(new ScriptLoader());
   }

   @Override
   public String getHandlerName()
   {
      return "script";
   }

   @Override
   public boolean execute(ControllerContext context) throws Exception
   {
      String resourceParam = context.getParameter(RESOURCE_QN);
      String scopeParam = context.getParameter(SCOPE_QN);

      //
      if (scopeParam != null && resourceParam != null)
      {
         String compressParam = context.getParameter(COMPRESS_QN);
         String lang = context.getParameter(LANG_QN);
         String moduleParam = context.getParameter(MODULE_QN);

         //
         Locale locale = null;
         if (lang != null && lang.length() > 0)
         {
            locale = I18N.parseTagIdentifier(lang);
         }

         //
         ResourceScope scope;
         try
         {
            scope = ResourceScope.valueOf(ResourceScope.class, scopeParam);
         }
         catch (IllegalArgumentException e)
         {
            HttpServletResponse response = context.getResponse();
            String msg = "Unrecognized scope " + scopeParam;
            log.error(msg);
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, msg);
            return true;
         }
         
         //
         ResourceId resource = new ResourceId(scope, resourceParam);
         
         ScriptKey key = new ScriptKey(
            resource,
            moduleParam,
            "min".equals(compressParam),
            locale
         );

         //
         ScriptResult result = cache.get(context, key);
         HttpServletResponse response = context.getResponse();

         //
         if (result instanceof ScriptResult.Resolved)
         {
            ScriptResult.Resolved resolved = (ScriptResult.Resolved)result;

            // Content type + charset
            response.setContentType("text/javascript");
            response.setCharacterEncoding("UTF-8");

            // One hour caching
            // make this configurable later
            response.setHeader("Cache-Control", "max-age:3600");
            response.setDateHeader("Expires", System.currentTimeMillis() + 3600 * 1000);
            
            // Set content length
            response.setContentLength(resolved.bytes.length);
            
            // Send bytes
            ServletOutputStream out = response.getOutputStream();
            try
            {
               out.write(resolved.bytes);
            }
            finally
            {
               Safe.close(out);
            }
         }
         else if (result instanceof ScriptResult.Error)
         {
            ScriptResult.Error error = (ScriptResult.Error)result;
            log.error("Could not render script " + key + "\n:" + error.message);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
         }
         else
         {
            String msg = "Resource " + key + " cannot be found";
            log.error(msg);
            response.sendError(HttpServletResponse.SC_NOT_FOUND, msg);
         }
      }
      else
      {
         HttpServletResponse response = context.getResponse();
         String msg = "Missing scope or resource param";
         log.error(msg);
         response.sendError(HttpServletResponse.SC_BAD_REQUEST, msg);
      }
      
      //
      return true;
   }
   
   @Override
   protected boolean getRequiresLifeCycle()
   {
      return false;
   }
}
