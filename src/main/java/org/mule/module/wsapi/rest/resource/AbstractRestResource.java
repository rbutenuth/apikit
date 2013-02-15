
package org.mule.module.wsapi.rest.resource;

import static org.mule.module.wsapi.rest.action.ActionType.EXISTS;
import static org.mule.module.wsapi.rest.action.ActionType.RETRIEVE;

import org.mule.api.MuleEvent;
import org.mule.module.wsapi.rest.RestException;
import org.mule.module.wsapi.rest.RestRequest;
import org.mule.module.wsapi.rest.UnexceptedErrorException;
import org.mule.module.wsapi.rest.action.ActionNotSupportedException;
import org.mule.module.wsapi.rest.action.ActionType;
import org.mule.module.wsapi.rest.action.RestAction;
import org.mule.transformer.types.MimeTypes;
import org.mule.transport.NullPayload;
import org.mule.transport.http.HttpConnector;
import org.mule.transport.http.HttpConstants;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractRestResource implements RestResource
{
    protected final Logger logger = LoggerFactory.getLogger(getClass());

    protected String name;
    protected List<RestAction> actions;

    public AbstractRestResource(String name)
    {
        this.name = name;
    }

    @Override
    public String getName()
    {
        return name;
    }

    public List<RestAction> getActions()
    {
        return actions;
    }

    public void setActions(List<RestAction> actions)
    {
        this.actions = actions;
    }

    private RestAction getAction(ActionType actionType)
    {
        RestAction action = null;
        for (RestAction a : getActions())
        {
            if (a.getType() == actionType)
            {
                action = a;
                break;
            }
        }
        return action;
    }

    protected RestAction getAction(ActionType actionType, MuleEvent muleEvent)
        throws ActionNotSupportedException
    {
        if (!isActionSupported(actionType))
        {
            throw new ActionNotSupportedException(this, actionType);
        }
        RestAction action = getAction(actionType);
        if (action == null && EXISTS == actionType)
        {
            action = getAction(RETRIEVE);
        }
        if (action == null)
        {
            throw new ActionNotSupportedException(this, actionType);
        }
        return action;
    }

    @Override
    public boolean isActionSupported(ActionType actionType)
    {
        return getSupportedActions().contains(actionType);
    }

    @Override
    public MuleEvent handle(RestRequest restCall) throws RestException
    {
        return processResource(restCall);
    }

    protected MuleEvent processResource(RestRequest restRequest) throws RestException
    {
        try
        {
            if (restRequest.getProtocolAdaptor().getActionType().equals(ActionType.RETRIEVE)
                && restRequest.getProtocolAdaptor()
                    .getAcceptedContentTypes()
                    .contains("application/swagger+json"))
            {
                new SwaggerResourceAction(this, restRequest).handle(restRequest);
            }
            else
            {

                this.getAction(restRequest.getProtocolAdaptor().getActionType(), restRequest.getMuleEvent())
                    .handle(restRequest);
                if (ActionType.EXISTS == restRequest.getProtocolAdaptor().getActionType())
                {
                    restRequest.getMuleEvent().getMessage().setPayload(NullPayload.getInstance());
                }
            }
        }
        catch (RestException rana)
        {
            restRequest.getProtocolAdaptor().handleException(rana, restRequest.getMuleEvent());
        }
        return restRequest.getMuleEvent();
    }

    class SwaggerResourceAction implements RestAction
    {

        RestResource resource;
        RestRequest request;

        public SwaggerResourceAction(RestResource resource, RestRequest request)
        {
            this.resource = resource;
            this.request = request;
        }

        @Override
        public MuleEvent handle(RestRequest restRequest) throws RestException
        {
            try
            {
                ObjectMapper mapper = new ObjectMapper();
                String json = mapper.writeValueAsString(restRequest.getInterface());
                json = json.replace("{baseSwaggerUri}", restRequest.getMuleEvent()
                    .getMessageSourceURI()
                    .toString());

                restRequest.getMuleEvent().getMessage().setPayload(json);
                restRequest.getMuleEvent()
                    .getMessage()
                    .setOutboundProperty(HttpConnector.HTTP_STATUS_PROPERTY,
                        String.valueOf(HttpConstants.SC_OK));
                restRequest.getMuleEvent()
                    .getMessage()
                    .setOutboundProperty(HttpConstants.HEADER_CONTENT_TYPE, MimeTypes.JSON);
                restRequest.getMuleEvent()
                    .getMessage()
                    .setOutboundProperty(HttpConstants.HEADER_CONTENT_LENGTH, json.length());
                return restRequest.getMuleEvent();

            }
            catch (JsonProcessingException e)
            {
                throw new UnexceptedErrorException(e);
            }
        }

        @Override
        public ActionType getType()
        {
            return ActionType.RETRIEVE;
        }
    }

}
