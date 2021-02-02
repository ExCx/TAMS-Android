package com.frekanstan.asset_management.app.locations;

import com.frekanstan.asset_management.data.locations.ELocationType;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;

import java.lang.reflect.Type;

public class LocationTypeDeserializer implements JsonDeserializer<ELocationType>
{
    @Override
    public ELocationType deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext ctx)
            throws JsonParseException
    {
        int typeInt = json.getAsInt();
        return ELocationType.findByAbbr(typeInt);
    }
}