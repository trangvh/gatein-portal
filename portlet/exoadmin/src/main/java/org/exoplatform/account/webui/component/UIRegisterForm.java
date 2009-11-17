/**
 * Copyright (C) 2009 eXo Platform SAS.
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

package org.exoplatform.account.webui.component;

import java.util.ArrayList;
import java.util.List;

import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.organization.UserHandler;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIApplication;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.event.Event.Phase;
import org.exoplatform.webui.form.UIForm;
import org.exoplatform.webui.form.UIFormInputWithActions;
import org.exoplatform.webui.form.UIFormInputWithActions.ActionData;

/**
 * 
 * @author <a href="mailto:hoang281283@gmail.com">Minh Hoang TO</a>
 * @version $Id$
 *
 */

@ComponentConfig(
		lifecycle = UIFormLifecycle.class,
		template = "system:/groovy/webui/form/UIForm.gtmpl",
		events = {
				@EventConfig( listeners = UIRegisterForm.SubscribeActionListener.class),
            @EventConfig( listeners = UIRegisterForm.ResetActionListener.class, phase = Phase.DECODE),
            @EventConfig( name = UIRegisterForm.CheckUsernameAvailability.LISTENER_NAME, 
                          listeners = UIRegisterForm.CheckUsernameAvailability.class, 
                          phase = Phase.DECODE)
		}
)
public class UIRegisterForm extends UIForm {

	private final static String[] ACTIONS = {"Subscribe", "Reset"};
   
	public UIRegisterForm() throws Exception{
	   UIFormInputWithActions registerInput = new UIRegisterInputSet("RegisterInputSet");
      //Set actions on registerInput 's User Name field
      List<ActionData> fieldActions = new ArrayList<ActionData>();
      ActionData checkAvailable = new ActionData();
      checkAvailable.setActionListener(CheckUsernameAvailability.LISTENER_NAME);
      checkAvailable.setActionName(CheckUsernameAvailability.LISTENER_NAME);
      checkAvailable.setActionType(ActionData.TYPE_ICON);
      checkAvailable.setCssIconClass("SearchIcon");
      fieldActions.add(checkAvailable);
      registerInput.setActionField(UIRegisterInputSet.USER_NAME, fieldActions);
      
      addUIFormInput(registerInput);
      setActions(ACTIONS);
	}
	
	static public class SubscribeActionListener extends EventListener<UIRegisterForm>{
		@Override
		public void execute(Event<UIRegisterForm> event) throws Exception {
		   UIRegisterForm registerForm = event.getSource();
         OrganizationService orgService = registerForm.getApplicationComponent(OrganizationService.class);
         UserHandler userHandler = orgService.getUserHandler();
         WebuiRequestContext context = WebuiRequestContext.getCurrentInstance();
         UIRegisterInputSet registerInput = registerForm.getChild(UIRegisterInputSet.class);
         
         if(registerInput.save(userHandler, context)){
            //TODO: Send email and add Account Activating feature
         }
		}
	}
   
   static public class CheckUsernameAvailability extends EventListener<UIRegisterForm>{
      
      final static String LISTENER_NAME =  "CheckUsernameAvailability";
      
      @Override
      public void execute(Event<UIRegisterForm> event) throws Exception
      {
         UIRegisterForm registerForm = event.getSource();
         OrganizationService orgService = registerForm.getApplicationComponent(OrganizationService.class);
         UIRegisterInputSet registerInput = registerForm.getChild(UIRegisterInputSet.class);
         String typedUsername = registerInput.getUIStringInput(UIRegisterInputSet.USER_NAME).getValue();
         
         if(usernameIsUsed(typedUsername, orgService)){
            WebuiRequestContext context = WebuiRequestContext.getCurrentInstance();
            UIApplication uiApp = context.getUIApplication();
            
            uiApp.addMessage(new ApplicationMessage("UIAccountInputSet.msg.user-exist", new String[]{ typedUsername}));
         }
      }
      
      private boolean usernameIsUsed(String username, OrganizationService orgService){
         UserHandler userHandler = orgService.getUserHandler();
         try{
            if( userHandler.findUserByName(username) != null){
               return true;
            }
         }catch(Exception ex){
            ex.printStackTrace();
         }
         return false;
      }
   }
   
   static public class ResetActionListener extends EventListener<UIRegisterForm>{
      @Override
      public void execute(Event<UIRegisterForm> event) throws Exception
      {
         // TODO Auto-generated method stub
         UIRegisterForm registerForm = event.getSource();
         registerForm.reset();
      }
   }
}
