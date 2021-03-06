package io.purplejs.http.internal.response;

import com.google.common.net.HttpHeaders;
import com.google.common.net.MediaType;

import io.purplejs.core.value.ScriptValue;
import io.purplejs.http.Cookie;
import io.purplejs.http.Response;
import io.purplejs.http.ResponseBuilder;
import io.purplejs.http.Status;
import io.purplejs.http.internal.websocket.WebSocketConfigFactory;

public final class ScriptToResponse
{
    public Response toResponse( final ScriptValue value )
    {
        final ResponseBuilder builder = ResponseBuilder.newBuilder();
        if ( value == null )
        {
            builder.contentType( MediaType.PLAIN_TEXT_UTF_8 );
            return builder.build();
        }

        builder.value( value );
        populateStatus( builder, value.getMember( "status" ) );

        final ScriptValue body = value.getMember( "body" );
        final MediaType type = findContentType( value.getMember( "contentType" ), body );
        populateContentType( builder, type );

        populateBody( builder, body );
        populateHeaders( builder, value.getMember( "headers" ) );
        populateCookies( builder, value.getMember( "cookies" ) );
        setRedirect( builder, value.getMember( "redirect" ) );
        setWebSocket( builder, value.getMember( "webSocket" ) );

        return builder.build();
    }

    private MediaType findContentType( final ScriptValue value, final ScriptValue body )
    {
        final String type = ( value != null ) ? value.getValue( String.class ) : null;
        if ( type != null )
        {
            return MediaType.parse( type );
        }

        return new BodySerializer().findType( body );
    }

    private void populateStatus( final ResponseBuilder builder, final ScriptValue value )
    {
        final Integer status = ( value != null ) ? value.getValue( Integer.class ) : null;
        builder.status( status != null ? Status.from( status ) : Status.OK );
    }

    private void populateContentType( final ResponseBuilder builder, final MediaType type )
    {
        builder.contentType( type != null ? type : MediaType.PLAIN_TEXT_UTF_8 );
    }

    private void populateBody( final ResponseBuilder builder, final ScriptValue value )
    {
        builder.body( new BodySerializer().toBody( value ) );
    }

    private void populateHeaders( final ResponseBuilder builder, final ScriptValue value )
    {
        if ( value == null )
        {
            return;
        }

        if ( !value.isObject() )
        {
            return;
        }

        for ( final String key : value.getKeys() )
        {
            builder.header( key, value.getMember( key ).getValue( String.class ) );
        }
    }

    private void setRedirect( final ResponseBuilder builder, final ScriptValue value )
    {
        final String redirect = ( value != null ) ? value.getValue( String.class ) : null;
        if ( redirect == null )
        {
            return;
        }

        builder.status( Status.SEE_OTHER );
        builder.header( HttpHeaders.LOCATION, redirect );
    }

    private void populateCookies( final ResponseBuilder builder, final ScriptValue value )
    {
        if ( value == null )
        {
            return;
        }

        for ( final String key : value.getKeys() )
        {
            addCookie( builder, value.getMember( key ), key );
        }
    }

    static <T> T getValue( final ScriptValue value, final Class<T> type, final T defValue )
    {
        if ( value == null )
        {
            return null;
        }

        final T result = value.getValue( type );
        return result != null ? result : defValue;
    }

    private <T> T getMemberValue( final ScriptValue value, final String name, final Class<T> type, final T defValue )
    {
        final T result = getValue( value.getMember( name ), type, defValue );
        return result != null ? result : defValue;
    }

    private Cookie newCookie( final ScriptValue value, final String key )
    {
        final Cookie cookie = new Cookie( key );

        if ( value.isObject() )
        {
            cookie.setValue( getMemberValue( value, "value", String.class, "" ) );
            cookie.setPath( getMemberValue( value, "path", String.class, null ) );
            cookie.setDomain( getMemberValue( value, "domain", String.class, null ) );
            cookie.setComment( getMemberValue( value, "comment", String.class, null ) );
            cookie.setMaxAge( getMemberValue( value, "maxAge", Integer.class, -1 ) );
            cookie.setSecure( getMemberValue( value, "secure", Boolean.class, false ) );
            cookie.setHttpOnly( getMemberValue( value, "httpOnly", Boolean.class, false ) );
        }
        else
        {
            cookie.setValue( getValue( value, String.class, "" ) );
        }

        return cookie;
    }

    private void addCookie( final ResponseBuilder builder, final ScriptValue value, final String key )
    {
        if ( value != null )
        {
            builder.cookie( newCookie( value, key ) );
        }
    }

    private void setWebSocket( final ResponseBuilder builder, final ScriptValue value )
    {
        builder.webSocket( new WebSocketConfigFactory().create( value ) );
    }
}
