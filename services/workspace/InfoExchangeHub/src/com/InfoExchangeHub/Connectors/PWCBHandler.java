package com.InfoExchangeHub.Connectors;

import org.apache.ws.security.WSPasswordCallback;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;

import java.io.IOException;

public class PWCBHandler implements CallbackHandler
{
    public void handle(Callback[] callbacks) throws IOException,
            UnsupportedCallbackException
    {
        for (int i = 0; i < callbacks.length; i++)
        {
            // To use the private key to sign messages, we need to provide 
        	// the private key password 
            WSPasswordCallback pwcb = (WSPasswordCallback)callbacks[i];
               
            if (pwcb.getIdentifier().equals("client") )
            {
            	pwcb.setPassword("IEXhub");
                return;
            }
        }
    }
}